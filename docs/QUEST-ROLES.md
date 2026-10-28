# Quest ranks using the supplied artwork — proposal

The Advanced Pack contains 18 colour styles and existing English role labels, so these require no image editing. Roles are a campaign identity, separate from character level: a level-80 dedicated miner can still be an early-tier Technician until their quest team advances. Keep the number-only level prefix as requested; do not automatically add these role labels beside it until a separate rank display is wanted.

This is a proposed mapping, not an enabled name-prefix feature. It uses the actual chapter source and stage script inspected at pack commit recorded in the release notes. Tier 1 is the starting campaign tier. Entering a later tier corresponds to completing the previous chapter, consistent with the existing `tk3_stages.js`.

| Tier entered | Current chapter theme | Existing artwork label | Required previous COMPLETE CHAPTER quest |
|---|---|---|---|
| 1 | Kinetic workshop | APPRENTICE | Starting rank |
| 2 | Hydraulic machinery | TECHNICIAN | `B0A94968CD02DE8A` |
| 3 | Precision machinery | ENGINEER | `60278B0905A64DC9` |
| 4 | Inductive/network machinery | SPECIALIST | `B81077B20C8371D3` |
| 5 | Ender machinery | EXPERT | `1CD504854E287D44` |
| 6 | Chemical machinery | MASTER | `4B5E0820B553B7C9` |
| 7 | Expedition frame | GRANDMASTER | `DCEB2AF9B398CAEB` |
| 8 | Containment frame | CHAMPION | `BCC05513E2AD3BA9` |
| 9 | Singularity frame | LEGEND | `91CC6BEFB3017B3F` |

The current source also has a **tenth Sovereign chapter**, entered after `DD2408B49260ECA5`; MONARCH is an available existing artwork label if a tenth rank is desired. The pack does not contain a SOVEREIGN label, so that name would require new artwork, which is outside this change.

Before implementing rank sync, choose whether the rank means highest campaign achievement (permanent) or current team's tier (can decrease after leaving). A sensible default is current team progression for the active rank, with permanent personal achievements retained separately. Sync completion/team data at login, team changes and relevant quest completions, including offline team members on their next login. Do not derive ranks from character level or grant an old team's rank permanently just by joining temporarily. Rank assignments must remain cosmetic unless new restrictions are explicitly designed.

Existing chapter completion tasks are checkmarks labelled “Confirm the working setup”, behind chapter dependencies. They are not machine-verification sensors. Awarding a rank at these IDs follows the current quest book, and does not prove a factory is still operating.
