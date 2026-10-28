# 0.5.3-beta.1

- Replace the survival inventory's recipe-book button with a player portrait that opens Character. Close previously open inventory recipe books before layout, keep the inventory centered, and return to inventory when closing Character. The I key remains available.
- Refine the warm parchment GUI with softer corners, colored active tabs/section headers, bold headings, activity icons, colored progress bars and clearer point-counter spacing. Capitalize Milestone consistently, including historical journal entries. Use the original number badge on the character sheet and compact HUD.
- Render mob labels as two centered lines: original number badge above the mob's name. Send the saved level only when entity tracking begins; use client rendering rather than generating custom names. Preserve custom/affixed names and natural despawn, remove only exact former TNK-generated names on reload, and keep nearest-player scaling/loot rules unchanged.
- Update client/server protocol to 4. Retain all 588 skilltree nodes exclusively under config/puffish_skills; artwork remains embedded in the private JAR.

# 0.5.2-beta.1

- Remove the entire Pufferfish tree and passive-node data from the mod JAR. Supply it exclusively under config/puffish_skills, with native config.json and categories/character/*.json files.
- Load conditional-node coefficients from config/puffish_skills/tnk_passives.json. Keep icons, frames, warm background and original number/title artwork inside the mod.
- Use Pufferfish's native config-folder category ID puffish_skills:character. Migrate the known former tnk:character bridge/title settings; retain 175 level-based points, all node effects and earned titles.
- Keep the Skill tree entry point on the I-key screen and open the configured native category. Verify the JAR has no embedded tree and the actual Pufferfish mod loads all 588 nodes from config.

# 0.5.1-beta.1

- Add a Skill tree button beside the skill-point counter in the I-key character screen. The complete 588-node tree was already embedded in 0.5.0; this patch adds the missing direct entry point.
- Open the configured native Pufferfish category through a server-validated GUI action, with no command or client-controlled category/player target.
- Explain missing Pufferfish/category loading, and warn if an old datapack/config overrides the bundled 588-node tree with a different node count.
- Keep the complete 588-node graph, 175-point entitlement, own artwork, original prefixes and all perks embedded. Add a loaded-Pufferfish openScreen assertion.

# 0.5.0-beta.1

- Replace the basic 240-node draft with the supplied 588-node topology and all 669 original links, retaining six disciplines and roots. Use the existing TNK icons, frame states and warm background; do not port the original space background or original mod sprites.
- Embed the complete Pufferfish configuration and TNK skilltree assets directly into the mod. Keep original number/title PNGs embedded in the private release. No companion datapack or resource pack is required.
- Raise absolute level-based skill-point entitlement to 175 at level 100, update HUD/status/level messages and the category spending limit, and retain the same source ID to avoid duplicate awards. Each node costs one point.
- Add actual optional Iron's Spells mana, regeneration, spell power, resistance, school power, summon damage, casting speed and cooldown attributes, plus Ars Nouveau capacity/regeneration/damage/warding.
- Modernize old-mod-only crafting, enchantment, XP-per-minute, gem/socket and duplication mechanics into supported personal perks. Ship a full per-node conversion catalogue rather than claiming unsupported original effects still exist.
- Add reversible tagged conditional passives: shield guard, bounded evasion, burning-target/beneficial-effect/well-fed/longshot damage, combat recovery, ignite, food recovery, beneficial potion duration and Second Wind with saved cooldown.
- Migrate six known old tree-title requirements to valid converted node IDs without revoking earned titles or changing custom unrelated conditions.
- Add graph/assets/points checks, passive-rule tests and an isolated check through the official Pufferfish 0.19.2 parser and native attribute/tag update/respec implementations. Also load the actual Pufferfish mods in a disposable server to verify 175-point sync and live respec. Full-pack client integration remains to be playtested.

# 0.4.3-beta.1

- Correct the delivered private JAR to use the supplied Advanced Pack number/title artwork by default, without requiring an enabled companion resource pack.
- Embed all 100 original number PNGs and 43 original title PNGs without changing any bytes. Keep the chosen ten-level colour brackets and quartz-white level 100.
- Remove the old bronze LVL sheet from the private release JAR, override its font mapping with the original number images, and merge title glyph translations without losing keybinding labels.
- Add a reproducible private-artwork Gradle packaging option and byte/hash/font/class validation. No gameplay, skin, keybind, network or compiled Java behavior changes in this resource-only patch.

# 0.4.2-beta.1

- Cache eligible player positions/levels once per dimension per spawning tick and reuse bounded chunk-section candidate shortlists during spawn bursts. Preserve nearest selection on boundaries and a separate random offset for each mob.
- Add empty/single-player fast paths, lifecycle/teleport/level-up invalidation and world/server cleanup. No recurring per-mob nearest-player scan or entity-retaining spawn cache is added.
- Reuse unchanged mob attribute modifiers on rejoin and omit zero-value modifiers.
- Verify leveled, saved/reloaded mobs can still naturally distance-despawn; retain existing persistence and custom names without marking ordinary mobs persistent.
- Add 10,000-position full-search equivalence, cache bounds, multiplayer-boundary and 31-mob burst assertions. Document the original Auto Leveling and Otherworld Apotheosis integration, including the current 1.21.1 port's differences. Apotheosis loot/boss behavior is unchanged by this patch.

# 0.4.1-beta.1

- Replace distance-from-spawn, underground depth and dimension-baseline mob levels with the nearest living survival/adventure player's actual character level in the same dimension.
- Roll a uniform −4 to +4 level offset and clamp to 1–100; configure `mobLevelVariance` (default 4). Ignore creative, spectator and fake players; use level 1 if no eligible player is present.
- Keep assigned mob levels persistent, including existing mobs from older builds. Player movement, level-ups and chunk reloads never reroll existing mobs.
- Remove obsolete distance/baseline config keys on reload. Keep mob stat bonuses, Apotheosis behavior, title perks, loot rules and network protocol unchanged.
- Add level-bound unit checks and dedicated-server nearest-player/multiplayer-selection, fallback, exclusion and persistence assertions.

# 0.4.0-beta.1

- Add the I-key character screen with live player model, parchment/bronze controls, XP, campaign ceiling, title picker and seven activity tracks.
- Add a toggleable compact player/XP/pinned-activity HUD and persistent tracking preferences, plus recent completed milestones.
- Add authoritative private snapshots and bounded client actions; clients cannot submit XP, grants, levels, perks or target players.
- Replace cosmetic-only title behavior with modest equipped title perks, one title at a time, 30-second swap cooldown, no stacking or swap healing, and cleanup on hide/revoke/respawn.
- Add a reviewable release balance preset, progressive XP/work curves, activity starting rewards, mob scaling, passive budget audit and pacing CSVs; retain the agreed loot chance/tier/rarity rolls.
- Connect campaign ceiling unlocks before engineering commissions; catch up completed gates without duplicating rewards.
- Preserve legacy level/rank completion percentages on curve migration and retain customized starting configuration values.
- Fix first-client-login name formatting when a badge snapshot has not arrived, and account for transformed player-preview clipping.
- Add dedicated-server character/perk assertions and an opt-in integrated-client UI test. No identification discount or conditional ability is added.

# 0.3.0-beta.1

- Add 43 personal, collectible cosmetic titles with server-validated unlock, equip, hide, revoke and restore commands.
- Show the equipped title beside the character level in standard chat and overhead display names. Preserve the selection when new titles unlock; retain readable text without an artwork pack.
- Persist title ownership, selection and automatic-revocation blocks across restarts, death and team changes; earned skill achievements remain after respec.
- Add a configurable, atomically validated title catalog and actual Pufferfish spent-point/node achievement checks.
- Add 24 quest-binding rules using verified campaign, project-board, equipment, magic-guide and actual boss-kill quest IDs, with inactive templates for absent Let's Do/boss chapters.
- Package unchanged original number/title PNG files in one private client resource pack; add server-authoritative title snapshots and network protocol version 2.
- Correct namespaced IDs in title, engineering and one-time quest XP commands, preserving legacy reward IDs.
- Add title server assertions, three title-rule unit tests and an executable quest-bridge fixture check.

# 0.2.0-beta.1

- Add repeatable farming, audited crafting and engineering activity milestones; retain mining, fishing, exploration and combat.
- Replace fish-stat polling with real catch validation; add open-water, hook-age and duplicate-payout checks.
- Add mature crop/berry harvesting, confirmed right-click harvests and shared position cooldowns.
- Weight combat work by enemy strength and distribute a conserved reward pool by actual health damage, including supported pets, projectiles, owned summons and optional Iron's Spells summons.
- Persist contribution windows and prevent tag-then-environment full credit, repeat death payouts, summon/spawner farming, conversion resets and slime splitting rewards. Apply nonzero diminishing progress to repeated local kills.
- Store mining provenance in persistent section bitsets; exclude placed, recycled, fluid-created and piston-moved mineral positions. Honor legacy placement data.
- Exclude fake/creative/spectator players from activity progress; validate recipes and coalesce crafting callbacks without trusting output-stack sizes.
- Add permissioned, configured, one-time engineering commissions and optional KubeJS/FTB Quests chapter bridge.
- Add `/tnkprogress activities` and migration of old four-track configurations to the seven defaults.
- Package a separate client resource pack from 100 unmodified supplied number images, with ten-level colour changes and a quartz-white level 100. Level zero uses text because the supplied pack has no zero image.
- Document remaining design work and a nine-tier quest-rank proposal; role prefixes and the six explained future systems are not implemented by this release.
