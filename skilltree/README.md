# TNK character tree — 0.1.1 icon draft

Minecraft 1.21.1 data pack for Pufferfish's Skills and Pufferfish's Attributes, using the 1.21 configuration format (version 2). Integrates with TNK Progression's default category `tnk:character`. Just Leveling is not needed.

80 nodes: 8 foundation nodes and 12 each for Wayfarer, Delver, Homesteader, Artificer, Survivor and Adventurer. Foundation costs 16 points; each branch costs 24. There are 160 points of choices and a category spending limit of 100. No native experience/exchange files or starting points are supplied: TNK Progression owns the point entitlement. Keystones cost 4 points and require 20 points already spent in the category; this is a spending requirement, not a character-level gate.

## Installation

1. Back up the world and old tree/player skill data. This draft does not migrate the previous tree.
2. Install Pufferfish's Skills, Pufferfish's Attributes and TNK Progression on the appropriate client/server sides. Match the installed Minecraft/loader versions.
3. Generate the ZIPs with `python3 skilltree/tools/build_tree.py`, or use the supplied bundle. Put `TNK-Character-Tree-0.1.1-draft.zip` in the world's `datapacks` directory. Put `TNK-Skilltree-Icons-0.1.1.zip` in each client's `resourcepacks` directory and enable it in Resource Packs. A dedicated server can distribute that resource pack using its normal server-resource-pack configuration. Both ZIPs have `pack.mcmeta` at their root.
4. Remove/disable the previous character tree rather than allowing it to award a second point pool. Do not load two data packs defining `tnk:character`.
5. Run `/reload`, then reconnect so TNK Progression synchronizes the player's current level entitlement. Open Pufferfish's Skills with its configured key. Leave `skillCategory` in `config/tnkprogression.json` set to `tnk:character`.

For standalone inspection without TNK Progression, an administrator can award temporary test points using Pufferfish's own commands. Those are not part of normal TNK progression and must be removed before pack testing.

## Editing

At https://puffish.net/skillsmod/editor/ import, in order, `definitions.json`, `skills.json`, and `connections.json` from `datapack/data/tnk/puffish_skills/categories/character/`. Export skills and connections after layout changes. Edit definitions to change rewards and point costs. The web editor displays generated preview symbols; it does not render custom Minecraft texture icons. In-game definitions now reference the selected HAS icons in the accompanying resource pack. The in-game background remains warm sandstone. The icon resource pack is required on every client.

`tools/build_tree.py` reproduces the supplied tree and catalogue. Manual editor changes must also be reflected in the generator before regenerating, otherwise regeneration replaces them.

## Scope and balance

Every node has an attribute reward; there are no inactive placeholder custom passives. The attribute draft intentionally uses anvil discounts and vanilla-orb XP in Artificer. It does not implement identification discounts, salvage bonuses, fishing or crop-drop bonuses, emergency cooldown passives, classes or subclasses. Those require separate implementation/design. Homesteader currently supports food endurance, recovery and tamed creatures.

Vanilla XP bonuses do not award character XP. Fortune only affects block drops. No node changes unidentified equipment tiers, rarity eligibility, quest ceilings or character-XP rewards. Modifiers are removed by Pufferfish when their node is refunded. Interactions with Apotheosis gear and modded damage tags require pack balance testing.

## Verification

- Generator checks: 80 unique node positions, all reachable, 160 total point cost, 24 per branch.
- Imported all three files into the official live web editor and visually checked the layout.
- Exported skills and connections from the editor; all 80 node records and 79 directed connections matched the generated configuration. Non-root `root: false` fields are explicit to match editor exports.
- Icon update: all 80 node texture references and the category texture resolve to transparent 16x16 PNGs in the client pack. Rewards, node positions, connections, costs and spending limit are unchanged. Selected sprites match the exact source atlas pixels.
- Reward schemas checked against the upstream 1.21 source. No in-game load, mod-version registry, respec or multiplayer verification has been completed for this data pack yet. Treat this as a reviewable draft, not a verified pack release.

See `NODE-CATALOGUE.md` for every effect and cost.

## Artwork credits

Icons from the user-supplied HAS IconPack (v.1.2), by Aleksandr Makarov / @IKnowKingRabbit. Only selected sprites integrated into the TNK skilltree are included. See `resourcepack/CREDITS.txt` and `resourcepack/HAS-LICENSE.txt`. `icon-map.json` records every source sprite rectangle for maintenance. The original purchased asset archive is not part of this repository.
