# Spiral Abyss and Unidentified Loot

Progression 1.1.1 targets Minecraft 1.21.1, NeoForge 21.1.262 and Java 21. Install the same Progression JAR on clients and server. Integrations remain optional; Loot itself requires Progression and its own Apotheosis companions.

Use **Spiral Abyss 2.0.5**, **Unidentified Loot 1.1.3 TNK** from `compat/tnk-1.1.3-pack-spiral` (not Loot's default edition), and optionally the existing **TNK Abyss Rotations bridge 1.0.0** from the rotations bundle 1.0.1. No extra helper mod is required. These mixins target Spiral 2.0.5's verified API; later Spiral versions require verification before upgrading.

## Levels

| Normal floor | Regular enemy | Full boss before a higher configured minimum |
|---|---:|---:|
| 1 | 50 | No full-boss slot |
| 2 | 60 | No full-boss slot |
| 3 | 70 | No full-boss slot |
| 4 | 80 | No full-boss slot |
| 5 | 90 | 100, wave 3 only |
| 6 | 100 | 110, waves 2–3 only |

Regular enemies have no nearest-player variance. Full bosses use `min(110, max(target + 10, bossBaseLevels[entity]))`. Spiral's actual `BOSS` category controls this rule; `MINI_BOSS`, champions and registry-listed bosses selected as regular enemies retain the regular target. Normal-world assignment remains unchanged.

The adapter consumes `WaveScaling.apply` at RETURN, after native initialization and the rotation bridge's HEAD injection. It verifies Spiral's dimension, live session/run UUID, actual floor/wave and arena bounds before capturing anything. Matching bridge floor, target (1–100), native boss flag, valid theme ID and monthly label take precedence over fallback settings. Invalid/incomplete bridge metadata falls back to the configured normal-floor mapping. Arbitrary floor tags alone do nothing.

Assignments and their party/rotation snapshots persist under `tnkprogression_abyss_v1`, alongside the standard `tnk_level`. Reloads retain combat levels, health damage and modifier identity. Changing floor settings or monthly themes affects new assignments. Native reinforcement adoption and phase handoff inherit verified context; split/conversion handling leaves Spiral responsible for its stage membership. No new world-wide entity scan is used.

## Scaling

Default health is the mob's native baseline with TNK's ordinary level modifier, then Spiral party scaling once. This **replaces** Spiral's normal-floor health multipliers and absolute boss-health targets. `SkipRaisingHealth` skips TNK and party health increases too, preserving the native health range. Native enchantments, equipment, AI, boss environments and other unrelated modifiers are retained.

Arena damage uses one outgoing TNK level factor for melee, projectiles, spells and areas with a resolvable arena owner, followed by Spiral's party factor once. The arena attack-damage attribute does not also receive TNK's modifier, so both attribute-derived attacks and fixed-damage attacks get the same factor once. This also covers fixed area attacks that use the generic mob-attack damage type. Unattributed damage is not guessed from location. `scaleIndirectDamage=false` limits the TNK outgoing factor to direct mob-attack sources; party scaling remains. Normal-world attack attributes are unchanged.

## Configuration and saves

The following object is added automatically to existing `config/tnkprogression.json`, preserving existing settings. [Reference fragment](../config-defaults/abyss.json):

```json
"abyss": {
  "enabled": true,
  "floorLevels": [50, 60, 70, 80, 90, 100],
  "tnkHealthBaseline": true,
  "tnkDamageBaseline": true,
  "scaleIndirectDamage": true
}
```

Do not replace your complete configuration with this fragment. Each of the six floor levels must be 1–100. Invalid configuration retains the previous valid settings. The existing `healthPerLevel`, `damagePerLevel`, `bossBaseLevels` and `scaleMobs` settings retain their meanings. Disabling a baseline retains Spiral's native scaling for that stat without adding a second TNK modifier. Baseline choices and party scale are captured per encounter; restart/rejoin does not rebase an already assigned mob. `enabled=false` disables new captures; existing saved assignments remain stable. Restart after installation.

Player data, XP curve, origins/Ascension, titles, chapter ceilings, claims and skill points are unchanged. Verified wave spawns can earn their existing combat XP despite Spiral's internal COMMAND spawn type. Contribution accounting and the paid receipt still control the award; no extra arena completion XP is granted.

## Loot boundary

The public API signatures and ordinary meanings are retained:

| API | Meaning |
|---|---|
| `level(ServerPlayer)` | Current personal character level, 1–100 |
| `ceiling(ServerPlayer)` | Existing personal/team milestone ceiling |
| `rarityCap(ServerPlayer)` | Existing milestone rarity cap |
| `mobLevel(LivingEntity)` | Actual saved combat level, including 101–110 |
| `lootLevel(ServerPlayer, LivingEntity)` | Ordinary mob-loot level bounded by the recipient's milestone ceiling |

Patched Loot performs the ordinary equipment tier/rarity restrictions at its generation boundary. Explicit `tnkloot:dungeon_reward`/`seal_reward` outcomes remain Loot's responsibility: authored source level, tier, rarity, exact equipment, native components, exclusions, fixed bonuses and saved affix plans are not touched by Progression. Loot's identification, trading, reforging access and one-golden-prize-per-party rules remain intact. There is no item-pickup/crafting/chest reward listener or extra item grant here.

A floor-6 reward authored at source level **100** remains level 100 even when its boss has combat level **110**. This integration never substitutes a mob's combat level for a chest's reward source.

Endless Mode, portal access, dimension locks, floor eligibility and Spiral's own reward/claim logic are unchanged. See the validation report for tested combinations and the limits of automated versus full-pack playtesting.
