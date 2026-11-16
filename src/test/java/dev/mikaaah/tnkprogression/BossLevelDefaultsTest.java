package dev.mikaaah.tnkprogression;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BossLevelDefaultsTest {
    @Test void allEyeBossMinimumsStayBelow60AndDragonIs75() {
        var levels = BossLevelDefaults.levels();
        for (String id : java.util.List.of("block_factorys_bosses:sandworm", "cataclysm:ignis",
                "twilightforest:snow_queen", "legendary_monsters:overgrown_colossus",
                "cataclysm:the_harbinger", "archaion:last_of_deepslate", "alexscaves:luxtructosaurus",
                "mowziesmobs:ferrous_wroughtnaut", "minecraft:wither", "aether:valkyrie_queen",
                "ars_nouveau:wilden_boss", "cataclysm:maledictus", "cataclysm:the_leviathan",
                "cataclysm:ancient_remnant", "block_factorys_bosses:underworld_knight",
                "legendary_monsters:posessed_paladin")) assertTrue(levels.get(id) < 60, id);
        assertEquals(75, levels.get("minecraft:ender_dragon"));
    }
    @Test void requestedLevelsAndMinibossBounds() {
        var levels = BossLevelDefaults.levels();
        assertEquals(35, levels.get("minecraft:warden"));
        assertEquals(30, levels.get("cataclysm:clawdian"));
        assertEquals(45, levels.get("block_factorys_bosses:kraken"));
        assertEquals(55, levels.get("block_factorys_bosses:underworld_knight"));
        assertEquals(75, levels.get("minecraft:ender_dragon"));
        assertEquals(30, levels.get("alexscaves:magnetron"));
        assertEquals(20, levels.get("alexscaves:brainiac"));
        for (String id : BossLevelDefaults.minibosses()) {
            assertNotNull(levels.get(id), id);
            assertTrue(levels.get(id) <= 35, id);
        }
        Map.of("ender_golem", "ender_guardian", "coralssus", "the_leviathan",
                "ignited_revenant", "ignis", "kobolediator", "ancient_remnant",
                "wadjet", "ancient_remnant", "aptrgangr", "maledictus",
                "the_prowler", "the_harbinger", "clawdian", "scylla")
                .forEach((mini, boss) -> assertTrue(levels.get("cataclysm:" + mini) * 2
                        <= levels.get("cataclysm:" + boss), mini));
    }

    @Test void migrationUpdatesPreviousDefaultsAndPreservesCustomOrRemovedEntries() {
        var configured = new HashMap<>(Map.of("minecraft:warden", 75,
                "cataclysm:clawdian", 50, "alexscaves:brainiac", 12,
                "other:custom_boss", 88, "minecraft:wither", 45));
        BossLevelDefaults.migrate(configured);
        assertEquals(35, configured.get("minecraft:warden"));
        assertEquals(30, configured.get("cataclysm:clawdian"));
        assertEquals(12, configured.get("alexscaves:brainiac"));
        assertEquals(88, configured.get("other:custom_boss"));
        assertEquals(45, configured.get("minecraft:wither"));
        assertFalse(configured.containsKey("block_factorys_bosses:kraken"));
        assertEquals(55, configured.get("archaion:last_of_deepslate"));
        var once = new HashMap<>(configured);
        BossLevelDefaults.migrate(configured);
        assertEquals(once, configured);
    }

    @Test void nativeMinibossesStopAt100WhileFullBossesReach110() {
        assertEquals(35, MobLevelRules.minibossLevel(35, 1));
        assertEquals(60, MobLevelRules.minibossLevel(30, 50));
        assertEquals(100, MobLevelRules.minibossLevel(35, 100));
        assertEquals(100, MobLevelRules.minibossLevel(110, Integer.MAX_VALUE));
        assertEquals(110, MobLevelRules.bossLevel(75, 100));
    }

    @Test void invadersStayExactlyFiveAbovePlayersUntilThe100Cap() {
        for (int player = 1; player <= 100; player++)
            assertEquals(Math.min(100, player + 5), MobLevelRules.invaderLevel(player));
        assertEquals(100, MobLevelRules.invaderLevel(Integer.MAX_VALUE));
    }
}
