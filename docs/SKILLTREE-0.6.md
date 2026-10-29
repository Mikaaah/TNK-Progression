# Skilltree update — 0.6.0-beta.1

The tree remains entirely in config/puffish_skills. Update the progression JAR and the supplied external tree files together on the server and clients. Keep the same category ID, node IDs, 588 nodes, 669 links, six roots, original TNK art and 175-point level entitlement. Existing earned titles, levels and activity progress are retained.

## Mana and spellcasting

The 0.5.x conversion granted Iron's and Ars mana capacity, regeneration and spell-damage rewards together. This release removes the duplicate Ars maximum-mana, mana-regeneration and spell-damage attribute rewards from every node. The tree uses a single Iron's attribute route, with Ars 'n Spells providing the configured bridge. Independent Ars warding remains a separate defensive perk.

Reference checked: Ars 'n Spells NeoForge 1.21.1, upstream commit c234f19145d4380a849597f7e390fd535cc45e4f (3.4.0). Its default is ISS_PRIMARY; ARS_PRIMARY folds foreign Iron's mana modifiers into Ars calculations. The bridge has several modes and unit conversions. These tooltips explicitly name the Iron's attribute route rather than claiming a second separate Ars reward. If mana unification is disabled, this tree does not add a second native Ars mana reward. Native Ars maximums, the bridge's ceiling rules and gear still participate in the effective shared capacity. This update does not change the other mod's settings.

The loaded-server fixture includes actual Pufferfish Skills and Attributes. It does not include Iron's, Ars Nouveau and Ars 'n Spells together; that complete combination and custom mana modes still require pack testing.

## Original values

The previous tree kept the original topology but scaled down many values. This release restores original JSON magnitudes, operations, player conditions, target conditions and supported multipliers for **141 complete nodes**. For example:

- Enchanter Life: +2 maximum Health while enchanted equipment is equipped; +6 for its notable node. These are conditional again.
- Alchemist offense: +5% damage against poisoned targets rather than an unconditional smaller attack bonus.
- Miner: the original +15% root / +5% minor mining-speed values while holding a pickaxe.
- Blacksmith defensive keystone: the two original +10% Armor conditions at 50 and 100 Armor, alongside the existing TNK Second Wind addition.

All source bonus JSON for these nodes is preserved verbatim in tnk_passives.json/source_bonuses. Unchanged ownership is cached; only owned source effects are evaluated. Conditional attributes refresh every five player ticks, reuse unchanged modifiers, exclude their own modifier from attribute-based tests and never heal when maximum Health rises. Respec removes the tag; the next refresh removes the modifier. Damage bonuses evaluate their real target at the hit. Logout, player cloning and server stop clear caches.

Comparable magnitudes are also restored for jump, incoming healing, vanilla-orb XP, attack recovery, potion duration, evasion and shield-guard replacements. Their altered triggers and existing caps remain explicitly described. Original critical-chance/damage magnitudes are also restored on the existing Apothic Attributes replacements. Where the old bonus had a specialized trigger, the tooltip explicitly describes the personal replacement as applying to all eligible hits.

**447 nodes remain adaptations**, not one-to-one ports. The old mod's crafting-item affixes, potion-amplification systems, gem/socket manipulation, extra ring slot, recipe unlocks, loot duplication and AFK XP do not become those original mechanics just by copying their numbers. They retain explicit personal replacements and an Adapted effect footer. conversion-catalogue.json contains each original full bonus, the implemented tooltip, branch path and value policy. ORIGINAL-VALUE-AUDIT.csv provides a compact index.

Existing capped TNK replacement passives retain their caps and cooldowns. Restored source effects use their own original magnitudes and conditions instead of the former blanket reduction; they are not subject to the separate +75% cap used by TNK replacement effects. ATTRIBUTE-BOUNDS.csv includes unscaled source-attribute coefficients; condition-dependent multipliers and routing must still be considered when assessing actual builds. Restoring the original values increases power relative to 0.5.x; no full-pack endgame balance claim is made.

## Tooltips

Every node identifies Towns & Kingdoms, its main branch, subclass/Core, node tier, one-point cost, actual effects and conditions. Long effect lines wrap at 52 characters. Footer text explains requirements, refund behavior, optional integrations and adaptations. Mastery nodes show their unlocked title. Original icons and warm background remain unchanged.

## Mastery titles

Unlock the corresponding subclass Mastery with at least 60 points spent to earn the title. The existing title watcher checks actual Pufferfish purchases; administrators and FTB Quests can also grant these through the existing title command. Titles remain earned after a respec. Only the equipped title applies its perk; owning all twelve does not stack their bonuses. All use unmodified original AMETYST sprites.

| Branch | Subclass | Title | Equipped perk |
|---|---|---|---|
| Alchemist | Assassin | Shadow | +2% base movement speed |
| Alchemist | Healer | Guardian | +1 maximum Health |
| Blacksmith | Soldier | Soldier | +1 Armor |
| Blacksmith | Artisan | Steel | +0.5 Armor Toughness |
| Cook | Berserker | Berserker | +3% base Attack Damage |
| Cook | Fisherman | Tide | +0.25 Luck |
| Enchanter | Arsonist | Blaze | +3% base Iron's fire Spell Power |
| Enchanter | Scholar | Astral | +10 Iron's maximum Mana; one route |
| Hunter | Ranger | Ranger | +3% bow projectile speed |
| Hunter | Fletcher | Archer | +3% ranged damage |
| Miner | Explorer | Explorer | +2% base movement speed |
| Miner | Jeweler | Amethyst | +0.25 Luck |

Title IDs are tnk:mastery_<branch>_<1 or 2>. FTB Quests example: `tnkprogress title grant {p} tnk:mastery_hunter_1`. Existing title/perk configs receive these twelve defaults once, preserving custom entries and settings. The skillTitleExpansion marker prevents removed titles from being recreated on later reloads. The original 43 titles stay at their existing glyph indices; the new twelve are appended, giving 55 total.

## Updating

Stop the server/game and replace the old progression JAR with the new JAR. Merge the supplied config/puffish_skills files; definitions.json and tnk_passives.json must be updated together. Preserve customized files separately and merge intentional changes. Restart; runtime tables are read at startup. Protocol remains 4, but install the matching release for its new effects and artwork.

For a clean point reallocation after changing values, respec through the normal Pufferfish controls. No automatic wipe, category-ID change or extra point source is introduced. If updating from the old tnk:character category rather than 0.5.2+, follow the category migration guide first. The JAR contains no Pufferfish tree JSON.
