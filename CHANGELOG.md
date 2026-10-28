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
