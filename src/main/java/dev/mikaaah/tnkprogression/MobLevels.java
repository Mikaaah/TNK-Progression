package dev.mikaaah.tnkprogression;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.util.FakePlayer;
import java.util.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Choose once at first world join; existing saved levels never follow passing players. */
public final class MobLevels {
 private MobLevels() {}
 private record NearbyPlayer(ServerPlayer player,int level){}
 private record Snapshot(long tick,int playerCount,NearbyPlayerIndex<NearbyPlayer> index){}
 private static final Map<ServerLevel,Snapshot> SNAPSHOTS=new IdentityHashMap<>();
 private static long snapshotBuilds;
 public static boolean eligible(ServerPlayer p){
  return !(p instanceof FakePlayer) && p.isAlive() && !p.isSpectator() && !p.isCreative();
 }
 public static ServerPlayer nearest(ServerLevel world,Mob mob){
  var nearest=lookup(world,mob);
  return nearest==null?null:nearest.player;
 }
 private static Snapshot snapshot(ServerLevel world){
  var state=SNAPSHOTS.get(world);long tick=world.getServer().getTickCount();
  if(state!=null && state.tick==tick && state.playerCount==world.players().size())return state;
  var players=new ArrayList<NearbyPlayerIndex.Point<NearbyPlayer>>();
  for(var p:world.players()){
   if(p.level()!=world || !eligible(p))continue;
   players.add(new NearbyPlayerIndex.Point<>(new NearbyPlayer(p,ProgressionAPI.level(p)),p.getX(),p.getY(),p.getZ()));
  }
  state=new Snapshot(tick,world.players().size(),new NearbyPlayerIndex<>(players));
  SNAPSHOTS.put(world,state);snapshotBuilds++;return state;
 }
 private static NearbyPlayer lookup(ServerLevel world,Mob mob){
  var result=snapshot(world).index.nearest(mob.getX(),mob.getY(),mob.getZ());
  if(result!=null && (result.player.level()!=world || !eligible(result.player))){
   invalidate(world);result=snapshot(world).index.nearest(mob.getX(),mob.getY(),mob.getZ());
  }
  return result;
 }
 public static int assign(ServerLevel world,Mob mob){
  var player=lookup(world,mob);
  if(player==null)return 1;
  int variance=ProgressionConfig.current.mobLevelVariance;
  return MobLevelRules.withOffset(player.level,mob.getRandom().nextInt(2*variance+1)-variance);
 }
 public static void invalidate(ServerLevel world){SNAPSHOTS.remove(world);}
 static long snapshotBuilds(){return snapshotBuilds;}
 static int areaSearches(ServerLevel world){var state=SNAPSHOTS.get(world);return state==null?0:state.index.searches();}
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){SNAPSHOTS.clear();}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){SNAPSHOTS.clear();}
 @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){SNAPSHOTS.clear();}
 @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){SNAPSHOTS.clear();}
 @SubscribeEvent public static void teleport(EntityTeleportEvent event){if(event.getEntity() instanceof ServerPlayer)SNAPSHOTS.clear();}
 @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel world)invalidate(world);}
 @SubscribeEvent public static void stopped(ServerStoppedEvent event){SNAPSHOTS.clear();}
}
