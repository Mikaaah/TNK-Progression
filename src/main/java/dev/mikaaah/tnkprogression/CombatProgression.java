package dev.mikaaah.tnkprogression;

import java.util.*;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.living.*;

public final class CombatProgression {
    private static final List<Function<Entity, ServerPlayer>> OWNER_ADAPTERS = new ArrayList<>();
    /** Register on the server thread for summon mods without OwnableEntity/TraceableEntity. */
    public static void registerOwnerAdapter(Function<Entity, ServerPlayer> adapter) { OWNER_ADAPTERS.add(Objects.requireNonNull(adapter)); }
    private static final Class<?> MAGIC_SUMMON;
    private static final java.lang.reflect.Method GET_SUMMONER;
    static {
        Class<?> type=null;java.lang.reflect.Method method=null;
        try {type=Class.forName("io.redspace.ironsspellbooks.entity.mobs.IMagicSummon");method=type.getMethod("getSummoner");}
        catch(ClassNotFoundException ignored) {} catch(ReflectiveOperationException | LinkageError ex) { TnkProgression.LOG.warn("Iron spell summon integration unavailable",ex); }
        MAGIC_SUMMON=type;GET_SUMMONER=method;
    }
    private static Entity magicOwner(Entity source) {
        if(MAGIC_SUMMON==null || GET_SUMMONER==null || !MAGIC_SUMMON.isInstance(source))return null;
        try {return GET_SUMMONER.invoke(source) instanceof Entity owner?owner:null;}
        catch(ReflectiveOperationException ex){return null;}
    }
    public static ServerPlayer owner(Entity source) {
        Set<UUID> visited = new HashSet<>();
        for (int depth=0; source!=null && depth<8 && visited.add(source.getUUID()); depth++) {
            if (source instanceof ServerPlayer p) return p;
            for (var adapter: OWNER_ADAPTERS) { var p=adapter.apply(source); if(p!=null)return p; }
            Entity magic=magicOwner(source);
            if(magic!=null)source=magic;
            else if (source instanceof OwnableEntity owned) source=owned.getOwner();
            else if (source instanceof TraceableEntity traced) source=traced.getOwner();
            else return null;
        }
        return null;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void spawn(FinalizeSpawnEvent event) {
        event.getEntity().getPersistentData().putString("tnk_spawn_reason", event.getSpawnType().name());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void spawnCheck(MobSpawnEvent.PositionCheck event) {
        event.getEntity().getPersistentData().putString("tnk_spawn_reason",event.getSpawnType().name());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void split(MobSplitEvent event) {
        for(var child:event.getChildren()) {
            child.getPersistentData().putString("tnk_spawn_reason","SPLIT");
            child.getPersistentData().putInt("tnk_level",ProgressionAPI.mobLevel(event.getParent()));
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void conversion(LivingConversionEvent.Post event) {
        var from=event.getEntity().getPersistentData();var to=event.getOutcome().getPersistentData();
        for(String key:List.of("tnk_level","tnk_spawn_reason","tnk_combat_paid","tnk_contributions"))if(from.contains(key))to.put(key,from.get(key).copy());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void before(LivingDamageEvent.Pre event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof Enemy)
            event.getEntity().getPersistentData().putFloat("tnk_health_before",event.getEntity().getHealth());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void damage(LivingDamageEvent.Post event) {
        var target=event.getEntity();
        if (!(target.level() instanceof ServerLevel world) || !(target instanceof Enemy)) return;
        var p=owner(event.getSource().getEntity()); if(p==null)p=owner(event.getSource().getDirectEntity());
        if(p==null || !ActivityGuard.player(p))return;
        CompoundTag n=target.getPersistentData();
        double damage=Math.min(event.getNewDamage(),n.getFloat("tnk_health_before"));
        if(!Double.isFinite(damage) || damage<=0)return;
        long now=world.getServer().overworld().getGameTime();
        var ledger=n.getCompound("tnk_contributions");
        for(String key:Set.copyOf(ledger.getAllKeys())) {
            long time=ledger.getCompound(key).getLong("time");
            if(now<time || now-time>ProgressionConfig.current.contributionTicks)ledger.remove(key);
        }
        String key=p.getUUID().toString();
        if(ledger.size()>=64 && !ledger.contains(key))return;
        var row=ledger.getCompound(key);row.putDouble("damage",Math.min(1e9,row.getDouble("damage")+damage));row.putLong("time",now);
        ledger.put(key,row);n.put("tnk_contributions",ledger);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        var target=event.getEntity();
        if(!(target.level() instanceof ServerLevel world) || !(target instanceof Enemy))return;
        var n=target.getPersistentData();if(n.getBoolean("tnk_combat_paid"))return;
        n.putBoolean("tnk_combat_paid",true);
        var c=ProgressionConfig.current;
        if(c.excludedSpawns.contains(n.getString("tnk_spawn_reason"))
            || (MAGIC_SUMMON!=null && MAGIC_SUMMON.isInstance(target))
            || (target instanceof OwnableEntity owned && owned.getOwnerUUID()!=null)
            || owner(target)!=null)return;
        var ledger=n.getCompound("tnk_contributions");long now=world.getServer().overworld().getGameTime();
        Map<ServerPlayer,Double> eligible=new LinkedHashMap<>();double total=0;
        for(String id:ledger.getAllKeys()) {
            var row=ledger.getCompound(id);long time=row.getLong("time");double damage=row.getDouble("damage");
            if(now<time || now-time>c.contributionTicks || !Double.isFinite(damage) || damage<=0)continue;
            // All recent shares remain in the denominator, including offline/distant attackers.
            total+=damage;
            try {
                var p=world.getServer().getPlayerList().getPlayer(UUID.fromString(id));
                if(p!=null && ActivityGuard.player(p) && p.isAlive() && p.level()==world && p.distanceToSqr(target)<=c.contributionRange*c.contributionRange)eligible.put(p,damage);
            }catch(IllegalArgumentException ignored) {}
        }
        if(total<=0)return;
        double health=target.getAttribute(Attributes.MAX_HEALTH)==null?20:target.getAttributeValue(Attributes.MAX_HEALTH);
        double attack=target.getAttribute(Attributes.ATTACK_DAMAGE)==null?0:target.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double weight=ActivityRules.combatWeight(health,attack,ProgressionAPI.mobLevel(target));
        // A token hit followed by fall/lava/machine damage earns only that hit's share.
        total=Math.max(total,target.getMaxHealth());
        String farm=world.dimension().location()+"/"+BuiltInRegistries.ENTITY_TYPE.getKey(target.getType())+"/"+(target.getBlockX()>>6)+"/"+(target.getBlockZ()>>6);
        for(var entry:eligible.entrySet()) {
            double repeat=ActivityGuard.combatRepetition(entry.getKey(),farm);
            ProgressionAPI.activityFractional(entry.getKey(),"combat",ActivityRules.combatShare(weight,entry.getValue(),total,target.getMaxHealth())*repeat);
        }
        n.remove("tnk_contributions");
    }
}
