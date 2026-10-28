# TNK Character Web — 0.2.1 theme draft

Minecraft 1.21.1 datapack for Pufferfish's Skills and Pufferfish's Attributes. TNK Progression supplies the character-level point entitlement to category `tnk:character`. Just Leveling is not required.

## The new tree

240 selectable nodes, every one costing exactly **1 point**. Players can spend at most **100 points**. Six branches contain 34 nodes each, with 12 foundation nodes and 24 hybrid crossroads connecting neighbouring branches. The graph has 362 bidirectional links and 123 independent cycles. Multiple paths lead through each cluster; there are no mutually exclusive branches and no extra point pools. Large outer keystones also cost 1 and require 40 category points already spent. That is a spending requirement, not a character-level or quest gate.

The old tree screenshot supplied by the user is a visual layout reference only. The configuration and effects are implemented for Pufferfish, not for the mod in that screenshot. The HAS icons and artist credit are retained.

## Install

1. Back up the world and existing player skill data. If an earlier draft has been used, respec through Pufferfish before swapping packs. The original 80 node IDs remain, but costs and routing have changed; this release does not perform an automatic build migration.
2. Replace the earlier character-tree datapack with `TNK-Character-Web-0.2.1-draft.zip` in the world's `datapacks` directory. Do not enable two packs defining `tnk:character`.
3. Place `TNK-Skilltree-Icons-0.2.1.zip` in each client's `resourcepacks` directory and enable it. Servers can distribute this using normal server-resource-pack configuration. The selected sprite files are the same ones used in the preceding icon draft.
4. Run `/reload` and reconnect so TNK Progression synchronizes points. Leave its `skillCategory` set to `tnk:character`.

The category supplies zero starting points and has no native experience or exchange config. TNK Progression grants one earned point per character level, including the starting point at level 1. Its milestone ceilings and loot eligibility remain separate from this tree.

## Preview and editor

`TNK-Character-Web-Preview.jpg` draws the real node positions, links and packaged sprites. `TNK-Wayfarer-Closeup.jpg` gives a closer view of one branch. These are layout diagrams, not game captures. They now load the exact packaged 26px custom frame textures and atlas background, with the same 26:16 frame-to-icon ratio used by Pufferfish. `TNK-Frame-States.jpg` shows all 20 frame sprites at enlarged pixel scale. The in-game configuration selects these real textures rather than vanilla advancement frames or sandstone.

Import `definitions.json`, `skills.json` and `connections.json`, in that order, from `datapack/data/tnk/puffish_skills/categories/character/` into https://puffish.net/skillsmod/editor/. This editor uses generated symbols rather than the custom Minecraft texture icons. Export skills/connections after editing. All normal links are bidirectional, so return paths and sideways movement work.

## Rebuild

`python3 skilltree/tools/build_tree.py` regenerates config, catalogue, layout metadata, icon map, validation summary and both installable ZIPs. `python3 skilltree/tools/render_preview.py` regenerates the previews (requires Pillow). `tools/web_rewards.py` holds the new small passives. `tools/build_theme.py` generates native 26x26 frames for four node roles across five states, plus frame/background/connection-colour config. The generated atlas PNG is a separate project asset retained in the resource pack. Pillow is now required to build the theme. Manual JSON/editor changes must also be reflected in these generators before regeneration.

Every node has an actual attribute reward. Original 80 rewards are retained, with 160 smaller foundation, branch and hybrid rewards added; the outdated original full-branch bonus descriptions have been removed. The icon map reuses selected HAS sprites for related effects. No extra source asset archive is required to rebuild.

## Scope and validation

This is a reviewable attribute-tree draft. It does not add class/subclass selection, custom identification/salvage discounts, emergency cooldown abilities or fishing/crop-drop mechanics. Artificer supports anvil XP costs, vanilla-orb XP and tool convenience. Homesteader supports food endurance, recovery and tamed creatures. Vanilla XP rewards do not grant character XP; Fortune affects block drops, not equipment tier or rarity.

Validation checks: 240 unique positions, all rewards present, every cost exactly 1, a single root, all nodes reachable within the 100-point budget, 34 nodes per branch, at least 60 independent cycles, and at least 35 layout units between centres. The official editor import/export roundtrip preserved all 240 nodes and 362 bidirectional connections. All 240 texture references resolve, and the original 80 rewards are retained. `validation-summary.json` records measured results. In-game loading, respec behaviour, mod-version attribute registration and complete-pack balance are still unverified. Test in a disposable world first.

## Credits

Skill icons: Aleksandr Makarov / @IKnowKingRabbit, from the user's HAS IconPack (v.1.2). Selected sprites are included as part of the TNK skilltree. See `resourcepack/CREDITS.txt` and `resourcepack/HAS-LICENSE.txt`. The original purchased archive and the user's old-tree reference image are not distributed in this repository.

## 0.2.1 theme verification

The exact uploaded `puffish_skills-0.19.2-1.21-neoforge.jar` accepted all four custom frame definitions (20 state textures), the background definition and colour configuration through its own Java parsers. Its renderer was inspected to confirm 26px frame and 16px icon dimensions. All 1,200 node-state references resolve to 20 RGBA PNGs, and the background reference resolves to the opaque atlas PNG. Rewards, all one-point costs, IDs, positions and 362 links are unchanged from 0.2.0; visual node sizes are slightly larger for icon readability. This does not establish a successful Minecraft client launch.

**Version note:** the uploaded JAR declares Minecraft `[1.21,1.21.1)`, which excludes 1.21.1. A pack on 1.21.1 needs a compatible Pufferfish build. The JAR is not modified or redistributed in this bundle.

The background is original artwork generated with the built-in image tool for this tree; the frame textures are reproducible pixel UI assets. See `ART-DIRECTION.md`. No class, progression or loot behaviour changes are included.
