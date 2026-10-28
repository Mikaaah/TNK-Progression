# TNK 588-node Pufferfish tree — 0.5.0-beta.1

The supplied T&K2 extracted Passive Skill Tree is the topology/content reference. This port preserves **588 IDs, six disciplines with 98 nodes each, six starting roots and all 669 undirected links**, including the three source long connections. Original coordinates are uniformly doubled and rounded to Pufferfish's integer coordinates; no branch is removed. Every node costs 1 point, the category has no native XP/point exchange, zero starting points and a 175-point spending limit. All nodes are reachable; the longest shortest route from a root is 15 links. Large keystones require 40 spent points; outer masteries require 60.

## Appearance and installation

The uploaded tree's original icons, frames and space background are **not used**. The port uses the existing TNK HAS icons, five-state warm frames, gold/brown links and quiet warm background. The generated space-background experiment is not included. The whole `tnk:character` configuration and all existing TNK textures are built into the mod JAR. Original number/title sprites remain embedded through the private artwork build hook. No resource pack or datapack is required for the delivered JAR.

Replace the progression JAR on server and clients. Remove/disable an older external tree datapack or config defining `tnk:character`; higher-priority definitions override bundled mod data. A different category can remain installed. Respec an existing old 240-node build before replacing its category; this port does not invent a one-to-one build migration for unrelated node IDs. Preserve world/player progression data. Known old title-node requirements migrate, earned titles remain and existing levels receive the new absolute point entitlement on login. Do not manually add another 175 points or enable a second native point/XP source.

## Level-based points

`points = level + floor(3 × level / 4)`, clamped to character levels 1–100. Examples: level 1 → 1, 10 → 17, 20 → 35, 30 → 52, 50 → 87, 75 → 131, 100 → 175. Individual level-ups give one or two points. The bridge sets the existing `tnkprogression:character_level` entitlement, rather than adding points repeatedly. Reconnects, respecs and title selection do not duplicate grants. Character XP/campaign ceilings, mob levels and loot progression remain independent.

## Perks and modernizations

The node graph is a faithful structural port; effects are a documented modernization rather than a promise to recreate every private mechanic of the original mod. `conversion-catalogue.json` records every node's original bonus types, current descriptions, runtime passives and replacements. 247 nodes replace an old-mod-only mechanism. No obsolete `skilltree:*` attribute, missing old recipe or fictitious Pufferfish reward is referenced.

- **Enchanter:** real Iron's Spells maximum mana, base regeneration, spell power, cast-time/cooldown reduction attributes, spell resistance, eight school attributes across branches and summon damage. Ars Nouveau uses its real maximum-mana, mana-regeneration, spell-damage and warding perk attributes. These are the mods' attribute units; casting/cooldown soft caps and school applicability remain those mods' rules. Increasing a reduction attribute is not a guarantee of that exact percentage off the final timer.
- **Alchemist:** beneficial drunk-potion duration, healing, critical stats, holy magic, supported recovery and offensive passives. Old crafted-potion amplification is modernized; potions are not permanently rewritten at crafting time. Instant, harmful and infinite-duration effects are not extended. Duration bonus caps at 50%.
- **Blacksmith:** armor/toughness, attack speed/damage, shield guard and anvil-cost efficiency. Second Wind triggers after surviving a hit below 25% maximum health and restores 10% maximum health. Its saved 180-second cooldown persists through respec/reconnect and cannot prevent lethal hits. It is unrelated to identification costs.
- **Hunter:** ranged damage, bow/crossbow velocity, mobility, stealth, nature magic and tamed-creature damage. Longshot grants its full capped bonus at 16+ blocks and applies to projectiles only; arrow retrieval/quiver capacity are modernized rather than duplicating arrows or injecting slots.
- **Miner:** mining/break speed, block Fortune, defensive/offensive routes and resource utility. Old gem power/socket/loot-duplication bonuses are replaced. Apotheosis gems, sockets, equipment pools, drop chances, tiers and rarities are not altered by these skills.
- **Cook:** exhaustion capacity, natural regeneration, healing, well-fed damage and a bounded health recovery when consuming nutritious food. There are no free character XP/AFK-XP or recursive crafting outputs.

Optional attributes use Pufferfish's native `required: false` reward setting. Missing optional mods produce optional-reward warnings and skip those bonuses, but do not remove nodes or route connections. Nodes containing only optional attributes also have a small always-available health bonus. The description identifies the optional mod; this is a pack-oriented tree and the magic bonuses need the respective mod installed.

## Conditional implementation and bounds

182 nodes contain runtime passives. Each uses a unique native Pufferfish `tag` reward, which adds its ownership marker on unlock and removes it on lock/respec. The server aggregates only known markers, caches unchanged tag sets, clamps effect totals and periodically removes stale markers from replaced categories. Native attribute rewards remain managed by Pufferfish. No client packet can choose passive amounts.

Caps: evasion 15%, shield half-damage proc 25%, ignite 20%, potion duration +50%, food recovery 3 health/meal, leech 1 health at most once/second, Longshot +15%; all conditional offensive bonuses together cap at +75%. Offensive on-hit effects target hostile enemies; fire/recovery do not create character XP. Second Wind/leech timestamps live in existing saved player progression data, so respec cannot reset their timers. No permanent affixes are added to crafted items.

`ATTRIBUTE-BOUNDS.csv` gives whole-tree and optimistic 175-point attribute bounds before route cost; runtime caps are separate. This is an inspectable balance preset, not measured full-pack balance. Equipment, optional mod formulas, spell schools and boss fights still require pack playtesting.

## Rebuild and verification

`python3 tools/convert_tk2_tree.py /path/TK2_Original_Skill_Tree_588_Nodes_Extracted.zip` regenerates the converted JSON, catalogue and bounds. It reads the reference archive but copies none of its artwork. `python3 tools/render_tk2_preview.py` draws the actual coordinates, links and bundled existing TNK sprites. The preview is a layout render, not an in-game screenshot. The prototype space artwork is discarded.

Build: Java 21, `./gradlew build -PtnkArtworkSource=/path/advancedpack.zip`. Standard builds include the complete tree and TNK art; the private flag additionally embeds the user's original licensed number/title PNGs. The old mod and Pufferfish libraries are not bundled or patched.

Verification includes 24 unit tests, dedicated-server graph/texture/title/point assertions, the previous activity/mob/despawn suites and an isolated call through the official Pufferfish 0.19.2 parser inside a live NeoForge server. Its real native tag and attribute rewards are exercised with counts 1/0: application, nonstacking refresh and respec removal. Missing optional attribute warnings are expected in that minimal fixture. An additional dedicated-server run loads Pufferfish Skills 0.19.2 and Attributes 0.8.3 as actual mods and verifies the TNK bridge grants exactly 175 points, repeat sync does not duplicate them, and a real category respec removes attributes/tags while preserving the 175-point entitlement. The same loaded-mod fixture exercises Second Wind using real incoming mob damage and verifies respec/re-unlock cannot reset its saved cooldown. This is not a full client session or an Iron's/Ars/Apotheosis runtime playtest.

The official downloaded `puffish_skills-0.19.2-1.21-neoforge.jar` and `puffish_attributes-0.8.3-1.21-neoforge.jar` list 1.21.1 in download metadata but declare `[1.21,1.21.1)` inside their mod metadata, excluding it. They are unmodified and not redistributed. Use a Pufferfish build your 1.21.1 instance successfully loads; the isolated parser check does not bypass that loader requirement in the delivered mod.

Existing icons: Aleksandr Makarov / HAS IconPack, with the existing credit/license files retained in the JAR. See source credits. The user's number/title images remain private and unmodified.

### Downloaded Pufferfish metadata compatibility

The actual loaded-mod server test uses NeoForge's documented dependency-override mechanism in a disposable test instance to soften the two overly strict Minecraft constraints, without editing either downloaded JAR. If your pack already loads Pufferfish, keep its current working setup. If it rejects those exact files on Minecraft 1.21.1, merge these two entries into the existing `dependencyOverrides` map in `config/fml.toml` on both sides:

```toml
dependencyOverrides = { puffish_skills = ["-minecraft"], puffish_attributes = ["-minecraft"] }
```

Keep existing overrides and other settings; do not replace the whole FML config. This fixture validates server behavior only, not arbitrary future versions or the full client pack. A corrected author-provided compatible build is preferable when available. No override is applied automatically by the TNK JAR. The shipped configuration uses Pufferfish's accepted version-2 format for broader compatibility; 0.19.2 reports a format-update warning but successfully loads it.
