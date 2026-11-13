# Progression 1.1.1 validation

**13 November 2026 — Minecraft 1.21.1 / NeoForge 21.1.262 / Java 21.**

Artifact: `tnkprogression-1.1.1-neoforge-1.21.1.jar`

SHA-256: `9989095f4c4ce67423e9640ce0d6785b642d67c4b792330b8148a34de60c2937`

Implementation commit: `4ee3624de44ad0bdc50db55947513f6118364e31`

Progression base: `f05d58d55a5a7a8a22d70fea7019048cb75fd94d`.

The production JAR contains **137 runtime-matching class files**, all Java 21 bytecode (major 65). Every packaged class was compared byte-for-byte with the production classes exercised by the integration servers. Verification drivers are excluded from the JAR. [Machine-readable release record](validation-1.1.1/release.json).

## Results

| Check | Result |
|---|---|
| Java 21 compilation and Gradle build | Passed |
| JUnit | 41 passed; zero failures/errors |
| Original direct-XP server assertions | Passed: level/progress migration, Ascension, XP curve, placed-block protection, quest commands, cooking, UI snapshot and ceilings |
| Original boss/activity server assertions | Passed: minimum/+10/110, existing-boss migration, neutral bosses, saved health, badge codec and activity help |
| Progression alone | Passed; optional classes absent |
| Progression + Loot | Passed; 100 ordinary/legacy cap and idempotency cases |
| Progression + Spiral | Passed; real native spawning and damage/scaling paths |
| Progression + both | Passed; arena and authored-reward checks together |
| Both + rotations bridge | Passed, also with Iron's Spells and Iron's Jewelry |
| Interactive client / complete TNK pack | **Not performed** |

These are automated tests inside actual disposable NeoForge dedicated servers, rather than compilation-only or mocked-server claims. The server tests use controlled vanilla encounter entities through Spiral's real `WaveSpawner`/`WaveScaling`, native reinforcement/phase code, entity NBT roundtrips and damage adapters. Test-only arena fixtures set the floor, wave and run rather than completing every arena manually. Actual modded boss AI and graphical rendering require combined-pack playtesting.

Each Spiral combination checks **12 floor/player cases** (six floors with character levels 1 and 100), the three legal full-boss slots, boss minimums and combat cap 110, independent parties, non-Enemy encounters, health exceptions, native-scaling compatibility switches, saved level/modifier/health persistence, reinforcement and phase membership, badge glyph/110 codec, outgoing melee/projectile/spell/area factors, and real contributed kill XP with duplicate-death protection. Save/load means serialized entity NBT reconstructed and joined twice; it does not claim that Spiral resumes an active party attempt after a server restart. Spiral's existing run lifecycle remains unchanged.

Loot regression checks per combined run: **18 floor/chest combinations, 96 saved exact-equipment/identification cases, 900 golden payouts, nine party/chest cases**, paid NPC reveal, native components and fixed bonuses, reforge material/access rules, monthly catalog replacement, preview packet roundtrip and native prize announcements. The bridge run adds **48 actual Iron's Spells helmet checks** and directly verifies **combat level 110 versus authored floor-six source 100**, including a level-1/ceiling-10 recipient. No duplicate item grant or reward listener was introduced.

The bridge test uses the supplied December rotation definitions, with targets changed in memory to 50/55/60/65/70/75 to prove precedence. Its boss candidate lists are substituted with registered vanilla candidates for the isolated test. It is not a full playthrough of the twelve thematic mod rosters. [Reproduction and fixture details](../verification/README.md). [Per-combination results](validation-1.1.1).

## Pinned inputs

- Loot: maintained `compat/tnk-1.1.3-pack-spiral`, **`1fee79b367859b74f6f279ec3c0a0f4e9e2c619e`**. Rechecked during delivery; no newer branch head was available. Used the completed patched JAR, not the default edition.
- Spiral: original **2.0.5 JAR**, API compared with source **`085aba1b98dbc5d4ed9ce28a2e9ee83a9f71b4d1`**. The current upstream branch had advanced to 2.0.6; that incompatible signature was deliberately not used.
- Rotations: **TNK3 bundle 1.0.1**, included **bridge 1.0.0** and bridge source. Its separate `DUNGEON-REWARDS.md` contract was inspected. The bridge has no supplied Git commit; its exact JAR hash is recorded below.
- Build: Gradle 8.14, ModDevGradle 2.0.148, OpenJDK 21.0.13+9. No optional integration JAR is bundled into Progression.

| Runtime input | Exact metadata version |
|---|---|
| spiral_abyss | 2.0.5 |
| atlas_api | 1.21.1-1.2.0 |
| irons_lib | 1.21.1-2.3.0 |
| geckolib | 4.9.3 |
| architectury | 13.0.11 |
| apotheosis | 8.9.0 |
| apothic_attributes | 2.11.0 |
| curios | 9.5.1+1.21.1 |
| patchouli | 1.21.1-93-NEOFORGE |
| placebo | 9.9.4 |
| tnk_abyss_rotations | 1.0.0 |
| tnkloot | 1.1.3 |
| playeranimator | 2.0.4+1.21.1 |
| irons_spellbooks | 1.21.1-3.16.3 |
| irons_jewelry | 1.21.1-2.0.2 |

[Every input filename and SHA-256](validation-1.1.1/dependencies.json).

## Compatibility and limits

Existing public API signatures, player saves, XP/Ascension/title systems, ordinary loot caps and milestone/reforging access remain intact. Only verified normal-floor encounters receive the new arena behavior. Their default regular levels are **50/60/70/80/90/100**, and full bosses use **`min(110, max(target + 10, configured minimum))`**. Existing assignments retain their snapshot through monthly changes. Authored rewards remain Loot's exact saved result and never borrow a boss's combat level or a later recipient's cap.

TNK is the default arena health/damage baseline, with party scaling once. Spiral's competing normal-floor health multipliers and absolute boss-health targets are replaced; `SkipRaisingHealth` retains native HP. Arena damage uses one outgoing TNK factor across attributable attack types, so the attack attribute is not also scaled. Unknown/unattributed damage is left to its owning mod. Full-pack boss balance and custom damage-source ownership still need live pack testing.

[Configuration defaults and installation](ABYSS-COMPATIBILITY.md). Install Progression on clients and server; the existing JSON configuration gains the new `abyss` object automatically. Endless progression, portal access, ordinary world behavior and Spiral's own reward lifecycle were not redesigned.
