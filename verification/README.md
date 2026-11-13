# Optional integration verification

Production builds require no optional-mod JARs. `verification/abyss` is compiled only with `-PtnkAbyssSmoke`; its classes are excluded from distributable JARs. These checks run actual Minecraft/NeoForge dedicated servers with disposable, offline test players and worlds. They are automated runtime checks, not interactive client playtests.

Supply the pinned original Spiral 2.0.5 JAR, patched Loot 1.1.3 and its required companion JARs in one local directory. That directory is compile-only. Select the runtime combination independently by putting just its applicable JARs in `<run>/mods`. Compile all verification sources even for the absent-mod case, so absent optional APIs are tested at runtime rather than compiled away.

```sh
./gradlew runServer -PtnkAbyssSmoke \
  -PtnkVerificationJars=/absolute/pinned-jars \
  -PtnkVerificationRun=/absolute/disposable-run
```

Accept the Minecraft EULA in the disposable run directory before launching. A successful Gradle exit alone is insufficient: require `TNK_ABYSS_SMOKE_OK` and `abyss-validation.json` with `status: passed`, and reject `TNK_ABYSS_SMOKE_FAILED`.

Matrix: Progression only; Loot + required companions; Spiral + Architectury; both; both + the supplied rotation bridge. For both-mod reward checks, install Loot's pinned six-floor reward example datapack in the disposable world's datapacks. For the bridge case, extract the supplied December rotation's `rotation.json` and `themes/*.json` under `<run>/bridge-fixture/`. The harness parses those real definitions and changes target levels to 50/55/60/65/70/75 and substitutes registered vanilla boss candidates in memory, proving that completed bridge context wins over fallback levels. It does not claim that all optional thematic boss mods have been playtested.

The native WaveSpawner, WaveScaling, EncounterReinforcements, damage adapters, entity save/load, badge codec, Loot bridge, loot functions, NPC identifier, reforger and reward roll/save methods are exercised. Private arena fields are set only in test fixtures to control floor/wave/run without completing each quest or waiting for a full attempt. Production integration validates public live session APIs and does not mutate those fields.

`dev/mikaaah/tnkloot/verification/DungeonRewardSmoke.java` is copied unchanged from the maintained Loot branch at `1fee79b367859b74f6f279ec3c0a0f4e9e2c619e`. It is an external regression fixture, not a second implementation of Loot's reward system. It covers exact saved results, fixed bonuses, native components, 18 floor/chest combinations, 900 golden payouts, party limits, paid identification and rotation/reforge persistence. Unloaded optional Iron's Spells equipment is excluded by its existing mod conditions.

The ordinary Progression unit tests remain `./gradlew test`. Existing direct-XP and boss/activity server checks remain available through the documented CI properties.
