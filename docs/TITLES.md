# Titles — 1.1.0

Collect **58 titles** through configured achievements, quests, skill milestones or administrator awards. Ownership is saved per player. The original title and number badge artwork is included in the mod.

## Two independent slots

The **cosmetic title** controls your displayed badge. The **passive title** provides one earned title’s benefits. They may match or differ. Both require personal ownership. Cosmetic changes are independent of the passive swap cooldown, normally 30 seconds.

Only the selected passive contributes benefits. Clearing the cosmetic preserves the passive; clearing the passive removes its benefits. Repeated selections do not stack modifiers or heal the player. Owner and Admin are cosmetic and do not grant operator permissions. [Slot controls and behavior](TITLE-SLOTS.md).

## Player commands

```text
/tnkprogress title list
/tnkprogress title select tnk:beta_tester
/tnkprogress title clear
```

The character menu also provides cosmetic and passive selection with separate clear controls. Titles that require a missing optional attribute show unavailable benefits rather than inventing a replacement.

## Granting titles

Administrators and permission-level-2 quest rewards can grant an existing title:

```text
/tnkprogress title grant PlayerName tnk:beta_tester
/tnkprogress title revoke PlayerName tnk:beta_tester
```

In an FTB Quests Command Reward, omit the slash and replace `PlayerName` with `{p}`. `tnk:beta_tester` displays **Founder**. [Quest setup](QUEST-ROLES.md) · [UUID-based defaults](DEFAULT-TITLES.md).

A grant is saved in that world. Removing a user from an optional default-title roster prevents future automatic grants; it does not erase ownership already saved in a world. Use explicit revocation for that world when needed.

## Configuration

- `config/tnk-titles.json`: title definitions, unlock conditions and collection options.
- `config/tnk-title-perks.json`: passive bonuses and swap cooldown.
- `/tnkprogress title reload`: reloads the title configuration.

Keep the pack’s existing custom definitions and UUID roster when updating. Original badge resources are embedded; an extra badge resource pack is not needed.

[Character guide](CHARACTER-GUIDE.md) · [Optional integrations](../integration/README.md) · [Project home](../README.md)
