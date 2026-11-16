# TNK Progression 1.1.5 — quest title rewards

Add these yourself as **FTB Quests → Command Reward**, with **permission level 2**. Paste commands without a leading slash. `{p}` is FTB Quests’ verified placeholder for the player claiming the reward.

The placements below describe the intended reward quests; they do not assert that those exact quest names or milestones already exist in your quest book. No quest files or reward bindings were changed. Progression does not automatically grant these titles for boss kills, character levels or old activity ranks.

Use `earn` for quest rewards: repeating it does not duplicate ownership, and it respects administrator revocations. All existing IDs remain compatible.

## Chapter completion rewards

| Complete | Title | Command reward |
| --- | --- | --- |
| Chapter I — Getting Started | Apprentice | `tnkprogress title earn {p} tnk:apprentice` |
| Chapter II — Andesite (Kinetic) | Technician | `tnkprogress title earn {p} tnk:technician` |
| Chapter III — Copper (Sealed) | Engineer | `tnkprogress title earn {p} tnk:engineer` |
| Chapter IV — Brass (Precision) | Specialist | `tnkprogress title earn {p} tnk:specialist` |
| Chapter V — AE2 (Calculation) | Expert | `tnkprogress title earn {p} tnk:expert` |
| Chapter VI — Light Engineering (Steel) | Master | `tnkprogress title earn {p} tnk:master` |
| Chapter VII — Heavy Engineering (Aluminium & Oil) | Grandmaster | `tnkprogress title earn {p} tnk:grandmaster` |
| Chapter VIII — Magic & Mana | Champion | `tnkprogress title earn {p} tnk:champion` |
| Chapter IX — Radiant & Shadow | Legend | `tnkprogress title earn {p} tnk:legend` |
| Chapter X — Creative Core | Monarch | `tnkprogress title earn {p} tnk:monarch` |

## Adventure and specialist quest rewards

Put each command on the final quest or milestone that proves the listed achievement. Project-board thresholds are authored quest milestones, not hidden native counters.

| Intended quest completion | Title | Command reward |
| --- | --- | --- |
| the Adventure milestone for your first project-board assignment | Recruit | `tnkprogress title earn {p} tnk:board_recruit` |
| the Adventure milestone for three project-board assignments | Citizen | `tnkprogress title earn {p} tnk:board_citizen` |
| the Adventure milestone for all seven project-board assignments | Taskmaster | `tnkprogress title earn {p} tnk:board_taskmaster` |
| the Adventure preparation quest for a Backpack, Hopper Upgrade and Pickup Upgrade | Survivor | `tnkprogress title earn {p} tnk:prepared` |
| the Adventure storage and transport milestone | Planner | `tnkprogress title earn {p} tnk:planner` |
| the Adventure building milestone | Architect | `tnkprogress title earn {p} tnk:architect` |
| the Iron's Spells questline in Magic & Mana | Arcane | `tnkprogress title earn {p} tnk:irons_arcane` |
| the Ars Nouveau questline in Magic & Mana | Mystic | `tnkprogress title earn {p} tnk:ars_mystic` |
| the Witchery questline in Magic & Mana | Oracle | `tnkprogress title earn {p} tnk:witchery_oracle` |
| Adventure > Defeat Ender Dragon | Slayer | `tnkprogress title earn {p} tnk:dragon_slayer` |
| Adventure > Defeat Wither | Vanquisher | `tnkprogress title earn {p} tnk:wither_vanquisher` |
| Adventure > Defeat Ignis | Inferno | `tnkprogress title earn {p} tnk:ignis_inferno` |
| Adventure > Defeat Harbinger | Conqueror | `tnkprogress title earn {p} tnk:harbinger_conqueror` |
| Adventure > Defeat Leviathan | Tidal | `tnkprogress title earn {p} tnk:leviathan_tidal` |
| Adventure > Defeat Netherite Monstrosity | Titanium | `tnkprogress title earn {p} tnk:monstrosity_titanium` |
| Adventure > Defeat Ancient Remnant | Ancient | `tnkprogress title earn {p} tnk:remnant_ancient` |
| Adventure > Defeat Naga | Hunter | `tnkprogress title earn {p} tnk:naga_hunter` |
| Adventure > Defeat Twilight Lich | Phantom | `tnkprogress title earn {p} tnk:lich_phantom` |
| Adventure > Defeat Ender Guardian | Sentinel | `tnkprogress title earn {p} tnk:guardian_sentinel` |

## Automatic titles — no quest command needed

These still require real Pufferfish Skills purchases; removing activity ranks does not replace or satisfy skill-tree requirements.

| Title | Requirement |
| --- | --- |
| Initiate | spend 10 Skill Points in the Character skill tree. |
| Disciplined | spend 25 Skill Points in the Character skill tree. |
| Veteran | spend 50 Skill Points in the Character skill tree. |
| Paragon | spend 100 Skill Points in the Character skill tree. |
| Wanderer | unlock Ranger Mastery in Character > Hunter > Ranger. |
| Adamant | unlock Miner Mastery in the Character skill tree. |
| Caretaker | unlock Cook Mastery in the Character skill tree. |
| Creator | unlock Blacksmith Mastery in the Character skill tree. |
| Unbroken | unlock the first Blacksmith Defensive Keystone in the Character skill tree. |
| Adventurer | unlock Hunter Mastery in the Character skill tree. |

## Staff awards — keep out of ordinary quest rewards

Run these manually as an administrator, replacing `<player>` with the player name.

```text
tnkprogress title grant <player> tnk:beta_tester
tnkprogress title grant <player> tnk:owner
tnkprogress title grant <player> tnk:admin
```

Founder is for beta participation. Owner and Admin remain cosmetic-only, command-only titles and grant no permissions. Owner uses the original clean blue `PODSTAWOWY` badge.

## Catalogue cleanup and saves

New players see **42 distinct titles**: 29 quest rewards, 10 automatic skill titles and 3 staff awards. The 12 subclass variant badges and 4 unbound Let’s Do variants are retired. They are retained under **Legacy Titles** only for players who already own them, including their original artwork, passive bonuses, saved selections and revocations. Duplicate custom names are also archived without rewriting their definitions.

Existing title descriptions migrate only when they still match the old bundled descriptions. Custom descriptions are retained. `config/tnk-title-legacy.json` stores archived definitions; `tnk-title-perks.json` is unchanged. No equipment, identification, loot or XP rules are changed.

FTB Quests placeholder and permission handling were checked against `FTBTeam/FTB-Quests` 1.21.1/main commit `d76327d69f9392961746c6e3fd6e976b88308ef8`, `CommandReward.claim()`.
