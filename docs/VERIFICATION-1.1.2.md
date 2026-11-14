# Progression 1.1.2 validation

Target: Minecraft 1.21.1, NeoForge 21.1.262, Java 21.

Artifact: `tnkprogression-1.1.2-neoforge-1.21.1.jar`

SHA-256: `fe991833c0de4a0608867cf076ddcbcda3633a7b0d7848246f7b1467618feccf`

- Java 21 Gradle build passed; 41 existing unit tests passed with zero failures.
- Dedicated-server HUD checks passed: old config migration, hidden default for untouched profiles, explicit show/hide overrides, saved opt-in persistence, enabled pack default and unchanged character level/XP.
- Existing direct-XP server checks passed, including Ascension, migration, quest rewards, natural/placed blocks and cooking.
- No graphical client playtest was performed for this small HUD-default change.

The new top-level `hudEnabledByDefault` setting defaults to `false` and is added to existing configuration files. Explicit player preferences take priority. The 1.1.1 Loot/Spiral integration is retained; its full five-combination compatibility matrix was not repeated for this UI-preference change.
