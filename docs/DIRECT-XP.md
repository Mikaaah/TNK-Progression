# Direct character XP — 1.1.0

Release **1.1.0** continues the former **1.5.0-private.1** development build under the CurseForge version number. That development build was based on the actual **1.4.0-private.1** distribution, restoring its source-only Ascension changes on `feat/direct-xp-starcatcher`. The 20 base origins, 15 ascended origins, consumable Sigil of Ascension, title slots, supplied artwork, boss levels, chapter ceilings and Pufferfish Skills integration remain available.

## Progression

Level 1 starts at zero. Advancing from level L costs **1,000 × L XP**. Reaching level 100 costs **4,950,000 XP** cumulatively. Combat, mining, farming, cooking, fishing, exploration and quest commands all fill the same personal character XP bar. Their individual totals are accounting, not separate skill levels. Activity milestones, escalating activity requirements, crafting XP and engineering XP are retired.

| Character level | Total XP from level 1 | Next level |
|---:|---:|---:|
| 10 | 45,000 | 10,000 |
| 30 | 435,000 | 30,000 |
| 50 | 1,225,000 | 50,000 |
| 70 | 2,415,000 | 70,000 |
| 80 | 3,160,000 | 80,000 |
| 94 | 4,371,000 | 94,000 |
| 100 | 4,950,000 | Maximum |

Campaign ceilings remain 10 / 30 / 50 / 70 / 80 / 94 / 100. XP pauses at the current ceiling. Existing character levels and their percentage toward the next level are migrated once; old milestone counters remain in saved data but no longer award XP. The old activity history is archived. Existing origin, title, skill and claim data is preserved. The old balance file is backed up automatically before changing its default curve.

## Default rewards

| Source | Reward |
|---|---|
| Ordinary hostile mob | 10 + 2 × mob level |
| Passive mob | ×0.1 |
| Tough mob, 60+ max health | ×2 |
| Configured miniboss | ×5; includes Deeper and Darker's Stalker |
| Configured boss | ×12 |
| Natural dirt, sand, gravel, netherrack | 0.25 per block |
| Natural stone, deepslate, blackstone, end stone | 1 per block |
| Coal / copper ore | 8 per block |
| Iron / zinc ore | 12 per block |
| Gold / redstone / lapis / quartz ore | 20 per block |
| Diamond / emerald ore | 50 per block |
| Ancient debris | 100 per block |
| Mature crop | 1 per harvest |
| Mature nether wart / cocoa | 2 per harvest |
| Grown pumpkin / melon | 2 per harvest |
| Ripe berries | 1 per harvest |
| Personally loaded food cooked in furnace, smoker, campfire | 2 per output item; configurable item overrides |
| Vanilla fishing | 25 per successful catch |
| Starcatcher common / uncommon / rare | 40 / 75 / 125 per catch |
| Starcatcher epic / legendary / golden | 250 / 500 / 750 per catch |
| New visited chunk | 1 |
| First biome type | 150 |
| First visit to each structure instance | 250 |
| First dimension entry | 750 |

Combat rewards are divided by recent player damage contribution; environmental damage does not become a full kill reward for a token hit. Owned targets, spawn eggs, commands, breeding and split offspring give no XP. Legitimate dungeon, spawner and triggered encounters remain eligible. Damage sources from supported pets/summons still credit their owner.

Mining checks durable position provenance. Placed, moved, fluid-generated and already mined positions give zero XP; Fortune cannot multiply XP. Fake players/machines do not earn XP. Provenance recorded by older releases is honored. Placement before this mod recorded the relevant block type cannot be reconstructed retroactively; mods that bypass placement/fluid/piston events require their own integration. Modded ores use common ore tags, with explicit block overrides taking precedence. Unknown ores default to 12 XP.

Cooking XP is paid **when a recipe completes**, to the player who inserted the raw ingredients by hand. Stay online, in the same dimension and within 64 blocks. Input ownership persists with the block. Hopper/pipe inputs and merely withdrawing finished food give no XP. External removal/replacement of input clears ownership conservatively. Farmer's Delight cooking pots and other custom appliances are not integrated in this build; food cooked through the supported vanilla cooking blocks can use item overrides of 4–8 XP for substantial meals.

Starcatcher is optional. Its completed catch method and actual bobber/rarity are checked; the optional hook also handles catches that spawn an entity instead of an item. Item catch cancellation prevents XP, and one catch pays once. No vanilla open-water restriction is imposed on Starcatcher lava catches. Vanilla rod catches retain the server's open-water setting and minimum hook lifetime. Collecting fish from inventory never pays.

Exploration tracks personal visits across restarts using packed chunk bits. Loading/pregenerating chunks gives no XP. Sampling occurs once a second; a jump over 64 blocks suppresses chunk/biome/structure rewards for that destination sample. The first sample after login/dimension change records the current chunk without paying. Dimension rewards use actual dimension-change events, not login. Existing visited chunk records remain authoritative.

## FTB Quests chapter rewards

Add a **Command Reward** with **permission level 2**, using this literal command (without a leading slash):

```text
tnkprogress xp {p} 150
```

`{p}` is replaced by FTB Quests with the claiming player's name. Apply the same command reward to every quest in the chapter and change only the amount. No unique reward ID is required. FTB Quests owns one-time/repeatable claim behavior. This mod does not edit existing chapters automatically.

| Quest size | Suggested XP |
|---|---:|
| Small | 50 |
| Normal | 150 |
| Major | 400 |
| Boss | 1,200+ |
| Chapter finale | 2,000–5,000 |
| Repeatable | 50–200 |

Quests fill the same character bar and are recorded as **Quest** XP separately from gameplay sources. Command rewards can also target a player directly, e.g. `tnkprogress xp Mikaaah 150`. The old `reward <player> <id> <amount>` interface remains available for integrations that explicitly need a permanent claim receipt. The old `engineering` command is removed. Millwright and Artificer retain their XP perk by applying it to quest command XP (+10% / +35%) instead of the retired engineering activity.

## Server configuration

- `config/tnk-xp.json`: direct payouts, block/tag/item overrides, Starcatcher rarity rewards, mob multipliers and miniboss IDs. Values must be finite and non-negative; zero disables a reward. `/tnkprogress reload` reloads these rewards and the existing progression configuration.
- `config/tnk-balance.json`: versioned character curve and legacy curve retained for migration of offline characters. Restart to apply curve changes consistently.
- `config/tnkprogression.json`: existing boss roster/scaling, chapter ceilings, contribution and harvesting guards. Old activity/crafting/engineering settings are retained for save/config compatibility and have no XP effect.
- Activity Info screens are generated from the server's active rules, including item/tag overrides. The Activities tab and HUD show direct XP totals/recent gains rather than milestone bars.

## Compatibility and verification

Minecraft **1.21.1**, NeoForge **21.1.262+**, Java **21**. Install the same JAR on server and clients; the character payload protocol has changed. Starcatcher remains optional; the integration is targeted at **3.1.4.1 for NeoForge 1.21.1**. No optional boss, fishing or magic mod is newly required.

Build and test commands:

```text
gradle test jar
gradle runServer -PtnkDirectXpSmoke
```

The opt-in smoke harness uses a disposable world and shuts down after logging `TNK_DIRECT_XP_SMOKE_OK` or failure. With the real Starcatcher JAR in `run/mods`, it also exercises completed common, rare, epic and legendary catches and logs `TNK_STARCATCHER_SMOKE_OK`. These automated checks do not replace full TNK3 balance/playthrough testing.
