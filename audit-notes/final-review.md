# Final integration and network review

Reviewed the audit branch against `a01e74172cb266896cb1ee85d508560a5660eaad`. No separate Gradle or runtime test was executed by this reviewer.

## Confirmed packet-size defect and fix

`DirectXpConfig.reload` accepts 4,096 entries independently in each mining block/tag/cooking map, and ResourceLocation validation does not bound the identifier length. `ActivityHelp.describe` formerly sent every map entry. For example, 8,192 ordinary legal identifier help rows produce approximately 458,752 JSON characters, already above `CharacterPayload`'s 262,144-character codec limit. Large legal automatic title skill requirements can also exceed that limit independently. The result is an encoder failure on full character sync, including login/refresh.

`CharacterSnapshots.encode` now leaves ordinary JSON unchanged, and budgets unusually large views at 240,000 JSON characters. It abbreviates only displayed help/acquisition text and, if needed, displayed descriptions/perk text/history. It preserves every title ID, ownership and legacy flag, label, translation key, colour, perk availability, cosmetic/passive selections and dynamic character counters. The view explicitly directs readers to the server configuration for omitted details. Configured rewards, titles, bonuses and unlock rules are never modified. Raw JSON is never sliced.

Three focused JUnit cases cover ordinary JSON equivalence, 8,192 reward-help rows, and 256 titles with 64 long skill requirements and worst-case HTML-escaped descriptions/perk text. The coordinator runs these with the full suite.

## Additional confirmed command findings sent to coordinator

- Title catalog reload could succeed before perk reload failed; its previous feedback incorrectly claimed the previous catalog remained active. Full catalog checkpoint/rollback is required.
- Passive-title clear ignored its selection return. Further inspection confirmed the existing policy intentionally permits immediate passive removal during cooldown, so no cooldown-bypass defect is claimed. The command now propagates its result without changing that policy.

## Reviewed behavior

- Main-thread transient per-player sync/action caches avoid SavedData writes for packet pacing and are cleared on logout/server stop.
- Incremental snapshots preserve cached static help/title metadata; matching client/server protocol revision is required.
- FTB Teams caches reflected Method bindings only and resolves current manager/membership live.
- Progression reload restores prior runtime config references if either config rejects, without scanning entities.
- Abyss adoption retains live run/floor/wave/type validation and preserves actual native boss-phase classification. Already-inserted neutral reinforcements retain current health.

No further regression was identified in these reviewed changes. Actual optional-mod and dedicated-server execution is coordinator-owned.
