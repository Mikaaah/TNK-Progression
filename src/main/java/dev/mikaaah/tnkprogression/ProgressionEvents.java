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
  if(!(e.getLevel() instanceof ServerLevel world) || !(e.getEntity() instanceof Mob mob) || !ProgressionConfig.current.scaleMobs)return;
  var c=ProgressionConfig.current;CompoundTag n=mob.getPersistentData();boolean fresh=!n.contains("tnk_level");
  int previousLevel=ProgressionAPI.mobLevel(mob);
  boolean arena=AbyssIntegration.prepareJoin(mob);
  if(!arena && !(mob instanceof Enemy) && BossLevels.baseLevel(mob)==0)return;
  boolean migrateBoss=!arena && BossLevels.baseLevel(mob)>0 && !n.getBoolean("tnk_boss_level_v1");
  if(!arena && (fresh || migrateBoss))n.putInt("tnk_level",MobLevels.assign(world,mob));
  if(BossLevels.baseLevel(mob)>0)n.putBoolean("tnk_boss_level_v1",true);
  int level=ProgressionAPI.mobLevel(mob);
  scale(mob.getAttribute(Attributes.MAX_HEALTH),"health",arena && (!AbyssIntegration.healthBaseline(mob) || AbyssIntegration.skipHealth(mob))?0:c.healthPerLevel*(level-1));
  // Arena damage is scaled once at the damage event for melee AND fixed-damage attacks.
  // Scaling this attribute too would double melee and attribute-derived spells/projectiles.
  scale(mob.getAttribute(Attributes.ATTACK_DAMAGE),"damage",arena?0:c.damagePerLevel*(level-1));
  if(fresh)mob.setHealth(mob.getMaxHealth());
  else if(mob.getHealth()>mob.getMaxHealth())mob.setHealth(mob.getMaxHealth());
  // Remove only our exact old generated name. Never rename player-named or affixed mobs.
  var old=net.minecraft.network.chat.Component.literal("Lv. "+level+" ").append(mob.getType().getDescription());
  var previousName=net.minecraft.network.chat.Component.literal("Lv. "+previousLevel+" ").append(mob.getType().getDescription());
  if(old.equals(mob.getCustomName()) || previousName.equals(mob.getCustomName()))mob.setCustomName(null);
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
}
