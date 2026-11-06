# Founder beta reward and pack-wide title grants

**0.7.1 update:** Founder can supply First Footsteps (+3% base movement speed) and Founding Hands (+5% base block-breaking speed) through the passive slot. Its cosmetic badge stays independent. Owner and Admin remain command-only cosmetics; the UUID `earn` roster cannot award those staff titles. See [TITLE-SLOTS.md](TITLE-SLOTS.md).

Minecraft 1.21.1 / NeoForge 21.1.262 / Java 21; TNK Progression 1.0.0-private.1, made for Towns & Kingdoms 3 | Beyond the Forge.
The shipped pack integration uses its existing KubeJS installation; the progression JAR gains no new hard dependency.

`tnk:beta_tester` displays **Founder** using the original amethyst artwork. Its description records participation in the Towns & Kingdoms 3 beta. Selecting it as the passive grants the two modest movement/breaking bonuses described above. Displaying the badge alone supplies no benefits. It grants no XP, skill points, permissions or progression bypasses.

## Assign a tester

Edit `kubejs/config/tnk-title-defaults.json` in the pack. Add their **Minecraft account UUID**, keeping the existing title definition:

```json
"grants": {
  "657bf016-419a-43dd-9b37-e0f29f08009c": ["tnk:beta_tester"]
}
```

That UUID is a test fixture, not an enrolled player. Replace it with the actual tester's UUID. The shipped roster is empty because no testers have been specified. UUIDs can be copied from the authenticated server's `usercache.json` or player log; use the UUID from the actual profile. Names in the optional `names` object are notes, not identity checks. Name changes retain access; servers using offline-mode UUIDs need those separate UUIDs listed.

To award another existing title, add its ID to that UUID's array. Multiple users receive separate entries. The optional helper edits the same JSON without requiring a running game:

```text
python tools/manage_default_titles.py --config /path/to/instance/kubejs/config/tnk-title-defaults.json add PLAYER_UUID tnk:beta_tester --name PlayerName
python tools/manage_default_titles.py --config /path/to/instance/kubejs/config/tnk-title-defaults.json list
python tools/manage_default_titles.py --config /path/to/instance/kubejs/config/tnk-title-defaults.json remove PLAYER_UUID tnk:beta_tester
```

## Distribute and use

Ship the edited JSON and `kubejs/server_scripts/progression/tnk_default_titles.js` in **client pack updates and on the dedicated server**. Listed UUIDs automatically receive their titles at login in existing worlds and new singleplayer worlds, including worlds with cheats disabled. Reconnect after editing the roster. No network connection or manual player command is needed for the award.

The script appends missing default definitions to an existing `config/tnk-titles.json`; it preserves custom entries/options and validates through the mod's real reload command. Malformed rosters award nothing and log an error. Grants use the existing server-side `earn` command, so duplicates do not stack and an existing equipped or hidden selection is preserved. A first title equips according to the existing `autoEquipFirst` option. Players can choose Founder in the character menu.

**A change made only to the multiplayer server's file does not update someone else's singleplayer installation.** Distribute the updated roster with the pack, or send that config file to the player. This is an offline-capable pack roster, not a hosted account service. Ordinary `/tnkprogress title grant` remains a per-world grant.

Removing a UUID/title from this roster prevents future automatic grants; it does not erase an achievement already saved in a world. To remove an earned title there, use `/tnkprogress title revoke PLAYER tnk:beta_tester`. Automatic awards respect that revocation; explicit `title grant` restores it in that world. A local world owner can edit their files, as with other singleplayer settings.

## Installation

Replace the previous progression JAR with `tnkprogression-1.0.0-private.1.jar` on clients and server. For UUID-default grants, install `integration/kubejs/server_scripts/progression/tnk_default_titles.js` and `integration/kubejs/config/tnk-title-defaults.json` under the instance's `kubejs/` folder, preserving any existing roster. Include the roster in client pack updates for singleplayer use. Badge and GUI artwork is embedded in the JAR and needs no KubeJS assets. Keep the original 588-node skill tree/config and unrelated scripts, configs and addon JARs. Do not install a second progression JAR.

## Verification

Built with Java 21 and checked with 26 unit tests, the NeoForge dedicated-server suite and a disposable integrated Minecraft client. Founder checks cover its fixed early/late additive contributions, no stacking, configuration migration, real menu selection, synced movement/breaking attributes and an independent staff cosmetic. GUI scales 2 and 4 were checked. This is isolated client/server verification, not a full TNK3 mod-list test. All original number/title image bytes are preserved. KubeJS UUID award fixtures and existing quest-binding fixtures also pass; the default-title roster still needs distribution to each singleplayer installation.
