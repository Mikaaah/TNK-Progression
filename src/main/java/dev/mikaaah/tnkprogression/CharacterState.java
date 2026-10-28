package dev.mikaaah.tnkprogression;

import java.util.*;
import com.google.gson.Gson;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Only a player's own, bounded, server-calculated progression view is sent to their client. */
public final class CharacterState {
 public static final List<String> ACTIVITIES=List.of("combat","mining","fishing","exploration","farming","crafting","engineering");
 public record Activity(String id,long rank,double work,long required,long reward) {}
 public record Perk(String text,boolean available) {}
 public record Title(String id,String label,String description,String translation,int colour,boolean earned,List<Perk> perks) {}
 public record Snapshot(int level,int ceiling,long xp,long required,String gate,int pointsEarned,int pointsSpent,String selected,String tracked,boolean hud,long swapTicks,List<Activity> activities,List<Title> titles,List<String> recent) {}
 public static final Gson JSON=new Gson();
 private static final Map<ServerPlayer,String> LAST=new WeakHashMap<>();
 public static Snapshot snapshot(ServerPlayer p){return snapshot(p,true);}
 private static Snapshot snapshot(ServerPlayer p,boolean includeTitles){
  var n=ProgressionAPI.profile(p);List<Activity> activities=new ArrayList<>();
  for(String key:ACTIVITIES){var a=ProgressionConfig.current.activities.get(key);long rank=n.getLong(key+"_rank");activities.add(new Activity(key,rank,n.getLong(key+"_work")+n.getDouble(key+"_fraction"),ProgressionMath.taskCost(a.initialRequirement(),rank),ProgressionMath.taskReward(a.initialXp(),rank)));}
  List<Title> titles=new ArrayList<>();
  if(includeTitles)for(var t:TitleConfig.current.titles())titles.add(new Title(t.id(),t.label(),t.description(),t.translationKey(),t.rgb(),Titles.unlocked(p,t.id()),TitlePerks.forTitle(t.id()).stream().map(perk->new Perk(perk.description(),TitlePerks.available(p,perk))).toList()));
  int cap=ProgressionAPI.ceiling(p);String gate=switch(cap){case 10->"Chapter 1 · Kinetic workshop";case 30->"Chapter 3 · Precision workshop";case 50->"Chapter 5 · Ender machinery";case 70->"Chapter 6 · Chemical machinery";case 80->"Chapter 8 · Containment";case 94->"Chapter 10 · Sovereign";default->cap>=100?"Maximum level":"Your next campaign milestone";};
  int spent=SkillBridge.titleSnapshot(p,ProgressionConfig.current.skillCategory).map(SkillBridge.TitleSnapshot::spent).orElse(-1);
  return new Snapshot(ProgressionAPI.level(p),cap,n.getLong("xp"),ProgressionMath.levelCost(ProgressionAPI.level(p)),gate,ProgressionMath.skillPoints(ProgressionAPI.level(p)),spent,Titles.selected(p),tracked(p),!n.getBoolean("hud_hidden"),TitlePerks.cooldown(p),activities,titles,n.getList("activity_history",net.minecraft.nbt.Tag.TAG_STRING).stream().map(net.minecraft.nbt.Tag::getAsString).toList());
 }
 public static String tracked(ServerPlayer p){String key=ProgressionAPI.profile(p).getString("tracked_activity");return ACTIVITIES.contains(key)?key:"combat";}
 public static boolean track(ServerPlayer p,String id){if(!ACTIVITIES.contains(id))return false;ProgressionAPI.profile(p).putString("tracked_activity",id);ProgressionData.get(p.server).setDirty();return true;}
 public static void send(ServerPlayer p){send(p,false);}
 public static void sendFull(ServerPlayer p){send(p,true);}
 private static void send(ServerPlayer p,boolean full){
  if(p instanceof net.neoforged.neoforge.common.util.FakePlayer)return;var s=snapshot(p,full);
  if(!full)s=new Snapshot(s.level(),s.ceiling(),s.xp(),s.required(),s.gate(),s.pointsEarned(),s.pointsSpent(),s.selected(),s.tracked(),s.hud(),s.swapTicks(),s.activities(),List.of(),s.recent());
  String json=JSON.toJson(s);if(full || !json.equals(LAST.get(p))){PacketDistributor.sendToPlayer(p,new CharacterPayload(json));LAST.put(p,json);}
 }
 public static boolean action(ServerPlayer p,String action,String value){
  return switch(action){
   case "refresh"->true;
   case "skills"->value.isEmpty() && SkillBridge.open(p);
   case "title"->Titles.select(p,value);
   case "track"->track(p,value);
   case "hud"->{if(!value.equals("on")&&!value.equals("off"))yield false;ProgressionAPI.profile(p).putBoolean("hud_hidden",value.equals("off"));ProgressionData.get(p.server).setDirty();yield true;}
   default->false;
  };
 }
 @SubscribeEvent public static void tick(PlayerTickEvent.Post e){if(e.getEntity() instanceof ServerPlayer p && p.tickCount%20==0)send(p);}
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)sendFull(p);}
}
