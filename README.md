<p align="center"><img src="https://i.imgur.com/lGbam9k.png" alt="Towns & Kingdoms Progression" width="100%"></p>

<h1 align="center">Towns & Kingdoms Progression</h1>

<p align="center"><strong>A character sheet for the life you build and the adventures you take.</strong></p>

<p align="center"><a href="https://www.curseforge.com/minecraft/mc-mods/towns-kingdoms-progression">Download</a> · <a href="docs/CHARACTER-GUIDE.md">Player guide</a> · <a href="https://github.com/Mikaaah/TNK-Progression/wiki">Wiki</a> · <a href="CHANGELOG.md">Changelog</a> · <a href="https://github.com/Mikaaah/TNK-Progression/issues">Report an issue</a></p>

<p align="center"><img src="https://img.shields.io/badge/Progression-1.1.1-7f384a?style=flat-square" alt="Progression 1.1.1"> <img src="https://img.shields.io/badge/Minecraft-1.21.1-5b8731?style=flat-square" alt="Minecraft 1.21.1"> <img src="https://img.shields.io/badge/Loader-NeoForge-d77828?style=flat-square" alt="NeoForge"></p>

## Make everyday adventures count

Explore unfamiliar places, mine natural deposits, bring in a rare catch or take on a dangerous boss. **Combat, mining, farming, cooking, fishing, exploration and quest rewards** all contribute directly to one personal character level.

- **Your own background:** choose one of 20 origins, with perks for different ways of playing.
- **A new chapter for your character:** consume a Sigil of Ascension to unlock 15 ascended origins and choose again.
- **A build to grow into:** reach level 100 with up to 175 earned skill points for the matching Pufferfish Skills tree.
- **Titles with personality:** collect 58 titles and choose your visible badge independently from your passive title.
- **An adventure that keeps up:** ordinary hostile mobs follow the nearest eligible player’s level; configured bosses also have their own level floors.
- **A readable character sheet:** inspect XP, origin perks, titles and activity details, with a compact optional HUD.

Press **I**, or click the **player portrait in your survival inventory**, to open Character. Change the key under Controls → Towns & Kingdoms.

## Current release: 1.1.1

Optional [Spiral Abyss integration](docs/ABYSS-COMPATIBILITY.md) adds fixed normal-floor levels, verified rotation context and coordinated stat scaling, while respecting patched Unidentified Loot 1.1.3 saved rewards.

Direct XP replaces the old activity milestones. The default journey from level 1 to 100 costs **4,950,000 XP**. Starcatcher catches have configurable rarity rewards, and the Sigil of Ascension uses the supplied purple token artwork. Crafting and engineering no longer award automatic XP.

[What changed](CHANGELOG.md) · [XP rewards and migration](docs/DIRECT-XP.md) · [Version history and aliases](docs/VERSIONING.md)

## Install

1. Put **`tnkprogression-1.1.1-neoforge-1.21.1.jar`** in `mods/` on the server and clients. Replace the previous Progression JAR.
2. For the TNK skill tree, install the pack’s matching Pufferfish Skills/Attributes setup and the external [588-node configuration](skilltree/rebuilt588/README.md). Keep the configuration on both sides.
3. Pack authors can add [quest rewards](docs/QUEST-ROLES.md) and the optional [title integrations](integration/README.md).

| Component | Support |
| --- | --- |
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.262+ |
| Java | 21 |
| Spiral Abyss | Optional; pinned integration for 2.0.5 |
| Unidentified Loot | Maintained TNK 1.1.3 compatibility branch |
| TNK Abyss Rotations | Optional existing bridge 1.0.0 / bundle 1.0.1 |
| Starcatcher | Optional; catch integration verified with 3.1.4.1 |
| Pufferfish Skills / Attributes | Optional integration; required for the supplied skill tree |
| FTB Quests / Teams | Optional integrations for rewards and shared chapter ceilings |

Character XP stays personal. Pack authors control chapter ceilings, rewards and optional team sharing through configuration. Cooking XP currently supports personally loaded vanilla furnaces, smokers and campfires; custom cooking pots need their own integration.

## Find your way around

| Guide | What it covers |
| --- | --- |
| [Character guide](docs/CHARACTER-GUIDE.md) | Starting out, levels, skill points, teams and bosses |
| [Origins and Ascension](docs/ORIGINS.md) | All 20 base and 15 ascended origins, perks and the Sigil |
| [Direct XP](docs/DIRECT-XP.md) | Default rewards, qualifying actions and save migration |
| [Character screen and HUD](docs/CHARACTER-HUD.md) | Controls, activity tracking and title slots |
| [Quest rewards](docs/QUEST-ROLES.md) | Reusable FTB Quests commands and chapter unlocks |
| [Title slots](docs/TITLE-SLOTS.md) | Cosmetic badges, passive benefits and cooldowns |
| [Founder and default titles](docs/DEFAULT-TITLES.md) | Optional pack-wide title awards |
| [Skill tree](skilltree/rebuilt588/README.md) | External configuration and point entitlement |
| [Release verification](docs/VERIFICATION-1.1.1.md) | Checks performed for the delivered JAR |

## Pull up a chair

Share your build, ask a question or help test the next adventure in the **Towns & Kingdoms Modding Hub**.

[![Join the Towns & Kingdoms Discord](https://i.imgur.com/Ojgcccm.png)](https://discord.gg/CRYk3uDPVt)

[Join Discord](https://discord.gg/CRYk3uDPVt) · [Explore all projects](https://mikaaah.github.io/TownsAndKingdoms/projects/) · [Get help](SUPPORT.md)
