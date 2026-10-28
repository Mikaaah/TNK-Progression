# Mob spawn performance, despawning and Apotheosis — 0.4.2-beta.1

## Natural despawning

TNK assigns `tnk_level` and attribute modifiers once when a hostile mob first joins the world. A saved level is not a persistence flag. The mod never calls `setPersistenceRequired`, cancels a despawn event, resets a mob's inactivity timer or forces its chunk to stay loaded. Minecraft 1.21.1's `Entity.setCustomName` only updates the synchronized name field; the separate mob persistence flag controls ordinary distance despawn. A name tag also marks persistence, which is why applying a name tag and programmatically assigning a display label differ.

The dedicated-server suite creates a real zombie, applies TNK's level/name/stats, saves and reloads it, then calls the actual vanilla `checkDespawn` with a player farther than the despawn distance. The zombie is removed. A second zombie with a pre-existing custom name and persistence flag retains both and is not removed. Other mods and creature-specific despawn rules can still legitimately retain their own mobs; TNK does not forcibly clear those flags.

## Spawn lookup cache

- On the first level lookup in a dimension during a server tick, copy eligible player positions and actual character levels once. Reuse that snapshot for later spawns in the same dimension/tick. No work is done in dimensions without a spawn lookup. Empty worlds and one-player snapshots have fast paths.
- Cache candidate players by 16×16×16 chunk section within the snapshot, with a maximum of 2,048 cached areas. Do not remember the first player's identity as a permanent area owner.
- To build a shortlist, compare player distances to the section center and retain everyone within the nearest center-distance plus the section diameter. By triangle inequality, excluded players cannot be nearest anywhere inside the section. Compare the remaining candidates against each spawn's exact position. This retains different nearest players on opposite sides of a boundary, even in one section, and accounts for vertical separation and negative coordinates.
- Every mob still receives its own random −4…+4 offset, clamped to 1–100. Sharing an area does not give every mob an identical rolled level. Existing mob levels never change.
- Refresh on the next spawning tick. Login, logout, dimension changes, respawn, exposed teleport events and earned character level changes invalidate snapshots. Recheck the selected player's live eligibility; invalidate immediately if it became invalid. Unload/server-stop releases entries. Sampled ordinary positions may be up to one server tick old; this is deliberate, bounded spawn-time sampling, not a seconds-long sticky player cache.
- No mob references, UUID-to-mob maps, chunk tickets or repeating mob tick handlers are kept. The optimization changes TNK's player lookup work only; it cannot eliminate Minecraft's own spawn checks or other mods' searches.

Verification: 31 simulated spawns in one section/tick use one player snapshot and one area shortlist build. A deterministic 10,000-position randomized test matches a full nearest-player search, including underground/negative positions. Other tests cover two different nearest players inside the same cached section, bounded memory and logout invalidation. These are correctness and work-count checks, not a measured full-pack TPS benchmark. Widely dispersed spawns may touch many new sections and gain less from the area cache.

Mob attribute reapplication also avoids removing/re-adding unchanged modifiers on every chunk load. A changed configuration still replaces its modifier, and zero bonuses are omitted. No modifier stacks or automatic healing are introduced on rejoin.

## Original-mod research

These are source inspections, not bundled third-party code. No new dependency or Apotheosis gameplay override is introduced by 0.4.2.

**Auto Leveling**, Daripher, inspected commit `1a82e000dd4f82dfbedfe372d1ca5afeaac44ba2` (the checked-out original 1.19.2 branch, version 1.19b): [`MobsLevelingEvents.java`](https://github.com/Daripher/Auto-Leveling/blob/1a82e000dd4f82dfbedfe372d1ca5afeaac44ba2/src/main/java/daripher/autoleveling/event/MobsLevelingEvents.java). It stores a saved level, reapplies attributes, adds equipment from equipment loot tables and injects its leveled-mob loot table. It synchronizes levels when players start tracking and renders a client-side level label. The inspected source has no direct Apotheosis import/integration. Auto Leveling's original distance-based level rule is not used by TNK.

**Otherworld – Apotheosis**, inspected 1.20.1 commit `f707ae77807395d5e8652e55df185f5da337a10c`:

- [`LeveledAffixLootModifier`](https://github.com/muon-rw/OtherworldApoth/blob/f707ae77807395d5e8652e55df185f5da337a10c/src/main/java/dev/muon/otherworldapoth/loot/LeveledAffixLootModifier.java) is a global loot modifier. It obtains a mob's level from the Otherworld level bridge, chooses a level-bounded rarity, and calls Apotheosis `LootController.createLootItem`. It guards against nested processing and already-affixed items. Conversion chance also uses configured level/luck factors.
- [`LootUtils`](https://github.com/muon-rw/OtherworldApoth/blob/f707ae77807395d5e8652e55df185f5da337a10c/src/main/java/dev/muon/otherworldapoth/loot/LootUtils.java) maps mob and player levels to configured rarity ranges, rolls with rarity weights/luck, and separately handles gem/champion ranges.
- [`BossEventsMixin`](https://github.com/muon-rw/OtherworldApoth/blob/f707ae77807395d5e8652e55df185f5da337a10c/src/main/java/dev/muon/otherworldapoth/mixin/apotheosis/loot/BossEventsMixin.java) cancels natural bosses, minibosses and delayed minibosses. Other mixins replace default affix/gem injection. That suppression belongs to the bridge; it is not an Auto Leveling feature.

**The same repository's current 1.21.1 port**, now named Raven Apoth, inspected commit `118bfd8bdd874ceb5d8139ccbb9f7d1ebb5ce57b`:

- [`DropProfile`](https://github.com/muon-rw/OtherworldApoth/blob/118bfd8bdd874ceb5d8139ccbb9f7d1ebb5ce57b/src/main/java/dev/muon/raven_apoth/loot/DropProfile.java) uses Dynamic Difficulty's `LevelingAPI`, level/champion rarity brackets, spawn-origin checks and boss-specific chances.
- [`SpawnTier`](https://github.com/muon-rw/OtherworldApoth/blob/118bfd8bdd874ceb5d8139ccbb9f7d1ebb5ce57b/src/main/java/dev/muon/raven_apoth/tiers/SpawnTier.java) records the nearest player's Apotheosis World Tier during spawn finalization so later drops can use the mob's recorded spawn tier instead of the killer's current tier.
- [`WorldTierMixin`](https://github.com/muon-rw/OtherworldApoth/blob/118bfd8bdd874ceb5d8139ccbb9f7d1ebb5ce57b/src/main/java/dev/muon/raven_apoth/mixin/apotheosis/WorldTierMixin.java) optionally gates World Tier unlocks by player level. This newer source must not be confused with the old 1.20.1 boss-suppression design.

## TNK comparison

TNK Progression owns saved character/mob levels and stat modifiers. The companion TNK Loot beta already chooses tier from mob level capped by campaign ceiling, seals rarity and uses Apotheosis's affix rules during identification. Its direct-player-kill replacement path removes competing equipment drops before the single 2% TNK equipment roll, while retaining materials/gems.

TNK currently does not synchronize Apotheosis World Tiers with character levels, stamp the Apotheosis spawn tier, suppress Invaders/Elites, change gem purity/drop rules, import the reference's luck scaling or modify its global loot modifiers. Those are distinct gameplay/balance changes and are not silently added as a performance patch. Apotheosis health/damage bonuses still combine with TNK's bonuses. Optional integration behavior and full multiplayer pack performance need a real pack playtest.
