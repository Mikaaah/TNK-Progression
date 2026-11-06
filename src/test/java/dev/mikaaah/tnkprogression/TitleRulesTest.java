package dev.mikaaah.tnkprogression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class TitleRulesTest {
 @Test void onlyStaffAreCosmeticOnly(){assertTrue(SpecialTitles.commandOnly("tnk:owner"));assertTrue(SpecialTitles.commandOnly("tnk:admin"));assertFalse(SpecialTitles.commandOnly("tnk:beta_tester"));assertFalse(SpecialTitles.cosmeticOnly("tnk:beta_tester"));assertTrue(SpecialTitles.cosmeticOnly("tnk:owner"));assertFalse(SpecialTitles.cosmeticOnly("tnk:ars_mystic"));}
 @Test void staffCannotBecomeAutomaticThroughCatalogEdits(){var d=new TitleConfig.Definition("tnk:owner","Owner","OWNER","ZŁOTO","FFFFFF","Command-only staff title.","tnk:character",1,List.of("first"));assertFalse(TitleConfig.qualifies(d,100,Set.of("first")));}
 private TitleConfig.Definition definition(String category,int spent,List<String> skills){return new TitleConfig.Definition("tnk:test","Test","EXPERT","ZŁOTO","FFFFFF","An earned title.",category,spent,skills);}
 @Test void pointsAndNodesAreBothRequired(){var d=definition("tnk:character",40,List.of("first","last"));assertFalse(TitleConfig.qualifies(d,100,Set.of("first")));assertFalse(TitleConfig.qualifies(d,39,Set.of("first","last")));assertTrue(TitleConfig.qualifies(d,40,Set.of("first","last")));}
 @Test void questOnlyTitlesAreNeverAutoGranted(){assertFalse(TitleConfig.qualifies(definition("",0,List.of()),100,Set.of()));assertFalse(TitleConfig.qualifies(definition("tnk:character",0,List.of()),100,Set.of()));}
 @Test void unsafeOrUnreadableLabelsAreRejected(){assertTrue(TitleConfig.validLabel("Mystic",40));assertFalse(TitleConfig.validLabel("Mystic\nAdmin",40));assertFalse(TitleConfig.validLabel("§cAdmin",40));assertFalse(TitleConfig.validLabel(" ",40));assertFalse(TitleConfig.validLabel("long".repeat(20),40));}
}
