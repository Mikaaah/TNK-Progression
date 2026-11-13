package dev.mikaaah.tnkprogression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbyssRulesTest {
    @Test void allFloorsAreFixedRegardlessOfPlayers() {
        int[] levels={50,60,70,80,90,100};
        for(int target:levels) {
            assertEquals(target,AbyssRules.level(target,false,110));
            assertEquals(Math.min(110,target+10),AbyssRules.level(target,true,1));
        }
    }
    @Test void bossMinimumAndCombatCapAreSeparateFromRewardLevel() {
        assertEquals(75,AbyssRules.level(50,true,75));
        assertEquals(110,AbyssRules.level(100,true,110));
        assertEquals(100,AbyssRules.level(100,false,110));
        assertEquals(110,AbyssRules.level(90,true,110));
    }
    @Test void bossesStayInSpiralsActualSlots() {
        for(int floor=1;floor<=6;floor++)for(int wave=1;wave<=3;wave++)
            assertEquals(floor==5&&wave==3 || floor==6&&wave>=2,AbyssRules.bossSlot(floor,wave));
        assertFalse(AbyssRules.bossSlot(6,4));
    }
    @Test void invalidAndEndlessContextsAreRejected() {
        assertFalse(AbyssRules.normalFloor(0));assertFalse(AbyssRules.normalFloor(7));
        assertFalse(AbyssRules.partyScale(Double.NaN));assertFalse(AbyssRules.partyScale(Double.POSITIVE_INFINITY));
        assertFalse(AbyssRules.partyScale(0));assertFalse(AbyssRules.partyScale(129));
        assertTrue(AbyssRules.partyScale(1));assertTrue(AbyssRules.partyScale(1.75));
    }
}
