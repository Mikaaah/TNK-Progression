package dev.mikaaah.tnkprogression;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Opt-in disposable-server checks of actual nearest-player selection and entity joins. */
public final class MobLevelSmoke {
 private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
 private static ServerPlayer player(MinecraftServer server,String name,int level,double x){
  var profile=new GameProfile(UUID.randomUUID(),name);
  var p=new ServerPlayer(server,server.overworld(),profile,ClientInformation.createDefault());
  p.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),p,CommonListenerCookie.createInitial(profile,false));
  p.setGameMode(GameType.SURVIVAL);p.setPos(x,64,0);
  ProgressionAPI.profile(p).putInt("level",level);
  return p;
 }
 public static void run(MinecraftServer server){
  var world=server.overworld();int variance=ProgressionConfig.current.mobLevelVariance;
  var near=player(server,"NearLevel20",20,100000);
  var far=player(server,"FarLevel80",80,100100);
  try{
   ProgressionConfig.current.mobLevelVariance=4;
   var mob=EntityType.ZOMBIE.create(world);check(mob!=null,"zombie created");mob.setPos(100001,64,0);
   check(MobLevels.assign(world,mob)==1,"no eligible player falls back to one even far from spawn");
   world.players().add(near);world.players().add(far);
   MobLevels.invalidate(world);long snapshots=MobLevels.snapshotBuilds();
   check(MobLevels.nearest(world,mob)==near,"nearest player wins over stronger player");
   for(int i=0;i<31;i++){
    var burst=EntityType.ZOMBIE.create(world);burst.setPos(100000+i%16,64,i%16);
    int level=MobLevels.assign(world,burst);check(level>=16 && level<=24,"burst uses cached nearest player's level");
   }
   check(MobLevels.snapshotBuilds()==snapshots+1 && MobLevels.areaSearches(world)==1,"31-mob burst builds one player snapshot and one area shortlist");
   ProgressionEvents.join(new EntityJoinLevelEvent(mob,world));
   int assigned=ProgressionAPI.mobLevel(mob);check(assigned>=16 && assigned<=24,"spawn follows nearest actual character level");
   check(Math.abs(mob.getMaxHealth()-20*(1+ProgressionConfig.current.healthPerLevel*(assigned-1)))<.001,"assigned level scales health");
   mob.setHealth(5);mob.setPos(100099,-60,0);
   check(MobLevels.nearest(world,mob)==far,"nearest changes with three dimensional position");
   ProgressionEvents.join(new EntityJoinLevelEvent(mob,world));
   check(ProgressionAPI.mobLevel(mob)==assigned && mob.getHealth()==5,"rejoin preserves level and damage despite a different nearest player");
   check(mob.getAttribute(Attributes.MAX_HEALTH).getModifiers().stream().filter(m->m.id().toString().equals("tnkprogression:health")).count()==1,"rejoin never stacks scaling");
   near.setGameMode(GameType.CREATIVE);mob.setPos(100001,64,0);
   check(MobLevels.nearest(world,mob)==far,"creative player ignored");
   near.setGameMode(GameType.SPECTATOR);check(MobLevels.nearest(world,mob)==far,"spectator ignored");
   near.setGameMode(GameType.SURVIVAL);near.setHealth(0);check(MobLevels.nearest(world,mob)==far,"dead player ignored");near.setHealth(20);
   var fake=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(world);
   check(!MobLevels.eligible(fake),"fake players ignored");
   MobLevels.invalidate(world);ProgressionConfig.current.mobLevelVariance=0;check(MobLevels.assign(world,mob)==20,"zero variance gives exact nearest level");
   var existing=EntityType.ZOMBIE.create(world);existing.getPersistentData().putInt("tnk_level",73);
   ProgressionEvents.join(new EntityJoinLevelEvent(existing,world));check(ProgressionAPI.mobLevel(existing)==73,"older saved mobs keep their existing level");
   // Exercise vanilla despawning with the real generated label, including a saved/reloaded entity.
   var despawning=EntityType.ZOMBIE.create(world);despawning.setPos(200000,64,0);
   ProgressionEvents.join(new EntityJoinLevelEvent(despawning,world));
   check(despawning.hasCustomName() && !despawning.isPersistenceRequired() && !despawning.requiresCustomPersistence(),"TNK label never makes a normal mob persistent");
   var saved=despawning.saveWithoutId(new net.minecraft.nbt.CompoundTag());var loaded=EntityType.ZOMBIE.create(world);loaded.load(saved);
   ProgressionEvents.join(new EntityJoinLevelEvent(loaded,world));loaded.checkDespawn();
   check(loaded.isRemoved(),"leveled and reloaded named mob still distance-despawns");
   var persistent=EntityType.ZOMBIE.create(world);persistent.setPos(200000,64,0);persistent.setPersistenceRequired();persistent.setCustomName(net.minecraft.network.chat.Component.literal("Player named pet"));
   ProgressionEvents.join(new EntityJoinLevelEvent(persistent,world));persistent.checkDespawn();
   check(persistent.isPersistenceRequired() && !persistent.isRemoved() && persistent.getCustomName().getString().equals("Player named pet"),"existing name and persistence remain intact");
   // Logout clears immediately, rather than waiting for another tick.
   MobLevels.logout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(near));
   world.players().remove(near);check(MobLevels.nearest(world,mob)==far,"logout cannot leave a sticky cached player");
   MobLevels.stopped(new net.neoforged.neoforge.event.server.ServerStoppedEvent(server));
   check(MobLevels.areaSearches(world)==0,"server stop releases cached worlds and players");
   TnkProgression.LOG.info("TNK_MOB_CACHE_DESPAWN_SMOKE_OK: 31 spawns, 1 player snapshot, 1 area shortlist; natural despawn and existing persistence verified");
   TnkProgression.LOG.info("TNK_MOB_LEVEL_SMOKE_OK");
  }finally{world.players().remove(near);world.players().remove(far);MobLevels.invalidate(world);ProgressionConfig.current.mobLevelVariance=variance;}
 }
}
