package dev.mikaaah.tnkprogression;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ActivityRulesTest {
 @Test void harderEnemiesPayMore(){assertTrue(ActivityRules.combatWeight(200,12,90)>ActivityRules.combatWeight(20,3,1));}
 @Test void boundedWeightsAndInvalidValues(){assertEquals(0,ActivityRules.combatWeight(Double.NaN,3,1));assertEquals(20,ActivityRules.combatWeight(1e20,100,100));assertEquals(.25,ActivityRules.combatWeight(1,0,1));}
 @Test void repetitionNeverCapsLegitimateProgress(){assertEquals(1,ActivityRules.repetition(0));assertTrue(ActivityRules.repetition(64)<ActivityRules.repetition(8));assertTrue(ActivityRules.repetition(Long.MAX_VALUE)>0);}
 @Test void sharedKillNeverMultipliesThePool(){assertEquals(5,ActivityRules.combatShare(5,30,100,100)+ActivityRules.combatShare(5,70,100,100),1e-9);assertEquals(.05,ActivityRules.combatShare(5,1,1,100),1e-9);assertEquals(5,ActivityRules.combatShare(5,200,200,100),1e-9);}
 @Test void cooldownSurvivesRestartAndClockRollback(){assertFalse(ActivityRules.elapsed(1200,1000,1200));assertTrue(ActivityRules.elapsed(2200,1000,1200));assertTrue(ActivityRules.elapsed(100,1000,1200));assertTrue(ActivityRules.elapsed(100,-1,1200));}
}
