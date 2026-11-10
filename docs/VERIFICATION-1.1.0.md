# Verification — 1.1.0

- Built successfully with Java 21.0.13, Gradle 9.2.1 and NeoForge 21.1.265 for Minecraft 1.21.1. The existing minimum NeoForge 21.1.262 metadata is retained.
- The final JAR embeds version 1.1.0 and is named `tnkprogression-1.1.0-neoforge-1.21.1.jar`.
- The token model resolves to `tnkprogression:item/origin_ascension`, whose original supplied 16×16 PNG is bundled under `textures/item`. Minecraft 1.21.1's actual item atlas includes this directory.
- The supplied PixelMine `magic_card.png` is copied unchanged; its SHA-256 is `2612118421ade715cc10fe4738a208647405c519bf899cd4d35cd64eed2fb49f`.
- Existing 20 origin icons, 100 number badges and 58 title badges passed the repository's original-artwork hash checks.
- Compared with the previously delivered 1.5.0-private.1 JAR, all original class bytes and other resources are unchanged except the intended version metadata and token model. Only the token PNG and its credits were added.
- No new graphical-client run was performed for this resource/version fix.

Final JAR SHA-256: `52e5b2dd1003404afc50c9b8b9e5bfa416a2c9a77e30b429fa70c4f9716ae8f2`.
