# Towns & Kingdoms Progression — 0.5.3-beta.1

Minecraft **1.21.1**, NeoForge **21.1.262+**, Java **21**. Original implementation; no Auto Leveling or Just Leveling code is bundled. This is an initial pack beta; tune the balance in a test world.

## Character screen and HUD

Open the survival inventory and click the **player portrait replacing the recipe-book button**, or press **I** (rebind in Controls → Towns & Kingdoms). The warm parchment screen uses colored headers, bold labels, softened corners and activity icons. It shows your live player model, character XP/ceiling, seven activity tracks, milestone journal and title collection. Equip/hide titles and pin an activity directly with buttons. A compact player/XP/activity HUD can be toggled from the screen. Use **Skill Tree** beside the point counter to open the configured Pufferfish category. Closing a screen opened from inventory returns to inventory. No command is needed. Update server and clients together (protocol 4).

**Equipped title perks replace aptitudes:** only one title applies at a time, with a server-enforced 30-second swap cooldown, no stacking and no healing from swaps. Identification discounts are not implemented. The external skilltree also includes bounded conditional abilities such as Second Wind.

See [character HUD, perks, balance and migration](docs/CHARACTER-HUD.md). The numerical release preset and pacing/passive reports are shipped alongside it; full-pack playtesting is still needed to validate the final tuning.

## Progression

Personal character level 1–100. Skill-point entitlement is `level + floor(3 × level / 4)`: 1 point at level 1, 17 at level 10, 87 at level 50 and **175 at level 100**. The same named point source is set absolutely on login/level-up, so existing characters catch up and reconnects/respecs cannot multiply awards. Vanilla XP remains independent. Mining, weighted combat, fishing, farming, audited crafting, engineering and discovery of unique chunks contribute to indefinitely repeatable activity milestones. Their work requirements grow faster than their rewards under the configured release curves. The release curve costs `floor(150 + 45 * level^1.35)` XP per level. These initial values deliberately favor a mixed playstyle; a single repeatable activity can still supply all XP.

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

**The 588-node tree is supplied in `config/puffish_skills/`, outside the mod.** Its native config category is `puffish_skills:character`. All 669 source links, six roots, one-point costs and the 175-point spending limit are editable in `config/puffish_skills/categories/character/`. The JAR contains our icons, frames, warm background and supplied number/title artwork, plus runtime integration code. It contains no Pufferfish category/config JSON or passive-node table. See [the config-folder install guide](skilltree/rebuilt588/README.md).

Merge the release ZIP's `config` folder into the instance/server root and replace the progression JAR in `mods`. Remove the previous JAR and any earlier external `tnk:character` datapack/config to avoid loading an obsolete category. Keep other unrelated Pufferfish categories by merging `categories` in `config/puffish_skills/config.json` if necessary. No resource pack or datapack installation is needed.

Optional bridge targets the public 0.19.2 API on the 1.21 branch. Configure the exact existing tree category ID (default `puffish_skills:character`). Disable that category's native experience and point exchange sources. This mod sets **total earned entitlement**, not unspent points, under `tnkprogression:character_level`, at login and level-up. Respecs and reconnects do not add points. Remove old point sources during migration; this mod does not destroy existing skill data automatically. Missing/API-incompatible integration logs a warning. The category is loaded from the supplied config folder. Iron's Spells, Ars Nouveau, Apothic Attributes and Pufferfish's Attributes bonuses are optional rewards; missing optional mods do not remove route nodes. Runtime conditional passives use reversible native Pufferfish tag rewards.

## Player level badges

The delivered private **0.4.3-beta.1 JAR includes the supplied number-only Advanced Pack artwork by default**. No companion resource pack needs to be enabled. The earlier optional-only packaging allowed Minecraft to display the old bronze `LVL` fallback when the pack was absent; this release corrects that. All 100 number PNGs and 43 title PNGs are copied byte-for-byte, without recolouring or changing pixels. Existing compatible number/title packs may stay enabled, but remove or disable any pack that deliberately overrides the font with old artwork.

Colours remain: 1–9 silver; 10–19 copper; 20–29 gold; 30–39 emerald; 40–49 prismarine; 50–59 diamond; 60–69 lapis; 70–79 amethyst; 80–89 ruby; 90–99 obsidian; 100 quartz white. Level zero is plain `0` because the supplied archive has no zero image. Actual progression starts at 1.

Licensed private artwork is not redistributed in this public source repository. To reproduce the private release, run `./gradlew jar -PtnkArtworkSource=/absolute/path/to/advancedpack.zip`; the release hook embeds and verifies the original sprites after the normal JAR task. Source-only builds without this property retain generic repository artwork. `tools/embed_prefix_artwork.py` can also apply the same resource overlay to an already built JAR without changing any compiled class bytes. `--version` explicitly updates only the mod metadata for a resource-only release. Standard resource-pack overrides still take normal precedence.

`tools/package_number_prefixes.py` and `tools/package_title_prefixes.py` remain available to generate compatible companion resource packs if wanted. Their original source images are unchanged.

See [activity implementation](docs/ACTIVITY-UPDATE.md), [design explanations](docs/DESIGN-EXPLAINED.md), and [quest-rank proposal](docs/QUEST-ROLES.md).

## Earned titles

Collect **43 configurable collectible titles** from quests and actual skilltree achievements. Equip one in chat and overhead names, or hide it without losing it. Titles persist personally across death, team changes and respecs. The first title equips automatically; new rewards preserve an existing selection. Only the equipped title now supplies modest attribute perks; see the character-screen guide. Title ownership does not stack bonuses or supply character XP, skill points or permissions.

- FTB Command Reward (permission 2): `tnkprogress title grant {p} tnk:ars_mystic`
- Player collection: `/tnkprogress title list`; equip with `/tnkprogress title select <id>` or click an earned title; hide with `/tnkprogress title clear`.
- Server catalog: `config/tnk-titles.json`; reload with `/tnkprogress title reload`.
- Optional KubeJS quest bridge includes 24 rules using real current quest IDs, plus inactive templates for chapters/boss quests not present in the source. Pufferfish milestones use actual spent points and purchased nodes.
- The private release JAR includes all 100 number images and 43 title sprites unchanged. Missing artwork uses readable title text. Both server and client must update to the new protocol.

See [full setup, commands and catalog](docs/TITLES.md) for chapter semantics, original-art packaging, permanence and revocation. The old [quest-role proposal](docs/QUEST-ROLES.md) is historical; the title system uses completed chapters as described in the catalog.

## Mob levels

Hostile `Mob` entities implementing vanilla `Enemy` receive a persistent level on first world join, based on the nearest living survival/adventure player's actual character level in the same dimension. A uniformly random offset from −4 through +4 is applied, then clamped to 1–100. Creative, spectator and fake players are ignored. Distance from world spawn, depth, dimension and campaign ceiling do not determine mob level. Distance is used only to select the nearest eligible player. If no eligible player is present in that dimension, the mob receives level 1. All dimensions use this same rule.

`mobLevelVariance` in `config/tnkprogression.json` defaults to 4 (0 gives the exact nearest player's level). Obsolete `blocksPerMobLevel` and `dimensionBaseLevels` settings are removed from the configuration on reload. Existing mobs retain their saved levels, including mobs from the old distance-based build; replacements/new spawns use the new rule. Level stays fixed when players approach, leave, level up, move the mob or reload its chunk. The nearest player wins even if a higher-level player is also nearby; there is no averaging, team-level lookup or highest-level selection.

Nearest-player data is sampled lazily once per dimension per spawning server tick. A bounded 16×16×16 area shortlist cache reuses candidates for spawn bursts while still comparing positions at player boundaries. There are no per-mob tick scans. Login/logout, dimension changes, respawn, supported teleports and earned player level-ups invalidate the relevant snapshots; unload/server-stop releases cached worlds and players. Ordinary movement refreshes by the next spawning tick (at most one tick of position sampling), rather than leaving areas attached to a past player. See [mob performance, despawn tests and Apotheosis comparison](docs/MOB-PERFORMANCE-AND-COMPAT.md).

TNK never calls `setPersistenceRequired`, resets despawn age, or cancels despawn events. The display does not require a generated custom name. Existing name-tag/equipment/mod persistence flags are preserved. Dedicated-server checks verify an ordinary leveled mob still distance-despawns after save/reload and an already persistent mob stays persistent.

Health and melee attack damage use stable modifier IDs so reloads cannot stack modifiers. Unchanged modifiers are reused on chunk rejoin rather than removed/re-added; zero modifiers are omitted. Defaults: +3% base health and +1.25% base damage per level above 1. A centered original number badge renders **above** the mob name when targeted, or whenever its name is normally always visible. The two lines use depth testing and respect F1, invisibility and team name visibility. Existing custom/affixed names retain their content and styling. Exact old TNK-generated `Lv. N <type>` names migrate on chunk load without rerolling levels. A small server-to-client payload sends the saved level when tracking starts; there is no mob tick scan or global client entity cache. Apotheosis attributes/equipment and World Tiers remain independent; their bonuses can combine with TNK's scaling.

Loot API uses `min(mob level, recipient milestone ceiling)`. Difficulty itself is not reduced by the recipient's progression. Custom creatures without the `Enemy` marker still need explicit pack integration. Combat credits recent player damage, including owned entities/projectiles and the optional Iron's Spells IMagicSummon integration. It does not globally increase vanilla XP.

## Persistence and beta limitations

World saved data: `tnkprogression.dat`. Profiles, claims, team gates, unique chunks and activity work remain compatible with beta.2. Existing mining placement records are honored; a persistent bitset per touched block section prevents recycled positions, generated minerals and piston-moved minerals from paying new mining work. Only real survival players earn activity work.

Fishing now uses validated catch events rather than statistic deltas. Farming counts mature broken crops and observed successful right-click harvests. Crafting/engineering use audited recipe IDs, not inventory pickup or factory output counts. See `docs/ACTIVITY-UPDATE.md` for exact coverage and limitations. This is broad protection against known reward loops, not a guarantee of detecting every client macro or arbitrary mod's block movement.

Existing worlds begin at character level 1. Back up before installation. No automatic conversion of the old 150-point tree or old loot tokens is performed. Legacy unsealed loot tokens fall back to Common in the TNK loot beta rather than upgrading when identified later.

## Build / validation

`./gradlew build` with Java 21. Run dedicated-server assertions in a **disposable world only** using `-Dtnkprogression.smoke=true`; they create a test profile, validate caps, quest claims and save roundtrip, then stop the server. See the workflow. Runtime integration with FTB Teams and the installed Pufferfish version must be checked in the pack before publishing.
