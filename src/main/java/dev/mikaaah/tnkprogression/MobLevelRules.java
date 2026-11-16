package dev.mikaaah.tnkprogression;

public final class MobLevelRules {
 private MobLevelRules() {}
 public static int bossLevel(int baseLevel,int playerLevel){return (int)Math.clamp(Math.max((long)baseLevel,(long)playerLevel+10),1,110);}
 public static int minibossLevel(int baseLevel,int playerLevel){return Math.min(100,bossLevel(baseLevel,playerLevel));}
 public static int invaderLevel(int playerLevel){return (int)Math.clamp((long)playerLevel+5,1,100);}
 public static int withOffset(int playerLevel,int offset){return (int)Math.clamp((long)playerLevel+offset,1,100);}
}
