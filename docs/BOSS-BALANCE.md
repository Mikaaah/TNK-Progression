# Boss and miniboss balance — 1.1.4

Minecraft 1.21.1, NeoForge 21.1.262, Java 21.

## Assignment

| Encounter outside Spiral arenas | New mob level |
| --- | --- |
| Ordinary enemy | Nearest eligible player ± configured variance (default 4), clamped to 1–100 |
| Apotheosis invader (`apoth.boss`) | Nearest eligible player +5, capped at 100; no variance |
| Apotheosis elite (`apoth.miniboss`) | Ordinary enemy rule; no native boss minimum |
| Native miniboss in `minibosses` | Higher of configured minimum or nearest player +10, capped at 100 |
| Full boss | Higher of configured minimum or nearest player +10, capped at 110 |

Without an eligible player, invaders use level 6, ordinary enemies/elites use 1, and native bosses/minibosses use their authored minimum. Creative, spectator and fake players remain excluded from level selection.

Apotheosis's registry-id elite marker is present before its LOWEST-priority join callback changes that marker into a boolean. Progression recognizes both forms, avoiding a dependency on registration order. Apotheosis's native attributes, equipment, effects, drops and spawn frequency are unchanged. No hard dependency is added.

Verified Spiral context takes precedence over these normal-world rules. Its actual full-boss groups can still reach 110, while regular/elite groups use their saved floor target. Existing arena snapshots are never clamped or reassigned by the normal-world miniboss rules.

## Changed base minimums

| Entity | Previous | New |
| --- | ---: | ---: |
| Warden | 75 | 35 |
| Ender Dragon | 50 | 75 |
| Cataclysm Ignis | 75 | 55 |
| Cataclysm Harbinger | 60 | 55 |
| Cataclysm Maledictus | 80 | 55 |
| Cataclysm Leviathan | 70 | 55 |
| Cataclysm Ancient Remnant | 65 | 55 |
| Cataclysm Coralssus | 35 | 25 |
| Cataclysm Ender Golem | 35 | 30 |
| Cataclysm Ignited Revenant | 40 | 25 |
| Cataclysm Kobolediator | 40 | 25 |
| Cataclysm Wadjet | 45 | 25 |
| Cataclysm Aptrgangr | 45 | 25 |
| Cataclysm Prowler | 45 | 25 |
| Cataclysm Clawdian | 50 | 30 |
| Block Factory's Kraken | 65 | 45 |
| Alex's Caves Luxtructosaurus | 75 | 55 |
| Legendary Monsters Possessed Paladin | 75 | 55 |
| Archaion Last of Deepslate | Not listed | 55 |
| Alex's Caves Magnetron | 40 | 30 |
| Alex's Caves Brainiac | 35 | 20 |
| Alex's Caves Forsaken | 50 | 35 |
| Alex's Caves Hullbreaker | 60 | 35 |
| Iron's Spells Citadel Keeper | 40 | 30 |
| Twilight Forest Minoshroom | 25 | 20 |
| Twilight Forest Alpha Yeti | 30 | 20 |
| Legendary Monsters Ancient Guardian | 40 | 35 |
| Legendary Monsters Lava Eater | 45 | 35 |
| Legendary Monsters Beheaded Knight | 45 | 25 |
| Legendary Monsters Resurrected Knight | 45 | 25 |
| Legendary Monsters Skeletosaurus | 50 | 35 |
| Legendary Monsters Fractured Apostle | 50 | 25 |
| Legendary Monsters Withered Abomination | 55 | 35 |
| Legendary Monsters Endersent | 55 | 35 |
| Legendary Monsters Shulker Mimic | 60 | 35 |
| Legendary Monsters Annihilation Pursuer | 65 | 35 |

Cataclysm Amethyst Crab remains 25. Legendary Monsters' registered Possessed Paladin, Cloud Golem and Obliterator remain full bosses at 55/70/90 minimum respectively. Cataclysm minibosses associated with a newly level-55 eye boss use minimum 25, so they remain below half that boss's minimum. All 16 eye-boss minimums are below 60; the Ender Dragon minimum is 75. Helvar is the Underworld Knight: the final eye-boss requirement supersedes the earlier proposed minimum 60, leaving him at 55. The registered Paladin uses the current `IAnimatedBoss` implementation, despite its misspelled registry ID `posessed_paladin`; it is not classified from the unused older miniboss class with a similar name.

## Configuration and saves

`config/tnkprogression.json` adds `bossBalanceVersion: 2` and the editable `minibosses` ID set. `bossBaseLevels` remains the existing public configuration map. All shipped miniboss minimums are at most 35. Their combat levels can grow to 100; 35 is a minimum-setting policy, not their combat ceiling. Explicit Warden minimum 35 takes precedence over comparisons with other vanilla bosses.

On the first upgrade, matching old shipped values are replaced with new values. Custom values and deliberately removed old roster entries are retained. The newly supported Last of Deepslate entry is added when absent. The revision prevents future reloads from repeatedly applying this migration. An existing custom value identical to an old shipped default cannot be distinguished from that default.

Changed minimums apply to newly assigned enemies. Existing saved `tnk_level` values are retained, except non-arena miniboss levels above 100 are clamped once through the normal join path. Attribute modifiers are replaced by the existing stable IDs, and health is never replenished on reload. Character XP, skills, origins, titles, HUD preferences and Loot reward handling are unchanged.

## Source references

- [Apotheosis 1.21 source](https://github.com/Shadows-of-Fire/Apotheosis/tree/84e27e7031c7e2ab2f2e8328daf491c88203fcbc): `mobs/ApothMobEvents.java`, `mobs/types/Invader.java`, `mobs/types/Elite.java`.
- [Legendary Monsters 1.21.1 source](https://github.com/Miauczel/Legendary-Monsters-1.21.1-NeoForge/tree/f412a3e5a583705502dd924aee9a0d672a607e76): registered entity implementations and `IAnimatedMiniBoss`/`IAnimatedBoss` classes.

- Archaion `archaion-1.21.1-1.4.4.jar` ([author file](https://www.curseforge.com/minecraft/mc-mods/archaion/files/8983496)): verified `archaion:last_of_deepslate` in `ACEntityTypes` registration bytecode and its language resources.
