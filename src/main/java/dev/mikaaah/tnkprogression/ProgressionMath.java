package dev.mikaaah.tnkprogression;
/** Pure balance rules. All thresholds use additional work, not character levels. */
public final class ProgressionMath {
 private ProgressionMath() {}
 public static long levelCost(int level) {var c=BalanceConfig.current;return BalanceConfig.bounded(c.levelBase+c.levelFactor*Math.pow(Math.max(1,level),c.levelExponent));}
 public static long taskCost(long base,long completed){var c=BalanceConfig.current;return BalanceConfig.bounded(base*Math.pow(1+Math.max(0,completed)*c.activityGrowth,c.requirementExponent)+Math.max(0,completed));}
 public static long taskReward(long base,long completed){var c=BalanceConfig.current;return BalanceConfig.bounded(base*Math.pow(1+Math.max(0,completed)*c.activityGrowth,c.rewardExponent));}
 public static int rollTier(int level, int roll) {
  if (level < 1 || level > 100 || roll < 0 || roll >= 100) throw new IllegalArgumentException();
  if (level <= 10) return roll < 75 ? 0 : 1;
  int first = level <= 30 ? 0 : level <= 50 ? 1 : level <= 70 ? 2 : level <= 80 ? 3 : level <= 94 ? 4 : 5;
  return first + (roll < (level >= 95 ? 60 : 50) ? 0 : roll < 95 - (level >= 95 ? 0 : 10) ? 1 : 2);
 }
}
