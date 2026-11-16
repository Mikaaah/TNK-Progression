# Towns & Kingdoms Progression — technical audit 1.1.6

Target: **Minecraft 1.21.1 · NeoForge 21.1.262 · Java 21**.

Audit baseline: `a01e74172cb266896cb1ee85d508560a5660eaad` (1.1.5, `fix/title-unlocks`). Work is isolated on `audit/technical-checkup-1.1.6`; the working main branch and its saved-data format are preserved. This report describes source inspection, actual dependency/API inspection, compilation, automated dedicated-server fixtures and their limits separately.

## Findings and fixes

| Area | Verified issue | Resolution |
| --- | --- | --- |
| Character ceilings | Invalid saved personal/team values could expose a ceiling above 100. | Clamp the effective public ceiling to 1–100; retain raw saved milestone data and the configured starting floor. |
| XP accounting | NaN/infinite saved totals or invalid fractional remainders could poison subsequent awards and JSON. | Sanitize accounting/remainders at the award/read boundary without minting XP from invalid values. Reuse the ceiling lookup within a multi-level award. |
| Multi-placement | Secondary fruit snapshots were protected for mining but not farming. | Record farming provenance for every actual placed block snapshot. |
| Deferred harvest | A right-click harvest could pay after death, a dimension change or moving away before confirmation. | Recheck the player, world, distance and actual harvested block at confirmation. |
| Harvest TPS | Large cooldown ledgers were copied/scanned on every harvest. | Amortize expiry sweeps to at most once per 100 game ticks per server; retain all unexpired anti-farm receipts. |
| Exploration TPS | Stationary players repeated structure queries and dirtied global SavedData every second. | Skip already surveyed unchanged positions and dirty only actual persistent changes. Preserve the first eligible post-login/teleport survey. |
| Campfire TPS | Even empty/unowned fires incurred four extra recipe lookups per tick. | Inspect only manually owned slots due to finish on the current tick; empty/unowned fires do no added recipe work. |
| Cooking provenance | Clearing/refilling a campfire could retain an old player's ownership. Empty furnace receipts also lingered. | Clear relevant ownership with contents/non-player insertion and discard exhausted receipts; retain legitimate manual FIFO ownership. |
| Scoped hooks | Furnace-click, potion and Starcatcher HEAD/RETURN context could leak after exceptions; nested potion scopes could erase an outer scope. | Use NeoForge's supplied MixinExtras `WrapMethod` with `finally`, restoring prior context. Failed catches award nothing. |
| Potion bonuses | Finish-event skill reapplication could replace the Origin extension or reintroduce a removed effect. | Apply the two authored factors once to the original drink effect; remove the second raw-potion application. |
| Hunting drops | A delayed owned projectile could construct a bonus drop in the killer's new dimension. | Construct the drop in the victim's world. Preserve components, amount and coordinates. |
| Optional API TPS | Skill, Teams and fishing calls repeatedly resolved reflection bindings. | Cache verified Methods/fields. Query live team membership, categories and skill state; do not cache stale player snapshots. |
| Skill status TPS | Periodic character status enumerated unlocked nodes just to read spent points. | Add the scalar `SkillBridge.spentPoints` query; preserve the richer snapshot API. |
| Conditional passives TPS | Source-bonus JSON/conditions/resource IDs were reinterpreted every refresh/hit. | Compile definitions once and evaluate only current conditions at runtime. Retain existing responsiveness. |
| Passive modifiers | Identical title modifiers were removed/readded and cleanup metadata dirtied repeatedly. | Reconcile stable ResourceLocation modifiers, retain identical instances, clean removed attributes and clamp health downward only. |
| Title choices | Repeated selections rebuilt appearance state; an explicit empty first passive could be lost before a later grant. | Make selections idempotent and persist an explicit empty passive. Keep cosmetic/passive slots independent. |
| Title validation | Duplicate archived IDs were silently overwritten before validation. | Reject duplicates within each supplied list before merging. Preserve intentional saved overrides of bundled legacy definitions. |
| Reload commands | A second-file failure could leave mixed progression/XP or title/perk runtime settings. | Restore the complete prior runtime references on rejection and provide accurate feedback. Migration writes already completed on disk are not a multi-file transaction. |
| Periodic character TPS | Static help was rebuilt/sorted/serialized every player-second, even without an outgoing packet. | Omit static help/catalogue from incremental construction; compare immutable dynamic records before serialization and merge retained detail client-side. |
| Duplicate sync | Login sent duplicate full views; a full view forced another partial update. | Remove the duplicate login sender and establish the dynamic baseline when sending a full view. Same-tick content changes still sync. |
| Refresh amplification | A client could request ten full catalogues per second. | Independently pace full refreshes at one per second and interactive actions at one per two ticks, using transient weak state. |
| Packet size | Valid large reward maps/title requirements could exceed `writeUtf`'s 262,144-character limit. | Budget the final JSON at 240,000 characters. Abbreviate display-only extended help, then verbose descriptions if necessary, with a visible notice. Keep every title ID, ownership, selection, colour and perk availability; do not alter configuration or gameplay. |
| Existing-mob health | Disk-loaded mobs lacking a TNK tag were treated as undamaged fresh spawns. | Assign the level but preserve damaged current health; clamp only above the resulting maximum. |
| Reinforcement health | Late non-Enemy reinforcements/native phases could heal on first TNK assignment. | Distinguish pre-insertion spawn from an already inserted entity with an O(1) ID lookup and preserve transferred health. |
| Arena classification | Copied full-boss metadata could leave ordinary children at 110; native phase flags alone could misclassify a regular root. | Reclassify non-phase children with the saved regular target; use the verified root's actual category for native phases. Established assignments remain unchanged. |
| Arena badges | Late adopted entities could leave already tracking clients with the previous level. | Send the changed saved level to current tracking clients. |
| Arena damage | A RUN tag without a valid DAMAGE multiplier could suppress the party fallback even though native Spiral would not scale it. | Match Spiral's actual finite-positive multiplier predicate. Preserve party scaling exactly once for melee and owned indirect sources. |
| Extreme scaling settings | A finite configured multiplier could still overflow the float damage event. | Saturate computed arena damage at `Float.MAX_VALUE`, preserving zero damage and normal balance. |
| Normal damage TPS | Ordinary-world hits resolved arena ownership twice and allocated sets. | Reject non-arena hits before traversal; retain an eight-link ownership bound without per-hit sets. |
| Malformed mob saves | A nonnumeric `tnk_level` tag prevented assignment indefinitely. | Only numeric tags establish a saved level; repair other tags through the normal path. |
| Reporting | The balance tool still printed retired activity-mastery pacing and a stale XP curve. | Read the current Java curve defaults and generate level-cost/cumulative XP rows. Do not revive crafting/engineering XP. |

## XP, anti-farm and persistence results

All seven active sources retain the existing shared character bar: combat, mining, farming, cooking, fishing, exploration and authored quest commands. The default curve is `1000 × current level`, totaling **4,950,000 XP** from 1 to 100. A configured `tnk-balance.json` curve remains supported. Grants cross multiple thresholds, carry within-level XP and stop at the active chapter ceiling; no overflow addition of an entire unbounded grant is used. Skill rewards use absolute level entitlement, not an additive login award.

Natural mining uses persistent dimension/section bitsets and existing legacy tombstones. Player placement, piston source/destination and fluid generation keep their exclusion hooks. Silk Touch may pay on the first natural break; placing and breaking that drop cannot pay again. Logs have no direct reward by default, but configured rewarding logs use the same provenance. Vanilla pumpkins/melons are piston-destroyed in the target version; the suspected movable-fruit exploit was rejected after inspecting the actual resolver. Fortune/drop counts do not multiply block XP.

Combat retains personal/owned damage attribution, post-absorption damage, the pre-hit-health overkill clamp, expiration, distance/life checks, environmental-damage denominator and spawn exclusions. Verified arena command-spawned enemies retain their existing exemption. No duplicate death award, extra boss award or new vanilla XP-drop rule is added. Ordinary summon/split provenance remains separate from verified arena adoption.

Cooking remains manual-input, actual-completion based, with same-dimension/64-block/online checks. Native NeoForge furnace `InvWrapper` automation routes through the intercepted inventory methods. Mods writing directly to backing lists or creating blocks without the placement/fluid/piston hooks require their own integrations; this does not intercept every arbitrary world mutation.

Vanilla fishing retains ownership, hook age/open-water settings and paid markers. Starcatcher retains its own valid catch/minigame/rarity rules and no double payment. Exploration records discoveries per character and preserves teleport protections; chunk pre-generation alone awards nothing. Quest `xp` remains repeatable under the quest system's claim policy; named `reward` receipts remain once per character and are recorded only after payment.

Persistence is **UUID-keyed overworld SavedData**, not an entity attachment/capability. Replacement players after death, End return, dimension travel or reconnect resolve the same compound. Adding a second Clone copy would be redundant and could duplicate migration. `HolderLookup.Provider` serialization signatures match the target API. The actual NeoForge SavedData implementation already copies the returned tag before asynchronous compression. No saved key/schema was renamed or erased.

## Skills, origins, Ascension and titles

Pufferfish is optional. The actual 0.16.8, 0.17.3 and 0.19.2 JAR signatures match the consumed API. `setPoints(player, tnkprogression:character_level, total)` replaces that source's absolute entitlement; respec/refunds and other point sources remain Pufferfish-owned. Live queries observe respec/import/reload. Target `PlayerList.respawn` already copies scoreboard tags to the replacement player; no fabricated reward reconstruction is added. FML deliberately accepts the 1.21 Pufferfish build on 1.21.1 despite its raw metadata bound.

Origins retain saved revision-1 behavior, revision-2 choices, stable modifier IDs, no-heal reconciliation and optional attribute checks. Ascension remains the authored sigil/admin path and consumes the existing item for reselection; it is not an automatic character-level unlock. The cosmetic and passive title selections remain independent, with existing immediate passive removal/cooldown policy. Clearing a passive does not remove cosmetic ownership.

The catalogue remains **42 active titles / 16 legacy definitions**: 10 actual skill-purchase rewards, 29 manual authored quest/boss rewards and three explicit staff/special awards. No automatic quest/boss grants are introduced. Saved legacy ownership, selections and revocations persist; Owner stays clean blue `7DABF2` and grants no permissions. Existing quest commands remain in [TITLE-QUEST-REWARDS-1.1.5.md](TITLE-QUEST-REWARDS-1.1.5.md).

## Mob scaling, Loot and Spiral

Ordinary enemies retain nearest eligible player scaling and configured variance. The existing on-demand, per-world/per-tick player snapshot/spatial candidate cache is retained; assigned mob levels do not reroll when another player approaches. TNK scales **MAX_HEALTH and ATTACK_DAMAGE**. Armor scaling and altered vanilla XP drops are not current features and were not added under the guise of a fix.

Boss balance is unchanged: Warden 35; Dragon 75; all sixteen End-eye boss minimums below 60; Kraken 45; Clawdian/Magnetron 30; Brainiac 20. Default native miniboss minima remain capped at 35/associated half-boss rules and final level 100; Apotheosis invaders remain nearest player +5 without variance, capped at 100. Full bosses can reach 110. Custom boss minima remain configurable. See [BOSS-BALANCE.md](BOSS-BALANCE.md).

Normal Spiral floors retain configurable targets **50/60/70/80/90/100**, without ordinary player variance. Actual full bosses use `min(110, max(regular target + 10, configured minimum))` only in the native permitted slots. Verified bridge target context takes precedence for newly assigned enemies; existing assignments survive theme changes. Run/dimension/session identity is validated, and arbitrary similarly named tags are insufficient. Fallback context is consumed after native `WaveScaling.apply` completes and before world insertion, without competing HEAD order assumptions. Simultaneous arenas remain run-local; native Endless behavior is untouched.

TNK is the intended normal-floor stat baseline. Native competing floor/absolute-boss multipliers are replaced only under the existing configurable baseline settings; party scaling stays once. Keep Spiral's zero-valued health modifier: its presence permits health above vanilla's 1024 limit. Preserve `SkipRaisingHealth`. Melee, owned projectiles, spells and area proxies use the same damage boundary; no attribute-plus-event double scaling is introduced. Compatibility settings remain `abyss.enabled`, `floorLevels`, `tnkHealthBaseline`, `tnkDamageBaseline` and `scaleIndirectDamage`.

The public APIs `level`, `ceiling`, `rarityCap`, `mobLevel` and `lootLevel` retain their signatures and meanings. `mobLevel` exposes the actual saved combat level through 110; `lootLevel` retains ordinary reward caps. Patched Loot owns authored `dungeon_reward` outcomes, exact items, equipment presets/components, fixed bonuses, identification/trading/reforging/rotation persistence and golden party limits. Progression adds no item handler/grant and does not recap/reroll an explicit saved result. A level-110 floor-six boss does not replace the chest's authored source level 100.

## Commands and configuration

This codebase uses its existing **JSON configs**, not `ModConfigSpec`; replacing the configuration system would create unnecessary migration risk. Important files are `tnkprogression.json`, `tnk-xp.json`, `tnk-balance.json`, `tnk-titles.json`, `tnk-title-legacy.json`, `tnk-title-perks.json`, and the external editable Pufferfish tree. Fresh generated defaults are included with the validation evidence. Existing custom values are preserved under existing migration rules. The HUD remains hidden by default, with saved player opt-in/out choices retained.

The real root command is `/tnkprogress`, not `/tkp`. Read-only commands include `status`, `activities` and title listing. Permission-level-2 commands include `xp <player> <amount>`, `reward <player> <id> <amount>`, `unlock <player> <configured ceiling>`, `reload`, title `grant/earn/revoke/reload`, and origin `get/set/reset`. Self selections validate actual ownership and known origin/title IDs. Clients cannot submit XP, stat values, grants or a target player. C2S handlers use the target registrar's default **main thread**. No direct set-level/set-skill-points command exists; none was invented. Use native Pufferfish commands for that mod's manual point administration.

Progression reload reloads progression/XP files; title reload handles title/perk files. Balance curves retain their existing restart-load behavior. Failed multi-file reloads restore runtime settings, while already completed migration file writes can remain on disk. All reward permissions, amount bounds, ID validation and personal-versus-team claim policies remain intact.

## Performance evidence and remaining limits

The standalone Java 21/Gson fixture measured the actual published 1.1.5 record/helper behavior: periodic JSON **7,174 → 915 raw UTF-8 bytes** (87.25% smaller); allocated bytes per helper/serialization fixture **43,584 → 7,224** (83.43% lower). These are component measurements before packet framing/compression, not a measured whole-pack TPS gain. Timing rounds and reproduction are in [../audit-notes/tps-review.md](../audit-notes/tps-review.md) and `tools/audit_snapshot_benchmark`.

No new world-wide entity scan or tick listener is added. Existing periodic player UI/skill/condition checks remain bounded and responsive; the waste inside them was reduced. Spatial lookup still has a clustered-player worst case of O(players), with a bounded 2048-cell candidate cache per snapshot. A more complex tree is not justified without representative profiling.

Persistent mining/discovery/unique-reward histories intentionally grow with world activity; deleting receipts would reopen exploits. A regionized SavedData migration is separate work. Client UUID appearance caches clear on disconnect but can retain historical players through a very long, high-churn connection; safely pruning them requires matching tracking resynchronization. A same-final-priority third-party death cancellation after the existing LOWEST XP listener is a potential integration boundary, not a reproduced installed-mod exploit; standard totems are handled earlier. These were not changed speculatively.

## Validation results and exact inputs

The machine-readable manifest, dedicated-server reports/log excerpts, generated defaults and test counts are stored in `docs/validation-1.1.6/`. Runtime fixtures use real Minecraft/NeoForge servers with disposable synthetic players and controlled arenas, not a human-controlled client session. They may invoke private fixture setup to avoid completing entire runs; production integration does not mutate those private run fields.

<!-- AUDIT_RESULTS -->
- **Compilation:** Java 21/Zulu 21.0.13.0.3, Gradle 8.14, ModDevGradle 2.0.148; Minecraft 1.21.1, NeoForge 21.1.262, FML 4.0.45. Production build status and artifact hash are recorded in `artifact.json`.
- **Automated unit checks:** 60 tests, zero failures/errors, including curve/ceiling overflow rules, title catalogue validation, packet budgets and incremental merge/pacing.
- **Repository checks:** both title-binding/default-title scripts passed; current curve/passive report and original bundled-artwork hash checks passed.
- **Automated dedicated-server suites:** all eleven cases below passed. A Gradle/process exit alone was not accepted; the required success markers and JSON were inspected. Fixture corrections were needed for retired mastery expectations, an unticketed fishing-bob chunk, native reinforcement eligibility, offline network channels and legitimate third-party damage modifiers. Those were fixture issues, not a reason to restore retired features or disable other mods.

| Case | Actual runtime result |
| --- | --- |
| Progression alone | Passed; optional absence, packets, origins/titles and actual second-JVM SavedData sentinel |
| Patched Loot 1.1.3 plus required companions | Passed; 100 ordinary/legacy cap cases |
| Pufferfish Skills 0.19.2 + Attributes 0.8.3 + FTB Teams 2101.1.12 + Iron’s Spells 3.16.3 | Passed; live teams, 588 nodes/669 links, 175 points, refunds/respec, potion bonuses, 15 ascended origins |
| Original Spiral 2.0.5 + Architectury 13.0.11 | Passed; 12 floor/player cases, 3 boss slots, damage/health/save/reinforcement/combatXP |
| Loot + Spiral 2.0.5 | Passed; exact authored rewards and combat 110/chest 100 separation |
| Both + original rotation bridge 1.0.0 | Passed; supplied bundle 1.0.1 context and authored target precedence |
| Spiral 2.0.6, without bridge | Passed; native changed signatures and the same floor/scaling checks |
| All above with Spiral 2.0.5 + Iron’s Spells + Starcatcher | Passed; all native integrations together, including 48 magic-helmet outcome checks |
| Direct-XP suite, Progression alone | Passed; migrations, overflow, natural/placed/piston mining, cooking/automation, exploration and UUID state |
| Direct-XP suite with all optional integrations | Passed; real Starcatcher 3.1.4.1 rarity catches, failed minigame, paid-once guards and throwing original-method cleanup |
| Boss/activity suite | Passed; boss minima/caps, malformed tags, injured disk loads, miniboss persistence and 110 badge/codec |

Each applicable Loot dungeon run checked 18 floor/chest cases, 96 exact equipment save/identify cases, 900 golden payouts, 9 party/chest cases, native preview/loot-function paths, paid NPC identification and monthly/reforge persistence. Ordinary caps were checked separately. Controlled arena fixtures covered melee, arrow/spell/area ownership, party scaling, regular/full-boss classification, simultaneous arenas, saved modifiers and no-heal reload/adoption.

**Final packaged JAR:** passed dedicated-server startup and clean shutdown both alone and with the full optional combination. SHA-256: `e6f341ee2e6324a84697bd6319fa4a1dd8475d5008d16723e2342e06010a5550` (2, 397, 321 bytes). These checks load the distributable itself and do not execute the excluded verification drivers; evidence is in `packaged-startup.json`.
<!-- /AUDIT_RESULTS -->

Inspected sources: Progression baseline above; maintained Loot `compat/tnk-1.1.3-pack-spiral` **`1fee79b367859b74f6f279ec3c0a0f4e9e2c619e`** (no newer completed branch changes found), completed Loot publication **`a186232517af1b010afe1eefe882da32c35f7b4c`**; Spiral 2.0.5 **`085aba1b98dbc5d4ed9ce28a2e9ee83a9f71b4d1`**, 2.0.6 **`d2df48c23632b13b8f806d278cc6ff041c8c1928`**; Pufferfish 1.21 source **`78485d944719516667e648398e6d469b059c8c32`**. Actual original JARs are pinned by SHA-256 in the manifest. The supplied rotations bundle is 1.0.1 and its included bridge is 1.0.0, pinned by its own metadata to Spiral **exactly 2.0.5**; bridge + 2.0.6 is not a supported/tested combination.

Individual Cataclysm, Mowzie’s Mobs, Bosses’Rise and Legendary Monsters boss gameplay was not exercised without those complete runtime mods; their configured roster/minimum rules were audited separately. FTB Quests command rewards were exercised through the real server dispatcher, rather than an interactive quest-claim client.

No manual in-game/client rendering playtest, public multiplayer session, Spark/JFR whole-pack recording or controlled production TPS benchmark is claimed. Generated test reports, compilation and source inspection are distinct evidence. Update clients and server together: character protocol changes from 9 to **10** to support retained incremental help.
