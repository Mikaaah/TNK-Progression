# Progression 1.1.4 — focused validation

Validated 16 November 2026. Source baseline: `publish/curseforge-1.1.3` at `88c651391874e7c575b7b34ce79eafb01ca79fc1`. Changes are isolated on `balance/boss-minimums`.

## Build

- Minecraft **1.21.1**, NeoForge **21.1.262**, Java **21** (Zulu JDK **21.0.13.0.3**), Gradle **8.14**, ModDevGradle **2.0.148**.
- `build` completed successfully. Final `build runServer -PtnkBossActivityServerSmoke --console=plain` also completed successfully in offline dependency mode.
- Output: `tnkprogression-1.1.4-neoforge-1.21.1.jar`.
- SHA-256: `fa480755051beb0ee9087b17b48c3cfdfdea9c56b358df9d6625398e5200e63e`.
- Packaged production classes match the classes used by the final dedicated-server run. The two Spiral callback mixin classes from the verified 1.1.3 artifact are preserved byte-for-byte.

## Automated checks

**46 JUnit tests passed, zero failures.** New coverage checks all 16 eye-boss default minimums below 60, Dragon minimum 75, requested individual levels, every shipped miniboss minimum at most 35, associated Cataclysm miniboss minimums at most half their boss minimum, native miniboss cap 100 versus full-boss cap 110, and exact invader player +5 across all player levels 1–100. Migration checks cover unchanged old defaults, custom overrides, removed old entries, the newly supported Archaion entry and repeat application.

Existing level rules, nearby-player indexing, Abyss rules, progression math, origins, activity guards, titles and skill passive tests also pass. The normal build's bundled artwork checks pass.

## Actual dedicated-server checks

Started a real Minecraft 1.21.1 / NeoForge 21.1.262 development dedicated server in a disposable test world, with Progression and its loader dependencies. The opt-in server fixture used server-side synthetic players and actual Minecraft mobs and NBT.

The final log contains `TNK_BOSS_ACTIVITY_SMOKE_OK`. Checks passed for:

- Full-boss minimums and player +10 without variance; full bosses reach 110.
- Apotheosis elite pre-insertion registry-id marker and invader boolean marker: neither inherits a configured native boss minimum 65 at player level 1; the invader is exactly level 6.
- Native minibosses stop at 100; saved legacy miniboss levels 110 clamp to 100 without healing.
- A serialized/reloaded miniboss retains level 100, current health 7 and identical maximum health; repeated joins do not stack modifiers.
- Existing full bosses retain saved level and damage across rejoins, including the previous one-time boss migration.
- Non-Enemy configured bosses, level-110 API/badge/network roundtrip, and existing activity-help snapshots.

Apotheosis was **not installed** in this server run. Its two real marker formats were simulated, using the inspected upstream spawn and delayed elite-initialization implementation. This confirms Progression's handling and safe loading without the optional mod; it does not constitute a full Apotheosis gameplay test.

## Inspected integration sources

| Dependency | Evidence |
| --- | --- |
| Apotheosis 1.21 branch | Commit `84e27e7031c7e2ab2f2e8328daf491c88203fcbc`; `ApothMobEvents`, `Invader` and `Elite` spawn markers and delayed initialization |
| Legendary Monsters 1.21.1 | Commit `f412a3e5a583705502dd924aee9a0d672a607e76`; registered classes distinguish native minibosses from current full Possessed Paladin, Cloud Golem and Obliterator |
| Archaion 1.21.1-1.4.4 | Author JAR file 8983496; `ACEntityTypes` registration and language resources confirm `archaion:last_of_deepslate`; JAR SHA-256 `15431063b27f43aa5321100bc29acc9b49ab0c97e24e16627a759a0312ea0f11` |
| Spiral Abyss 2.0.5/2.0.6 | Existing verified 1.1.3 compatibility mixin binaries preserved exactly; no new Spiral JAR runtime test performed for this balance patch |
| Unidentified Loot | Reward and Progression API classes unchanged; no new Loot JAR runtime test performed for this balance patch |

## Limits and installation

No graphical client, manual combat session or complete TNK modpack test was performed. Actual Apotheosis random spawns and the complete optional-mod combinations were not rerun. This patch changes TNK level assignment and defaults; it retains Apotheosis's own attribute bonuses and spawn frequency.

Install the new JAR on clients and server, replacing the older Progression JAR. Configuration migrates once, without a reset. Existing saved enemy levels remain assigned, except non-arena miniboss values above 100 clamp at load. In particular, already-saved Wardens at level 75 are not automatically rewritten to 35. Changed base minimums affect new level assignments. Authored native boss minimums are not fixed combat levels: the existing player +10 rule still applies. Verified Spiral arena assignments retain their separate rules.
