package dev.mikaaah.tnkprogression;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
public final class ProgressionEvents {
 @SubscribeEvent public static void join(EntityJoinLevelEvent e){
  if(!(e.getLevel() instanceof ServerLevel world) || !(e.getEntity() instanceof Mob mob) || !(mob instanceof Enemy) || !ProgressionConfig.current.scaleMobs)return;
  var c=ProgressionConfig.current;CompoundTag n=mob.getPersistentData();boolean fresh=!n.contains("tnk_level");
  if(fresh)n.putInt("tnk_level",MobLevels.assign(world,mob));
  int level=ProgressionAPI.mobLevel(mob);
  scale(mob.getAttribute(Attributes.MAX_HEALTH),"health",c.healthPerLevel*(level-1));
  scale(mob.getAttribute(Attributes.ATTACK_DAMAGE),"damage",c.damagePerLevel*(level-1));
  if(fresh)mob.setHealth(mob.getMaxHealth());
  else if(mob.getHealth()>mob.getMaxHealth())mob.setHealth(mob.getMaxHealth());
  // Remove only our exact old generated name. Never rename player-named or affixed mobs.
  var old=net.minecraft.network.chat.Component.literal("Lv. "+level+" ").append(mob.getType().getDescription());
  if(old.equals(mob.getCustomName()))mob.setCustomName(null);
 }
 @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e){
  if(e.getEntity() instanceof ServerPlayer p && e.getTarget() instanceof Mob mob && mob.getPersistentData().contains("tnk_level"))
   net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,new MobLevelPayload(mob.getId(),mob.getUUID(),ProgressionAPI.mobLevel(mob)));
 }
 private static void scale(AttributeInstance a,String key,double value){
  if(a==null)return;
  var id=ResourceLocation.parse("tnkprogression:"+key);var previous=a.getModifier(id);
  if(previous!=null && previous.amount()==value && previous.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE)return;
  if(previous!=null)a.removeModifier(id);
  if(value!=0)a.addPermanentModifier(new AttributeModifier(id,value,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
 }

 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)SkillBridge.sync(p);}
 @SubscribeEvent public static void tick(PlayerTickEvent.Post e){
  if(!(e.getEntity() instanceof ServerPlayer p) || p.tickCount%20!=0 || !ActivityGuard.player(p))return;
  CompoundTag n=ProgressionAPI.profile(p);
  String chunk=p.level().dimension().location()+"/"+ChunkPos.asLong(p.chunkPosition().x,p.chunkPosition().z);
  CompoundTag seen=n.getCompound("explored");if(!seen.contains(chunk)){seen.putBoolean(chunk,true);n.put("explored",seen);if(ActivityGuard.ready(p,"explore",100))ProgressionAPI.activity(p,"exploration",1);ProgressionData.get(p.server).setDirty();}
 }
}
