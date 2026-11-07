package dev.mikaaah.tnkprogression;

import java.util.ArrayList;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Resource bonuses apply to actual manual harvests, never crafting output or machine inventories. */
public final class OriginPerks {
    private static final TagKey<net.minecraft.world.level.block.Block> ORES = TagKey.create(Registries.BLOCK, ResourceLocation.parse("c:ores"));
    private record Catch(ItemFishedEvent event, ServerPlayer player, ItemStack extra) {}
    private static final java.util.List<Catch> CATCHES = new ArrayList<>();
    private OriginPerks() {}

    private static String origin(Player player) {
        if (player instanceof ServerPlayer serverPlayer) return Origins.selected(serverPlayer);
        return CharacterClientState.current == null ? "" : CharacterClientState.current.origin();
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        String origin = origin(player);
        ItemStack tool = player.getMainHandItem();
        boolean miner = origin.equals("tnk:miner") && tool.is(ItemTags.PICKAXES) && event.getState().is(BlockTags.MINEABLE_WITH_PICKAXE);
        boolean forester = origin.equals("tnk:forester") && tool.is(ItemTags.AXES) && event.getState().is(BlockTags.MINEABLE_WITH_AXE);
        if (miner || forester) event.setNewSpeed(event.getNewSpeed() + event.getOriginalSpeed() * (float) OriginRules.WORK_SPEED);
    }

    // Runs before ActivityEvents' LOWEST listener records the mining XP receipt.
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void drops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || !ActivityGuard.player(player)) return;
        var state = event.getState();
        ServerLevel world = event.getLevel();
        BlockPos pos = event.getPos();
        boolean log = state.is(BlockTags.LOGS);
        boolean usedLog = log && ActivityGuard.originLog(world, pos, false);
        if (log) ActivityGuard.originLog(world, pos, true);
        if (event.getDrops().isEmpty()) return;
        String origin = Origins.selected(player);
        Predicate<ItemStack> eligible = null;
        if (origin.equals("tnk:miner") && state.is(ORES) && !ActivityGuard.marked(world, pos)
            && event.getTool().is(ItemTags.PICKAXES) && event.getTool().isCorrectToolForDrops(state)
            && EnchantmentHelper.getItemEnchantmentLevel(world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), event.getTool()) == 0) {
            eligible = stack -> !(stack.getItem() instanceof BlockItem);
        } else if (origin.equals("tnk:forester") && log && !usedLog && event.getTool().is(ItemTags.AXES)) {
            eligible = stack -> stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().is(BlockTags.LOGS);
        } else if (origin.equals("tnk:farmer") && ActivityEvents.mature(state)) {
            eligible = stack -> stack.has(DataComponents.FOOD) || stack.is(Items.WHEAT) || stack.is(Items.NETHER_WART) || stack.is(Items.COCOA_BEANS);
        }
        if (eligible == null || player.getRandom().nextDouble() >= OriginRules.BONUS_DROP_CHANCE) return;
        Predicate<ItemStack> filter = eligible;
        ItemStack source = event.getDrops().stream().map(ItemEntity::getItem).filter(stack -> !stack.isEmpty() && filter.test(stack)).findFirst().orElse(ItemStack.EMPTY);
        if (source.isEmpty()) return;
        event.getDrops().add(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, source.copyWithCount(1)));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel world)) return;
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (var snapshot : multi.getReplacedBlockSnapshots()) if (world.getBlockState(snapshot.getPos()).is(BlockTags.LOGS)) ActivityGuard.originLog(world, snapshot.getPos(), true);
        } else if (event.getPlacedBlock().is(BlockTags.LOGS)) ActivityGuard.originLog(world, event.getPos(), true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void moved(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel world)) return;
        var resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) return;
        var direction = event.getPistonMoveType().isExtend ? event.getDirection() : event.getDirection().getOpposite();
        for (var pos : resolver.getToPush()) if (world.getBlockState(pos).is(BlockTags.LOGS)) {
            ActivityGuard.originLog(world, pos, true);
            ActivityGuard.originLog(world, pos.relative(direction), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void fish(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ActivityGuard.player(player)
            || !Origins.selected(player).equals("tnk:fisher") || event.getHookEntity().tickCount < 80) return;
        var receipt = event.getHookEntity().getPersistentData();
        if (receipt.getBoolean("tnk_origin_fish_rolled")) return;
        receipt.putBoolean("tnk_origin_fish_rolled", true);
        if (player.getRandom().nextDouble() >= OriginRules.BONUS_DROP_CHANCE) return;
        for (ItemStack stack : new ArrayList<>(event.getDrops())) if (!stack.isEmpty() && stack.is(ItemTags.FISHES)) {
            // NeoForge's event has a copy of the loot list. Queue the extra only after cancellation settles.
            CATCHES.add(new Catch(event, player, stack.copyWithCount(1)));
            return;
        }
    }

    @SubscribeEvent
    public static void completedCatches(ServerTickEvent.Post event) {
        for (Catch caught : java.util.List.copyOf(CATCHES)) {
            ServerPlayer player = caught.player();
            if (player.server != event.getServer()) continue;
            CATCHES.remove(caught);
            if (caught.event().isCanceled() || !player.isAlive() || player.isRemoved()) continue;
            var extra = new ItemEntity(player.serverLevel(), player.getX(), player.getY() + 0.5, player.getZ(), caught.extra());
            extra.setNoPickUpDelay();
            player.serverLevel().addFreshEntity(extra);
        }
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { CATCHES.clear(); }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void fall(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Origins.selected(player).equals("tnk:wanderer")
            && event.getSource().is(DamageTypeTags.IS_FALL)) event.setAmount(event.getAmount() * 0.85f);
    }
}
