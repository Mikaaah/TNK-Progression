package dev.mikaaah.tnkprogression;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Bit-packed personal visits. Generating/loading chunks cannot award exploration XP. */
public final class ExplorationXp {
 private record Sample(Vec3 pos,String dimension){}
 private static final Map<ServerPlayer,Sample> LAST=new WeakHashMap<>();
 @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
  if(!(event.getEntity() instanceof ServerPlayer p)||p.tickCount%20!=0||!ActivityGuard.player(p))return;
  String dim=p.level().dimension().location().toString();var last=LAST.put(p,new Sample(p.position(),dim));
  boolean walked=last!=null&&last.dimension().equals(dim)&&last.pos().distanceToSqr(p.position())<=64*64;
  var n=ProgressionAPI.profile(p);var visits=n.getCompound("chunk_visits_v3");int x=p.chunkPosition().x,z=p.chunkPosition().z;
  String key=dim+"/"+(x>>5)+"/"+(z>>5);long[] bits=visits.getLongArray(key);int index=(x&31)|((z&31)<<5);
  boolean seen=bits.length==16&&(bits[index>>6]&(1L<<(index&63)))!=0;
  seen|=n.getCompound("explored").contains(dim+"/"+net.minecraft.world.level.ChunkPos.asLong(x,z));
  if(!seen){if(bits.length!=16)bits=new long[16];bits[index>>6]|=1L<<(index&63);visits.putLongArray(key,bits);n.put("chunk_visits_v3",visits);if(walked)ProgressionAPI.awardSourceXp(p,"exploration",DirectXpConfig.current.chunk);}
  if(walked){
   p.level().getBiome(p.blockPosition()).unwrapKey().ifPresent(id->claim(p,"biome_visits_v3",id.location().toString(),DirectXpConfig.current.biome));
   var world=p.serverLevel();var manager=world.structureManager();var registry=world.registryAccess().registryOrThrow(Registries.STRUCTURE);
   for(var structure:manager.getAllStructuresAt(p.blockPosition()).keySet()){
    var start=manager.getStructureWithPieceAt(p.blockPosition(),structure);
    if(start.isValid())claim(p,"structure_visits_v3",dim+"/"+registry.getKey(structure)+"/"+start.getChunkPos().toLong(),DirectXpConfig.current.structure);
   }
  }
  ProgressionData.get(p.server).setDirty();
 }
 @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p&&ActivityGuard.player(p)){
  LAST.remove(p);claim(p,"dimension_visits_v3",event.getTo().location().toString(),DirectXpConfig.current.dimension);
 }}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){LAST.remove(event.getEntity());}
 @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event){LAST.clear();}
 private static void claim(ServerPlayer p,String ledger,String id,double xp){var n=ProgressionAPI.profile(p);var claims=n.getCompound(ledger);if(claims.getBoolean(id))return;claims.putBoolean(id,true);n.put(ledger,claims);ProgressionData.get(p.server).setDirty();ProgressionAPI.awardSourceXp(p,"exploration",xp);}
}
