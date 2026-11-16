# FTB Teams 2101.1.12: original runtime artifacts and team-ceiling fixture

Inspected on 2026-11-16. This is dependency metadata and original-bytecode verification, not an executed server test. No production source changed and no Gradle build ran for this follow-up.

## Recovered artifacts

The Teams 2101.1.12 official POM and Gradle module metadata pin `dev.ftb.mods:ftb-library-neoforge:2101.1.37` and `dev.architectury:architectury-neoforge:13.0.8`. Library itself requires Architectury 13.0.6 or newer, so the Teams pin satisfies it. All three downloaded JAR hashes match the publisher's Gradle `.module` SHA-256 values.

Files are in `/workspace/scratch/53794ff7c067/dependencies`; adjacent `.pom` and `.module` files preserve the official dependency/hash evidence. `ftb-runtime-2101.1.12.sha256` can be checked with `sha256sum -c` from that directory.

| Original JAR | Bytes | SHA-256 |
| --- | ---: | --- |
| `ftb-teams-neoforge-2101.1.12.jar` | 308430 | `b3f33436070b7ae4a706280561f1d2f1d1eb1a089cb13ec9851e99646165aed2` |
| `ftb-library-neoforge-2101.1.37.jar` | 1439881 | `624c2d2d1cbadb946d967e17757bd5b2d129d4d3ed71f2da0c70607d04476ac2` |
| `architectury-neoforge-13.0.8.jar` | 591636 | `2eb06668281be9c57ed6ba8b2ca39b155567063c3096aa94a1c2140694f787df` |

Official artifact URLs:

- [Teams original JAR](https://maven.ftb.dev/releases/dev/ftb/mods/ftb-teams-neoforge/2101.1.12/ftb-teams-neoforge-2101.1.12.jar), [POM](https://maven.ftb.dev/releases/dev/ftb/mods/ftb-teams-neoforge/2101.1.12/ftb-teams-neoforge-2101.1.12.pom), [Gradle metadata](https://maven.ftb.dev/releases/dev/ftb/mods/ftb-teams-neoforge/2101.1.12/ftb-teams-neoforge-2101.1.12.module)
- [Library original JAR](https://maven.ftb.dev/releases/dev/ftb/mods/ftb-library-neoforge/2101.1.37/ftb-library-neoforge-2101.1.37.jar), [POM](https://maven.ftb.dev/releases/dev/ftb/mods/ftb-library-neoforge/2101.1.37/ftb-library-neoforge-2101.1.37.pom), [Gradle metadata](https://maven.ftb.dev/releases/dev/ftb/mods/ftb-library-neoforge/2101.1.37/ftb-library-neoforge-2101.1.37.module)
- [Architectury original JAR](https://maven.architectury.dev/dev/architectury/architectury-neoforge/13.0.8/architectury-neoforge-13.0.8.jar), [POM](https://maven.architectury.dev/dev/architectury/architectury-neoforge/13.0.8/architectury-neoforge-13.0.8.pom), [Gradle metadata](https://maven.architectury.dev/dev/architectury/architectury-neoforge/13.0.8/architectury-neoforge-13.0.8.module)

Teams and Library embedded `META-INF/neoforge.mods.toml` require NeoForge >=21.1.0 and Minecraft >=1.21.1. Teams requires Architectury >=13.0.8 and Library >=2101.1.37. Architectury's manifest requires NeoForge >=21.0.110-beta and Minecraft >=1.21. These declared ranges include the audit target Minecraft 1.21.1 / NeoForge 21.1.262; this does not substitute for a boot test.

## Actual JAR API verification

Java 21 `javap -public -s` confirmed the following public methods in the original Teams JAR. Complete output is saved in `audit-notes/ftb-teams-runtime-signatures.txt`.

| Declaring type | Actual signature |
| --- | --- |
| `dev.ftb.mods.ftbteams.api.FTBTeamsAPI` | `static FTBTeamsAPI.API api()` |
| `dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API` | `boolean isManagerLoaded()` |
| `dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API` | `TeamManager getManager()` |
| `dev.ftb.mods.ftbteams.api.TeamManager` | `Optional<Team> getTeamForPlayerID(UUID)` |
| `dev.ftb.mods.ftbteams.api.Team` | `UUID getId()` |

The nested binary class name really is `$API`; it is not an integration mismatch. These signatures match `TeamBridge`'s reflection bindings. Bytecode for `TeamManagerImpl.getTeamForPlayerID(UUID)` reads the live known-player map and returns that player's current `PlayerTeam.getEffectiveTeam()`. Caching reflective `Method` objects is appropriate; caching manager/team membership indefinitely would become stale after a party join, leave, or server replacement.

Reproduction, from the workspace root with Java 21 on PATH:

```sh
javap -classpath dependencies/ftb-teams-neoforge-2101.1.12.jar -public -s 'dev.ftb.mods.ftbteams.api.FTBTeamsAPI' 'dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API' 'dev.ftb.mods.ftbteams.api.TeamManager' 'dev.ftb.mods.ftbteams.api.Team'
javap -classpath dependencies/ftb-teams-neoforge-2101.1.12.jar -p -c dev.ftb.mods.ftbteams.data.TeamManagerImpl
```

## Proposed dedicated-server fixture; not executed

Use a disposable Java 21 / Minecraft 1.21.1 / NeoForge 21.1.262 dedicated-server world with the audit Progression JAR and exactly the three originals above. Use two connected test players, AuditA and AuditB, with fresh UUIDs. Fresh players avoid pre-existing personal/team ceiling receipts contaminating assertions. Keep the default ceilings `10,30,50,70,80,94,100` and `shareTeamUnlocks=true` initially.

FTB Teams' original command-registration bytecode verifies `ftbteams party create <name>`, `party invite <players>`, `party join <team>`, and `party leave`; use tab-completion for the live team argument. The following sequence exercises actual Teams manager/membership and Progression SavedData without a mock bridge:

1. Have both players run `/tnkprogress status`: ceiling 10 for each. Record each UUID and current effective team UUID. A personal team is a valid Teams context; it must remain independent of another player's personal team.
2. AuditA runs `/ftbteams party create AuditPartyA`, then `/ftbteams party invite AuditB`. AuditB runs `/ftbteams party join AuditPartyA` using the completed team selector. Confirm both effective team UUIDs match before testing sharing.
3. As console/operator, run `/tnkprogress unlock AuditA 50`. Both players' `/tnkprogress status` must immediately report ceiling 50. AuditA's personal ceiling and the current team ceiling become 50; AuditB's personal ceiling remains 0 in the profile. There is no requirement to retroactively migrate an unlock earned before party creation into a new party.
4. AuditB runs `/ftbteams party leave`; their next status must return to ceiling 10, while AuditA remains at 50. AuditB creates a distinct party and must still report 10. This directly detects stale membership caching even within one server session.
5. Grant AuditB ceiling 30 in the second party. A stays 50 and B stays 30. Have B leave that party, then invite B back into A's party; B now sees 50, and after leaving sees their retained personal ceiling 30. Join/leave changes must not erase earned personal progress.
6. In A's party, grant 70; confirm sharing. Set `shareTeamUnlocks=false` in Progression's existing JSON config and run `/tnkprogress reload`. B must fall back to their personal 30 while A retains the personal 70 earned by that command. Re-enable and reload; B sees team 70 again. This tests both the config guard and live membership lookup.
7. Save and stop the server cleanly, then restart and reconnect both players. Confirm party membership and team UUID persisted, A's personal 70 and B's personal 30 persisted, and sharing still gives B 70. Make B leave after restart and check 30 immediately, detecting both stale manager and stale membership references.
8. In a second disposable instance, boot Progression with all three optional Teams runtime JARs absent, run status and unlock, and save/restart. Personal ceilings must continue working without linkage errors or warnings about a missing optional integration.

For automation on a real server, a development-only test command may perform the same assertions on the server thread using `FTBTeamsAPI.api().getManager()` and actual connected `ServerPlayer` instances. The original JAR exposes `TeamManager.createPartyTeam(ServerPlayer,String,String,Color4I)` and implementation `PartyTeam.invite(ServerPlayer,Collection<GameProfile>)`, `join(ServerPlayer)`, and `leave(UUID)`. Use the public party API/commands so Teams updates its own effective-team records and events; do not insert directly into internal maps. Assert `TeamBridge.key`, `ProgressionAPI.ceiling` and saved player/team entries at every transition. The fixture belongs only in the test harness, not production code. A fabricated API class with identical signatures proves reflection syntax but cannot establish real FTB Teams lifecycle behavior.

Pending: dedicated-server boot, connected-client command sequence, save/restart, and optional-dependency-absent runtime checks. This follow-up has established artifact provenance and binary API compatibility only.
