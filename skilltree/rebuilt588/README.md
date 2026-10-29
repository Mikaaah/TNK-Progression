# TNK 588-node tree — config-folder release 0.6.0-beta.1

The tree belongs in **config/puffish_skills/**. It is not bundled in the mod and is not delivered as a datapack. The JAR supplies the runtime integration and the existing TNK icons, frames, background and original number/title sprites (100 numbers / 55 titles).

## Install

Extract the release into the instance/server root, merging `mods` and `config`. Replace the old progression JAR rather than keeping both versions. Install the same files on clients and the server. Remove the earlier embedded-tree 0.5.0/0.5.1 JAR and any old external TNK character-tree datapack. Other unrelated categories can remain: merge their names into the native config.json categories list instead of deleting them.

Actual files:

- `config/puffish_skills/config.json` — native version-3 configuration, with `character` in `categories`.
- `config/puffish_skills/categories/character/category.json` — appearance, roots and 175-point limit.
- `config/puffish_skills/categories/character/definitions.json` — all 588 perk definitions.
- `config/puffish_skills/categories/character/skills.json` — positions and roots.
- `config/puffish_skills/categories/character/connections.json` — all 669 links.
- `config/puffish_skills/tnk_passives.json` — editable TNK conditional passive coefficients, read by the progression mod on startup.

The config-folder category ID is **puffish_skills:character**. The bridge and default skill-title conditions use it. Known old tnk:character settings migrate; unrelated customized category IDs remain. Restart after changing files. Press **I → Skill tree** to open Pufferfish from the character screen. If the mod/category is unavailable the message identifies that; no command is required.

Respec an existing old build before changing its category ID. Character levels, activity progress and earned titles are preserved; unrelated skill allocations are not silently remapped. Do not add 175 extra points manually or enable another native skill XP/point source.

## Tree and points

588 source node IDs, six disciplines with 98 nodes each, six starting roots and 669 links. Original positions are uniformly doubled and rounded to native integer coordinates. All nodes cost one point; the longest shortest path from a root is 15 links. Large keystones require 40 spent points, outer masteries 60. The original mod's icons and space background are not used: existing TNK HAS icons, warm background and native five-state frames remain in the JAR.

Points are `level + floor(3 × level / 4)`: level 1 → 1, 10 → 17, 20 → 35, 50 → 87, 75 → 131, 100 → **175**. The existing named point entitlement is set absolutely, so login, respec and repeat synchronization do not duplicate awards. A changed category receives that entitlement based on the retained character level.

## Effects and title update

See [the 0.6 update guide](../../docs/SKILLTREE-0.6.md) for the single mana route, original-value restoration, clear tooltips, 12 mastery titles and upgrade instructions. 141 complete nodes preserve original values and conditions; 447 remain explicitly labeled adaptations. Original critical magnitudes are restored on the current personal critical-attribute replacements. Native mana and general spell-power rewards no longer buff both Iron's and Ars separately. Existing conditional replacement caps remain separate from restored source effects.

## Build and checks

`python3 tools/convert_tk2_tree.py /path/to/TK2_Original_Skill_Tree_588_Nodes_Extracted.zip` generates this config folder. No source PNGs are copied. `python3 tools/render_tk2_preview.py` renders the actual data using existing TNK sprites. The preview is a layout render, not a game capture. `./gradlew build -PtnkArtworkSource=/path/to/advancedpack.zip` embeds the original number/title art; it never embeds the tree configuration.

24 unit tests and the actual loaded Pufferfish Skills/Attributes dedicated-server suite verify config loading, 588 nodes, 669 links, all referenced TNK textures, 175 points without duplicates, native attribute/tag application and removal, and live Second Wind damage/cooldown through respec. The JAR is also checked for absence of Pufferfish category data. Full-pack client and Iron's/Ars runtime playtesting remain separate checks.

The official downloaded Skills 0.19.2 and Attributes 0.8.3 metadata declares [1.21,1.21.1), despite advertising 1.21.1. The disposable test server uses existing NeoForge config dependency overrides without editing those JARs. If your pack already loads Pufferfish, keep that working setup. Otherwise the compatibility note in the release explains how to merge the two overrides into FML's existing map; the TNK mod does not overwrite FML settings. No third-party mod JAR is redistributed.

Icons: Aleksandr Makarov / HAS IconPack; existing credit/license files remain in the JAR. Supplied number/title PNGs remain byte-identical originals.
