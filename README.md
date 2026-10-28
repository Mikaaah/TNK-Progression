# Towns & Kingdoms Progression — 0.4.2-beta.1

Minecraft **1.21.1**, NeoForge **21.1.262+**, Java **21**. Original implementation; no Auto Leveling or Just Leveling code is bundled. This is an initial pack beta; tune the balance in a test world.

## Character screen and HUD

Press **I** (rebind in Controls → Towns & Kingdoms). The parchment-and-bronze screen shows your live player model, character XP/ceiling, seven activity tracks, milestone journal and title collection. Equip/hide titles and pin an activity directly with buttons. A compact player/XP/activity HUD can be toggled from the screen. No command is needed.

**Equipped title perks replace aptitudes:** only one title applies at a time, with a server-enforced 30-second swap cooldown, no stacking and no healing from swaps. Identification discounts and conditional abilities are not part of this release.

See [character HUD, perks, balance and migration](docs/CHARACTER-HUD.md). The numerical release preset and pacing/passive reports are shipped alongside it; full-pack playtesting is still needed to validate the final tuning.

## Progression

Personal character level 1–100. Level 1 supplies one skill point, with one more per earned level: 100 total. Vanilla XP remains independent. Mining, weighted combat, fishing, farming, audited crafting, engineering and discovery of unique chunks contribute to indefinitely repeatable activity milestones. Their work requirements grow faster than their rewards under the configured release curves. The release curve costs `floor(150 + 45 * level^1.35)` XP per level. These initial values deliberately favor a mixed playstyle; a single repeatable activity can still supply all XP.

`config/tnkprogression.json` controls activity starting costs/rewards, level ceilings, team sharing, skill category, mob attribute scaling and nearest-player level variance. `/tnkprogress reload` atomically validates and reloads it. Default level ceilings: 10, 30, 50, 70, 80, 94, 100. Default highest eligible rarity by ceiling: Uncommon, Rare, Epic, Legendary, Legendary, Mythic, Mythic. These are starting balance proposals, not fixed campaign decisions.

No XP overflow is banked at a ceiling. Activity reward progress pauses at the ceiling; unique exploration locations are still recorded, so revisiting cannot cash them in later. Quest rewards claimed while capped are consumed without banking. Finish unlock quests before claiming XP rewards.

## Commands and FTB Quests

Use FTB Quests **Command Rewards**, with permission level **2**, executing as the claiming player. `{p}` is FTB's player substitution. Do not use nearest-player selectors.

* One-time campaign XP: `tnkprogress reward {p} tnk:chapter_1_quest_04 250`
* Repeatable quest XP: `tnkprogress xp {p} 100`
* Unlock the next bracket: `tnkprogress unlock {p} 30`
* A world event can issue the same unlock command from the server or KubeJS.
* Personal status: `/tnkprogress status`

Set personal XP rewards to be claimable **by each player**, not a shared reward claimed once for the team. The once-only reward ledger is per player and persists across team changes; keep reward IDs stable and unique. Do not use the once-only command for repeatable quests.

FTB Teams is optional. When installed, milestones are stored by team UUID and immediately apply to current/offline/new members. XP, activity ranks and character levels are **not pooled**. The actor also keeps their personal unlock. Other members borrowing a team unlock do not keep that unlock after leaving. Existing character levels/skills are retained after leaving, but further XP and new loot are constrained by their effective ceiling. Team switching never duplicates quest XP claims.

## Pufferfish's Skills

Optional bridge targets the public 0.19.2 API on the 1.21 branch. Configure the exact existing tree category ID (default `tnk:character`). Disable that category's native experience and point exchange sources. This mod sets **total earned entitlement**, not unspent points, under `tnkprogression:character_level`, at login and level-up. Respecs and reconnects do not add points. Remove old point sources during migration; this mod does not destroy existing skill data automatically. Missing/API-incompatible integration logs a warning. The reduced tree itself must be authored separately from this mod.

## Player level badges

The optional **TNK Number and Title Prefixes 0.3.0** client resource pack replaces the original bronze `LVL` artwork with the user's supplied number-only Advanced Pack images. All 100 images are copied byte-for-byte, without recolouring or modifying pixels. Eleven colour brackets cover 1–9, each ten-level range, and a separate quartz-white 100. No zero image exists in the supplied pack; level 0 uses plain `0` text. Actual progression still starts at 1.

Enable the resource pack above older prefix packs. The mod's existing bundled bronze artwork remains a fallback if that pack is absent. Install the updated mod on server and clients. Authoritative level sync, normal chat and overhead display-name integration are unchanged; other chat/nameplate mods still need client verification. Tab-list prefixes are not enabled.

`python tools/package_number_prefixes.py advancedpack.zip output.zip` generates the private resource pack from the original user-owned input. The supplied artwork is not redistributed in this source repository. The existing `tools/build_level_badges.py` regenerates only the original bronze fallback.

See [activity implementation](docs/ACTIVITY-UPDATE.md), [design explanations](docs/DESIGN-EXPLAINED.md), and [quest-rank proposal](docs/QUEST-ROLES.md).

## Earned titles

Collect **43 configurable collectible titles** from quests and actual skilltree achievements. Equip one in chat and overhead names, or hide it without losing it. Titles persist personally across death, team changes and respecs. The first title equips automatically; new rewards preserve an existing selection. Only the equipped title now supplies modest attribute perks; see the character-screen guide. Title ownership does not stack bonuses or supply character XP, skill points or permissions.

- FTB Command Reward (permission 2): `tnkprogress title grant {p} tnk:ars_mystic`
- Player collection: `/tnkprogress title list`; equip with `/tnkprogress title select <id>` or click an earned title; hide with `/tnkprogress title clear`.
- Server catalog: `config/tnk-titles.json`; reload with `/tnkprogress title reload`.
- Optional KubeJS quest bridge includes 24 rules using real current quest IDs, plus inactive templates for chapters/boss quests not present in the source. Pufferfish milestones use actual spent points and purchased nodes.
- Combined private resource pack copies all 100 number images and 43 title sprites unchanged. Missing artwork uses readable title text. Both server and client must update to the new protocol.

See [full setup, commands and catalog](docs/TITLES.md) for chapter semantics, original-art packaging, permanence and revocation. The old [quest-role proposal](docs/QUEST-ROLES.md) is historical; the title system uses completed chapters as described in the catalog.

## Mob levels

Hostile `Mob` entities implementing vanilla `Enemy` receive a persistent level on first world join, based on the nearest living survival/adventure player's actual character level in the same dimension. A uniformly random offset from −4 through +4 is applied, then clamped to 1–100. Creative, spectator and fake players are ignored. Distance from world spawn, depth, dimension and campaign ceiling do not determine mob level. Distance is used only to select the nearest eligible player. If no eligible player is present in that dimension, the mob receives level 1. All dimensions use this same rule.

`mobLevelVariance` in `config/tnkprogression.json` defaults to 4 (0 gives the exact nearest player's level). Obsolete `blocksPerMobLevel` and `dimensionBaseLevels` settings are removed from the configuration on reload. Existing mobs retain their saved levels, including mobs from the old distance-based build; replacements/new spawns use the new rule. Level stays fixed when players approach, leave, level up, move the mob or reload its chunk. The nearest player wins even if a higher-level player is also nearby; there is no averaging, team-level lookup or highest-level selection.

Nearest-player data is sampled lazily once per dimension per spawning server tick. A bounded 16×16×16 area shortlist cache reuses candidates for spawn bursts while still comparing positions at player boundaries. There are no per-mob tick scans. Login/logout, dimension changes, respawn, supported teleports and earned player level-ups invalidate the relevant snapshots; unload/server-stop releases cached worlds and players. Ordinary movement refreshes by the next spawning tick (at most one tick of position sampling), rather than leaving areas attached to a past player. See [mob performance, despawn tests and Apotheosis comparison](docs/MOB-PERFORMANCE-AND-COMPAT.md).

TNK never calls `setPersistenceRequired`, resets despawn age, or cancels despawn events. The generated custom-name label alone does not enable persistence in Minecraft 1.21.1. Existing name-tag/equipment/mod persistence flags are preserved. Dedicated-server checks verify an ordinary leveled mob still distance-despawns after save/reload and an already persistent mob stays persistent.

Health and melee attack damage use stable modifier IDs so reloads cannot stack modifiers. Unchanged modifiers are reused on chunk rejoin rather than removed/re-added; zero modifiers are omitted. Defaults: +3% base health and +1.25% base damage per level above 1. Display uses a custom name when none existed; no client renderer is required. Existing custom mob names remain unchanged. Apotheosis attributes/equipment and World Tiers remain independent; their bonuses can combine with TNK's scaling.

Loot API uses `min(mob level, recipient milestone ceiling)`. Difficulty itself is not reduced by the recipient's progression. Custom creatures without the `Enemy` marker still need explicit pack integration. Combat credits recent player damage, including owned entities/projectiles and the optional Iron's Spells IMagicSummon integration. It does not globally increase vanilla XP.

## Persistence and beta limitations

World saved data: `tnkprogression.dat`. Profiles, claims, team gates, unique chunks and activity work remain compatible with beta.2. Existing mining placement records are honored; a persistent bitset per touched block section prevents recycled positions, generated minerals and piston-moved minerals from paying new mining work. Only real survival players earn activity work.

Fishing now uses validated catch events rather than statistic deltas. Farming counts mature broken crops and observed successful right-click harvests. Crafting/engineering use audited recipe IDs, not inventory pickup or factory output counts. See `docs/ACTIVITY-UPDATE.md` for exact coverage and limitations. This is broad protection against known reward loops, not a guarantee of detecting every client macro or arbitrary mod's block movement.

Existing worlds begin at character level 1. Back up before installation. No automatic conversion of the old 150-point tree or old loot tokens is performed. Legacy unsealed loot tokens fall back to Common in the TNK loot beta rather than upgrading when identified later.

## Build / validation

`./gradlew build` with Java 21. Run dedicated-server assertions in a **disposable world only** using `-Dtnkprogression.smoke=true`; they create a test profile, validate caps, quest claims and save roundtrip, then stop the server. See the workflow. Runtime integration with FTB Teams and the installed Pufferfish version must be checked in the pack before publishing.
