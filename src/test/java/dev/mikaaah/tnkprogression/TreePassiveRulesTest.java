package dev.mikaaah.tnkprogression;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TreePassiveRulesTest {
 @Test void unlockingAndRespeccingChangesOnlyOwnedBonuses(){
  var defs=Map.of("a",Map.of("leech",.1),"b",Map.of("leech",.2));
  assertEquals(.1,TreePassiveRules.aggregate(Set.of("tnk_tree.a"),defs).get("leech"),.00001);
  assertEquals(.3,TreePassiveRules.aggregate(Set.of("tnk_tree.a","tnk_tree.b"),defs).get("leech"),.00001);
  assertTrue(TreePassiveRules.aggregate(Set.of(),defs).isEmpty());
 }
 @Test void capsRejectInvalidOrUnknownEffects(){
  var defs=Map.of("a",Map.of("evasion",100.0,"invented",20.0,"leech",Double.NaN));
  var stats=TreePassiveRules.aggregate(Set.of("tnk_tree.a","other.a","tnk_tree.missing"),defs);
  assertEquals(Map.of("evasion",.15),stats);
 }
 @Test void longshotRequiresAProjectileAndCapsAtSixteenBlocks(){
  var stats=Map.of("longshot",.12);
  assertEquals(1,TreePassiveRules.damageMultiplier(stats,false,false,false,false,100));
  assertEquals(1.06,TreePassiveRules.damageMultiplier(stats,false,false,false,true,8),.0001);
  assertEquals(1.12,TreePassiveRules.damageMultiplier(stats,false,false,false,true,100),.0001);
 }
 @Test void conditionsMustBeActiveAndCombinedDamageIsBounded(){
  var stats=Map.of("burning_damage",.35,"well_fed_damage",.2,"effect_damage",.2,"longshot",.15);
  assertEquals(1,TreePassiveRules.damageMultiplier(stats,false,false,false,false,0));
  assertEquals(1.75,TreePassiveRules.damageMultiplier(stats,true,true,true,true,100),.0001);
 }
 @Test void potionDurationCannotBeInfiniteOrOverflow(){
  assertEquals(1500,TreePassiveRules.potionDuration(1000,50));
  assertEquals(-1,TreePassiveRules.potionDuration(-1,.5));
  assertEquals(Integer.MAX_VALUE,TreePassiveRules.potionDuration(Integer.MAX_VALUE,.5));
 }
}
