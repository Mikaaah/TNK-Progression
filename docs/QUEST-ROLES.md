# FTB Quests rewards — 1.1.0

Use a **Command Reward** with **permission level 2**. Enter commands without a leading slash. FTB Quests substitutes `{p}` with the claiming player’s name.

## Reusable character XP

```text
tnkprogress xp {p} 150
```

Apply the same reward to every quest in a chapter and adjust only the amount. No unique reward ID is required. FTB Quests controls whether its rewards can be claimed once or repeatedly. This fills the personal character XP bar and records the gain under **Quest**.

| Quest size | Suggested XP |
| --- | ---: |
| Small | 50 |
| Normal | 150 |
| Major | 400 |
| Boss | 1,200+ |
| Chapter finale | 2,000–5,000 |
| Repeatable | 50–200 |

These are pack-author starting points, not automatic chapter edits. Millwright and Artificer apply their +10% / +35% XP perk to quest command rewards.

## Chapter unlocks

For a chapter that opens the level-30 ceiling:

```text
tnkprogress unlock {p} 30
```

The number must be one of the server’s configured ceilings. Unlock the ceiling **before** awarding chapter-completion XP. XP claimed while capped is not stored for later. Add the commands to the actual chapter’s completion quest; this repository does not silently rewrite the pack’s quest IDs.

Optional FTB Teams sharing follows `config/tnkprogression.json`; character XP remains personal.

## Persistent one-time integration receipts

Integrations that need their own permanent per-player receipt can still use:

```text
tnkprogress reward {p} tnk:chapter_1_complete 2000
```

Use a stable unique ID for that particular reward. Reusing it for unrelated quests will prevent later claims. Prefer the reusable `xp` command for ordinary FTB Quests rewards.

## Titles and Ascension

```text
tnkprogress title grant {p} tnk:beta_tester
give {p} tnkprogression:origin_ascension 1
```

The first example grants the Founder title. The second supplies a Sigil of Ascension; it unlocks 15 ascended origins and enables reselection when used. Title grants and an origin are separate from character XP.

## Updating an old integration

The former `tnkprogress engineering` command has been removed. Remove `tnk_activity_commissions.js` from installed `kubejs/server_scripts/progression/` folders and use the explicit chapter commands above. The old bridge is kept only as [historical source](history/tnk_activity_commissions.js.txt), outside the executable integration folder.

[Direct XP rules](DIRECT-XP.md) · [Optional title integrations](../integration/README.md) · [Old quest-rank proposal](history/QUEST-ROLES.md)
