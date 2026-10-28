# Activity update — 0.2.0-beta.1

Minecraft 1.21.1 · NeoForge 21.1.262+ · Java 21. Install the updated progression JAR on server and clients. The TNK Loot JAR does not need changing for this update. Existing public ProgressionAPI method signatures remain compatible. The 240-node skilltree and point entitlement are unchanged.

## Seven activity tracks

| Track | First additional requirement | First character-XP reward | Eligible work |
|---|---:|---:|---|
| Mining | 100 | 25 | Previously unworked natural stone/ores, including `c:ores` |
| Combat | 10 weighted units | 35 | Actual player/owned-entity health damage, shared proportionally |
| Fishing | 1 successful catch | 15 | Real rod catch, hook at least 80 ticks old; open water by default |
| Exploration | 10 | 30 | First visit to a dimension/chunk pair |
| Farming | 40 | 25 | Mature crop breaks or verified harvest state changes |
| Crafting | 30 | 25 | Audited, consuming manual crafting recipes |
| Engineering | 10 | 40 | Audited machine crafting and one-time engineering commissions |

All tracks use the existing repeatable progressive milestone curve, with no lifetime or daily activity cap. XP stops at the character ceiling. Weighted fractional work persists, so a small contribution can accumulate across kills. Fractional work also pauses at the ceiling.

### Farming and fishing

Mature CropBlock subclasses, cocoa and nether wart count when successfully broken with drops by a real player. Right-click attempts are deferred until end of server tick and pay only if the same plant's ripe state became unripe. This covers berries/glow berries and harvest mods that actually reset supported crop age. Shared per-position cooldown defaults to 1,200 ticks (60 seconds at 20 TPS), across all players, to suppress instant bonemeal/replant loops; genuine regrowth can pay again indefinitely. Tree farms, animals and arbitrary custom crop APIs are not inferred automatically.

Fishing gives one work unit per valid catch, regardless of loot stack size. No fish-stat polling remains, so changing statistics or reconnecting cannot duplicate progress. Fake players, cancelled catches, duplicate hook payouts, newly created hooks and narrow non-open-water AFK designs do not pay. Legitimate modded fishing that bypasses ItemFishedEvent needs an adapter. Open-water checks do not distinguish every real-player macro from manual input.

### Crafting and engineering

A crafting event must resolve a real crafting-grid recipe and its actual assembled result. Only explicit recipe IDs in `craftingRecipes` / `engineeringRecipes` pay. The default list includes bread, paper, torches, iron pickaxes/swords, selected Create machines, and the pack's `kubejs:tk3/frames/rotation_manual` and `.../create_millstone` recipes. Items and recipes from absent optional mods simply never match.

A validated action pays its configured work once, independent of output-stack size. Duplicate callbacks and multiple shift-craft callbacks in one tick are coalesced. This intentionally counts conservative crafting actions rather than every item in a batch. Unlisted packing/unpacking, dyeing, repair, smelting collection, machine ticks and inventory pickups pay nothing. Review newly allowed recipes against the pack's salvage/recycling paths before allowing them.

`tnkprogress engineering <player> <objective>` (permission 2) pays a configured commission only once per player, with a persistent claim independent of team changes. Objective weights live in `engineeringObjectives`. Unknown IDs fail. Claims made while capped are consumed without storing XP/work, as with existing quest rewards. There is no unrestricted player-facing XP-report endpoint.

Copy `integration/kubejs/` into the pack's existing `kubejs/` directory to enable chapter commissions. The script uses the current ten COMPLETE CHAPTER quest IDs from TownsAndKingdoms, follows the existing FTBQuests/KubeJS bridge in `tk3_stages.js`, and catches up completed commissions at login. It does not change chapter files, roles or level ceilings. Run in a test pack to verify the installed KubeJS/FTB API and keep IDs aligned when changing the quest book. These commissions complement repeatable manual engineering; automated factories do not produce idle character XP. Further recurring engineering contracts should consume real requested materials and issue server-owned unique receipts, not report arbitrary output counts.

## Combat credit

The target records actual post-mitigation health damage by player UUID for up to 1,200 ticks (60 seconds). Overkill is capped to health before the hit. Credit resolves player attacks, projectiles, vanilla OwnableEntity/TraceableEntity chains, and Iron's Spells IMagicSummon.getSummoner when available. External summon mods can register an owner adapter through `CombatProgression.registerOwnerAdapter`; unknown ownership is never guessed by nearest player.

A single reward pool is weighted by actual maximum health, attack attribute and saved TNK mob level, between 0.25 and 20 combat units. Recent damage determines each player's fraction. The denominator is at least the target's maximum health, so tagging a target then letting lava/fall/machinery finish it does not pay full credit. Healing the target cannot multiply the fixed reward pool. Offline, dead, distant (>128 blocks by default), wrong-dimension, creative/spectator and fake-player recipients do not receive a payout; their damage is not reassigned to others. No final-hit bonus is added.

Repeated kills of the same entity type in the same 64×64 horizontal area receive `1/sqrt(1 + recentKills/8)` within a ten-minute window. This never reaches zero. Moving normally through the world or waiting out the window restores normal weighting. It is a heuristic against stationary natural-spawn farms, not proof that every kill in a location is automated.

Spawner/trial-spawner, egg/dispenser, command, breeding, mob-summoned and triggered targets are excluded by recorded spawn reason. Owned/summoned targets are excluded independently of spawn flags, preventing players farming their own summons. Conversion carries provenance and contributions. Split children are excluded to prevent slime multiplication. A persistent per-entity paid flag rejects duplicate death callbacks. Existing mobs or mods that bypass standard spawn hooks may have no historical spawn reason; it cannot be reconstructed reliably after the fact.

## Mining provenance and anti-farm boundaries

Natural mineral coordinates can pay once. Placement, fluid-created stone, piston source/destination mineral positions and paid natural breaks are remembered persistently in 16³-section bitsets. Old beta.2 placed-block entries remain honored. Silk-touch replacement, pick-up/replant and cobblestone-regeneration at a used coordinate cannot erase its record. Moving even natural minerals with pistons conservatively excludes them. Only real successful player breaks with drops count; quarries and fake players do not.

Arbitrary contraptions or world-editing mods that move minerals into entirely new coordinates without emitting these hooks need to call `ActivityGuard.mark(world,pos)`. The mod cannot promise universal provenance across every third-party world mutation. Provenance/visited locations and claim data grow with explored world area and are never evicted to make an old exploit pay again; temporary combat/cooldown windows are cleaned up. Natural mob farms can still earn diminishing credit by design, consistent with uncapped activity progression.

## Configuration and inspection

New fields: `craftingRecipes`, `engineeringRecipes`, `engineeringObjectives`, `requireOpenWater`, `excludedSpawns`, `harvestCooldownTicks`, `contributionTicks`, `contributionRange`, `combatWindowTicks`, plus farming/crafting/engineering entries in `activities`. Older activity maps receive the missing three defaults at load. Invalid settings keep the previous configuration. `/tnkprogress reload` reloads and `/tnkprogress activities` shows all seven current milestone counters.
