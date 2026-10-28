package dev.mikaaah.tnkprogression;

/** Contains no Minecraft client classes: safe to reference from common packet registration. */
public final class CharacterClientState {
 public static CharacterState.Snapshot current;
 public static void accept(String json){
  var s=CharacterState.JSON.fromJson(json,CharacterState.Snapshot.class);
  if(current!=null && s.titles().isEmpty())s=new CharacterState.Snapshot(s.level(),s.ceiling(),s.xp(),s.required(),s.gate(),s.pointsEarned(),s.pointsSpent(),s.selected(),s.tracked(),s.hud(),s.swapTicks(),s.activities(),current.titles(),s.recent());
  current=s;
 }
 public static void clear(){current=null;}
}
