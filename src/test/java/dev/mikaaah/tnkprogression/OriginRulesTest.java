package dev.mikaaah.tnkprogression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OriginRulesTest {
    @Test void eightDistinctBackgroundsHaveStableIdsAndVisiblePerks() {
        assertEquals(8, OriginRules.all().size());
        assertEquals(8, OriginRules.all().stream().map(OriginRules.Origin::id).distinct().count());
        for (var origin : OriginRules.all()) {
            assertTrue(origin.id().matches("tnk:[a-z_]+"));
            assertEquals(2, origin.perks().size());
            assertFalse(origin.story().isBlank());
            assertTrue(origin.icon().startsWith("minecraft:"));
        }
        assertTrue(OriginRules.find("tnk:miner").isPresent());
        assertTrue(OriginRules.find("tnk:invented").isEmpty());
        assertTrue(OriginRules.find(null).isEmpty());
    }

    @Test void onlyAnUnchosenPromptedCharacterCanChoose() {
        assertTrue(OriginRules.mayChoose(true, false, "tnk:miner"));
        assertFalse(OriginRules.mayChoose(false, false, "tnk:miner"));
        assertFalse(OriginRules.mayChoose(true, true, "tnk:miner"));
        assertFalse(OriginRules.mayChoose(true, true, "tnk:librarian"));
        assertFalse(OriginRules.mayChoose(true, false, ""));
        assertFalse(OriginRules.mayChoose(true, false, "tnk:invented"));
    }

    @Test void vanillaXpCurveMatchesThresholdsAndHandlesExtremeLevels() {
        assertEquals(0, OriginRules.pointsAtLevel(0));
        assertEquals(7, OriginRules.pointsAtLevel(1));
        assertEquals(352, OriginRules.pointsAtLevel(16));
        assertEquals(394, OriginRules.pointsAtLevel(17));
        assertEquals(1395, OriginRules.pointsAtLevel(30));
        assertEquals(1507, OriginRules.pointsAtLevel(31));
        assertEquals(1628, OriginRules.pointsAtLevel(32));
        assertEquals(Integer.MAX_VALUE, OriginRules.pointsAtLevel(Integer.MAX_VALUE));
        assertEquals(0, OriginRules.pointsAtLevel(-50));
    }

    @Test void fractionalCreditsConserveAnExactLongTermDiscount() {
        double carry = 0;
        int refunded = 0;
        for (int i = 0; i < 1000; i++) {
            var credit = OriginRules.refund(7, 0.20, carry);
            refunded += credit.points();
            carry = credit.remainder();
            assertTrue(carry >= 0 && carry < 1);
        }
        assertEquals(1400, refunded);
        assertEquals(0, carry, 1e-8);
    }

    @Test void inverseXpCurveHandlesThresholdsAndTheIntegerCap() {
        for (int level : new int[]{0, 1, 16, 17, 30, 31, 32, 100, 1000, 21863}) {
            long points = OriginRules.pointsAtLevel(level);
            assertEquals(level, OriginRules.levelForPoints(points));
            if (level > 0) assertEquals(level - 1, OriginRules.levelForPoints(points - 1));
        }
        assertEquals(0, OriginRules.levelForPoints(-1));
        assertEquals(21863, OriginRules.levelForPoints(Integer.MAX_VALUE));
    }

    @Test void refundsCannotExceedTheActualCostOrAcceptInvalidRates() {
        assertEquals(0, OriginRules.refund(0, .20, .9).points());
        assertEquals(0, OriginRules.refund(-100, .20, 0).points());
        assertEquals(0, OriginRules.refund(100, Double.NaN, 0).points());
        assertEquals(0, OriginRules.refund(100, 2, 0).points());
        assertTrue(OriginRules.refund(1, .20, .99).points() <= 1);
        assertEquals(20, OriginRules.refund(100, .20, Double.NaN).points());
        assertEquals(.20, OriginRules.enchantingRefund("tnk:librarian"));
        assertEquals(0, OriginRules.enchantingRefund("tnk:miner"));
        assertEquals(.20, OriginRules.anvilRefund("tnk:blacksmith"));
        assertEquals(.10, OriginRules.anvilRefund("tnk:librarian"));
    }
}
