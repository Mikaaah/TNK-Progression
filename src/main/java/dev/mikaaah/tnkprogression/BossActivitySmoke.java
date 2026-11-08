package dev.mikaaah.tnkprogression;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.util.UUID;

/** Opt-in checks in a disposable world; no optional boss mods are required. */
public final class BossActivitySmoke {
    private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }

    public static void fixture(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!Boolean.getBoolean("tnkprogression.activityUiFixture") || !(event.getEntity() instanceof ServerPlayer player)) return;
        player.setInvulnerable(true);
        if (Origins.selected(player).isEmpty()) { Origins.open(player); Origins.choose(player, "tnk:librarian"); }
        var n = ProgressionAPI.profile(player);
        n.putInt("level", 42); n.putInt("ceiling", 50);
        n.putLong("mining_work", 23); n.putLong("fishing_work", 3);
        CharacterState.sendFull(player);
    }

    public static void run(ServerStartedEvent event) {
        if (!Boolean.getBoolean("tnkprogression.bossActivitySmoke")) return;
        var server = event.getServer();
        var world = server.overworld();
        var config = ProgressionConfig.current;
        var profile = new GameProfile(UUID.randomUUID(), "BossActivityTest");
        var player = new ServerPlayer(server, world, profile, ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(0, 64, 0);
        world.players().add(player);
        Integer zombie = config.bossBaseLevels.put("minecraft:zombie", 65);
        Integer villager = config.bossBaseLevels.put("minecraft:villager", 35);
        try {
            var n = ProgressionAPI.profile(player);
            n.putInt("level", 20);
            MobLevels.invalidate(world);
            var mob = EntityType.ZOMBIE.create(world);
            check(MobLevels.assign(world, mob) == 65, "boss base floor wins");
            n.putInt("level", 100);
            MobLevels.invalidate(world);
            for (int i = 0; i < 20; i++) check(MobLevels.assign(world, mob) == 110, "boss reaches 110 without random offset");
            ProgressionEvents.join(new EntityJoinLevelEvent(mob, world));
            check(ProgressionAPI.mobLevel(mob) == 110 && mob.getMaxHealth() > 20, "spawn scaling and API preserve level 110");
            mob.setHealth(7);
            n.putInt("level", 1);
            MobLevels.invalidate(world);
            ProgressionEvents.join(new EntityJoinLevelEvent(mob, world));
            check(ProgressionAPI.mobLevel(mob) == 110 && mob.getHealth() == 7, "saved bosses retain level and damage on rejoin");
            var old = EntityType.ZOMBIE.create(world);
            old.getPersistentData().putInt("tnk_level", 4);
            old.setHealth(6);
            ProgressionEvents.join(new EntityJoinLevelEvent(old, world));
            check(ProgressionAPI.mobLevel(old) == 65 && old.getHealth() == 6, "old bosses migrate once without healing");
            var neutral = EntityType.VILLAGER.create(world);
            ProgressionEvents.join(new EntityJoinLevelEvent(neutral, world));
            check(ProgressionAPI.mobLevel(neutral) == 35, "configured bosses need not implement Enemy");
            check(LevelBadges.mobBadge(110).getString().contains("110"), "level 110 badge never silently displays 100");
            var packet = new MobLevelPayload(12, mob.getUUID(), 110);
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), world.registryAccess());
            try {
                MobLevelPayload.CODEC.encode(buffer, packet);
                check(packet.equals(MobLevelPayload.CODEC.decode(buffer)), "network codec preserves level 110");
            } finally { buffer.release(); }
            var mining = ActivityHelp.describe(player, "mining");
            check(mining.stream().anyMatch(line -> line.contains("piston")), "mining exclusions explained");
            check(ActivityHelp.describe(player, "crafting").stream().anyMatch(line -> line.contains("Bread") && line.contains("Work per craft")), "actual recipe results appear in help");
            check(ActivityHelp.describe(player, "engineering").stream().anyMatch(line -> line.contains("Chapter 10 complete")), "engineering commissions explained");
            boolean open = config.requireOpenWater;
            config.requireOpenWater = false;
            check(ActivityHelp.describe(player, "fishing").stream().anyMatch(line -> line.contains("allows catches")), "fishing help follows server settings");
            config.requireOpenWater = open;
            var snapshot = CharacterState.snapshot(player);
            check(snapshot.activities().stream().allMatch(activity -> !activity.info().isEmpty()), "all seven activities have help");
            check(CharacterState.JSON.fromJson(CharacterState.JSON.toJson(snapshot), CharacterState.Snapshot.class).equals(snapshot), "help survives server snapshot roundtrip");
            check(BossLevels.defaults().get("deeperdarker:stalker") == 35, "Stalker included as a miniboss");
            TnkProgression.LOG.info("TNK_BOSS_ACTIVITY_SMOKE_OK: boss floor, +10, cap 110, saved-level migration, neutral bosses, codec, badge and all activity help");
        } catch (Throwable failure) { TnkProgression.LOG.error("TNK_BOSS_ACTIVITY_SMOKE_FAILED", failure); }
        finally {
            if (zombie == null) config.bossBaseLevels.remove("minecraft:zombie"); else config.bossBaseLevels.put("minecraft:zombie", zombie);
            if (villager == null) config.bossBaseLevels.remove("minecraft:villager"); else config.bossBaseLevels.put("minecraft:villager", villager);
            world.players().remove(player);
            MobLevels.invalidate(world);
            server.halt(false);
        }
    }
}
