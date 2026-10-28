# Towns & Kingdoms Progression — 0.2.0-beta.1

Minecraft **1.21.1**, NeoForge **21.1.262+**, Java **21**. Original implementation; no Auto Leveling or Just Leveling code is bundled. This is an initial pack beta; tune the balance in a test world.

## Progression

Personal character level 1–100. Level 1 supplies one skill point, with one more per earned level: 100 total. Vanilla XP remains independent. Mining, weighted combat, fishing, farming, audited crafting, engineering and discovery of unique chunks contribute to indefinitely repeatable activity milestones. Their requirements grow quadratically; their XP rewards grow with the square root of the completed rank. Levels cost `100 + 40 * level²` XP. These initial values deliberately favor a mixed playstyle; a single repeatable activity can still supply all XP.

`config/tnkprogression.json` controls activity starting costs/rewards, level ceilings, team sharing, skill category, mob attribute scaling, distance scaling and dimension baselines. `/tnkprogress reload` atomically validates and reloads it. Default level ceilings: 10, 30, 50, 70, 80, 94, 100. Default highest eligible rarity by ceiling: Uncommon, Rare, Epic, Legendary, Legendary, Mythic, Mythic. These are starting balance proposals, not fixed campaign decisions.

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

The optional **TNK Number Prefixes 0.2.0** client resource pack replaces the original bronze `LVL` artwork with the user's supplied number-only Advanced Pack images. All 100 images are copied byte-for-byte, without recolouring or modifying pixels. Eleven colour brackets cover 1–9, each ten-level range, and a separate quartz-white 100. No zero image exists in the supplied pack; level 0 uses plain `0` text. Actual progression still starts at 1.

Enable the resource pack above older prefix packs. The mod's existing bundled bronze artwork remains a fallback if that pack is absent. Install the updated mod on server and clients. Authoritative level sync, normal chat and overhead display-name integration are unchanged; other chat/nameplate mods still need client verification. Tab-list prefixes are not enabled.

`python tools/package_number_prefixes.py advancedpack.zip output.zip` generates the private resource pack from the original user-owned input. The supplied artwork is not redistributed in this source repository. The existing `tools/build_level_badges.py` regenerates only the original bronze fallback.

See [activity implementation](docs/ACTIVITY-UPDATE.md), [design explanations](docs/DESIGN-EXPLAINED.md), and [quest-rank proposal](docs/QUEST-ROLES.md).

## Mob levels

Hostile `Mob` entities implementing vanilla `Enemy` receive a persistent level at first join. It uses the dimension baseline plus horizontal distance from world spawn and depth below Y48, clamped to 1–100. Defaults: Overworld 1, Nether 31, End 71; +1 level per 256 blocks; +1 per 16 blocks below Y48. It does **not** belong to whichever player triggered spawning. Level is retained on chunk reload and does not change with the killer or nearby players. Health and melee attack damage use stable modifier IDs so reloads cannot stack modifiers. Defaults: +3.5% base health and +1.5% base damage per level above 1. Display uses a custom name when none existed; no client renderer is required. Existing custom mob names remain unchanged.

Loot API uses `min(mob level, recipient milestone ceiling)`. Difficulty itself is not reduced by the recipient's progression. Custom creatures without the `Enemy` marker still need explicit pack integration. Combat credits recent player damage, including owned entities/projectiles and the optional Iron's Spells IMagicSummon integration. It does not globally increase vanilla XP.

## Persistence and beta limitations

World saved data: `tnkprogression.dat`. Profiles, claims, team gates, unique chunks and activity work remain compatible with beta.2. Existing mining placement records are honored; a persistent bitset per touched block section prevents recycled positions, generated minerals and piston-moved minerals from paying new mining work. Only real survival players earn activity work.

Fishing now uses validated catch events rather than statistic deltas. Farming counts mature broken crops and observed successful right-click harvests. Crafting/engineering use audited recipe IDs, not inventory pickup or factory output counts. See `docs/ACTIVITY-UPDATE.md` for exact coverage and limitations. This is broad protection against known reward loops, not a guarantee of detecting every client macro or arbitrary mod's block movement.

Existing worlds begin at character level 1. Back up before installation. No automatic conversion of the old 150-point tree or old loot tokens is performed. Legacy unsealed loot tokens fall back to Common in the TNK loot beta rather than upgrading when identified later.

## Build / validation

`./gradlew build` with Java 21. Run dedicated-server assertions in a **disposable world only** using `-Dtnkprogression.smoke=true`; they create a test profile, validate caps, quest claims and save roundtrip, then stop the server. See the workflow. Runtime integration with FTB Teams and the installed Pufferfish version must be checked in the pack before publishing.
