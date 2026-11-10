# Origins and Ascension — 1.1.0

Choose one of **20 base origins** when first opening Character. Preview its story and perks, then confirm. Your selected origin is saved per player and survives death, reconnects and restarts. Cosmetic and passive title choices are independent.

## Sigil of Ascension

Using **`tnkprogression:origin_ascension`** consumes a Sigil, unlocks **15 ascended origins** and lets you choose again. Your character level, skills, titles and activity progress are preserved. Further reselections require another Sigil or administrator action. The item uses the supplied purple PixelMine token artwork, embedded directly in the mod.

Mana origins require the optional Iron’s Spells mana attributes for normal selection. The pack’s unified-mana integration handles any Ars Nouveau bridge.

## Base origins

| Origin | Perks |
| --- | --- |
| **Deepdelver** (`tnk:deepdelver`) | +10% ore mining speed. 5% chance for an extra raw ore from natural deposits. |
| **Woodcutter** (`tnk:woodcutter`) | +15% log chopping speed. 5% chance for an extra natural log. |
| **Homesteader** (`tnk:homesteader`) | +10% mature crop yield when harvested by hand. 10% less hunger from exhaustion. |
| **Stablehand** (`tnk:stablehand`) | 10% shorter animal breeding cooldowns. +15% movement speed for living mounts you control. |
| **Riverfolk** (`tnk:riverfolk`) | 10% shorter fishing wait. +15% vanilla fishing XP. |
| **Huntsman** (`tnk:huntsman`) | +5% bow and crossbow damage. +10% meat and leather drops from your kills. |
| **Forgehand** (`tnk:forgehand`) | 10% of anvil repair XP refunded; normal requirements apply. 10% less weapon durability loss. |
| **Stonemason** (`tnk:stonemason`) | +15% stone breaking speed. +0.5 block reach while holding a building block. |
| **Millwright** (`tnk:millwright`) | +10% quest command XP. +1 block interaction reach while holding a wrench. |
| **Librarian** (`tnk:librarian`) | 10% of enchanting XP refunded; normal requirements apply. +15% XP recovered through grindstone disenchanting. |
| **Apothecary** (`tnk:apothecary`) | +15% duration for beneficial potions you drink. +10% healing from instant health potions you drink. |
| **Hedge Herbalist** (`tnk:hedge_herbalist`) | +10% natural health regeneration. 10% chance for an extra gathered flower or mushroom. |
| **Caravan Trader** (`tnk:caravan_trader`) | 10% lower villager buying prices, rounded down. +15% vanilla trading XP. |
| **Wayfarer** (`tnk:wayfarer`) | +5% movement speed. 20% less fall damage. |
| **Deckhand** (`tnk:deckhand`) | +15% swimming speed. +5 seconds of underwater breathing time. |
| **Town Watch** (`tnk:town_watch`) | +1 armour point. +10% knockback resistance. |
| **Street Urchin** (`tnk:street_urchin`) | +15% sneaking speed. 10% shorter hostile detection range while sneaking. |
| **Gravekeeper** (`tnk:gravekeeper`) | 10% less damage from undead attackers. +15% vanilla XP from undead kills. |
| **Temple Acolyte** (`tnk:temple_acolyte`) |  |
| **Hedge Mage** (`tnk:hedge_mage`) | +5% maximum mana. +5% mana regeneration. Requires Iron's Spells mana attributes. |

## Ascended origins

Available after using a Sigil of Ascension.

| Origin | Perks |
| --- | --- |
| **Archmage** (`tnk:archmage`) | +30% maximum mana. +25% mana regeneration. |
| **Battlemage** (`tnk:battlemage`) | +2 attack damage. +3 armour. +20% maximum mana. |
| **Dreadnought** (`tnk:dreadnought`) | +8 maximum health. +5 armour. +25% knockback resistance. |
| **Berserker** (`tnk:berserker`) | +4 attack damage. +4 maximum health. +10% movement speed. |
| **Deadeye** (`tnk:deadeye`) | +25% bow and crossbow damage. +10% movement speed. +1 luck. |
| **Shadowblade** (`tnk:shadowblade`) | +30% sneaking speed. 30% shorter hostile detection range while sneaking. +3 attack damage. |
| **Worldstrider** (`tnk:worldstrider`) | +15% movement speed. 60% less fall damage. +30% swimming speed. |
| **Deepforged** (`tnk:deepforged`) | +40% ore mining speed. 15% chance for extra raw ore from natural deposits. +3 armour. |
| **Grand Architect** (`tnk:grand_architect`) | +50% stone breaking speed. +2 block reach while holding a building block. |
| **Artificer** (`tnk:artificer`) | +35% quest command XP. +3 block interaction reach while holding a wrench. 20% of anvil repair XP refunded; normal requirements apply. |
| **Runesmith** (`tnk:runesmith`) | 25% of enchanting XP refunded; normal requirements apply. 25% of anvil repair XP refunded. 35% less weapon durability loss. |
| **Wildwarden** (`tnk:wildwarden`) | +30% mature crop yield when harvested by hand. +25% natural health regeneration. +25% meat and leather drops from your kills. |
| **Tidecaller** (`tnk:tidecaller`) | +50% swimming speed. +30 seconds of underwater breathing time. 25% shorter fishing wait. |
| **Grand Alchemist** (`tnk:grand_alchemist`) | +35% duration for beneficial potions you drink. +25% healing from instant health potions you drink. 25% shorter harmful potion effects. |
| **Dawnbringer** (`tnk:dawnbringer`) | +8 maximum health. 25% less damage from undead attackers. +35% vanilla XP from undead kills. |

## Gameplay details

Ore/log bonuses require natural qualifying positions and appropriate tools; placed/recycled positions do not provide repeated bonus drops. Extra raw-ore rolls add an item rather than copying a Fortune stack. Crop bonuses cover supported hand-harvested crops; arbitrary custom crop APIs need their own integration.

XP refunds retain normal level/cost requirements and refund only XP actually spent. Millwright and Artificer’s XP perks apply to **quest command XP**, following the removal of engineering XP. Stable attribute IDs prevent stacked bonuses; raising maximum health does not heal the player.

Earlier saved origin revisions retain their original perks. Legacy origin IDs remain available to administrator commands; selecting an origin through the current chooser uses its current definition.

## Administrator commands

Permission level **2**, with an online player target:

```text
/tnkprogress origin get PlayerName
/tnkprogress origin set PlayerName tnk:deepdelver
/tnkprogress origin reset PlayerName
```

`set` replaces the origin and its owned modifiers. `reset` allows selection again on the next Character visit. Neither clears levels, skills, titles or equipment. For a player-earned change, award a Sigil through a quest instead of invoking administration.

Install the same **1.1.0** JAR on clients and server. The supplied 20 origin icons, token texture and character artwork are embedded; no KubeJS asset update or extra resource pack is required.

[Character guide](CHARACTER-GUIDE.md) · [Quest rewards](QUEST-ROLES.md) · [Project home](../README.md)
