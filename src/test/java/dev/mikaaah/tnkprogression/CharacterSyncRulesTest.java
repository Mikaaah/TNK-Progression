package dev.mikaaah.tnkprogression;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CharacterSyncRulesTest {
 private CharacterState.Snapshot full(){return new CharacterState.Snapshot(3,10,100,3000,"Chapter I",5,2,"tnk:prepared","tnk:skill_delver","mining",false,20,
  List.of(new CharacterState.Activity("mining",100,5,20,List.of("Natural blocks only"))),
  List.of(new CharacterState.Title("tnk:prepared","Survivor","Quest reward","key",0xFFFFFF,true,List.of())),
  List.of("mining +5 XP"),"tnk:miner",2,false,false,10);}
 @Test void incrementalPacketsStripStaticDetailsAndMergeWithoutLosingThem(){
  var full=full();var partial=CharacterSnapshots.incremental(full);
  assertTrue(partial.titles().isEmpty());assertTrue(partial.activities().getFirst().info().isEmpty());
  assertEquals(full,CharacterSnapshots.merge(partial,full));
  assertEquals(partial,CharacterSnapshots.merge(partial,null));
 }
 @Test void aFullRefreshReplacesPreviouslyCachedHelp(){
  var full=full();var changed=new CharacterState.Snapshot(full.level(),full.ceiling(),200,full.required(),full.gate(),full.pointsEarned(),3,
   full.selected(),full.passive(),full.tracked(),true,0,List.of(new CharacterState.Activity("mining",150,50,21,List.of("Updated server rule"))),
   full.titles(),full.recent(),full.origin(),full.originRevision(),full.manaAvailable(),full.ascendedOriginsUnlocked(),11);
  assertEquals(changed,CharacterSnapshots.merge(changed,full));
  var partial=CharacterSnapshots.incremental(changed);var merged=CharacterSnapshots.merge(partial,full);
  assertEquals(200,merged.xp());assertEquals(3,merged.pointsSpent());assertTrue(merged.hud());assertEquals(11,merged.originSelectionRequest());
  assertEquals(List.of("Natural blocks only"),merged.activities().getFirst().info());
 }
 @Test void refreshCannotAmplifyCatalogueWorkOrStarveRealSelections(){
  var gate=new CharacterActionGate();assertTrue(gate.accept(0,true));assertFalse(gate.accept(1,true));assertFalse(gate.accept(19,true));assertTrue(gate.accept(20,true));
  assertTrue(gate.accept(20,false));assertFalse(gate.accept(21,false));assertTrue(gate.accept(22,false));
  assertTrue(gate.accept(5,false)); // a restarted clock starts a new pacing window
 }
 @Test void corruptDisplayedCountersCannotPoisonJson(){
  assertEquals(0,CharacterSnapshots.displayAmount(Double.NaN));assertEquals(0,CharacterSnapshots.displayAmount(Double.POSITIVE_INFINITY));
  assertEquals(0,CharacterSnapshots.displayAmount(-2));assertEquals(12.5,CharacterSnapshots.displayAmount(12.5));
 }
 @Test void normalSnapshotsKeepTheirExactExistingJson(){
  assertEquals(new com.google.gson.Gson().toJson(full()),CharacterSnapshots.encode(full()));
 }
 @Test void largeValidRewardHelpDoesNotOverflowThePacket(){
  var rows=java.util.stream.IntStream.range(0,8192).mapToObj(i->"tnk:example_resource_identifier_"+i+": 1 XP per block").toList();
  var snapshot=replace(List.of(new CharacterState.Activity("mining",10,1,10,rows)),full().titles());
  assertTrue(new com.google.gson.Gson().toJson(snapshot).length()>262144);
  var encoded=CharacterSnapshots.encode(snapshot);
  assertTrue(encoded.length()<=CharacterSnapshots.MAX_JSON_CHARS);
  var decoded=new com.google.gson.Gson().fromJson(encoded,CharacterState.Snapshot.class);
  assertEquals(snapshot.titles().getFirst().id(),decoded.titles().getFirst().id());
  assertTrue(decoded.activities().getFirst().info().getFirst().contains("omitted"));
  assertEquals(snapshot.xp(),decoded.xp());
 }
 @Test void maximalTitleDetailsPreserveEveryOwnershipAndPerkFlag(){
  var requirements=java.util.stream.IntStream.range(0,64).mapToObj(i->"a".repeat(96)+i+": not unlocked.").toList();
  var titles=java.util.stream.IntStream.range(0,256).mapToObj(i->new CharacterState.Title("tnk:custom_"+i,"Title "+i,
   "<".repeat(200),"tnkprogression.title.tnk.custom_"+i,0xFFFFFF,i%2==0,
   List.of(new CharacterState.Perk("<".repeat(100),true),new CharacterState.Perk("<".repeat(100),false),new CharacterState.Perk("<".repeat(100),true)),false,requirements)).toList();
  var snapshot=replace(full().activities(),titles);
  assertTrue(new com.google.gson.Gson().toJson(snapshot).length()>262144);
  var encoded=CharacterSnapshots.encode(snapshot);
  assertTrue(encoded.length()<=CharacterSnapshots.MAX_JSON_CHARS);
  var decoded=new com.google.gson.Gson().fromJson(encoded,CharacterState.Snapshot.class);
  assertEquals(256,decoded.titles().size());assertEquals(snapshot.selected(),decoded.selected());assertEquals(snapshot.passive(),decoded.passive());
  for(int i=0;i<titles.size();i++){
   var before=titles.get(i);var after=decoded.titles().get(i);
   assertEquals(before.id(),after.id());assertEquals(before.label(),after.label());assertEquals(before.translation(),after.translation());
   assertEquals(before.earned(),after.earned());assertEquals(before.legacy(),after.legacy());assertEquals(before.colour(),after.colour());
   assertEquals(before.perks().stream().map(CharacterState.Perk::available).toList(),after.perks().stream().map(CharacterState.Perk::available).toList());
   assertTrue(after.info().getFirst().contains("omitted"));
  }
 }
 private CharacterState.Snapshot replace(List<CharacterState.Activity> activities,List<CharacterState.Title> titles){
  var s=full();return new CharacterState.Snapshot(s.level(),s.ceiling(),s.xp(),s.required(),s.gate(),s.pointsEarned(),s.pointsSpent(),s.selected(),s.passive(),s.tracked(),s.hud(),s.swapTicks(),activities,titles,s.recent(),s.origin(),s.originRevision(),s.manaAvailable(),s.ascendedOriginsUnlocked(),s.originSelectionRequest());
 }
}
