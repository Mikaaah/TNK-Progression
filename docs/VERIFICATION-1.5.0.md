# Verification — 1.5.0-private.1

- Java 21 compilation and JAR packaging succeeded with NeoForge 21.1.262 / Minecraft 1.21.1.
- 37 JUnit tests passed, including the 4,950,000 cumulative XP curve and preservation of the 15 ascended origins' unlock rules.
- `TNK_DIRECT_XP_SMOKE_OK` in a dedicated server: existing-level/percentage migration, Sigil consumption/reselection, fractional payouts, rejected crafting/engineering rewards, exact coal/diamond payouts, natural/placed/repeated-position guards, mob-level payouts, repeated quest commands without IDs, furnace and campfire mixins, automated-input exclusion, activity payload/help and chapter cap.
- `TNK_STARCATCHER_SMOKE_OK` with the actual Starcatcher 3.1.4.1 NeoForge 1.21.1 JAR: complete common, rare, epic and legendary catches award exactly their configured character XP. Starcatcher is not required by the mod.
- Rendered Minecraft client check: character XP bar, all seven direct XP sources, Starcatcher rarity information with working pagination, and the reusable quest command information.
- Original packaged origin icons, 100 number badges and 58 title badges passed the repository's byte-hash artwork verification.

Known scope: custom cooking appliances (including Farmer's Delight pots) are not yet integrated. Placement provenance cannot reconstruct placements predating this mod's recording, or block changes from mods that bypass the relevant NeoForge events. Full TNK3 multiplayer balance and every optional boss mod still require pack playtesting. Both sides must update because the character payload changed.
