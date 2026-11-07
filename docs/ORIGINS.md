# Character origins — 1.1.0-private.1

Made for **Towns & Kingdoms 3 | Beyond the Forge**. Minecraft 1.21.1, NeoForge 21.1.262+, Java 21. Client and server must both use this update: network protocol **6**.

On your first visit to the Character screen, choose the background that shaped your adventurer. Selecting a card previews its story and both perks. **Confirm Origin** makes the choice permanent. Closing an unconfirmed preview leaves the choice open; the picker returns on your next Character visit.

After confirmation, the **Origin** tab shows your background and its permanent perks. It has no change or reset button. Origins are separate from your cosmetic title and passive title, so all three choices can coexist.

| Origin | Perk one | Perk two |
|---|---|---|
| Miner | +10% pickaxe mining speed | 10% chance for one extra natural ore drop |
| Librarian | Refund 20% of enchanting XP spent | Refund 10% of anvil XP spent |
| Forester | +10% axe chopping speed | 10% chance for one extra newly harvested natural log |
| Farmer | 10% chance for one extra mature crop drop when breaking crops | +1 maximum health, or half a heart |
| Soldier | +1 attack damage | +1 armour |
| Wanderer | +0.005 movement speed, equivalent to 5% of vanilla base speed | 15% less fall damage |
| Fisher | +1 luck | 10% chance for one extra fish on a successful rod catch |
| Blacksmith | Refund 20% of anvil XP spent | +1 armour |

The bonuses have no character-level scaling. Attribute refreshes, reconnects and title changes do not stack additional copies, and gaining maximum health does not heal you.

## Resource and XP rules

- Miner bonuses require a suitable pickaxe and an actual ore drop. Silk Touch and player-placed, moved or previously harvested ore positions do not qualify. A successful roll adds **one** item, including when Fortune has already increased the original stack.
- Forester bonuses require an axe and a previously unharvested natural log position. Placed, piston-moved and already harvested log positions are recorded persistently, so replanting at a recorded position does not reset its bonus eligibility.
- Farmer bonuses apply to mature crop breaks that drop food, wheat, nether wart or cocoa beans. Right-click harvests and arbitrary custom non-food crop products are not covered by this initial bonus.
- Fishing bonuses copy a fish, never treasure or junk. There is at most one roll per hook; canceled catches produce no extra item.
- Enchanting and anvil perks refund a share of the **vanilla XP points actually spent**, including partial-bar progress. Normal required levels, enchantment strength, lapis and repair materials are unchanged. Fractional XP credit carries over between uses. These refunds do not grant character XP or count as newly earned XP rewards.
- Material bonuses require a real survival player. Creative players and automated fake players receive no bonus materials or XP refunds.

## Persistence and existing worlds

The selected origin is stored in the existing personal UUID profile in the world's `tnkprogression.dat`. It survives death, respawn, dimension changes, logout and restart. Team membership and title selection do not change it. A different world or server has its own character profile.

Existing characters keep their levels, XP, skills and titles. They begin with no origin and receive the same one-time picker when they next open Character. Opening a world or logging in does not randomly assign a background.

## Administrator commands

All origin administration requires permission level **2**. Targets must be online players; selectors are supported.

```text
/tnkprogress origin get PlayerName
/tnkprogress origin set PlayerName tnk:miner
/tnkprogress origin set PlayerName tnk:librarian
/tnkprogress origin reset PlayerName
```

`set` replaces the origin and removes the previous origin's attribute modifiers before applying the new ones. It preserves progression and title slots. `reset` removes the origin perks and allows another choice on the next Character visit. Close and reopen Character after an administrative reset.

Stable IDs: `tnk:miner`, `tnk:librarian`, `tnk:forester`, `tnk:farmer`, `tnk:soldier`, `tnk:wanderer`, `tnk:fisher`, `tnk:blacksmith`.

## Installation

Replace the previous TNK Progression JAR on clients and server with `tnkprogression-1.1.0-private.1.jar`. Keep only one progression JAR installed. Retain the existing Pufferfish configuration and optional title-default integration. All character GUI resources and badges remain embedded in the JAR; no KubeJS asset update is required for origins.

Origins are implemented inside TNK Progression and introduce no additional required mod dependency. Full TNK3 integration testing remains separate from isolated NeoForge verification.
