# Character origins — 1.2.0-private.1

Made for **Towns & Kingdoms 3 | Beyond the Forge**. Minecraft **1.21.1**, NeoForge **21.1.262+**, Java **21**. Update both client and server: protocol **7**.

The first Character visit opens a three-page chooser with 20 backgrounds. Each card uses its original supplied 16×16 pixel icon. Select a background to preview its story and two perks, then confirm. Closing an unconfirmed preview leaves the choice open. The Origin tab shows the selected background afterward, without the permanence notice.

Selection is stored per UUID in the world's existing progression profile and survives death, reconnects and restarts. Only permission-level-2 administrator commands can replace or reset it. Cosmetic and passive titles remain independent.

| Origin | Perk one | Perk two |
|---|---|---|
| Deepdelver | +10% ore mining speed. | 5% chance for an extra raw ore from natural deposits. |
| Woodcutter | +15% log chopping speed. | 5% chance for an extra natural log. |
| Homesteader | +10% mature crop yield when harvested by hand. | 10% less hunger from exhaustion. |
| Stablehand | 10% shorter animal breeding cooldowns. | +15% movement speed for living mounts you control. |
| Riverfolk | 10% shorter fishing wait. | +15% vanilla fishing XP. |
| Huntsman | +5% bow and crossbow damage. | +10% meat and leather drops from your kills. |
| Forgehand | 10% of anvil repair XP refunded; normal requirements apply. | 10% less weapon durability loss. |
| Stonemason | +15% stone breaking speed. | +0.5 block reach while holding a building block. |
| Millwright | +10% engineering activity XP. | +1 block interaction reach while holding a wrench. |
| Librarian | 10% of enchanting XP refunded; normal requirements apply. | +15% XP recovered through grindstone disenchanting. |
| Apothecary | +15% duration for beneficial potions you drink. | +10% healing from instant health potions you drink. |
| Hedge Herbalist | +10% natural health regeneration. | 10% chance for an extra gathered flower or mushroom. |
| Caravan Trader | 10% lower villager buying prices, rounded down. | +15% vanilla trading XP. |
| Wayfarer | +5% movement speed. | 20% less fall damage. |
| Deckhand | +15% swimming speed. | +5 seconds of underwater breathing time. |
| Town Watch | +1 armour point. | +10% knockback resistance. |
| Street Urchin | +15% sneaking speed. | 10% shorter hostile detection range while sneaking. |
| Gravekeeper | 10% less damage from undead attackers. | +15% vanilla XP from undead kills. |
| Temple Acolyte | +2 maximum health (one heart). | 10% shorter harmful potion effects. |
| Hedge Mage | +5% maximum mana. | +5% mana regeneration. Requires Iron's Spells mana attributes. |

## Gameplay details

- Deepdelver requires a suitable pickaxe and a natural ore position. Only raw materials qualify for its extra item, not gems, coal, ore blocks or Silk Touch drops. The roll adds one item, rather than copying a Fortune stack.
- Woodcutter requires an axe and a natural log. Logs, flowers and mushrooms use persistent position receipts, including placement and piston movement, to prevent repeated bonus harvesting at the same position. Plants and stone can be extended with `tnkprogression:origin_herbs` and `tnkprogression:origin_stone` block tags.
- Homesteader applies proportional yield to mature crop **breaks**, including food, wheat, nether wart and cocoa. Other mods' right-click harvests and machine harvests are not covered. Fractional yield and XP bonuses carry forward rather than rounding every small action upward.
- Stablehand shortens both parents' cooldown after successful player-caused breeding. Its movement modifier applies to a **living mount controlled by that player**, including saddled horses; it is removed after dismount, rider death, origin replacement or logout. Boats use their own movement system and are not affected.
- Riverfolk reduces the initial fishing wait after Lure, without repeatedly shortening the timer each tick. Fishing and trading XP mean **vanilla experience orbs**, not character activity progress. Millwright increases character XP paid by engineering milestones, without accelerating the milestone requirements themselves.
- Huntsman covers arrows fired from bows/crossbows and crossbow-fired fireworks, excluding tridents and hand-launched fireworks. Meat/leather yield applies to the player's kills and honors normal canceled loot events.
- Forgehand refunds anvil XP only when the output actually repairs the same damaged item; renaming and enchant-only operations do not qualify. Weapon wear applies through common melee/ranged weapon tags, independently of tool wear. Anvil and enchanting refunds are based on **XP points actually spent**, with normal level requirements, offer strength and material consumption retained. They never count as newly earned XP.
- Librarian boosts XP from actual grindstone output collection. Apothecary boosts beneficial effects and instant healing only during **drinking a potion**, excluding splash/lingering potions and unrelated healing. Hedge Herbalist boosts the two vanilla food-regeneration paths, not every source of healing. Temple Acolyte shortens finite harmful effects while preserving amplifier, cures, particles and hidden effects.
- Stonemason's extra block reach requires holding a block; Millwright's requires a wrench tagged `tnkprogression:wrenches`. Common wrench tags and the Create/AE2 wrench items are supported optionally. Neither perk extends attack reach.
- Caravan Trader discounts villager purchase costs by 10%, rounded down to whole items, with a minimum cost still enforced by vanilla. Its own price adjustment is removed after the trading session and cannot stack through repeated refreshes. Existing reputation and Hero of the Village adjustments are preserved.
- Street Urchin reduces hostile visibility checks while sneaking; it does not erase an existing target or make the player invisible. Gravekeeper uses the undead entity-type tag and attributes indirect attacks to their owner.
- Hedge Mage uses Iron's Spells' `irons_spellbooks:max_mana` and `irons_spellbooks:mana_regen` attributes. The menu allows preview but disables confirmation when either attribute is missing. No additional hard dependency is introduced. TNK's existing unified-mana integration remains responsible for bridging to Ars Nouveau; this mod does not apply a duplicate Ars mana bonus.
- Attribute modifiers use stable IDs and do not stack across refreshes. Maximum-health increases do not heal the player. Creative/fake-player resource rewards and XP discounts are excluded.

## Existing characters

Characters with a saved origin from 1.1 retain that origin and its original perks, including the former Librarian's 20% enchanting and 10% anvil refunds. The saved revision prevents silent changes to confirmed choices. New characters choose from the 20 backgrounds above; legacy-only IDs remain available to administrator commands. An administrator setting Librarian now selects its new perks. No reset, migration command or KubeJS asset update is needed.

## Administrator commands

All origin administration requires permission level **2**, including in singleplayer. Targets must be online players.

```text
/tnkprogress origin get PlayerName
/tnkprogress origin set PlayerName tnk:deepdelver
/tnkprogress origin set PlayerName tnk:librarian
/tnkprogress origin reset PlayerName
```

`set` replaces only the origin and its owned modifiers. `reset` allows another selection on the next Character visit; close and reopen Character after an admin reset. Neither command clears progression, skills, titles or equipment. Administrators can deliberately set Hedge Mage even when its optional mana integration is absent.

Current IDs: `tnk:deepdelver`, `tnk:woodcutter`, `tnk:homesteader`, `tnk:stablehand`, `tnk:riverfolk`, `tnk:huntsman`, `tnk:forgehand`, `tnk:stonemason`, `tnk:millwright`, `tnk:librarian`, `tnk:apothecary`, `tnk:hedge_herbalist`, `tnk:caravan_trader`, `tnk:wayfarer`, `tnk:deckhand`, `tnk:town_watch`, `tnk:street_urchin`, `tnk:gravekeeper`, `tnk:temple_acolyte`, `tnk:hedge_mage`.

Legacy IDs: `tnk:miner`, `tnk:forester`, `tnk:farmer`, `tnk:soldier`, `tnk:wanderer`, `tnk:fisher`, `tnk:blacksmith` and revision-1 `tnk:librarian`.

## Installation and verification

Replace the previous progression JAR on client and server with **`tnkprogression-1.2.0-private.1.jar`**. Keep only one progression JAR installed. Keep the current Pufferfish config and optional title-default scripts. All icons, frames, 100 number badges and 58 title badges remain embedded in the mod.

Isolated NeoForge checks cover the 20 definitions and icons, legacy revisions, permission/persistence rules, transformed vanilla hooks, resource guards, and the actual chooser at GUI scales 2/3/4. Optional mana registration and other pack mods are not exercised by the standalone test environment; a full TNK3 playtest remains necessary.

Iron's attribute contract: [1.21 AttributeRegistry](https://github.com/iron431/irons-spells-n-spellbooks/blob/1.21/src/main/java/io/redspace/ironsspellbooks/api/registry/AttributeRegistry.java). Artwork is embedded unchanged under the supplied permission for Towns & Kingdoms.
