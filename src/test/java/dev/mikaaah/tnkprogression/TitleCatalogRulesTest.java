package dev.mikaaah.tnkprogression;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TitleCatalogRulesTest {
    private TitleConfig.Catalog catalog(String name){return new Gson().fromJson(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/data/tnkprogression/"+name)),StandardCharsets.UTF_8),TitleConfig.Catalog.class);}
    @Test void catalogueHas42UniqueNamesAndKeepsAll16LegacyDefinitions(){
        var active=catalog("default_titles.json").titles();
        var legacy=catalog("default_legacy_titles.json").titles();
        assertEquals(42,active.size());assertEquals(16,legacy.size());
        assertEquals(42,active.stream().map(d->d.label().toLowerCase(Locale.ROOT)).distinct().count());
        var ids=new HashSet<String>();
        for(var title:active)assertTrue(ids.add(title.id()));
        for(var title:legacy){assertTrue(ids.add(title.id()));assertTrue(TitleCatalogRules.retiredBuiltin(title.id()));}
    }
    @Test void onlyPurchasedSkillsAutomaticallyUnlockTitles(){
        var active=catalog("default_titles.json").titles();
        assertEquals(10,active.stream().filter(TitleConfig.Definition::automatic).count());
        assertEquals(29,active.stream().filter(d->d.description().startsWith("Quest reward:")).count());
        for(var title:active)if(title.description().startsWith("Quest reward:"))assertFalse(TitleConfig.qualifies(title,1000,Set.copyOf(title.requiredSkills())),title.id());
        var required=active.stream().filter(d->d.id().equals("tnk:skill_wayfarer")).findFirst().orElseThrow();
        assertFalse(TitleConfig.qualifies(required,175,Set.of()));
        assertTrue(TitleConfig.qualifies(required,0,Set.of("hunter_subclass_1_mastery")));
    }
    @Test void duplicateColourVariantsAreArchivedWithoutChangingTheirDefinitions(){
        var original=catalog("default_titles.json").titles().getFirst();
        var variant=new TitleConfig.Definition("custom:variant",original.label().toUpperCase(Locale.ROOT),original.artwork(),original.style(),"AABBCC","Custom unlock description.","",0,List.of());
        var split=TitleCatalogRules.split(List.of(original,variant),List.of());
        assertEquals(List.of(original),split.active());assertEquals(List.of(variant),split.legacy());
        assertEquals(split,TitleCatalogRules.split(split.active(),split.legacy()));
    }
    @Test void fullUnlockInfoShowsSkillProgressAndExplicitQuestRewardSource(){
        var list=catalog("default_titles.json").titles();
        var points=list.stream().filter(d->d.id().equals("tnk:paragon")).findFirst().orElseThrow();
        assertTrue(TitleHelp.describe(points,false,Optional.of(new SkillBridge.TitleSnapshot(75,Set.of()))).stream().anyMatch(s->s.contains("75 / 100")));
        var quest=list.stream().filter(d->d.id().equals("tnk:engineer")).findFirst().orElseThrow();
        assertTrue(quest.description().contains("Chapter III | Copper"));
        assertTrue(TitleHelp.describe(quest,false,Optional.empty()).stream().anyMatch(s->s.contains("command reward")));
        assertTrue(TitleHelp.describe(points,true,Optional.empty()).getFirst().startsWith("Legacy title"));
    }
}
