# Cosmetic and passive titles — 1.0.0-private.1

Minecraft 1.21.1, NeoForge 21.1.262+, Java 21. Update the progression JAR on **both clients and server**; network protocol 5 carries both selections.

## Two independent choices

The cosmetic title supplies the badge in chat, nameplates and the character sheet. The passive title supplies one existing title's configured benefits. Both must be personally unlocked. They may match or differ. For example, display **Owner** or **Founder** while using **Survivor** for +2 maximum health.

The character menu has separate **Use Cosmetic** and **Use Passive** buttons. Each X clears its own slot, with a tooltip describing the result. The sidebar shows the current level badge immediately followed by the cosmetic title badge, such as `100 [Owner]`; the old `/ 100` suffix is removed. The passive title is shown separately underneath. Text/badge widths determine placement and fitting.

Only passive swaps use the existing 30-second cooldown. Cosmetic changes and hiding the cosmetic are freely available during that cooldown and do not reset it. Clearing the passive removes its benefits immediately but does not clear the lockout. One passive title applies at a time; cosmetic titles and the rest of the collection add no bonuses. Increasing maximum health does not heal the player.

Existing profiles migrate their old selected title into both slots once, preserving their previous visible badge and benefits. A hidden old title stays hidden and supplies no passive. Staff titles migrate as cosmetic only. Existing split selections remain unchanged when Founder gains its new perks; select Founder in the passive slot to enable them. Both selections persist independently in the same personal world save. Revoking a title clears that title from either slot; revoking the passive does not remove a different cosmetic badge.

## Founder and staff titles

| ID | Display | Original artwork | Acquisition | Passives |
|---|---|---|---|---|
| `tnk:beta_tester` | Founder | Amethyst | Existing UUID roster or explicit grant | +3% vanilla-base movement speed; +5% vanilla-base block-breaking speed |
| `tnk:owner` | Owner | Gold | Explicit operator/server `title grant` command | None |
| `tnk:admin` | Admin | Ruby | Explicit operator/server `title grant` command | None |

Owner and Admin never unlock from quests using `earn`, skill criteria, the UUID default roster or simply being OP. Their badges confer no Minecraft permissions. They remain cosmetic only even if perk settings accidentally contain entries for them. Existing title artwork/glyph indices stay stable; staff badges append after Founder.

## Founder balance

**First Footsteps** adds a fixed `0.003` movement-speed attribute value, equal to 3% of the normal `0.1` base. **Founding Hands** adds `0.05` block-breaking speed, equal to 5% of the normal `1.0` base. Both use additive-value modifiers. Their amounts stay constant at every character level and do not multiply other additive bonuses. For example, with late bonuses bringing movement speed to `0.3` and breaking speed to `5.0`, Founder brings them to `0.303` and `5.05`: a 1% relative improvement in that example. Other mods' total multipliers still follow Minecraft's normal attribute pipeline.

Founder occupies the same single passive slot as every other perk title. Merely displaying its badge adds no bonuses. It adds no combat stats, experience rewards or skill points. The existing passive cooldown still applies.

The `founderPassives` marker migrates existing `config/tnk-title-perks.json` files once, adding the Founder defaults only when that ID is absent. Existing perks, cooldown settings and explicit Founder customizations (including an empty list) are preserved. Once migrated, removing the Founder entry intentionally keeps it disabled.

```text
/tnkprogress title grant PlayerName tnk:owner
/tnkprogress title grant PlayerName tnk:admin
/tnkprogress title revoke PlayerName tnk:owner
```

Grant/revoke requires permission level 2 or the server console. Nobody is enrolled automatically. New built-in definitions are appended to existing catalogs without replacing custom entries/options.

Player selection commands:

```text
/tnkprogress title cosmetic tnk:owner
/tnkprogress title passive tnk:prepared
/tnkprogress title cosmetic clear
/tnkprogress title passive clear
```

The old `title select` and `title clear` commands now control the cosmetic slot. Existing `grant`, `earn`, `revoke`, quest bindings and beta UUID awards retain their roles. The UUID roster still must be distributed to singleplayer clients; a server-only edit cannot update another installation.

## Installation

Replace the previous progression JAR with `tnkprogression-1.0.0-private.1.jar` on clients and server. Keep the existing skill tree and all unrelated mods/configs. All 100 number sprites, 58 title sprites and character GUI resources are embedded in the JAR; no KubeJS asset installation is required. The optional UUID-default integration is documented separately in [DEFAULT-TITLES.md](DEFAULT-TITLES.md).
