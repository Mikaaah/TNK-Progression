# Character screen, HUD and title perks — 0.4.2-beta.1

Minecraft 1.21.1 · NeoForge 21.1.262+ · Java 21. Replace the progression JAR on server **and clients**; the network protocol is now version 3. No command is needed to open the interface.

## Character interface

Press **I** by default. Rebind **Open Character** under Controls → Towns & Kingdoms if another mod uses I. Escape or the same key closes the screen. It does not pause the game.

The warm brown, bronze and parchment screen uses the same restrained palette as the current skilltree and supplied prefixes. A live, mouse-following player model shows the actual player's skin and equipment. The layout scales to the window and GUI scale; clipping and pointer coordinates account for that scale.

- **Character:** level, character XP, current campaign ceiling and next milestone, earned/spent skill points where Pufferfish is available, the pinned activity and recent completed milestones.
- **Titles:** all/owned filter, paged collection, locked-title requirements, active perk previews, equip and hide buttons. Locked titles can be inspected but cannot be equipped. No quest or skill purchase can be forged from the GUI.
- **Activities:** all seven activity tracks with completed milestone rank, current weighted work, next work requirement and XP reward. Track any one on the compact HUD.
- **HUD toggle:** hide/show the compact overlay directly from the screen. The preference and pinned activity persist across reconnects and server restarts.

The compact overlay sits above the lower-left vanilla HUD area. It shows the player's skin face/name/level, character XP and pinned activity. It hides with F1, in spectator mode and while another screen is open, including chat. Health/hunger/vanilla XP remain Minecraft's existing HUD elements. Other packs may have overlays in the same area; use the visibility toggle if needed until their preferred placement is checked together.

Each player receives only their own progression view. Idle snapshots are suppressed, and the full title catalog is sent on login, changes and UI refreshes; ordinary updates carry the small activity/XP view. Client actions contain an action and validated choice, with no target UUID, XP award, level, title unlock or perk amount. The server checks ownership, tracking IDs, cooldowns and request rate.

## Equipped titles replace aptitudes

The 43 earned titles now have modest **active perks**. Only the equipped title applies: owning many titles does not stack them. Hiding a title removes its perks while keeping ownership. Unlocking a new title preserves the current choice, as before.

Examples:

| Title | Equipped benefit |
|---|---|
| Survivor / Taskmaster / Unbroken | +2 maximum health (one heart), +1 armor |
| Adamant | +8% block breaking speed |
| Wanderer | +3% movement speed, +1 block safe fall distance |
| Creator / Planner | +5% block breaking speed, +0.25 block interaction reach |
| Slayer | +4% attack damage, +2% attack speed |
| Sentinel | +2 armor, +5% knockback resistance |
| Arcane | +1 maximum health; +5% Iron's Spells spell power when the attribute is installed |
| Mystic | +1 maximum health; +5% Pufferfish tagged magic damage when available |
| Oracle | +1 maximum health, +2 blocks safe fall distance |

The UI marks an unavailable optional attribute. Pufferfish magic damage follows its supported damage tags; it is not a promise that every third-party spell is boosted. Missing optional mods never stop ordinary perks or the character screen from working.

There is a **30-second title-swap cooldown** by default, enforced by the server and shared with command-based selection. Hiding is always allowed, but does not erase the cooldown. Perks use stable transient modifier IDs, remove the previous title's modifiers and reapply on login/respawn. Increasing maximum health never heals; reducing it clamps current health if needed. Admin revocation also removes active perks. No separate aptitude point system is added.

`config/tnk-title-perks.json` contains the complete ID-to-perk mapping and cooldown. It is created from the bundled defaults. Invalid definitions keep the previous file's active configuration. `/tnkprogress title reload` reloads title definitions and perks and refreshes online players. Original title ownership/quest commands remain compatible.

**No identification discount is implemented.** Title perks do not change the XP curve, equipment tier/rarity or progression gates. Conditional abilities such as Second Wind are deferred; the implemented effects are always-on attributes while the selected title is equipped.

## Release balance preset

The mod now ships a numerical release preset, rather than the earlier placeholder curves. Its complete settings are in `BALANCE-PRESET.json`; derived pacing and passive bounds are in the CSV reports. These are inspectable modelling assumptions, not measured full-modpack playtimes.

- Next-level XP: `floor(150 + 45 × level^1.35)`, for **963,270 character XP from 1 to 100**.
- Activity requirement: `floor(baseRequirement × (1 + 0.12 × completedRank)^0.65 + completedRank)`.
- Activity reward: `floor(baseReward × (1 + 0.12 × completedRank)^0.50)`.
- Requirements increase strictly and effort grows faster than rewards. Activities remain indefinitely repeatable with no daily/lifetime rank cap.
- Mob levels now use the nearest living survival/adventure player in the same dimension, with a random −4 to +4 offset clamped to 1–100. Creative, spectator and fake players are excluded. Without an eligible player the level is 1. Existing saved mob levels remain fixed. No distance-from-spawn, depth or dimension baseline is used.
- Mob scaling: +3% base maximum health and +1.25% base attack damage per saved mob level above one. At level 100 this is 3.97× base health and 2.2375× base attack damage. Existing stable modifier IDs prevent stacking.
- Exploration records every new chunk, but credits at most one new chunk every five seconds, limiting speed/teleport XP bursts without banking skipped chunks for later.
- The skilltree keeps all 240 nodes, one point per node and the 100-point limit. Its restrained existing values were audited with the added title budgets; no cosmetic/layout/node-ID changes or skill resets are needed.

| Activity | First requirement | First XP reward |
|---|---:|---:|
| Mining | 100 eligible blocks | 800 |
| Combat | 10 weighted units | 2,000 |
| Fishing | 20 valid catches | 3,000 |
| Exploration | 30 credited new chunks | 1,500 |
| Farming | 40 mature harvests | 1,000 |
| Crafting | 30 audited crafting actions | 1,800 |
| Engineering | 10 audited work units | 4,500 |

The optional campaign bridge now unlocks the applicable ceiling **before** paying engineering commission work. The release mapping is: chapter 1 → 30; chapter 3 → 50; chapter 5 → 70; chapter 6 → 80; chapter 8 → 94; chapter 10 → 100. It also catches up gates for existing completed chapters without duplicating commission claims. These use the default configured ceilings; edit the bridge if using a custom campaign/ceiling scheme.

TNK Loot's agreed **2% combined mob equipment roll, separate 2% chest roll, exact tier percentages and rarity weights remain intact**. The optional supplied `tnkloot/tnk-beta.json` preset records those same weights. It does not overwrite pack-specific equipment pools. For reference, a 0.1% Mythic rarity weight after a 2% equipment roll averages one Mythic token per 50,000 eligible kills once eligible; a T7 Mythic at the 5% T7 bracket averages one per 1,000,000. These independent-roll probabilities are deliberate retained inputs, not a guarantee or pity system.

The conservative whole-tree audit bounds base melee+sword bonuses at +13.5%, tagged magic at +9.5% and maximum health at +4 before the selected title. A title adds at most +2 health in the supplied preset, +4% attack damage, +5% tagged spell/magic damage or a comparable utility bonus. No 100-point build can exceed the corresponding full-tree bound, and it cannot reach every bound simultaneously. Actual equipment affixes, school tags, boss gear and other mods still need a full-pack balance playtest; calling the preset empirically final would require those results.

## Updating existing worlds

Known unchanged beta activity/mob defaults migrate to the release preset. Customized starting values remain intact. `config/tnk-balance.json` exposes the curve settings separately. Legacy activity requirements are retained for migration so offline players can join after a later restart.

Personal levels, unlocks, purchased skills and titles remain. On the first load of an older profile, character XP and activity work are rebased to preserve their **percentage completion** at the same level/rank. This avoids free levels from carrying a large old XP/work bar into a cheaper curve. Max-level/capped rules and duplicate quest claims remain authoritative. Existing engineering/title claims are not reset.

Install the optional KubeJS bridge update to connect the preset's chapter gates. Do not replace a customized quest book or copy example configuration files over intentional custom settings without reviewing them. No Loot JAR update or skilltree reinstall is required.

## Verification

The server smoke suite checks title attribute application/removal, nonstacking refreshes, no swap healing, cooldown enforcement, invalid client actions, title ownership, tracking/HUD persistence, payload serialization and proportional migration. The opt-in integrated client test joins a disposable world, opens the actual screen with its keybind, equips a title through the UI and server payload, verifies the synced health perk, pins mining, closes with the keybind and captures the rendered views. It also exposed and fixed a pre-snapshot first-login badge crash.

The client fixture uses a disposable vanilla/NeoForge world and supplied prefix pack, not the complete TNK modpack. Installed optional magic/skills integrations, external chat/HUD mods and real multiplayer remain pack-specific checks. The UI smoke flag changes its test profile and should only be used in a disposable test world.

Mob lookup caching, natural despawn verification and the original-mod Apotheosis comparison are documented in `MOB-PERFORMANCE-AND-COMPAT.md`.
