package dev.mikaaah.tnkprogression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class TitleRulesTest {
 private TitleConfig.Definition definition(String category,int spent,List<String> skills){return new TitleConfig.Definition("tnk:test","Test","EXPERT","ZŁOTO","FFFFFF","An earned title.",category,spent,skills);}
 @Test void pointsAndNodesAreBothRequired(){var d=definition("tnk:character",40,List.of("first","last"));assertFalse(TitleConfig.qualifies(d,100,Set.of("first")));assertFalse(TitleConfig.qualifies(d,39,Set.of("first","last")));assertTrue(TitleConfig.qualifies(d,40,Set.of("first","last")));}
 @Test void questOnlyTitlesAreNeverAutoGranted(){assertFalse(TitleConfig.qualifies(definition("",0,List.of()),100,Set.of()));assertFalse(TitleConfig.qualifies(definition("tnk:character",0,List.of()),100,Set.of()));}
 @Test void unsafeOrUnreadableLabelsAreRejected(){assertTrue(TitleConfig.validLabel("Mystic",40));assertFalse(TitleConfig.validLabel("Mystic\nAdmin",40));assertFalse(TitleConfig.validLabel("§cAdmin",40));assertFalse(TitleConfig.validLabel(" ",40));assertFalse(TitleConfig.validLabel("long".repeat(20),40));}
}
