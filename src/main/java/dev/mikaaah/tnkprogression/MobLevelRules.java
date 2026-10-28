package dev.mikaaah.tnkprogression;

public final class MobLevelRules {
 private MobLevelRules() {}
 public static int withOffset(int playerLevel,int offset){return (int)Math.clamp((long)playerLevel+offset,1,100);}
}
