# Snapshot/help serialization fixture

This is a standalone **Java 21** microbenchmark and raw-payload size fixture. It invokes the published **Progression 1.1.5** JAR's actual `ActivityHelp.describe()` implementation and uses that JAR's snapshot records with Gson. It **does not** launch Minecraft, simulate server ticks, invoke the complete `CharacterState.snapshot()` path, inspect packets on a network, or measure a full modpack's TPS.

The baseline used for this audit is `tnkprogression-1.1.5-neoforge-1.21.1.jar`, SHA-256 `8358c37029c73781501bb771ae2507b7578ac2e42bd3c58d8e4a947c06f84d56`.

## Run

The baseline JAR and the Mojang-mapped NeoForge development runtime must already exist. No Gradle task or mod rebuild is invoked. For this workspace:

```bash
JAVA_HOME=/workspace/scratch/8fcc0960d930/toolchain/zulu21.54.109-ca-crac-jdk21.0.13.0.3-linux_x64 \
python3 tools/audit_snapshot_benchmark/run.py \
  --baseline-jar ../tnk-progression/build/libs/tnkprogression-1.1.5-neoforge-1.21.1.jar \
  --minecraft-jar ../tnk-progression/build/moddev/artifacts/neoforge-21.1.262-merged.jar \
  --runtime-jar ../tnk-progression/build/moddev/artifacts/neoforge-21.1.262.jar
```

On another machine, substitute paths and optionally pass `--java-home` and `--dependencies-dir`. The dependency directory defaults to the local Gradle module cache. The baseline JAR is first on the classpath; Minecraft runtime classes supply lazily linked method types. The resolved Gson JAR is printed because a cache can contain several versions. This audit used **Gson 2.10.1**, **Zulu JDK 21.0.13.0.3**, and **NeoForge 21.1.262**. This fixture classpath is not a replacement for the mod's proper Gradle dependency resolution or integration tests.

The runner compiles only the small fixture into a temporary directory, then deletes it. It never rewrites the baseline mod JAR or game/config/save files.

## Exact fixture

- Level 42, ceiling 100, within-level XP 1234/42000, 73 earned and 30 spent skill points.
- Cosmetic `tnk:prepared`, passive `tnk:skill_delver`, tracked mining, HUD hidden, no passive cooldown.
- Seven activity rows, each with earned XP 12345, last gain 20, action count 40; two journal strings.
- Origin `tnk:miner`, revision 1, no optional mana availability or Ascension, request nonce 0.
- Full catalogue: 42 default active titles, four marked earned (`prepared`, `skill_delver`, `initiate`, `disciplined`), no legacy titles. Skill details use spent 30 and unlocked `miner_mastery`. Optional Iron's Spells perk attributes are marked unavailable. This is a constructed message fixture, not a tested skill build or live player.
- Current partial: no titles, all seven activity-help lists reconstructed using the actual 1.1.5 implementation on every iteration.
- Lean partial: same dynamic field values and activities, empty static activity-help lists. This represents separating catalogue/help text from periodic dynamic updates; it does not require changing gameplay values.

The default baseline helper builds **70 help strings**: combat 6, mining 32, farming 5, cooking 5, fishing 12, exploration 5, quests 5. It sorts four config streams per invocation of the seven-activity snapshot (13 mining block overrides, 14 mining tags, 6 Starcatcher rarity rewards, zero cooking overrides).

## Measurement method and limits

- JVM heap: `-Xms256m -Xmx512m`.
- Warmup: **2,000 iterations for each scenario** before measurements.
- Measurements: **three alternating rounds of 10,000 fixtures per scenario**.
- Duration: `System.nanoTime()` around each complete loop.
- Allocation: HotSpot's `com.sun.management.ThreadMXBean.getThreadAllocatedBytes()` for the current thread; not retained heap size.
- Fixture construction uses reflective record constructors and helper invocation. The production helper and Gson serializers are real, but this wrapper is not the production calling path.
- Report raw **UTF-8 JSON body bytes**, excluding Minecraft packet framing, compression, transport, client rendering and processing.
- Timing is sensitive to JIT compilation, GC, host load and cache/library versions. Allocation and body-size comparisons are more repeatable. Do not interpret these numbers as a whole-server TPS uplift or a fixed production speedup.

Audit results are preserved in `audit-notes/snapshot-benchmark-baseline.txt`. The fixture's raw body-size savings apply when a partial packet is actually emitted; the original implementation already suppresses unchanged packet transmission after constructing/serializing its state.
