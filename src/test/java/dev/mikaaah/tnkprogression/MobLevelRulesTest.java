package dev.mikaaah.tnkprogression;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MobLevelRulesTest {
 @Test void everyPlayerLevelStaysWithinFourAndGlobalBounds(){
  for(int level=1;level<=100;level++)for(int offset=-4;offset<=4;offset++){
   int mob=MobLevelRules.withOffset(level,offset);
   assertTrue(mob>=1 && mob<=100 && Math.abs(mob-level)<=4);
  }
 }
 @Test void middleLevelsHaveAllNineOutcomes(){
  var levels=new java.util.HashSet<Integer>();
  for(int offset=-4;offset<=4;offset++)levels.add(MobLevelRules.withOffset(50,offset));
  assertEquals(java.util.Set.of(46,47,48,49,50,51,52,53,54),levels);
 }
 @Test void boundaryRollsClampRatherThanWrap(){
  assertEquals(1,MobLevelRules.withOffset(1,-4));
  assertEquals(5,MobLevelRules.withOffset(1,4));
  assertEquals(96,MobLevelRules.withOffset(100,-4));
  assertEquals(100,MobLevelRules.withOffset(100,4));
 }
}
