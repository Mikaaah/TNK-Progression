# TPS, allocation and packet review

Baseline: Progression 1.1.5 source commit `a01e74172cb266896cb1ee85d508560a5660eaad`, NeoForge 21.1.262 / Minecraft 1.21.1 / Java 21. Review is read-only for production Java; fixes are assigned to the relevant owners. Findings below describe the baseline, not a claim that every path remains unchanged in the final audit build.

## High-value verified hotspots

### 1. Periodic character snapshots reconstruct immutable help and whole skill snapshots

`CharacterState.java:24–47,62` builds a state every player-second, even with the HUD hidden and no menu open. All seven `ActivityHelp.describe()` calls run before the last-JSON comparison. They create 70 help strings and sort four default config streams every time. `SkillBridge.titleSnapshot()` also enumerates all unlocked nodes when the periodic view needs only the spent-point count. Rebuilding and serializing the same static information still consumes CPU/allocation when the equality guard ultimately prevents a packet.

The normal partial message contains all static activity help while omitting only titles. `sendFull()` stores full-title JSON in the same `LAST` cache, so the next periodic empty-title JSON must differ and is emitted once unnecessarily. During a passive swap cooldown, the countdown changes each second and repeatedly sends the help strings.

Root is addressing this with lean periodic activity rows, client merging of retained full help, a lightweight spent-point query, and separate periodic/full deduplication. Full/help refresh must still work after config changes and origin/title state changes.

### 2. Campfire recipe work executes every lit tick, including empty/unowned slots

`CookingCampfireMixin.java:18–19` snapshots inputs and calls `CookingXp.campfireResults()` at every `cookTick` HEAD. `CookingXp.java:50–51` copies all four slots and performs four `RecipeManager.getRecipeFor()` calls plus result assembly when a recipe exists. This is **80 added recipe lookups/second per lit campfire** at 20 TPS, including empty decorative campfires. One hundred such campfires produce 8,000 lookups/s from this integration before vanilla's work.

XP agent is changing this to completion-candidate/owned-slot work. This is a direct algorithmic reduction, not a measured whole-pack TPS gain.

### 3. Harvest cooldown cleanup becomes quadratic with activity

`ActivityEvents.java:89–94` clones/scans the complete global `harvest_times` set for every harvest once its size exceeds 4096; a player's tick multiple of 100 also triggers it for every harvest in that tick. The threshold is a cleanup trigger, not a bound on unexpired cooldown entries. Cost is O(active cooldown positions × harvest rate) when large farms or multiple players keep many entries live.

XP agent was notified to amortize expiry pruning while retaining direct cooldown checks and never evicting unexpired anti-farm receipts.

### 4. Idle exploration repeats structure sampling and dirties global saved data

`ExplorationXp.java:18–33` queries biome/structure state every second, even while stationary, and unconditionally marks `ProgressionData` dirty. The same SavedData contains all players and mining provenance. This does not write the file every second, but keeps it eligible for whole-data autosaves even when no receipt changed.

XP agent is avoiding already-surveyed stationary sampling and setting dirty only on actual persistent mutations, while preserving the first eligible follow-up sample after login/teleport.

## Request and event-level amplification

- `CharacterAction.java:19–22` permits a full refresh every two ticks, up to ten full catalogue constructions/responses per second per requesting client. Root is separating expensive refresh throttling from responsive interaction actions. This is a bounded CPU/network amplification path, not an XP/permission bypass.
- `LevelBadges.login()` calls `changed()` → `appearanceChanged()` → `sendFull()`; `CharacterState.login()` sends another full state in the same login event. Root is eliminating redundant initial work. `Origins.login()` only applies modifiers and does not independently send a full state.
- Same-tick deduplication must compare content, not drop every later send by tick alone: a new origin-selection nonce or title change must still reach the client.
- Pufferfish method binding was repeatedly looked up by `SkillBridge`; the skills agent is binding methods once while resolving current categories each call. A cheap spent-point query avoids an entire skill stream for periodic UI state. Retained snapshots must not hide respec/import/reload mutations.
- `SourceTreeBonuses.refresh()` runs every five player ticks and re-parses source JSON, registry IDs and conditions for owned bonuses while creating a new desired-modifier map. The shipped table has 141 skill keys and 140 actual bonus entries (78 attribute, 55 damage, 7 breaking-speed). The skills agent is compiling reusable definitions; the existing periodic condition checks should remain responsive and not be globally slowed to manufacture a TPS result.
- Starcatcher reflection is per completed catch, not per tick; XP agent is caching verified bindings without making it a required dependency.

## Quantified standalone fixture

See `tools/audit_snapshot_benchmark/README.md`, its Java/Python harness, and `audit-notes/snapshot-benchmark-baseline.txt` for exact scenario and reproduction.

Using the actual published 1.1.5 helper/record implementation and Gson 2.10.1 in a standalone Java 21 process:

| Constructed message | Raw UTF-8 JSON bytes |
| --- | ---: |
| Full 42-title fixture | 30,676 |
| Current periodic partial, including help | 7,174 |
| Same partial, static help omitted | 915 |

The lean body is **87.25% smaller** before Minecraft packet framing/compression. This is not a measured compressed network reduction. It applies to emitted partials; unchanged baseline transmissions are already suppressed after serialization.

With 2,000 warmups/scenario and three alternating 10,000-iteration rounds:

| Scenario | Round times, microseconds/fixture | Stable allocated bytes/fixture |
| --- | --- | ---: |
| Current helper construction + partial serialization | 78.298 / 36.631 / 36.958 | 43,584 |
| Lean partial construction + serialization | 9.103 / 8.329 / 9.873 | 7,224 |

This fixture removes **83.43% of measured allocations**. The wrapper uses reflection and does not run the complete CharacterState/world/packet/client path. Timing is noisy and includes host/JIT/GC effects; no fixed production speedup or server TPS improvement is claimed.

## Bounded or intentional structures

- `MobLevels` scans a dimension's player list only on first demand in a server tick, caches player position/level snapshots and uses `NearbyPlayerIndex`. There is no continuous entity/world scan.
- `NearbyPlayerIndex` caches candidate lists by 16-block cube, bounded at 2048 cells per snapshot. First encounter with a cell is two O(players) passes; repeated queries use the exact candidate subset. Zero/one-player paths are cheap. Its worst case remains O(players) for clustered candidates, but replacing it with a more complex tree is not justified without a representative spawn benchmark.
- Combat contribution ledgers cap entities at 64 attackers and expire stale shares. Cooking ownership queues are capped at 64 entries. Recent XP history caps at eight messages. These are not unbounded player/entity caches.
- Runtime player caches generally use weak keys plus lifecycle cleanup. Mounted entities can reference their passengers, but explicit logout/server-stop cleanup and per-tick invalidation already protect the mount cache's normal lifecycle.
- Client UUID → level/title maps retain unique players for the duration of a connection and clear on local disconnect. That is a low-priority client-session retention risk on very long-lived/high-churn sessions, not a server TPS leak. A safe prune would need to retain tab-list players and currently tracked player entities; no new packet/protocol or untested pruning behavior was introduced by this review.

## Persistent history is not safe to evict

Mining provenance uses 4096-bit (512-byte) section bitsets; exploration uses 1024-bit (128-byte) region bitsets per player. Visited-world history, unique reward receipts, and natural-block tombstones necessarily grow with world/player activity. They are persistent anti-exploit state, not expired transient caches. Evicting them to make memory appear bounded would re-enable repeat rewards. Large long-lived worlds still require ordinary save-size monitoring; a future regionized storage migration would be a separate engineering task with explicit save compatibility testing.

## Limits

No Spark/JFR recording, full pack multiplayer TPS run, recipe-manager workload benchmark or compressed packet capture was performed by this review. The microbenchmark is a reproducible component fixture. Root and the owning agents are responsible for validating final production edits with the integrated build/server harnesses. Baseline source line references can shift in the fixed branch.
