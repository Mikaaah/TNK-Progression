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
   check(MobLevels.nearest(world,mob)==near,"nearest player wins over stronger player");
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
   ProgressionConfig.current.mobLevelVariance=0;check(MobLevels.assign(world,mob)==20,"zero variance gives exact nearest level");
   var existing=EntityType.ZOMBIE.create(world);existing.getPersistentData().putInt("tnk_level",73);
   ProgressionEvents.join(new EntityJoinLevelEvent(existing,world));check(ProgressionAPI.mobLevel(existing)==73,"older saved mobs keep their existing level");
   TnkProgression.LOG.info("TNK_MOB_LEVEL_SMOKE_OK");
  }finally{world.players().remove(near);world.players().remove(far);ProgressionConfig.current.mobLevelVariance=variance;}
 }
}
