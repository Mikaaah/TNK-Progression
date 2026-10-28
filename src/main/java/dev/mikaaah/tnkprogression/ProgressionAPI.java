package dev.mikaaah.tnkprogression;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
public final class ProgressionAPI {
 public static CompoundTag profile(ServerPlayer p){
  var data=ProgressionData.get(p.server);var n=data.entry("player:"+p.getUUID());
  if(!n.contains("balance_curve_version")){
   int level=Math.max(1,n.getInt("level"));long oldXp=100L+40L*level*level;
   n.putLong("xp",Math.min(ProgressionMath.levelCost(level)-1,(long)(Math.clamp(n.getLong("xp")/(double)oldXp,0,1)*ProgressionMath.levelCost(level))));
   for(var entry:ProgressionConfig.current.activities.entrySet()){
    String key=entry.getKey();long rank=n.getLong(key+"_rank"),base=ProgressionConfig.current.legacyActivityRequirements.getOrDefault(key,entry.getValue().initialRequirement());
    double oldCost=base*(1+rank*.65+rank*(double)rank*.08),ratio=Math.clamp((n.getLong(key+"_work")+n.getDouble(key+"_fraction"))/oldCost,0,1);
    double next=ProgressionMath.taskCost(entry.getValue().initialRequirement(),rank),converted=Math.min(Math.nextDown(next),ratio*next);long whole=(long)converted;
    n.putLong(key+"_work",whole);n.putDouble(key+"_fraction",converted-whole);
   }
   n.putInt("balance_curve_version",2);data.setDirty();
  }
  return n;
 }
 public static int level(ServerPlayer p){return Math.max(1,Math.min(100,profile(p).getInt("level")));}
 public static int ceiling(ServerPlayer p){
  int personal=profile(p).getInt("ceiling");
  int team=TeamBridge.key(p).map(k->ProgressionData.get(p.server).entry(k).getInt("ceiling")).orElse(0);
  return Math.max(ProgressionConfig.current.ceilings[0],Math.max(personal,team));
 }
 public static int rarityCap(ServerPlayer p){int cap=ceiling(p),i=0;while(i+1<ProgressionConfig.current.ceilings.length && cap>=ProgressionConfig.current.ceilings[i+1])i++;return ProgressionConfig.current.rarityCaps[i];}
 public static int mobLevel(LivingEntity e){return Math.max(1,Math.min(100,e.getPersistentData().getInt("tnk_level")));}
 public static int lootLevel(ServerPlayer p,LivingEntity mob){return Math.min(mobLevel(mob),ceiling(p));}
 public static long awardXp(ServerPlayer p,long amount){
  if(amount<=0 || level(p)>=ceiling(p) || level(p)>=100)return 0;
  CompoundTag n=profile(p);int level=level(p),old=level;long xp=Math.clamp(n.getLong("xp"),0,ProgressionMath.levelCost(level)-1);
  long remaining=amount;
  while(remaining>0 && level<ceiling(p) && level<100){long needed=ProgressionMath.levelCost(level)-xp;long take=Math.min(remaining,needed);xp+=take;remaining-=take;if(xp>=ProgressionMath.levelCost(level)){level++;xp=0;}}
  n.putInt("level",level);n.putLong("xp",xp);ProgressionData.get(p.server).setDirty();
  if(level!=old){MobLevels.invalidate(p.serverLevel());p.sendSystemMessage(Component.literal("Character level "+level+" — "+level+" skill points earned"));SkillBridge.sync(p);LevelBadges.changed(p);}
  return amount-remaining;
 }
 public static boolean rewardOnce(ServerPlayer p,String id,long amount){
  CompoundTag n=profile(p),claims=n.getCompound("claims");if(claims.getBoolean(id))return false;
  claims.putBoolean(id,true);n.put("claims",claims);awardXp(p,amount);ProgressionData.get(p.server).setDirty();return true;
 }
 public static void unlock(ServerPlayer p,int cap){
  if(java.util.Arrays.stream(ProgressionConfig.current.ceilings).noneMatch(c->c==cap))throw new IllegalArgumentException("Use a configured milestone ceiling");
  CompoundTag n=profile(p);n.putInt("ceiling",Math.max(cap,n.getInt("ceiling")));
  TeamBridge.key(p).ifPresent(k->{CompoundTag t=ProgressionData.get(p.server).entry(k);t.putInt("ceiling",Math.max(cap,t.getInt("ceiling")));});
  ProgressionData.get(p.server).setDirty();
 }
 /** Fractional weighted work persists; a teammate's small share is never rounded away per kill. */
 public static void activityFractional(ServerPlayer p,String key,double units){
  if(!ActivityGuard.player(p) || !Double.isFinite(units) || units<=0 || level(p)>=ceiling(p) || level(p)>=100)return;
  CompoundTag n=profile(p); double total=n.getDouble(key+"_fraction")+units;
  long whole=(long)Math.floor(total); n.putDouble(key+"_fraction",total-whole);
  if(whole>0)activity(p,key,whole); ProgressionData.get(p.server).setDirty();
 }
 public static boolean engineeringObjective(ServerPlayer p,String id){
  Double units=ProgressionConfig.current.engineeringObjectives.get(id);if(units==null)return false;
  CompoundTag n=profile(p),claims=n.getCompound("engineering_claims");if(claims.getBoolean(id))return false;
  claims.putBoolean(id,true);n.put("engineering_claims",claims);activityFractional(p,"engineering",units);ProgressionData.get(p.server).setDirty();return true;
 }
 public static void activity(ServerPlayer p,String key,long units){
  if(!ActivityGuard.player(p) || units<=0 || level(p)>=ceiling(p) || level(p)>=100)return;
  var a=ProgressionConfig.current.activities.get(key);if(a==null)return;
  CompoundTag n=profile(p);long rank=Math.max(0,n.getLong(key+"_rank")),work=Math.max(0,n.getLong(key+"_work"));
  work=units>Long.MAX_VALUE-work?Long.MAX_VALUE:work+units;
  while(work>=ProgressionMath.taskCost(a.initialRequirement(),rank)){
   work-=ProgressionMath.taskCost(a.initialRequirement(),rank);long paid=awardXp(p,ProgressionMath.taskReward(a.initialXp(),rank));if(rank<Long.MAX_VALUE)rank++;
   var history=n.getList("activity_history",net.minecraft.nbt.Tag.TAG_STRING);history.add(0,net.minecraft.nbt.StringTag.valueOf(key+" milestone "+rank+"  +"+paid+" XP"));while(history.size()>8)history.remove(history.size()-1);n.put("activity_history",history);
   if(level(p)>=ceiling(p) || level(p)>=100){work=0;n.putDouble(key+"_fraction",0);break;}
  }
  n.putLong(key+"_rank",rank);n.putLong(key+"_work",work);ProgressionData.get(p.server).setDirty();
 }
}
