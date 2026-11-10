# Optional pack integrations — 1.1.0

Progression does not require KubeJS, FTB Quests or FTB Teams to load. Use the integrations selected by your pack.

## Quest XP and chapter ceilings

Use [FTB Quests Command Rewards](../docs/QUEST-ROLES.md): `tnkprogress xp {p} 150` for XP and `tnkprogress unlock {p} 30` for a configured ceiling. Apply the unlock before awarding finale XP. No chapter-commission script is required.

If upgrading an existing installation, remove the obsolete `kubejs/server_scripts/progression/tnk_activity_commissions.js`. It called the retired engineering command. Its old quest IDs and source are retained under `docs/history/` for reference, not installation.

## Titles

- `kubejs/server_scripts/progression/tnk_titles.js` and `kubejs/config/tnk-title-quests.json` provide optional quest/title bindings. Review their quest IDs against the installed quest book before enabling them.
- `kubejs/server_scripts/progression/tnk_default_titles.js` and `kubejs/config/tnk-title-defaults.json` provide optional UUID-based title defaults. Preserve the pack’s existing roster. [Founder setup](../docs/DEFAULT-TITLES.md).

Copy only the integrations you intend to use into the instance’s existing `kubejs/` folder. Distribute matching files with client packs when singleplayer needs the same grants. Existing earned titles stay in world data; a change to an external roster does not revoke them automatically.
