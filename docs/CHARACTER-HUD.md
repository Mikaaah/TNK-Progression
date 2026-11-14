# Character screen and HUD — 1.1.2

## Open Character

Press **I** from the world, or use the **player portrait replacing the survival inventory’s recipe-book button**. Rebind the key under Controls → Towns & Kingdoms. A screen opened through the inventory returns there when closed; opening directly returns to the world. The screen does not pause gameplay.

The UI shows your actual player model and equipment, original number/title badges and the pack’s purple accents. Headings and panels follow the existing TNK character theme.

## Tabs and controls

| Area | What it shows |
| --- | --- |
| Character | Personal level, XP, chapter ceiling, earned/spent skill points and recent gains |
| Activities | Direct XP totals by source, activity tracking and server-backed Info buttons |
| Origin | Your selected background, story and active perks |
| Titles | Earned/locked titles, cosmetic and passive choices, benefit previews and cooldowns |
| Skill Tree | Opens the configured Pufferfish category when the optional integration is available |
| HUD | Compact character XP and the pinned activity; show/hide from Character |

Activities are **combat, mining, farming, cooking, fishing, exploration and quests**. Their totals explain your XP sources rather than separate levels. Crafting and engineering are no longer earning tracks. [Exact reward rules](DIRECT-XP.md).

## HUD default for modpacks

Starting with 1.1.2 the character HUD is **hidden by default**. In `config/tnkprogression.json`, pack authors can set:

```json
"hudEnabledByDefault": false
```

This is a top-level setting. Existing configurations receive it automatically. It applies to players who have never chosen a HUD preference, including existing characters. A player's explicit show/hide choice remains saved and takes priority. Press **I** and use the **HUD: Hidden / HUD: Shown** button to change your own preference.

Ship this configuration on the server and in the modpack for singleplayer. The server sends the resulting preference to the client; this is not a separate client-config file. Restart or use `/tnkprogress reload` after editing.

## Origins and Ascension

Your first Character visit offers 20 base origins. Using a **Sigil of Ascension** unlocks 15 ascended origins and reopens selection without clearing your other progress. The Sigil’s purple icon is included in the mod; no extra resource pack is needed. [Origin catalog](ORIGINS.md).

## Cosmetic and passive titles

The **cosmetic title** controls the displayed badge. The **passive title** provides one unlocked title’s benefits. The slots can match or differ, and each has its own clear control. Cosmetic changes do not reset the passive swap cooldown, normally 30 seconds.

Title benefits do not stack through repeated selection, and maximum-health changes do not heal the player. Owner/Admin remain cosmetic and never grant operator permissions. [Title details](TITLE-SLOTS.md).

## Mob badges

Targeted leveled mobs show a centered number badge above their existing name. Normal visibility, F1, invisibility, range and team-name visibility rules apply. Existing custom and affixed names retain their text and styling.

Bosses can display levels up to 110; players remain capped at 100. The mod preserves ordinary despawning and existing persistence flags. [Mob behavior and compatibility](MOB-PERFORMANCE-AND-COMPAT.md).

## Installation

Install **1.1.2** on server and clients. Original origin icons, 100 number badges, 58 title badges, the token texture and UI resources are embedded in the JAR. The 588-node skill configuration remains external. Full-pack GUI and multiplayer playtesting remains separate from the checks listed in [release verification](VERIFICATION-1.1.2.md).

[Project home](../README.md) · [Character guide](CHARACTER-GUIDE.md)
