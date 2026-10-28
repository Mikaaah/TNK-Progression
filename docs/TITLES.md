# Earned titles

**0.4 update:** the new [character screen](CHARACTER-HUD.md) opens with I and equips titles without commands. Equipped title perks now replace aptitudes; only one applies, with a swap cooldown. The collection, quest IDs and grant/revocation semantics below remain valid.

Titles are collectible, personal achievements. A player can earn many and equip **one**, displayed between their character-level badge and their name in standard chat and the normal overhead nameplate: `42 [MYSTIC] Mikaaah`. Titles never change skill points, character XP, equipment gates or permissions. Equipped title attribute perks are documented in the character-screen guide.

The first title equips automatically by default. Later unlocks preserve the player's choice. Hiding a title keeps the collection and remains hidden after future grants. Earned titles persist across death, logout, team changes and skill respecs: a skilltree title records an achievement, not the player's current class or build. Existing display-name content and scoreboard-team visibility remain in place; third-party chat/nameplate replacements need in-pack checking.

## Player commands

| Command | Result |
|---|---|
| `/tnkprogress title list` | Clickable list of owned titles; select a title by clicking |
| `/tnkprogress title list 2` | Next collection page |
| `/tnkprogress title catalog` | Browse all titles and their requirements, including locked ones |
| `/tnkprogress title select tnk:ars_mystic` | Equip an owned title |
| `/tnkprogress title clear` | Hide the title, retaining the level badge |

Players cannot equip a locked or unknown title. The server validates selection; clients cannot set another player's title.

## FTB Quests rewards

Add an FTB Quests **Command Reward** using permission level **2**:

```text
tnkprogress title grant {p} tnk:ars_mystic
tnkprogress title grant {p} tnk:board_taskmaster
tnkprogress title grant {p} tnk:dragon_slayer
```

Choose the command for the particular reward. Use `{p}` for the claimant rather than a nearest-player selector. If each team member should collect their own title, make the reward claimable by each player. Titles can be placed on any item, kill, advancement, chapter milestone or other supported quest; the mod does not require a new reward type or hard dependency on FTB Quests.

Operator/server commands also include:

```text
tnkprogress title grant <player> <title_id>
tnkprogress title revoke <player> <title_id>
tnkprogress title earn <player> <title_id>
tnkprogress title reload
```

`grant` explicitly unlocks/restores a title. `earn` is idempotent and respects an administrator's revocation, for automatic integrations. `revoke` removes the title and its active prefix, and blocks automatic reacquisition. Explicit `grant` restores it. None of these commands forces a different equipped title once the player has made a selection. Target players must be online; the quest bridge catches up offline players when they log in.

## Included quest bindings

Merge the shipped `kubejs/` folder into the pack's existing `kubejs/` directory. It contains a title script and `kubejs/config/tnk-title-quests.json`; preserve the pack's other scripts. This bridge requires KubeJS and its FTB Quests integration, following the existing pack's FTBQuests APIs. It checks current team quest completion at login and when a referenced quest completes, then earns personal titles for online members. Once earned, titles remain personal when leaving a team. Joining a team with completed quests can unlock their completed quest titles at login; use per-player command rewards instead for strictly personal claims.

There are **24 active rules**, all mapped to real quest IDs from TownsAndKingdoms commit `e9ce1934d32d385c0423941fc679912089dab0c3`:

- Ten campaign-completion titles: Apprentice through Monarch. These reward **completing** each chapter; the old tier-entry role proposal is not the active mapping.
- Project board: Recruit after one of the seven assignments, Citizen after three, Taskmaster after all seven. The introductory reading quest does not count.
- Survivor for the backpack, Hopper Upgrade and Pickup Upgrade item quests; Planner for the storage guide; Architect for the building guide.
- Arcane for the Iron's Spells guide; Mystic for Ars Nouveau; Oracle for Witchery. Each requires **all quests in its mapped guide**, including optional quests, rather than assuming a chapter is completed when only its nonoptional quests are done.
- Actual kill quests: Ender Dragon → Slayer; Harbinger → Conqueror; Ignis → Inferno; Twilight Lich → Phantom; Ender Guardian → Sentinel.

The project-board and most magic-guide tasks currently use manual checkmarks. These titles follow the authored quest book; they do not independently prove that a building, ritual or spell loadout works. Boss titles use real kill tasks and are not granted by reading guides or owning a boss drop.

The current quest source has **no separate Let's Do chapters** and no suitable kill quests for five additional catalog bosses. Nine binding templates therefore have empty quest lists and remain inactive: Let's Do Creator/Host/Caretaker/Expert, plus Wither, Leviathan, Netherite Monstrosity, Ancient Remnant and Naga titles. Attach the provided command reward directly to those future quests, or fill each template's `quests` list with their actual 16-character quest IDs and set `required` to the desired completion count. Empty lists never award anything. No quest IDs or completion events were invented.

The bridge does not rewrite existing quest files. Changes to its bindings require reloading server scripts or restarting; `/tnkprogress title reload` reloads the **title catalog**, not KubeJS bindings. If changing existing quest IDs, update the bindings too. Without the optional bridge, direct FTB Command Rewards still work.

## Skilltree achievements

Pufferfish's Skills is optional. Its public Category API supplies actual `getSpentPoints` and `streamUnlockedSkills` values; no achievement is inferred from character level or unused points. The server checks every five seconds and on login:

| Title | Actual condition in `tnk:character` |
|---|---|
| Initiate | 10 points spent |
| Disciplined | 25 points spent |
| Veteran | 50 points spent |
| Paragon | 100 points spent |
| Wanderer | `wayfarer_10` unlocked |
| Adamant | `delver_10` unlocked |
| Caretaker | `homesteader_10` unlocked |
| Creator | `artificer_10` unlocked |
| Unbroken | `survivor_10` unlocked |
| Adventurer | `adventurer_10` unlocked |

These node IDs exist in the shipped 240-node tree. A missing/incompatible optional API or missing category awards nothing, with no character-level substitute. Catalog conditions may combine a minimum point cost and multiple required nodes; all conditions must match. After earning a title, refunding skills does not erase the achievement. Revoked automatic titles stay revoked until explicitly restored.

## Catalog and original artwork

The server creates `config/tnk-titles.json` from the bundled **43-title** catalog. Each entry has a stable ID, readable label, description, original artwork label/style, hover colour and optional skill conditions. Edit this separate file to add titles, change labels/requirements or set `autoEquipFirst` to false. `/tnkprogress title reload` validates atomically; invalid or duplicate definitions keep the previous catalog. Removing a catalog entry hides its display without deleting the earned claim. Do not rename established IDs if you want to preserve ownership.

The combined resource pack replaces the previous number-only pack. It contains all 100 number images and 43 title sprites copied **byte-for-byte** from `advancedpack.zip`, without recolouring, cropping or editing. Number colours remain unchanged. Titles use the selected original colour styles. Original paid artwork stays out of the public source repository.

Without the optional artwork pack, title names remain readable as text such as `[Mystic]`. With it, translation keys select original bitmap glyphs. Unknown custom titles also remain readable until artwork is supplied. The bundled font references Minecraft's default font as a text fallback. Adding custom catalog art requires regenerating the client pack using:

```text
python tools/package_title_prefixes.py advancedpack.zip output.zip config/tnk-titles.json
```

The source image must exist in the provided pack; missing art falls back to text. Labels should match the selected existing artwork. Render height is nine pixels so title and number badges fit a normal chat line. The supplied artwork has no number zero, so zero remains plain text; characters still start at level one.

## Installation and verification

Replace the progression JAR on **server and clients**, and enable the combined number/title resource pack above earlier packs. The network protocol is now version 3, so both sides need this update. The updated mod keeps existing progression data and adds title ownership/selection/revocation to each personal saved profile. No TNK Loot replacement or skilltree datapack change is required.

Run `./gradlew build` and, in a disposable world only, the existing progression smoke server. It now asserts title command permissions, grant/duplicate/select/hide/revoke/restore behaviour, readable fallback names, no duplicate prefixes, packet roundtrip, save/reload and skill conditions. `node tools/check_title_bindings.cjs` exercises the real integration script with controlled completion events, threshold counts and login catch-up. It does not replace running the installed KubeJS/FTB/Pufferfish integrations in the full pack. Client artwork/nameplate rendering and real multiplayer remain in-pack checks.

This update also fixes namespaced IDs in engineering commands and one-time quest XP reward parsing, retaining existing nonnamespaced reward-claim IDs.

## Complete title catalog

| ID | Display | Unlock description |
|---|---|---|
| `tnk:apprentice` | Apprentice | Complete the first Kinetic workshop chapter. |
| `tnk:technician` | Technician | Complete the Hydraulic workshop chapter. |
| `tnk:engineer` | Engineer | Complete the Precision workshop chapter. |
| `tnk:specialist` | Specialist | Complete the Inductive and network chapter. |
| `tnk:expert` | Expert | Complete the Ender machinery chapter. |
| `tnk:master` | Master | Complete the Chemical machinery chapter. |
| `tnk:grandmaster` | Grandmaster | Complete the Expedition frame chapter. |
| `tnk:champion` | Champion | Complete the Containment chapter. |
| `tnk:legend` | Legend | Complete the Singularity chapter. |
| `tnk:monarch` | Monarch | Complete the Sovereign chapter. |
| `tnk:initiate` | Initiate | Spend 10 points in the TNK character skilltree. |
| `tnk:disciplined` | Disciplined | Spend 25 points in the TNK character skilltree. |
| `tnk:veteran` | Veteran | Spend 50 points in the TNK character skilltree. |
| `tnk:paragon` | Paragon | Spend 100 points in the TNK character skilltree. |
| `tnk:skill_wayfarer` | Wanderer | Unlock the Wayfarer outer milestone in the character tree. |
| `tnk:skill_delver` | Adamant | Unlock the Delver outer milestone in the character tree. |
| `tnk:skill_homesteader` | Caretaker | Unlock the Homesteader outer milestone in the character tree. |
| `tnk:skill_artificer` | Creator | Unlock the Artificer outer milestone in the character tree. |
| `tnk:skill_survivor` | Unbroken | Unlock the Survivor outer milestone in the character tree. |
| `tnk:skill_adventurer` | Adventurer | Unlock the Adventurer outer milestone in the character tree. |
| `tnk:board_recruit` | Recruit | Complete your first bulletin-board commission. |
| `tnk:board_citizen` | Citizen | Complete a substantial set of settlement commissions. |
| `tnk:board_taskmaster` | Taskmaster | Complete the bulletin-board milestone quest. |
| `tnk:prepared` | Survivor | Complete the useful-items preparation milestone. |
| `tnk:planner` | Planner | Complete the storage and transport preparation milestone. |
| `tnk:architect` | Architect | Complete the building chapter milestone. |
| `tnk:letsdo_creator` | Creator | Complete the Let’s Do baking or brewing chapter milestone. |
| `tnk:letsdo_host` | Host | Complete the Let’s Do Candlelight or hospitality milestone. |
| `tnk:letsdo_caretaker` | Caretaker | Complete the Let’s Do farming or Meadow chapter milestone. |
| `tnk:letsdo_expert` | Expert | Complete the Let’s Do Vinery chapter milestone. |
| `tnk:irons_arcane` | Arcane | Complete the Iron’s Spells and Spellbooks quest chapter. |
| `tnk:ars_mystic` | Mystic | Complete the Ars Nouveau quest chapter. |
| `tnk:witchery_oracle` | Oracle | Complete the Witchery quest chapter. |
| `tnk:dragon_slayer` | Slayer | Complete the Ender Dragon defeat quest. |
| `tnk:wither_vanquisher` | Vanquisher | Complete the Wither defeat quest. |
| `tnk:ignis_inferno` | Inferno | Complete the Ignis defeat quest. |
| `tnk:harbinger_conqueror` | Conqueror | Complete the Harbinger defeat quest. |
| `tnk:leviathan_tidal` | Tidal | Complete the Leviathan defeat quest. |
| `tnk:monstrosity_titanium` | Titanium | Complete the Netherite Monstrosity defeat quest. |
| `tnk:remnant_ancient` | Ancient | Complete the Ancient Remnant defeat quest. |
| `tnk:naga_hunter` | Hunter | Complete the Naga defeat quest. |
| `tnk:lich_phantom` | Phantom | Complete the Twilight Lich defeat quest. |
| `tnk:guardian_sentinel` | Sentinel | Complete the Ender Guardian defeat quest. |

## Default artwork correction — 0.4.3-beta.1

The delivered private mod JAR now embeds the original number/title fonts and PNGs directly. The companion resource pack is no longer required. All 143 supplied images remain byte-for-byte identical, and title translations are merged with the character key labels. This changes resources only; title ownership, perks, level rules, keybindings and networking are unchanged. Public source-only builds need the supplied archive through `-PtnkArtworkSource=/path/to/advancedpack.zip` to reproduce the private artwork.
