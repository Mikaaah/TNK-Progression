package dev.mikaaah.tnkprogression;

/** Contains no Minecraft client classes: safe to reference from common packet registration. */
public final class CharacterClientState {
 public static CharacterState.Snapshot current;
 public static boolean originPromptPending;private static long originRequest;
 public static void accept(String json){
  var s=CharacterState.JSON.fromJson(json,CharacterState.Snapshot.class);
  s=CharacterSnapshots.merge(s,current);
  if(s.origin().isEmpty() && s.originSelectionRequest()>originRequest)originPromptPending=true;originRequest=s.originSelectionRequest();current=s;
 }
 public static void clear(){current=null;originPromptPending=false;originRequest=0;}
}
