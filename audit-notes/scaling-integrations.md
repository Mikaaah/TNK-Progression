# Enemy scaling and optional integrations — audit notes

Baseline: Progression `a01e74172cb266896cb1ee85d508560a5660eaad` (1.1.5), Minecraft 1.21.1 / NeoForge 21.1.262 / Java 21.

## Verified problems and fixes

| Finding | Evidence / trigger | Fix |
| --- | --- | --- |
| Injured disk-loaded mobs without a TNK tag healed to full | `ProgressionEvents.join()` treated missing `tnk_level` as a new spawn even when NeoForge `EntityJoinLevelEvent.loadedFromDisk()` is true | Still assign the appropriate saved level, but retain existing damaged health on disk-loaded entities; clamp only when above the resulting maximum |
| Fresh native phase mobs could lose injured health | Spiral `EncounterReinforcements.inheritScaling()` preserves the phase's injured health before its adoption callback; non-Enemy phases can still lack `tnk_level`, causing TNK's subsequent unconditional full heal | Recognize the native phase only after arena validation, and preserve its health rather than applying a fresh-spawn full heal |
| Late non-Enemy ordinary reinforcements could heal on first assignment | These mobs are inserted before native discovery and may lack a prior TNK assignment, so they reached the same fresh-spawn full-heal branch even without a phase tag | Use the world's constant-time entity-ID lookup to distinguish an already-inserted mob from the initial pre-insertion join; retain its existing injured health |
| Ordinary children could remain full bosses at 110 | `AbyssIntegration.inherit()` copies a valid full-boss snapshot; `adopted()` previously skipped category correction when that copied snapshot was valid | Non-phase adopted children use the saved regular floor target and `boss=false`; established assigned entities are never rewritten |
| Native phase flags are not reliable full-boss classification | Spiral sets its native BOSS flag for any configured phase, including a phase originating from a regular/mini encounter | Preserve the actual root's saved encounter category. Fallback lookup also matches the native phase root's entity ID and current floor/wave rather than selecting an unrelated first mob |
| Late adoption left existing clients' level badges stale | Reinforcements enter the world before Spiral adopts them, while the previous payload was sent only on StartTracking | Broadcast a changed/new saved mob level to current tracking players, using NeoForge's server-side tracking distributor |
| An owned proxy with RUN but no valid DAMAGE tag lost party scaling | Native Spiral damage scaling requires RUN **and** a finite positive DAMAGE multiplier; TNK checked only RUN when deciding whether native party scaling would run | Match the native predicate; apply the saved party fallback exactly when native Spiral will not multiply the source |
| A nonnumeric `tnk_level` tag prevented assignment forever | A string tag satisfied the previous untyped `contains()` check, while numeric reads returned zero | Only a numeric tag counts as an existing saved assignment; repair malformed nonnumeric tags through ordinary assignment |
| Valid large damage settings overflowed the engine's float damage range | Finite `damagePerLevel=1e100` passes config validation but the previous final float cast produced infinity | Saturate computed arena damage at `Float.MAX_VALUE` at the event boundary; keep zero damage zero and retain normal configured balance |

New regressions are in `BossActivitySmoke` (disk-loaded injured mobs and malformed tags) and `verification/abyss/.../SpiralChecks` (injured non-Enemy native phases, regular-root phase classification, full-boss non-phase children, and owned RUN-only damage proxies).

The verification-only direct `WaveScaling` calls resolve the actual `Mob` (2.0.5) or `LivingEntity` (2.0.6) first-parameter signature. This prevents a fixture compiled against 2.0.5 from failing with an unrelated `NoSuchMethodError` on 2.0.6; production integration still uses the existing optional mixins.

Native discovery intentionally excludes friendly non-monsters. The ordinary non-Enemy reinforcement fixture therefore targets an admitted participant before discovery, as required by Spiral's actual ownership/hostility predicate; the separate phase fixture uses native phase ownership. A first runtime failure exposed that missing fixture setup, and required no additional production change.

No full-project build was run independently by this subtask. Root owns shared compilation and actual dedicated-server matrix execution; aggregate those results before claiming these runtime regressions passed.

## TPS work and preserved invariants

- The previous Abyss damage listener attempted ownership resolution twice on ordinary melee hits, allocating an ownership HashSet on each pass even outside the Abyss. Normal-world damage now returns before this work when neither source entity belongs to Spiral's dimension.
- Ownership walking remains limited to eight links, with self-link rejection, but no longer allocates sets; a distinct direct source is traversed only if necessary.
- No new entity/world scans were introduced. Native Spiral's existing bounded reinforcement discovery remains its responsibility. TNK fallback reads only the controller's current-stage roster.
- Nearest-player lookup is already an on-demand per-world/per-tick immutable position/progression snapshot, with bounded 16-block cell candidate caches. It does not continuously scan every entity, and saved mob joins do not re-run nearest-player assignment except the existing one-time legacy boss migration.
- Reflection for Spiral's common/server API is already cached once. No optional client classes or new hard mod dependencies are linked.
- Keep the zero-valued `spiral_abyss:health` modifier for solo parties: Spiral's `AbyssHealthAttributeMixin` uses its presence to permit max health above vanilla's 1024 limit. It is not a removable no-op.
- TNK's implemented baseline scales **MAX_HEALTH** and **ATTACK_DAMAGE**. It does not add mob armor scaling or change vanilla XP drops; character combat XP is a separate existing system.
- Boss minima, ordinary variance, miniboss cap 100, full-boss cap 110, invader player+5, existing saved assignments and floor reward source levels are unchanged. No extra XP/item grants or dimension locks were added.

## Inspected dependencies and contracts

| Input | Exact version / commit / SHA-256 |
| --- | --- |
| Maintained Loot source | `compat/tnk-1.1.3-pack-spiral` still exactly `1fee79b367859b74f6f279ec3c0a0f4e9e2c619e`; no newer completed branch changes found |
| Completed Loot 1.1.3 JAR | Approved publish commit `a186232517af1b010afe1eefe882da32c35f7b4c`; `f0c27294258697d1330831819070af52e3f9058f7b10040de9af2fa85bca5c53` |
| Original Spiral 2.0.5 | Source `085aba1b98dbc5d4ed9ce28a2e9ee83a9f71b4d1`; CurseForge file 9551114; JAR `6a548d6e5d82bfec0af1dedbea3b212db154b8f64a7bd75614142714a4d7e643` |
| Spiral 2.0.6 | Source `d2df48c23632b13b8f806d278cc6ff041c8c1928`; CurseForge file 9592770; JAR `e20fb5eeaec1348e49d71e9dd18501f1a1b9cf669fb3b42cb9e362b7d71f52b3` |
| Rotations bundle 1.0.1 | `51a63c7a41a19919dee84024e65102888054306500fe1018e66c1476718bba60` |
| Included rotations bridge 1.0.0 | `a999f11364c771eca0ddbc00bd5131c1c386a35b2eda71a385876c2061ed6f5a`; its actual TOML intentionally requires Spiral `[2.0.5]` |

All original/recovered JARs are available under shared `../dependencies`.

Loot's `TnkLevelBridge` caches calls to `level`, `ceiling`, `rarityCap` and `mobLevel`; it currently does not call `lootLevel`, which must remain public for API compatibility. `TnkLootRolls.effective()` applies ordinary source/player-ceiling/100 caps. Authored `DungeonEquipment` outcomes preserve their complete serialized result; `result(Level,ItemStack)` has no recipient parameter and sealed tokens bypass ordinary caps. No Progression item processing was introduced.

Spiral's native spawn pipeline stamps run/category, scales, then inserts. TNK still consumes completed RETURN context, after the bridge's HEAD metadata hook, rather than relying on competing HEAD ordering. 2.0.5 Mob and 2.0.6 LivingEntity callback signatures are already supported via the existing @Coerce mixins. Bridge + 2.0.6 is **not** a supported combination and must not be claimed.

Native floor and absolute-boss health/damage multipliers are replaced only for TNK's selected normal-floor baseline. Party scale is retained once; SkipRaisingHealth remains respected. Existing normal floor slot, source-level and reward rules are preserved.
