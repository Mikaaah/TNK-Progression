package dev.mikaaah.tnkprogression;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
public final class ProgressionAPI {
 public static CompoundTag profile(ServerPlayer p){
  var data=ProgressionData.get(p.server);var n=data.entry("player:"+p.getUUID());
  if(n.getInt("direct_xp_version")<3){
   int level=Math.clamp(n.getInt("level"),1,100);
   long oldCost=n.contains("balance_curve_version")?BalanceConfig.current.previousCost(level):100L+40L*level*level;
   n.putLong("xp",Math.min(ProgressionMath.levelCost(level)-1,(long)(Math.clamp(n.getLong("xp")/(double)oldCost,0,1)*ProgressionMath.levelCost(level))));
   n.putInt("level",level);n.putInt("direct_xp_version",3);n.putInt("balance_curve_version",3);
   // Archive the old history; milestones no longer drive XP or activity displays.
   if(n.contains("activity_history")){n.put("legacy_activity_history",n.get("activity_history").copy());n.remove("activity_history");}
   data.setDirty();
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
 public static int mobLevel(LivingEntity e){return Math.max(1,Math.min(BossLevels.MAX_LEVEL,e.getPersistentData().getInt("tnk_level")));}
 public static int lootLevel(ServerPlayer p,LivingEntity mob){return Math.min(mobLevel(mob),ceiling(p));}
 public static long awardXp(ServerPlayer p,long amount){
  if(amount<=0 || level(p)>=ceiling(p) || level(p)>=100)return 0;
  CompoundTag n=profile(p);int level=level(p),old=level;long xp=Math.clamp(n.getLong("xp"),0,ProgressionMath.levelCost(level)-1);
  long remaining=amount;
  while(remaining>0 && level<ceiling(p) && level<100){long needed=ProgressionMath.levelCost(level)-xp;long take=Math.min(remaining,needed);xp+=take;remaining-=take;if(xp>=ProgressionMath.levelCost(level)){level++;xp=0;}}
  n.putInt("level",level);n.putLong("xp",xp);ProgressionData.get(p.server).setDirty();
  if(level!=old){MobLevels.invalidate(p.serverLevel());p.sendSystemMessage(Component.literal("Character level "+level+" — "+ProgressionMath.skillPoints(level)+" skill points earned"));SkillBridge.sync(p);LevelBadges.changed(p);}
  return amount-remaining;
 }
 public static boolean rewardOnce(ServerPlayer p,String id,long amount){
  CompoundTag n=profile(p),claims=n.getCompound("claims");if(claims.getBoolean(id))return false;
  if(awardSourceXp(p,"quest",amount)<=0)return false;claims.putBoolean(id,true);n.put("claims",claims);ProgressionData.get(p.server).setDirty();return true;
 }
 public static void unlock(ServerPlayer p,int cap){
  if(java.util.Arrays.stream(ProgressionConfig.current.ceilings).noneMatch(c->c==cap))throw new IllegalArgumentException("Use a configured milestone ceiling");
  CompoundTag n=profile(p);n.putInt("ceiling",Math.max(cap,n.getInt("ceiling")));
  TeamBridge.key(p).ifPresent(k->{CompoundTag t=ProgressionData.get(p.server).entry(k);t.putInt("ceiling",Math.max(cap,t.getInt("ceiling")));});
  ProgressionData.get(p.server).setDirty();
 }
 /** Direct XP sources share one character bar; source totals are accounting, not skill levels. */
 public static long awardSourceXp(ServerPlayer p,String key,double amount){
  if(!CharacterState.ACTIVITIES.contains(key)||(key.equals("quest")?p instanceof net.neoforged.neoforge.common.util.FakePlayer:!ActivityGuard.player(p))||!Double.isFinite(amount)||amount<=0||level(p)>=ceiling(p)||level(p)>=100)return 0;
  var n=profile(p);double adjusted=Math.min(1_000_000_000,amount);
  if(key.equals("quest"))adjusted*=1+Origins.bonus(p,OriginRules.Perk.ENGINEERING_XP);
  String remainder=key+"_xp_remainder";double total=n.getDouble(remainder)+adjusted;
  long paid=awardXp(p,(long)Math.floor(total));
  n.putDouble(remainder,level(p)>=ceiling(p)||level(p)>=100?0:total-Math.floor(total));
  n.putDouble(key+"_xp_earned",n.getDouble(key+"_xp_earned")+paid);
  n.putDouble(key+"_xp_last",paid);n.putLong(key+"_actions",n.getLong(key+"_actions")+1);
  if(paid>0){var history=n.getList("activity_history",net.minecraft.nbt.Tag.TAG_STRING);String line=key+"  +"+paid+" XP";
   if(history.isEmpty()||!history.getString(0).equals(line))history.add(0,net.minecraft.nbt.StringTag.valueOf(line));
   while(history.size()>8)history.remove(history.size()-1);n.put("activity_history",history);}
  ProgressionData.get(p.server).setDirty();return paid;
 }
 /** Compatibility entry points now accept direct XP. Removed sources intentionally award nothing. */
 public static void activityFractional(ServerPlayer p,String key,double xp){awardSourceXp(p,key,xp);}
 public static void activity(ServerPlayer p,String key,long xp){awardSourceXp(p,key,xp);}
 @Deprecated public static boolean engineeringObjective(ServerPlayer p,String id){return false;}
}
