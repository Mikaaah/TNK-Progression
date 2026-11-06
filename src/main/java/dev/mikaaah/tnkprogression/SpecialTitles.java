package dev.mikaaah.tnkprogression;

import java.util.Set;

/** Acquisition/display policy without Minecraft, networking or profile state. */
public final class SpecialTitles {
 private static final Set<String> COMMAND_ONLY=Set.of("tnk:owner","tnk:admin");
 private SpecialTitles(){}
 public static boolean commandOnly(String id){return id!=null && COMMAND_ONLY.contains(id);}
 public static boolean cosmeticOnly(String id){return commandOnly(id);}
}
