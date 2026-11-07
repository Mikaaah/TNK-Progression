package dev.mikaaah.tnkprogression;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** Integration assertions reached only through the opt-in disposable smoke server. */
public final class OriginSmoke {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void equal(double expected, double actual, String message) { check(Math.abs(expected - actual) < 1e-6, message + ": " + actual); }

    static ServerPlayer player(MinecraftServer server, UUID uuid) {
        var profile = new GameProfile(uuid, "OriginTest");
        var player = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault()) {
            @Override public void sendSystemMessage(Component message) {}
        };
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(Packet<?> packet) {}
        };
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(850.5, 64, 850.5);
        return player;
    }

    static void experience(ServerPlayer player, int level) {
        player.experienceLevel = level;
        player.experienceProgress = 0;
        player.totalExperience = (int) OriginRules.pointsAtLevel(level);
    }

    static long winningSeed() {
        for (long seed = 0; seed < 100000; seed++) if (RandomSource.create(seed).nextDouble() < OriginRules.BONUS_DROP_CHANCE) return seed;
        throw new AssertionError("No deterministic bonus-drop fixture seed");
    }

    static BlockDropsEvent drop(ServerPlayer player, BlockPos position, net.minecraft.world.level.block.state.BlockState block, ItemStack output, ItemStack tool) {
        var drops = new ArrayList<ItemEntity>();
        drops.add(new ItemEntity(player.serverLevel(), position.getX(), position.getY(), position.getZ(), output));
        return new BlockDropsEvent(player.serverLevel(), position, block, null, drops, player, tool);
    }

    public static void run(MinecraftServer server) throws Exception {
        UUID uuid = UUID.fromString("77fb1029-fd5a-44c1-a595-6a50ef5899e6");
        ServerPlayer player = player(server, uuid);
        var profile = ProgressionAPI.profile(player);
        for (String key : Set.copyOf(profile.getAllKeys())) profile.remove(key);
        ProgressionAPI.profile(player); // Initialize the existing balance schema before seeding current-curve XP.
        profile.putInt("level", 42);
        profile.putInt("ceiling", 50);
        profile.putLong("xp", 123);
        check(Origins.selected(player).isEmpty(), "existing characters are not randomly assigned an origin at login");
        check(!CharacterState.action(player, "origin", "tnk:deepdelver"), "origin cannot be chosen before the first Character visit");
        profile.putLong("character_packet_tick", server.overworld().getGameTime());
        new CharacterAction("origin_open", "").handle(player);
        check(profile.getBoolean("origin_prompted"), "a throttled opening packet still records the Character visit on a busy server");
        check(CharacterState.action(player, "origin_open", ""), "Character visit opens the origin choice");
        check(!Origins.choose(player, "tnk:invented") && !Origins.choose(player, ""), "invalid and empty origin IDs rejected");
        check(CharacterState.action(player, "origin", "tnk:deepdelver"), "first valid origin chosen");
        check(!Origins.choose(player, "tnk:deepdelver") && !Origins.choose(player, "tnk:librarian"), "same-origin replay and permanent-origin replacement rejected");
        check(!CharacterState.action(player, "origin_reset", "") && !CharacterState.action(player, "origin_set", "tnk:soldier"), "client packets cannot reset or administratively replace origins");
        check(CharacterState.snapshot(player).origin().equals("tnk:deepdelver"), "origin synchronized in Character state");
        var saved = ProgressionData.get(server).save(new CompoundTag(), player.registryAccess());
        var stored = ProgressionData.load(saved, player.registryAccess()).entry("player:" + uuid);
        check(stored.getString(Origins.KEY).equals("tnk:deepdelver") && stored.getInt("level") == 42 && stored.getLong("xp") == 123, "origin save roundtrip preserves existing progression");
        ServerPlayer reconnect = player(server, uuid);
        Origins.apply(reconnect);
        check(Origins.selected(reconnect).equals("tnk:deepdelver") && !Origins.choose(reconnect, "tnk:soldier"), "new player entity retains the permanent UUID-owned origin");

        var dispatcher = server.getCommands().getDispatcher();
        for (String command : List.of("tnkprogress origin set @s tnk:soldier", "tnkprogress origin reset @s")) {
            boolean denied = false;
            try { dispatcher.execute(command, player.createCommandSourceStack().withPermission(0)); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { denied = true; }
            check(denied, "origin administration requires permission level 2");
        }
        check(dispatcher.execute("tnkprogress origin set @s tnk:librarian", player.createCommandSourceStack().withPermission(2)) == 1, "admin command accepts a namespaced origin ID");
        check(dispatcher.execute("tnkprogress origin set @s tnk:invented", player.createCommandSourceStack().withPermission(2)) == 0, "admin command rejects invalid origin without clearing the old one");
        check(Origins.selected(player).equals("tnk:librarian"), "rejected admin change preserves selection");

        profile.remove("origin_revision"); // Existing 1.1 Librarians retain their original discount.
        // This calls the real transformed Minecraft method, proving the enchanting mixin applies.
        experience(player, 30);
        long before = Origins.experiencePoints(player);
        player.onEnchantmentPerformed(new ItemStack(Items.IRON_PICKAXE), 3);
        long spent = before - OriginRules.pointsAtLevel(27);
        check(Origins.experiencePoints(player) == OriginRules.pointsAtLevel(27) + (int) Math.floor(spent * .20), "Librarian refunds 20% of the actual enchanting XP cost");
        long after = Origins.experiencePoints(player);
        player.giveExperiencePoints(100);
        check(Origins.experiencePoints(player) == after + 100, "ordinary XP awards are not amplified by the origin");
        Origins.adminSet(player, "tnk:miner");
        experience(player, 30);
        player.onEnchantmentPerformed(new ItemStack(Items.IRON_PICKAXE), 3);
        check(Origins.experiencePoints(player) == OriginRules.pointsAtLevel(27), "non-Librarian receives no enchanting refund");

        // A real anvil output slot charges levels, consumes materials, then refunds only the origin share.
        Origins.adminSet(player, "tnk:blacksmith");
        var anvilPos = new BlockPos(850, 64, 850);
        player.serverLevel().setBlockAndUpdate(anvilPos, Blocks.ANVIL.defaultBlockState());
        var anvil = new AnvilMenu(7, player.getInventory(), ContainerLevelAccess.create(player.serverLevel(), anvilPos));
        var damaged = new ItemStack(Items.IRON_PICKAXE);
        damaged.setDamageValue(180);
        anvil.getSlot(0).set(damaged);
        anvil.getSlot(1).set(new ItemStack(Items.IRON_INGOT));
        anvil.createResult();
        experience(player, 30);
        int cost = anvil.getCost();
        check(cost > 0 && !anvil.getSlot(2).getItem().isEmpty() && anvil.getSlot(2).mayPickup(player), "legitimate anvil repair fixture");
        long baseAfter = OriginRules.pointsAtLevel(30 - cost);
        before = Origins.experiencePoints(player);
        anvil.getSlot(2).onTake(player, anvil.getSlot(2).getItem().copy());
        check(Origins.experiencePoints(player) == baseAfter + (int) Math.floor((before - baseAfter) * .20), "Blacksmith refunds 20% of an actual anvil operation");
        check(anvil.getSlot(0).getItem().isEmpty() && anvil.getSlot(1).getItem().isEmpty(), "origin refund does not duplicate anvil materials");

        double baseDamage = player.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
        double baseArmour = player.getAttribute(Attributes.ARMOR).getBaseValue();
        double baseMove = player.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
        Origins.adminSet(player, "tnk:soldier");
        for (int i = 0; i < 4; i++) Origins.apply(player);
        equal(baseDamage + 1, player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Soldier attack bonus does not stack on refresh");
        equal(baseArmour + 1, player.getAttributeValue(Attributes.ARMOR), "Soldier armour bonus does not stack on refresh");
        Titles.grant(player, "tnk:beta_tester");
        Titles.grant(player, "tnk:owner");
        Titles.select(player, "tnk:owner");
        Origins.adminSet(player, "tnk:wanderer");
        equal(baseMove + .005 + .003, player.getAttributeValue(Attributes.MOVEMENT_SPEED), "origin and selected Founder passive coexist");
        check(Titles.selected(player).equals("tnk:owner") && Titles.passive(player).equals("tnk:beta_tester"), "origin changes preserve independent title slots");
        equal(baseDamage, player.getAttributeValue(Attributes.ATTACK_DAMAGE), "admin origin change removes the previous attack modifier");
        Origins.adminSet(player, "tnk:farmer");
        player.setHealth(5);
        for (int i = 0; i < 4; i++) Origins.apply(player);
        check(player.getMaxHealth() == 21 && player.getHealth() == 5, "Farmer grants half a heart without refresh healing");
        Origins.adminReset(player);
        check(Origins.selected(player).isEmpty() && player.getMaxHealth() == 20 && player.getHealth() == 5, "admin reset removes perks without changing title benefits or healing");
        check(!Origins.choose(player, "tnk:miner"), "reset requires a new Character visit");
        Origins.open(player);
        check(Origins.choose(player, "tnk:deepdelver"), "admin reset permits a fresh confirmed selection");
        Origins.adminSet(player, "tnk:miner"); // Legacy IDs remain valid for administrators.

        // Winning roll is deterministic; verify one extra item, no Fortune multiplication and no XP replay.
        long seed = winningSeed();
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        player.setItemSlot(EquipmentSlot.MAINHAND, pickaxe);
        var speed = new PlayerEvent.BreakSpeed(player, Blocks.STONE.defaultBlockState(), 10, anvilPos);
        speed.setNewSpeed(20);
        OriginPerks.breakSpeed(speed);
        equal(21, speed.getNewSpeed(), "Miner speed preserves earlier break-speed event contributions");
        var mineral = ProgressionData.get(server).entry("mining_provenance_v2");
        mineral.remove(player.level().dimension().location() + "/53/4/53");
        var orePos = new BlockPos(852, 64, 852);
        var ore = drop(player, orePos, Blocks.IRON_ORE.defaultBlockState(), new ItemStack(Items.RAW_IRON, 3), pickaxe);
        player.getRandom().setSeed(seed);
        NeoForge.EVENT_BUS.post(ore);
        check(ore.getDrops().size() == 2 && ore.getDrops().get(1).getItem().getCount() == 1, "Miner adds one ore, not an entire Fortune stack");
        var repeated = drop(player, orePos, Blocks.IRON_ORE.defaultBlockState(), new ItemStack(Items.RAW_IRON), pickaxe);
        player.getRandom().setSeed(seed);
        NeoForge.EVENT_BUS.post(repeated);
        check(repeated.getDrops().size() == 1, "already-mined or replaced ore does not repeat the origin bonus");
        var silk = pickaxe.copy();
        silk.enchant(player.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        var silkDrop = drop(player, orePos.offset(1, 0, 0), Blocks.IRON_ORE.defaultBlockState(), new ItemStack(Items.IRON_ORE), silk);
        player.getRandom().setSeed(seed);
        NeoForge.EVENT_BUS.post(silkDrop);
        check(silkDrop.getDrops().size() == 1, "Silk Touch cannot multiply ore blocks");
        var cancelled = drop(player, orePos.offset(2, 0, 0), Blocks.IRON_ORE.defaultBlockState(), new ItemStack(Items.RAW_IRON), pickaxe);
        cancelled.setCanceled(true);
        player.getRandom().setSeed(seed);
        NeoForge.EVENT_BUS.post(cancelled);
        check(cancelled.getDrops().size() == 1, "cancelled harvests receive no origin drops");

        Origins.adminSet(player, "tnk:forester");
        var logPos = orePos.offset(0, 0, 3);
        ProgressionData.get(server).entry("origin_log_provenance_v1").remove(player.level().dimension().location() + "/53/4/53");
        var axe = new ItemStack(Items.IRON_AXE);
        var log = drop(player, logPos, Blocks.OAK_LOG.defaultBlockState(), new ItemStack(Items.OAK_LOG), axe);
        player.getRandom().setSeed(seed);
        NeoForge.EVENT_BUS.post(log);
        check(log.getDrops().size() == 2, "Forester receives an extra newly harvested natural log");
        var placedLog = drop(player, logPos, Blocks.OAK_LOG.defaultBlockState(), new ItemStack(Items.OAK_LOG), axe);
        player.getRandom().setSeed(seed);
        NeoForge.EVENT_BUS.post(placedLog);
        check(placedLog.getDrops().size() == 1, "log provenance prevents repeated placement or harvesting bonuses");
        Origins.adminSet(player, "tnk:farmer");
        var crop = drop(player, logPos.above(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7), new ItemStack(Items.WHEAT), ItemStack.EMPTY);
        player.getRandom().setSeed(seed);
        NeoForge.EVENT_BUS.post(crop);
        check(crop.getDrops().size() == 2, "Farmer receives an extra mature harvest item");
        var young = drop(player, logPos.above(2), Blocks.WHEAT.defaultBlockState(), new ItemStack(Items.WHEAT_SEEDS), ItemStack.EMPTY);
        player.getRandom().setSeed(seed);
        NeoForge.EVENT_BUS.post(young);
        check(young.getDrops().size() == 1, "immature crops do not grant extra drops");

        Origins.adminSet(player, "tnk:wanderer");
        var fall = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(player,
            new net.neoforged.neoforge.common.damagesource.DamageContainer(player.damageSources().fall(), 10));
        OriginPerks.fall(fall);
        equal(8.5, fall.getAmount(), "Wanderer reduces real fall damage input by fifteen percent");
        var otherDamage = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(player,
            new net.neoforged.neoforge.common.damagesource.DamageContainer(player.damageSources().generic(), 10));
        OriginPerks.fall(otherDamage);
        equal(10, otherDamage.getAmount(), "Wanderer does not reduce unrelated damage");

        Origins.adminSet(player, "tnk:fisher");
        var spawn = player.serverLevel().getSharedSpawnPos();
        player.setPos(spawn.getX() + .5, spawn.getY() + 1, spawn.getZ() + .5);
        equal(player.getAttribute(Attributes.LUCK).getBaseValue() + 1, player.getAttributeValue(Attributes.LUCK), "Fisher luck is a real player attribute");
        var hook = new net.minecraft.world.entity.projectile.FishingHook(player, player.serverLevel(), 0, 0);
        hook.tickCount = 100;
        var catchEvent = new net.neoforged.neoforge.event.entity.player.ItemFishedEvent(List.of(new ItemStack(Items.COD), new ItemStack(Items.ENCHANTED_BOOK)), 1, hook);
        var box = player.getBoundingBox().inflate(4);
        int fishBefore = player.serverLevel().getEntitiesOfClass(ItemEntity.class, box, item -> item.getItem().is(Items.COD)).size();
        player.getRandom().setSeed(seed);
        NeoForge.EVENT_BUS.post(catchEvent);
        OriginPerks.completedCatches(new net.neoforged.neoforge.event.tick.ServerTickEvent.Post(() -> true, server));
        check(player.serverLevel().getEntitiesOfClass(ItemEntity.class, box, item -> item.getItem().is(Items.COD)).size() == fishBefore + 1, "Fisher extra fish actually spawns after the event completes; rolled=" + hook.getPersistentData().getBoolean("tnk_origin_fish_rolled") + ", fishTag=" + new ItemStack(Items.COD).is(net.minecraft.tags.ItemTags.FISHES) + ", alive=" + player.isAlive() + ", removed=" + player.isRemoved());
        check(player.serverLevel().getEntitiesOfClass(ItemEntity.class, box, item -> item.getItem().is(Items.ENCHANTED_BOOK)).isEmpty(), "Fisher never copies treasure");
        player.getRandom().setSeed(seed);
        NeoForge.EVENT_BUS.post(catchEvent);
        OriginPerks.completedCatches(new net.neoforged.neoforge.event.tick.ServerTickEvent.Post(() -> true, server));
        check(player.serverLevel().getEntitiesOfClass(ItemEntity.class, box, item -> item.getItem().is(Items.COD)).size() == fishBefore + 1, "a hook cannot reroll or replay its origin bonus");
        var cancelledHook = new net.minecraft.world.entity.projectile.FishingHook(player, player.serverLevel(), 0, 0);
        cancelledHook.tickCount = 100;
        var cancelledCatch = new net.neoforged.neoforge.event.entity.player.ItemFishedEvent(List.of(new ItemStack(Items.COD)), 1, cancelledHook);
        player.getRandom().setSeed(seed);
        OriginPerks.fish(cancelledCatch);
        cancelledCatch.setCanceled(true);
        OriginPerks.completedCatches(new net.neoforged.neoforge.event.tick.ServerTickEvent.Post(() -> true, server));
        check(player.serverLevel().getEntitiesOfClass(ItemEntity.class, box, item -> item.getItem().is(Items.COD)).size() == fishBefore + 1, "late fishing cancellation suppresses the queued extra item");
        player.setGameMode(GameType.CREATIVE);
        Origins.adminSet(player, "tnk:librarian");
        check(Origins.captureXp(player, false) == -1 && Origins.captureXp(player, true) == -1, "creative players receive no XP refunds");
        player.setGameMode(GameType.SURVIVAL);

        check(dispatcher.execute("tnkprogress origin reset @s", player.createCommandSourceStack().withPermission(2)) == 1, "admin reset command is available");
        check(profile.getInt("level") == 42 && profile.getLong("xp") == 123, "origin selection and XP refunds never grant character progression");
        OriginExpandedSmoke.run(server);
        TnkProgression.LOG.info("TNK_ORIGINS_SMOKE_OK: permanent choice, admin permissions, UUID persistence, real enchanting/anvil refunds, origin/title coexistence, no stacking/healing and guarded harvest bonuses");
    }
}
