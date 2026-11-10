# Character guide — 1.1.0

Minecraft 1.21.1 · NeoForge 21.1.262+ · Java 21. Install matching Progression versions on the server and clients.

## Start your character

Press **I**, or click the player portrait in the survival inventory. Your first visit opens the origin chooser. Preview a background’s story and perks, then confirm your choice. The Origin tab shows your current perks afterward.

There are **20 base origins**. Using a **Sigil of Ascension** consumes the item, unlocks **15 ascended origins** and lets you choose again. Your levels, skills, titles and activity progress stay intact. Further reselections require another Sigil or administrator action. [See every origin](ORIGINS.md).

## One character level

Combat, natural mining, farming, cooking, fishing, exploration and quest rewards fill the **same XP bar**. Activity totals show where your XP came from; they are not separate skill levels.

Level 1 starts at zero. Advancing from level L costs **1,000 × L XP**, for **4,950,000 XP** from level 1 to 100. Crafting and engineering do not award automatic XP. [Exact payouts and qualification rules](DIRECT-XP.md).

| Activity | What earns XP |
| --- | --- |
| Combat | Eligible kills, weighted by mob level and your recent damage contribution |
| Mining | Natural, previously unworked block positions; ores pay more than common stone |
| Farming | Supported mature crops and successful ripe-plant harvests |
| Cooking | Food completed in a supported vanilla cooking block you loaded by hand |
| Fishing | Valid vanilla catches and optional Starcatcher catches, including rarity rewards |
| Exploration | First visits to eligible chunks, biomes, structures and dimensions |
| Quests | Server-authorized command rewards, recorded separately from gameplay sources |

Placed or recycled mining positions, machine/fake-player actions, hopper-loaded cooking and inventory pickups do not earn XP. Cooking pots from other mods are not integrated. The activity Info buttons show the active server rules.

## Chapter ceilings and teams

The default configurable ceilings are **10 / 30 / 50 / 70 / 80 / 94 / 100**. Chapter reward commands raise the eligible ceiling. XP earned at the ceiling is not banked for later; unlock the next ceiling before claiming XP rewards.

Character levels, XP, titles and origins remain personal. Optional FTB Teams integration can share chapter unlocks according to the server’s configuration. Team progression is not a shared character XP pool.

## Skill points

Point entitlement is `level + floor(3 × level / 4)`: **17 points at level 10**, **87 at level 50** and **175 at level 100**. The entitlement is synchronized rather than added repeatedly on login.

The **Skill Tree** button opens the configured Pufferfish category. The supplied tree has **588 nodes**, each costing one point, and lives in `config/puffish_skills/`. Keep the pack’s matching Skills/Attributes versions and configuration. [Installation and configuration](../skilltree/rebuilt588/README.md).

## Titles and HUD

Collect titles through configured achievements, quests or administrator awards. The **cosmetic slot** chooses your visible badge; the **passive slot** chooses one earned title’s benefits. They can match or differ. Passive changes have the configured cooldown, normally 30 seconds; cosmetic changes do not reset it. Owner and Admin are cosmetic and do not grant permissions.

Track an activity from Character to show it on the compact HUD. The HUD can be hidden from Character and also respects F1 and spectator mode. [Screen controls](CHARACTER-HUD.md) · [Title slots](TITLE-SLOTS.md).

## Mobs and bosses

Ordinary hostile mobs receive a saved level based on the nearest eligible player’s character level, with the configured variance (normally ±4), clamped to 1–100. Their level stays fixed after assignment.

Configured bosses use the higher of **their base level** and **the nearest eligible player’s level + 10**, capped at **110**. The default roster includes a level-20 floor for the Twilight Lich and a level-75 floor for Ignis. Boss levels are independent of the player’s chapter ceiling.

A targeted mob’s original number badge appears above its name. Leveling does not force ordinary mobs to stay loaded or become persistent. Custom names and existing persistence are preserved. [Mob integration details](MOB-PERFORMANCE-AND-COMPAT.md).

## Commands and configuration

Players can inspect `/tnkprogress status` and `/tnkprogress activities`. Reward, ceiling and origin administration requires permission level **2**. [FTB Quests examples](QUEST-ROLES.md).

| File | Purpose |
| --- | --- |
| `config/tnk-xp.json` | Direct XP payouts, ore/item overrides, fishing rarities and combat multipliers |
| `config/tnk-balance.json` | Character XP curve and legacy migration curve |
| `config/tnkprogression.json` | Chapter ceilings, teams, mob scaling and activity guards |
| `config/tnk-titles.json` | Collectible title definitions and conditions |
| `config/tnk-title-perks.json` | Passive title benefits and swap cooldown |
| `config/puffish_skills/` | External skill tree and TNK conditional passives |

`/tnkprogress reload` reloads progression and direct reward settings. Restart after changing the character curve or skill-tree configuration. The old activity/crafting/engineering fields are retained for compatibility and do not restore their retired XP behavior.

Existing direct-XP development characters retain their progress. Earlier character levels and within-level percentages migrate once; origin, title and skill data is preserved. [Migration details](DIRECT-XP.md) · [Version aliases](VERSIONING.md).

[Project home](../README.md) · [Changelog](../CHANGELOG.md) · [Support](../SUPPORT.md)
