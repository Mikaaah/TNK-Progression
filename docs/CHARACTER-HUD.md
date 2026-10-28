# Character screen, inventory shortcut and mob badges — 0.5.4-beta.1

Minecraft 1.21.1 · NeoForge 21.1.262+ · Java 21. Replace the progression JAR on both server and clients; network protocol is **4**.

## Opening Character

The survival inventory's vanilla recipe-book button is replaced by a framed **player portrait**, with a Character tooltip and keyboard narration. Click it to open the character sheet. Escape, its close button or I returns to the inventory. Previously open inventory recipe books close before layout, so the inventory stays centered. The replacement targets the vanilla button at its own position; unrelated inventory buttons and JEI are not removed. Crafting-table and furnace screens are outside this change.

Press **I** to open Character directly from the world; this route closes back to the world. Rebind Open Character under Controls → Towns & Kingdoms. The screen does not pause gameplay.

## Appearance and navigation

Warm parchment, dark brown framing, softened pixel corners, a fine inset highlight and colored accents follow the pack's existing visual style. TNK purple marks Character and Activities; muted rose marks Titles. Gentle vertical gradients shade parchment, colored tabs, section ribbons and progress fills, with a soft plum backdrop behind the player. Headings, tab labels and buttons use bold text. Active tabs and pinned/selected choices have a colored fill. Activity rows have item icons and distinct matching progress colors. Milestone is consistently capitalized, including entries saved by previous versions.

The actual player model shows the client's skin and equipment. The original embedded number artwork appears on the character sheet and compact HUD. Mouse hitboxes and model clipping follow the sheet's scaled layout. The title image remains the original unchanged artwork.

- **Character:** character XP, campaign ceiling/next milestone, earned/spent skill points, Skill Tree button, tracked activity and recent milestone journal. The point summary and tree button have separate space.
- **Titles:** paged collection, all/owned filter, locked-title descriptions, equipped-perk preview, equip and hide actions. Ownership and swap cooldown are enforced by the server.
- **Activities:** all seven repeatable tracks, next Milestone number, work requirement, XP reward and a Track/Pinned button. The compact HUD follows the pinned activity.
- **HUD:** player face/name, original number badge, character XP and tracked progress. Hide/show it on the character sheet; it also hides with F1, in spectator mode and while another screen is open.

Only the equipped title grants its perks. Owning multiple titles does not stack them. Hiding a title removes its perk and keeps ownership. The default 30-second title-swap cooldown cannot be erased by hiding or reconnecting. Attribute changes do not heal the player. Optional missing attributes are marked unavailable. See TITLES.md and config/tnk-title-perks.json for the catalog and perk settings.

## Mob labels

When a leveled mob is targeted, its original **number badge is centered above its name**. Always-visible mob names retain that visibility. Custom names and affixed boss-name components keep their existing text, color and formatting beneath the badge. The two lines use depth testing, so the badge does not show through walls; F1, invisibility, range and team name visibility are respected.

The server sends the saved level once when the player starts tracking that entity. The client validates its entity ID and UUID and stores the level on that entity. There is no recurring mob-level packet, per-mob server tick scan or global client entity cache. Re-entering tracking range sends a fresh snapshot.

Fresh mobs no longer receive a generated custom name. Exact old TNK-generated `Lv. N <mob type>` components are removed on chunk load without rerolling levels. Other names are retained, even when they start with Lv. The update never changes persistence flags or despawn age. Nearest-player level assignment, variance, scaling, loot and Apotheosis behavior are unchanged.

## Skilltree and balance

All **588 nodes / 669 links / six roots** stay outside the JAR in config/puffish_skills. Keep the existing category puffish_skills:character and the same config files when updating from 0.5.2. The original number/title sprites, own skill icons, frames and warm background remain embedded in the private JAR. No datapack or resource pack installation is needed.

Level-based point entitlement remains `level + floor(3 × level / 4)`, reaching **175 at level 100**. The configured XP curve, campaign ceilings, anti-farm rules and passive caps are unchanged. Conditional tree abilities such as Second Wind still use the external coefficient table. No identification discount is added.

## Verification and limits

The dedicated-server suite checks mob scaling/cache/despawn, exact legacy-label migration, custom-name preservation and badge-payload serialization, alongside existing progression/title/skilltree checks. The build runs the existing 24 unit tests.

The opt-in client fixture uses a disposable integrated world, the actual private release JAR and the actual Pufferfish Skills/Attributes mods. It checks a previously open recipe book is hidden, exactly one portrait replaces its button, opening/returning to inventory, direct I-key opening, title equip through the server, synced title health, activity tracking, GUI-scale changes, real mob tracking and actual two-line nameplate rendering. Screenshots are captured from the game.

This fixture is not the complete TNK pack. JEI, other inventory mods and external nameplate mods have not been tested together here; their buttons and renderer ordering remain pack-specific integration checks. No skin authentication or unrelated lock-on/keybind settings are changed.
