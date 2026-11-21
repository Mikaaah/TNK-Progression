package dev.mikaaah.tnkprogression;

public final class MobLevelRules {
 private MobLevelRules() {}
 public static int bossLevel(int baseLevel,int playerLevel){return Math.clamp(Math.max(baseLevel,(long)playerLevel+10),1,110);}
 public static int withOffset(int playerLevel,int offset){return Math.clamp((long)playerLevel+offset,1,100);}
}
