package dev.mikaaah.tnkprogression;

import java.util.*;

/** Pure wire-view operations: incremental packets never repeat static help or title definitions. */
public final class CharacterSnapshots {
 private CharacterSnapshots() {}
 private static final com.google.gson.Gson JSON=new com.google.gson.Gson();
 public static final int MAX_JSON_CHARS=240_000;
 private static final String ACTIVITY_NOTICE="Extended help omitted from this large view; see the server's tnk-xp.json.";
 private static final String TITLE_NOTICE="Extended acquisition/perk details omitted; see the server's title configuration.";
 /** Bound display-only detail, never configured rewards, title ownership, or the selected IDs. */
 public static String encode(CharacterState.Snapshot snapshot){
  String json=JSON.toJson(snapshot);
  if(json.length()<=MAX_JSON_CHARS)return json;
  // Large, valid XP maps and skill requirements can otherwise exceed writeUtf's limit.
  var activities=snapshot.activities().stream().map(a->new CharacterState.Activity(a.id(),a.earned(),a.lastGain(),a.actions(),List.of(ACTIVITY_NOTICE))).toList();
  var titles=snapshot.titles().stream().map(t->new CharacterState.Title(t.id(),t.label(),t.description(),t.translation(),t.colour(),t.earned(),t.perks(),t.legacy(),List.of(TITLE_NOTICE))).toList();
  var compact=copy(snapshot,activities,titles);
  json=JSON.toJson(compact);
  if(json.length()<=MAX_JSON_CHARS)return json;
  for(int limit=128;;limit/=2){
   final int size=limit;
   var brief=titles.stream().map(t->new CharacterState.Title(t.id(),t.label(),abbreviate(t.description(),size),t.translation(),t.colour(),t.earned(),
    t.perks().stream().map(p->new CharacterState.Perk(abbreviate(p.text(),size),p.available())).toList(),t.legacy(),List.of(TITLE_NOTICE))).toList();
   var recent=snapshot.recent().stream().limit(8).map(text->abbreviate(text,size)).toList();
   var bounded=copy(snapshot,activities,brief,recent);
   json=JSON.toJson(bounded);
   if(json.length()<=MAX_JSON_CHARS)return json;
   if(limit==0)throw new IllegalArgumentException("Character metadata exceeds the supported title catalogue limits");
  }
 }
 private static String abbreviate(String text,int limit){
  if(text==null || text.length()<=limit)return text;
  if(limit==0)return "";
  int end=Math.min(text.length(),limit);
  if(end>0 && Character.isHighSurrogate(text.charAt(end-1)))end--;
  return text.substring(0,end)+"…";
 }
 public static double displayAmount(double value){return Double.isFinite(value)&&value>=0?value:0;}
 public static CharacterState.Snapshot incremental(CharacterState.Snapshot s){
  return copy(s,s.activities().stream().map(a->new CharacterState.Activity(a.id(),a.earned(),a.lastGain(),a.actions(),List.of())).toList(),List.of());
 }
 public static CharacterState.Snapshot merge(CharacterState.Snapshot incoming,CharacterState.Snapshot previous){
  if(previous==null)return incoming;
  var help=new HashMap<String,List<String>>();previous.activities().forEach(a->help.put(a.id(),a.info()));
  var activities=incoming.activities().stream().map(a->a.info().isEmpty()
   ?new CharacterState.Activity(a.id(),a.earned(),a.lastGain(),a.actions(),help.getOrDefault(a.id(),List.of())):a).toList();
  return copy(incoming,activities,incoming.titles().isEmpty()?previous.titles():incoming.titles());
 }
 private static CharacterState.Snapshot copy(CharacterState.Snapshot s,List<CharacterState.Activity> activities,List<CharacterState.Title> titles){
  return copy(s,activities,titles,s.recent());
 }
 private static CharacterState.Snapshot copy(CharacterState.Snapshot s,List<CharacterState.Activity> activities,List<CharacterState.Title> titles,List<String> recent){
  return new CharacterState.Snapshot(s.level(),s.ceiling(),s.xp(),s.required(),s.gate(),s.pointsEarned(),s.pointsSpent(),s.selected(),s.passive(),s.tracked(),s.hud(),s.swapTicks(),activities,titles,recent,s.origin(),s.originRevision(),s.manaAvailable(),s.ascendedOriginsUnlocked(),s.originSelectionRequest());
 }
}
