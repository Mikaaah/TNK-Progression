package dev.mikaaah.tnkprogression;

/** Pure balance rules. No daily/lifetime cap on legitimate activity. */
public final class ActivityRules {
    private ActivityRules() {}
    public static double combatWeight(double health, double attack, int level) {
        if (!Double.isFinite(health) || !Double.isFinite(attack)) return 0;
        return Math.clamp(Math.sqrt(Math.max(1, health) / 20.0)
            * (0.75 + Math.max(0, attack) / 12.0) * (1 + Math.clamp(level, 1, 100) / 100.0), 0.25, 20);
    }
    public static double combatShare(double weight,double damage,double total,double health) {
        if(!Double.isFinite(weight) || !Double.isFinite(damage) || !Double.isFinite(total) || !Double.isFinite(health) || damage<=0)return 0;
        return weight*Math.clamp(damage/Math.max(1,Math.max(total,health)),0,1);
    }
    /** Repeated local kills become less productive, but never stop progression. */
    public static double repetition(long recent) { return 1.0 / Math.sqrt(1 + Math.max(0, recent) / 8.0); }
    public static boolean elapsed(long now, long previous, long delay) {
        return previous < 0 || now < previous || now - previous >= delay;
    }
}
