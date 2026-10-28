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
