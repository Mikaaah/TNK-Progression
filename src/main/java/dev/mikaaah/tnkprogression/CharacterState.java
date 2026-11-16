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
 public static final List<String> ACTIVITIES=List.of("combat","mining","farming","cooking","fishing","exploration","quest");
 public record Activity(String id,double earned,double lastGain,long actions,List<String> info) {}
 public record Perk(String text,boolean available) {}
 public record Title(String id,String label,String description,String translation,int colour,boolean earned,List<Perk> perks,boolean legacy,List<String> info) {
  public Title(String id,String label,String description,String translation,int colour,boolean earned,List<Perk> perks){this(id,label,description,translation,colour,earned,perks,false,List.of(description));}
 }
 public record Snapshot(int level,int ceiling,long xp,long required,String gate,int pointsEarned,int pointsSpent,String selected,String passive,String tracked,boolean hud,long swapTicks,List<Activity> activities,List<Title> titles,List<String> recent,String origin,int originRevision,boolean manaAvailable,boolean ascendedOriginsUnlocked,long originSelectionRequest) {}
 public static final Gson JSON=new Gson();
 private static final class Sent { Snapshot partial,full;long fullTick=Long.MIN_VALUE; }
 private static final Map<ServerPlayer,Sent> LAST=new WeakHashMap<>();
 public static Snapshot snapshot(ServerPlayer p){return snapshot(p,true);}
 private static Snapshot snapshot(ServerPlayer p,boolean includeTitles){
  var n=ProgressionAPI.profile(p);List<Activity> activities=new ArrayList<>();
  for(String key:ACTIVITIES)activities.add(new Activity(key,CharacterSnapshots.displayAmount(n.getDouble(key+"_xp_earned")),CharacterSnapshots.displayAmount(n.getDouble(key+"_xp_last")),Math.max(0,n.getLong(key+"_actions")),includeTitles?ActivityHelp.describe(p,key):List.of()));
  List<Title> titles=new ArrayList<>();
  if(includeTitles){
   var snapshots=new HashMap<String,Optional<SkillBridge.TitleSnapshot>>();
   var activeIds=new HashSet<String>();TitleConfig.current.titles().forEach(t->activeIds.add(t.id()));
   for(var t:TitleConfig.all()){
    boolean legacy=!activeIds.contains(t.id());if(legacy && !Titles.unlocked(p,t.id()))continue;
    var skills=t.automatic()?snapshots.computeIfAbsent(t.skillCategory(),id->SkillBridge.titleSnapshot(p,id)):Optional.<SkillBridge.TitleSnapshot>empty();
    titles.add(new Title(t.id(),t.label(),t.description(),t.translationKey(),t.rgb(),Titles.unlocked(p,t.id()),TitlePerks.forTitle(t.id()).stream().map(perk->new Perk(perk.description(),TitlePerks.available(p,perk))).toList(),legacy,TitleHelp.describe(t,legacy,skills)));
   }
  }
  int level=ProgressionAPI.level(p),cap=ProgressionAPI.ceiling(p);long required=ProgressionMath.levelCost(level);
  String gate=switch(cap){case 10->"Chapter I · Getting Started";case 30->"Chapter III · Copper (Sealed)";case 50->"Chapter V · AE2 (Calculation)";case 70->"Chapter VI · Light Engineering (Steel)";case 80->"Chapter VIII · Magic & Mana";case 94->"Chapter X · Creative Core";default->cap>=100?"Maximum level":"Your next campaign milestone";};
  int spent=SkillBridge.spentPoints(p,ProgressionConfig.current.skillCategory).orElse(-1);
  return new Snapshot(level,cap,Math.clamp(n.getLong("xp"),0,required-1),required,gate,ProgressionMath.skillPoints(level),spent,Titles.selected(p),Titles.passive(p),tracked(p),n.contains("hud_hidden")?!n.getBoolean("hud_hidden"):ProgressionConfig.current.hudEnabledByDefault,TitlePerks.cooldown(p),activities,titles,n.getList("activity_history",net.minecraft.nbt.Tag.TAG_STRING).stream().map(net.minecraft.nbt.Tag::getAsString).toList(),Origins.selected(p),Origins.revision(p),Origins.manaAvailable(p),Origins.ascendedUnlocked(p),n.getLong("origin_selection_request"));
 }
 public static String tracked(ServerPlayer p){String key=ProgressionAPI.profile(p).getString("tracked_activity");return ACTIVITIES.contains(key)?key:"combat";}
 public static boolean track(ServerPlayer p,String id){if(!ACTIVITIES.contains(id))return false;ProgressionAPI.profile(p).putString("tracked_activity",id);ProgressionData.get(p.server).setDirty();return true;}
 public static void send(ServerPlayer p){send(p,false);}
 public static void sendFull(ServerPlayer p){send(p,true);}
 private static void send(ServerPlayer p,boolean full){
  if(p instanceof net.neoforged.neoforge.common.util.FakePlayer)return;
  var s=snapshot(p,full);var sent=LAST.computeIfAbsent(p,ignored->new Sent());long tick=p.server.overworld().getGameTime();
  if(full){
   if(sent.fullTick==tick && s.equals(sent.full))return;
   PacketDistributor.sendToPlayer(p,new CharacterPayload(CharacterSnapshots.encode(s)));
   sent.fullTick=tick;sent.full=s;sent.partial=CharacterSnapshots.incremental(s);
  }else{
   if(sent.fullTick!=tick)sent.full=null;
   if(s.equals(sent.partial))return;
   PacketDistributor.sendToPlayer(p,new CharacterPayload(CharacterSnapshots.encode(s)));sent.partial=s;
  }
 }
 public static boolean action(ServerPlayer p,String action,String value){
  return switch(action){
   case "refresh"->value.isEmpty();
   case "origin_open"->value.isEmpty() && Origins.open(p);
   case "origin"->Origins.choose(p,value);
   case "skills"->value.isEmpty() && SkillBridge.open(p);
   case "title"->Titles.select(p,value);
   case "passive"->Titles.selectPassive(p,value);
   case "track"->track(p,value);
   case "hud"->{if(!value.equals("on")&&!value.equals("off"))yield false;ProgressionAPI.profile(p).putBoolean("hud_hidden",value.equals("off"));ProgressionData.get(p.server).setDirty();yield true;}
   default->false;
  };
 }
 @SubscribeEvent public static void tick(PlayerTickEvent.Post e){if(e.getEntity() instanceof ServerPlayer p && p.tickCount%20==0)send(p);}
 // LevelBadges.login sends the initial full view after synchronizing appearance.
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){LAST.remove(p);CharacterAction.forget(p);}}
 @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e){LAST.clear();CharacterAction.clear();}
}
