package dev.mikaaah.tnkprogression;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Choose once at first world join; existing saved levels never follow passing players. */
public final class MobLevels {
 private MobLevels() {}
 public static boolean eligible(ServerPlayer p){
  return !(p instanceof FakePlayer) && p.isAlive() && !p.isSpectator() && !p.isCreative();
 }
 public static ServerPlayer nearest(ServerLevel world,Mob mob){
  ServerPlayer nearest=null;double best=Double.POSITIVE_INFINITY;
  for(var p:world.players()){
   if(p.level()!=world || !eligible(p))continue;
   double distance=p.distanceToSqr(mob);
   if(distance<best){best=distance;nearest=p;}
  }
  return nearest;
 }
 public static int assign(ServerLevel world,Mob mob){
  var player=nearest(world,mob);
  if(player==null)return 1;
  int variance=ProgressionConfig.current.mobLevelVariance;
  return MobLevelRules.withOffset(ProgressionAPI.level(player),mob.getRandom().nextInt(2*variance+1)-variance);
 }
}
