package dev.mikaaah.tnkprogression.verification;

import com.google.gson.JsonObject;
import dev.mikaaah.tnkprogression.*;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Server-only verification fixtures; never included in a distributable mod JAR. */
public final class AuditChecks {
    private static final UUID PROCESS_NONCE = UUID.randomUUID();
    private static final UUID PERSISTENCE_FIXTURE = UUID.fromString("5da60079-dd96-4f07-83dc-95a4a51e1362");
    private AuditChecks() {}

    public static void run(MinecraftServer server, JsonObject report) throws Exception {
        persistence(server, report);
        networking(server, report);
        pacing();
        report.addProperty("character_action_pacing", true);
        teams(server, report);
    }

    private static void check(boolean condition, String message) {
        AbyssSmoke.check(condition, "audit: " + message);
    }

    /** An isolated test entry proves a later JVM loaded the real SavedData file; never a gameplay profile. */
    private static void persistence(MinecraftServer server, JsonObject report) {
        CompoundTag expected = new CompoundTag();
        expected.putInt("audit_fixture_version", 1);
        expected.putUUID("fixture_uuid", PERSISTENCE_FIXTURE);
        expected.putInt("level", 42);
        expected.putInt("ceiling", 70);
        expected.putLong("xp", 1234);
        expected.putInt("skill_points_fixture", 73);
        expected.putString(Origins.KEY, "tnk:miner");
        expected.putInt("origin_revision", 2);
        expected.putBoolean("ascended_origins_unlocked", true);
        expected.putString("title_selected", "tnk:prepared");
        expected.putString("title_passive", "tnk:skill_delver");
        CompoundTag unlocked = new CompoundTag();
        unlocked.putBoolean("tnk:prepared", true);
        unlocked.putBoolean("tnk:skill_delver", true);
        expected.put("titles_unlocked", unlocked);
        expected.putDouble("mining_xp_remainder", 0.375);

        ProgressionData data = ProgressionData.get(server);
        CompoundTag sentinel = data.entry("audit_saved_sentinel");
        if (sentinel.isEmpty()) {
            sentinel.merge(expected);
            sentinel.putUUID("writer_process_nonce", PROCESS_NONCE);
            data.setDirty();
            report.addProperty("saved_data_restart_mode", "seeded");
        } else {
            check(sentinel.hasUUID("writer_process_nonce")
                            && !sentinel.getUUID("writer_process_nonce").equals(PROCESS_NONCE),
                    "persistence verification must run in a different JVM from the sentinel writer");
            CompoundTag saved = sentinel.copy();
            saved.remove("writer_process_nonce");
            check(saved.equals(expected), "previous-process SavedData retained the complete isolated sentinel before any writes");
            report.addProperty("saved_data_restart_mode", "verified");
            report.addProperty("saved_data_actual_process_restart", true);
        }
        report.addProperty("saved_data_sentinel_uuid", PERSISTENCE_FIXTURE.toString());
    }

    private record Fixture(ServerPlayer player, List<CharacterPayload> packets) {}

    private static Fixture fixture(MinecraftServer server, String name) throws Exception {
        ServerPlayer player = AbyssSmoke.player(server, server.overworld(), name);
        List<CharacterPayload> packets = new ArrayList<>();
        player.connection = new ServerGamePacketListenerImpl(server,
                new Connection(PacketFlow.SERVERBOUND), player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            private void capture(Packet<?> packet) {
                if (packet instanceof ClientboundCustomPayloadPacket custom
                        && custom.payload() instanceof CharacterPayload state) packets.add(state);
            }
            @Override public void send(Packet<?> packet) { capture(packet); }
            @Override public void send(Packet<?> packet, PacketSendListener listener) { capture(packet); }
            // No client negotiated optional payload channels on this socket-free fixture.
            @Override public boolean hasChannel(ResourceLocation id) { return false; }
        };
        CompoundTag profile = ProgressionAPI.profile(player);
        profile.putInt("level", 1);
        profile.putInt("ceiling", 0);
        profile.putLong("xp", 0);
        return new Fixture(player, packets);
    }

    /** Native Teams broadcasts probe every online listener, including the outer harness's offline player. */
    private static Map<ServerPlayer, ServerGamePacketListenerImpl> guardOfflineListeners(MinecraftServer server) {
        Map<ServerPlayer, ServerGamePacketListenerImpl> originals = new IdentityHashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerGamePacketListenerImpl original = player.connection;
            if (original == null || original.getConnection().channel() != null) continue;
            originals.put(player, original);
            player.connection = new ServerGamePacketListenerImpl(server,
                    new Connection(PacketFlow.SERVERBOUND), player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) { original.send(packet); }
                @Override public void send(Packet<?> packet, PacketSendListener listener) { original.send(packet, listener); }
                @Override public boolean hasChannel(ResourceLocation id) { return false; }
            };
        }
        return originals;
    }

    @SuppressWarnings("unchecked")
    private static void remove(Fixture fixture) throws Exception {
        ServerPlayer player = fixture.player();
        CharacterState.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
        player.serverLevel().players().remove(player);
        var players = net.minecraft.server.players.PlayerList.class.getDeclaredField("players");
        players.setAccessible(true);
        ((List<ServerPlayer>) players.get(player.server.getPlayerList())).remove(player);
        var ids = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
        ids.setAccessible(true);
        ((Map<UUID, ServerPlayer>) ids.get(player.server.getPlayerList())).remove(player.getUUID(), player);
        MobLevels.invalidate(player.serverLevel());
    }

    private static CharacterState.Snapshot decode(CharacterPayload payload, ServerPlayer player) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        try {
            CharacterPayload.CODEC.encode(buffer, payload);
            CharacterPayload decoded = CharacterPayload.CODEC.decode(buffer);
            check(payload.equals(decoded) && buffer.readableBytes() == 0, "character payload codec consumes one exact message");
            return CharacterState.JSON.fromJson(decoded.json(), CharacterState.Snapshot.class);
        } finally {
            buffer.release();
        }
    }

    private static void networking(MinecraftServer server, JsonObject report) throws Exception {
        Fixture fixture = fixture(server, "AuditNetwork");
        ServerPlayer player = fixture.player();
        try {
            CompoundTag profile = ProgressionAPI.profile(player);
            CharacterState.sendFull(player);
            check(fixture.packets().size() == 1, "initial full view sent through the real PacketDistributor path");
            CharacterPayload fullPayload = fixture.packets().getFirst();
            CharacterState.Snapshot full = decode(fullPayload, player);
            check(full.activities().size() == 7 && !full.titles().isEmpty(), "full view includes activities and the configured title catalogue");
            check(full.activities().stream().anyMatch(activity -> !activity.info().isEmpty()), "full view includes server-authored activity help");

            CharacterState.sendFull(player);
            CharacterState.send(player);
            check(fixture.packets().size() == 1, "same-tick duplicate full and unchanged incremental views do not send again");

            long xp = Math.min(37, ProgressionMath.levelCost(1) - 1);
            profile.putLong("xp", xp);
            profile.putDouble("mining_xp_earned", 12.5);
            profile.putDouble("mining_xp_last", 2.5);
            profile.putLong("mining_actions", 3);
            profile.putBoolean("hud_hidden", true);
            profile.putLong("origin_selection_request", 11);
            check(CharacterState.track(player, "mining"), "real activity selection accepted");
            CharacterState.send(player);
            check(fixture.packets().size() == 2, "changed counters send exactly one incremental view");
            CharacterPayload partialPayload = fixture.packets().getLast();
            CharacterState.Snapshot partial = decode(partialPayload, player);
            check(partial.titles().isEmpty() && partial.activities().stream().allMatch(activity -> activity.info().isEmpty()),
                    "incremental packet omits static title and help definitions");
            CharacterState.Snapshot merged = CharacterSnapshots.merge(partial, full);
            check(merged.titles().equals(full.titles()), "incremental merge retains the complete static title catalogue");
            for (var activity : merged.activities()) {
                var previous = full.activities().stream().filter(old -> old.id().equals(activity.id())).findFirst().orElseThrow();
                check(activity.info().equals(previous.info()), "incremental merge retains help for " + activity.id());
            }
            var mining = merged.activities().stream().filter(activity -> activity.id().equals("mining")).findFirst().orElseThrow();
            check(mining.earned() == 12.5 && mining.lastGain() == 2.5 && mining.actions() == 3,
                    "live counters survive the static-data merge");
            check(merged.xp() == xp && !merged.hud() && merged.tracked().equals("mining") && merged.originSelectionRequest() == 11,
                    "incremental merge retains new XP, HUD preference, tracking and origin request nonce");
            check(CharacterSnapshots.merge(partial, null).equals(partial), "incremental view before first full view is safe");

            CharacterState.send(player);
            check(fixture.packets().size() == 2, "unchanged incremental view is suppressed");
            CharacterState.sendFull(player);
            check(fixture.packets().size() == 3, "same-tick changed full view is sent");
            CharacterState.Snapshot refreshed = decode(fixture.packets().getLast(), player);
            check(refreshed.equals(CharacterState.snapshot(player)), "full refresh exactly matches live server state");
            check(CharacterSnapshots.merge(refreshed, full).equals(refreshed), "full refresh replaces previous static and dynamic data");
            CharacterState.sendFull(player);
            check(fixture.packets().size() == 3, "identical repeated full refresh is suppressed");
            profile.putLong("origin_selection_request", 12);
            CharacterState.sendFull(player);
            check(fixture.packets().size() == 4 && decode(fixture.packets().getLast(), player).originSelectionRequest() == 12,
                    "same-tick origin request change is never discarded by full-view deduplication");

            RegistryFriendlyByteBuf actionBuffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
            try {
                CharacterAction action = new CharacterAction("track", "fishing");
                CharacterAction.CODEC.encode(actionBuffer, action);
                check(action.equals(CharacterAction.CODEC.decode(actionBuffer)) && actionBuffer.readableBytes() == 0,
                        "bounded client action codec roundtrip");
            } finally {
                actionBuffer.release();
            }
            long oldXp = profile.getLong("xp");
            int packetCount = fixture.packets().size();
            new CharacterAction("xp", "1000000000").handle(player);
            new CharacterAction("refresh", "invalid").handle(player);
            new CharacterAction(null, null).handle(player);
            check(profile.getLong("xp") == oldXp && fixture.packets().size() == packetCount,
                    "unsupported grants and malformed refreshes do not mutate or send state");
            new CharacterAction("refresh", "").handle(player);
            new CharacterAction("track", "fishing").handle(player);
            check(CharacterState.tracked(player).equals("fishing"), "full refresh cannot starve a legitimate selection in the same tick");
            new CharacterAction("track", "cooking").handle(player);
            check(CharacterState.tracked(player).equals("fishing"), "a second same-tick selection is paced by the production action handler");

            if (!ModList.get().isLoaded("puffish_skills")) {
                check(SkillBridge.spentPoints(player, ProgressionConfig.current.skillCategory).isEmpty(),
                        "missing optional Skills API returns an empty spent-point view");
                check(!SkillBridge.open(player), "missing optional Skills GUI safely refuses to open");
                report.addProperty("character_skills_optional_absence", true);
            }
            report.addProperty("character_full_utf8_bytes", fullPayload.json().getBytes(StandardCharsets.UTF_8).length);
            report.addProperty("character_incremental_utf8_bytes", partialPayload.json().getBytes(StandardCharsets.UTF_8).length);
            report.addProperty("character_snapshot_and_payloads", true);
            report.addProperty("character_incremental_static_preservation", true);
        } finally {
            remove(fixture);
        }
    }

    private static void pacing() {
        CharacterActionGate gate = new CharacterActionGate();
        check(gate.accept(100, true), "first catalogue request accepted");
        check(!gate.accept(100, true) && !gate.accept(119, true), "catalogue requests wait a full 20 ticks");
        check(gate.accept(100, false), "catalogue traffic leaves the independent action slot available");
        check(!gate.accept(101, false) && gate.accept(102, false), "interactive actions retain two-tick pacing");
        check(gate.accept(120, true), "catalogue request accepted at exactly 20 ticks");
        check(gate.accept(120, false), "catalogue and interactive actions can both proceed at the same tick");
        check(gate.accept(5, true) && gate.accept(5, false), "a restarted clock opens fresh pacing windows");
        check(!gate.accept(5, true) && !gate.accept(5, false), "clock reset does not remove same-tick throttling");
    }

    /** No optional API types occur in signatures or constants: the absent-mod matrix loads this class too. */
    private static void teams(MinecraftServer server, JsonObject report) throws Exception {
        boolean wasSharing = ProgressionConfig.current.shareTeamUnlocks;
        ProgressionConfig.current.shareTeamUnlocks = true;
        List<Fixture> fixtures = new ArrayList<>();
        Map<ServerPlayer, ServerGamePacketListenerImpl> guarded = Map.of();
        Object manager = null;
        Method logout = null;
        Throwable primaryFailure = null;
        try {
            Fixture first = fixture(server, "AuditTeamA");
            fixtures.add(first);
            ServerPlayer a = first.player();
            int[] ceilings = ProgressionConfig.current.ceilings;
            int base = ceilings[0];
            if (!ModList.get().isLoaded("ftbteams")) {
                check(TeamBridge.key(a).isEmpty() && ProgressionAPI.ceiling(a) == base,
                        "absent optional Teams API preserves personal base ceiling");
                ProgressionAPI.unlock(a, ceilings[ceilings.length - 1]);
                check(TeamBridge.key(a).isEmpty() && ProgressionAPI.ceiling(a) == ceilings[ceilings.length - 1],
                        "personal unlock works with every Teams class absent");
                report.addProperty("ftb_teams_optional_absence", true);
                return;
            }

            check(ceilings.length >= 3, "native Teams fixture needs three configured milestone ceilings");
            int low = ceilings[1], high = ceilings[2];
            Class<?> apiType = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
            Object api = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI").getMethod("api").invoke(null);
            check((boolean) apiType.getMethod("isManagerLoaded").invoke(api), "native FTB Teams manager loaded before fixture");
            manager = apiType.getMethod("getManager").invoke(api);
            Class<?> implementation = Class.forName("dev.ftb.mods.ftbteams.data.TeamManagerImpl");
            Class<?> partyType = Class.forName("dev.ftb.mods.ftbteams.data.PartyTeam");
            Class<?> teamType = Class.forName("dev.ftb.mods.ftbteams.api.Team");
            Method login = implementation.getMethod("playerLoggedIn", ServerPlayer.class, UUID.class, String.class);
            logout = implementation.getMethod("playerLoggedOut", ServerPlayer.class);
            Method create = implementation.getMethod("createParty", ServerPlayer.class, String.class);
            Method invite = partyType.getMethod("invite", ServerPlayer.class, Collection.class);
            Method join = partyType.getMethod("join", ServerPlayer.class);
            Method leave = partyType.getMethod("leave", UUID.class);
            Method teamId = teamType.getMethod("getId");
            guarded = guardOfflineListeners(server);

            Fixture second = fixture(server, "AuditTeamB");
            fixtures.add(second);
            Fixture third = fixture(server, "AuditTeamC");
            fixtures.add(third);
            ServerPlayer b = second.player(), c = third.player();
            for (Fixture fixture : fixtures) {
                ServerPlayer player = fixture.player();
                login.invoke(manager, player, player.getUUID(), player.getGameProfile().getName());
                check(TeamBridge.key(player).isPresent() && ProgressionAPI.ceiling(player) == base,
                        "native personal team exists and has an independent base ceiling");
            }
            check(!TeamBridge.key(a).equals(TeamBridge.key(b)), "different personal teams have different native IDs");
            Object party = create.invoke(manager, a, "AuditParty_" + a.getUUID().toString().substring(0, 8));
            String partyKey = "team:" + teamId.invoke(party);
            invite.invoke(party, a, List.of(b.getGameProfile()));
            join.invoke(party, b);
            check(TeamBridge.key(a).orElseThrow().equals(partyKey) && TeamBridge.key(b).orElseThrow().equals(partyKey),
                    "native invite and join immediately update the effective-team lookup");
            Object otherParty = create.invoke(manager, c, "AuditParty_" + c.getUUID().toString().substring(0, 8));
            String otherKey = "team:" + teamId.invoke(otherParty);
            check(!partyKey.equals(otherKey), "simultaneous native parties remain distinct");
            ProgressionAPI.unlock(a, high);
            ProgressionAPI.unlock(c, low);
            check(ProgressionAPI.ceiling(a) == high && ProgressionAPI.ceiling(b) == high && ProgressionAPI.ceiling(c) == low,
                    "native party shares its own ceiling without leaking into the second party");
            check(ProgressionAPI.profile(b).getInt("ceiling") == 0, "shared ceiling does not invent a personal milestone receipt");

            leave.invoke(party, b.getUUID());
            check(!TeamBridge.key(b).orElseThrow().equals(partyKey) && ProgressionAPI.ceiling(b) == base,
                    "native leave invalidates old membership immediately without a cached party cap");
            ProgressionAPI.unlock(b, low);
            invite.invoke(party, a, List.of(b.getGameProfile()));
            join.invoke(party, b);
            check(ProgressionAPI.ceiling(b) == high, "native rejoin reads the current party again");
            ProgressionConfig.current.shareTeamUnlocks = false;
            check(TeamBridge.key(b).isEmpty() && ProgressionAPI.ceiling(b) == low && ProgressionAPI.ceiling(a) == high,
                    "disabling team sharing preserves each earned personal ceiling");
            ProgressionConfig.current.shareTeamUnlocks = true;
            check(TeamBridge.key(b).orElseThrow().equals(partyKey) && ProgressionAPI.ceiling(b) == high,
                    "re-enabling sharing resolves current native membership");

            ProgressionData reloaded = ProgressionData.load(ProgressionData.get(server)
                    .save(new CompoundTag(), a.registryAccess()).copy(), a.registryAccess());
            check(reloaded.entry(partyKey).getInt("ceiling") == high
                            && reloaded.entry(otherKey).getInt("ceiling") == low
                            && reloaded.entry("player:" + b.getUUID()).getInt("ceiling") == low,
                    "player and independent native-party milestone receipts survive SavedData NBT roundtrip");
            leave.invoke(party, b.getUUID());
            check(ProgressionAPI.ceiling(b) == low, "second leave retains the actual personal receipt");
            leave.invoke(party, a.getUUID());
            leave.invoke(otherParty, c.getUUID());
            report.addProperty("ftb_teams_live_membership", true);
            report.addProperty("ftb_teams_independent_parties", true);
            report.addProperty("ftb_teams_saved_data_nbt_roundtrip", true);
        } catch (Exception | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            ProgressionConfig.current.shareTeamUnlocks = wasSharing;
            Exception cleanupFailure = null;
            for (Fixture fixture : fixtures) {
                try {
                    if (manager != null && logout != null) logout.invoke(manager, fixture.player());
                } catch (Exception failure) {
                    if (cleanupFailure == null) cleanupFailure = failure;
                    else cleanupFailure.addSuppressed(failure);
                }
                try {
                    remove(fixture);
                } catch (Exception failure) {
                    if (cleanupFailure == null) cleanupFailure = failure;
                    else cleanupFailure.addSuppressed(failure);
                }
            }
            guarded.forEach((player, original) -> player.connection = original);
            if (cleanupFailure != null) {
                if (primaryFailure != null) primaryFailure.addSuppressed(cleanupFailure);
                else throw cleanupFailure;
            }
        }
    }
}
