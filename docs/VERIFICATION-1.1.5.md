# Progression 1.1.5 validation

Validated 16 November 2026. Source baseline: `balance/boss-minimums` at `b4a3696ccf9df0f920cb9357e32baef214dba56c`. Title work is isolated on `fix/title-unlocks`.

## Compilation

Minecraft **1.21.1**, NeoForge **21.1.262**, Java **21** (Zulu JDK **21.0.13.0.3**), Gradle **8.14**, ModDevGradle **2.0.148**. `build runServer -PtnkTitleCleanupSmoke --console=plain` passed with cached dependencies.

Artifact: `tnkprogression-1.1.5-neoforge-1.21.1.jar` (2361447 bytes).

SHA-256: `8358c37029c73781501bb771ae2507b7578ac2e42bd3c58d8e4a947c06f84d56`.

Packaged classes match the final tested dedicated-server classes. Progression API, combat XP, boss balance and Spiral integration production classes remain byte-identical to 1.1.4.

## Automated checks

**50 JUnit tests passed, zero failures.** Added coverage checks 42 unique active names, retention of 16 legacy definitions, normalization/archiving of duplicate custom names without changing their definitions, ten skill-only automatic titles, all 29 quest rewards remaining non-automatic, real node purchase requirements, full skill-point progress text and explicit quest/staff/legacy acquisition sources.

Both Node fixtures pass: `tools/check_title_bindings.cjs` and `tools/check_default_titles.cjs`. They cover the existing optional script logic and staff default roster. They do not install scripts or bind quests; no actual quest files or reward bindings were changed.

The build validates all 100 level badges, 58 retained title badges and 20 origin icons. The optional prefix packaging tool also generated and byte-verified all 58 title sprites from the supplied original artwork archive, including archived badges.

## Actual dedicated-server checks

A real Minecraft/NeoForge development dedicated server ran with Progression and its loader dependencies, using the opt-in `TitleCleanupSmoke` fixture. The final log contains `TNK_TITLE_CLEANUP_SMOKE_OK`.

Checks passed for 42 unique active definitions and 16 archived definitions; only owned archived badges appearing in snapshots; original retired +8% breaking-speed passive; saved cosmetic and passive slots; ownership, level and XP NBT persistence; full snapshot JSON roundtrip; reload safety; actual Brigadier quest earn commands, duplicate-command idempotency and revocation; staff-only/cosmetic Owner behavior; and no automatic quest/boss grants.

An old `wayfarer_10` requirement with an already-migrated `puffish_skills:character` category was injected into a disposable config. Reload repaired it to the actual `hunter_subclass_1_mastery` node. A customized quest description survived the same reload. The fixture restored its config afterward.

## Artwork and manual quest commands

Owner now uses the supplied original `nexo/pack/assets/generated/textures/glyphs/NUSSE-OWNER-PODSTAWOWY.png` from `advancedpack.zip`. The PNG was visually inspected and copied unchanged, and the packaged bytes match the source exactly. Its provenance/hash entry and default style were updated together. No synthetic or recoloured substitute is used.

FTB Quests' `{p}` placeholder and permission-level behavior were checked against the official 1.21.1/main source at `d76327d69f9392961746c6e3fd6e976b88308ef8`, `CommandReward.claim()`. The command reference describes intended placements and supplies all 29 commands, without assigning them to quests. Quest/boss titles remain manual; real skill purchases still unlock the ten skill titles automatically.

## Limits

No graphical client, full TNK pack session, real FTB Quests reward claim or live Pufferfish category test was performed for this patch. UI changes compile and reuse the existing character-screen assets/layout. The source PNG was inspected directly. Dedicated-server commands and absent-optional-mod loading passed; the existing optional Pufferfish API code was retained.

Install on clients and server. No player-data or configuration reset is required. The active catalogue migrates once; archived definitions are retained in `config/tnk-title-legacy.json`. All saved title IDs, owned legacy variants, original perks, revocations, selected slots and cooldowns remain supported.
