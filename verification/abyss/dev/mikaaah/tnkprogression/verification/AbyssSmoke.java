package dev.mikaaah.tnkprogression.verification;

import com.mojang.authlib.GameProfile;
import dev.mikaaah.tnkprogression.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.util.*;
import java.nio.file.*;

@EventBusSubscriber(modid="tnkprogression")
public final class AbyssSmoke {
 public static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static ServerPlayer player(net.minecraft.server.MinecraftServer server,ServerLevel world,String name) throws Exception {
  var profile=new GameProfile(UUID.randomUUID(),name);
  var p=new ServerPlayer(server,world,profile,ClientInformation.createDefault());
  p.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),p,CommonListenerCookie.createInitial(profile,false)){
   @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
   @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
  };
  p.setGameMode(GameType.SURVIVAL);
  var list=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");list.setAccessible(true);((List<ServerPlayer>)list.get(server.getPlayerList())).add(p);
  var ids=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");ids.setAccessible(true);((Map<UUID,ServerPlayer>)ids.get(server.getPlayerList())).put(p.getUUID(),p);
  world.players().add(p);ProgressionAPI.profile(p).putInt("ceiling",100);return p;
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void run(ServerStartedEvent event){
  if(!Boolean.getBoolean("tnkprogression.abyssSmoke"))return;
  var server=event.getServer();var report=new com.google.gson.JsonObject();
  try {
   var world=server.overworld();var player=player(server,world,"AbyssTest");player.setPos(0,65,0);
   ProgressionAPI.profile(player).putInt("level",25);MobLevels.invalidate(world);
   var normal=EntityType.ZOMBIE.create(world);normal.setPos(0,65,0);world.addFreshEntity(normal);
   check(ProgressionAPI.mobLevel(normal)>=21&&ProgressionAPI.mobLevel(normal)<=29,"normal world nearest-player variance");
   normal.setHealth(7);int old=ProgressionAPI.mobLevel(normal);
   ProgressionEvents.join(new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(normal,world));
   check(normal.getHealth()==7&&ProgressionAPI.mobLevel(normal)==old,"normal saved levels and health");
   var bogus=EntityType.ZOMBIE.create(world);bogus.getPersistentData().putInt("tnk_abyss_floor",6);bogus.getPersistentData().putInt("tnk_abyss_target_level",100);bogus.getPersistentData().putUUID("spiral_abyss_run",UUID.randomUUID());
   world.addFreshEntity(bogus);check(!AbyssIntegration.assigned(bogus)&&ProgressionAPI.mobLevel(bogus)<30,"spoofed data outside dimension");
   check(ProgressionAPI.lootLevel(player,normal)==old,"ordinary loot API unchanged");
   var saved=ProgressionAPI.profile(player).copy();ProgressionAPI.profile(player).putInt("level",1);ProgressionAPI.profile(player).putInt("ceiling",10);
   normal.getPersistentData().putInt("tnk_level",110);
   check(ProgressionAPI.mobLevel(normal)==110&&ProgressionAPI.lootLevel(player,normal)==10&&ProgressionAPI.rarityCap(player)==1,"combat and ordinary loot caps remain separate");
   report.addProperty("ordinary_world_and_API",true);
   boolean spiral=ModList.get().isLoaded("spiral_abyss"),loot=ModList.get().isLoaded("tnkloot");
   if(spiral)SpiralChecks.run(server,report);
   if(loot)LootChecks.run(server,player,report,spiral);
   report.addProperty("spiral",spiral);report.addProperty("loot",loot);report.addProperty("bridge",ModList.get().isLoaded("tnk_abyss_rotations"));
   report.addProperty("status","passed");
   Files.writeString(Path.of("abyss-validation.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
   TnkProgression.LOG.info("TNK_ABYSS_SMOKE_OK: {}",report);
  }catch(Throwable failure){TnkProgression.LOG.error("TNK_ABYSS_SMOKE_FAILED",failure);}
  finally{server.halt(false);}
 }
}
