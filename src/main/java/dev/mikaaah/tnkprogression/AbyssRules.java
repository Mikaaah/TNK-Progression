package dev.mikaaah.tnkprogression;

/** Rules for normal floors only. Endless is deliberately outside this integration. */
public final class AbyssRules {
    private AbyssRules() {}
    public static boolean normalFloor(int floor) { return floor >= 1 && floor <= 6; }
    public static boolean bossSlot(int floor, int wave) {
        return floor == 5 && wave == 3 || floor == 6 && (wave == 2 || wave == 3);
    }
    public static int level(int target, boolean boss, int minimum) {
        return boss ? Math.min(110, Math.max(target + 10, minimum)) : Math.clamp(target, 1, 100);
    }
    public static boolean partyScale(double value) { return Double.isFinite(value) && value >= 1 && value <= 128; }
}
