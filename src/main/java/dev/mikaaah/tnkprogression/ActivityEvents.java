package dev.mikaaah.tnkprogression;

import net.minecraft.server.level.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.inventory.CraftingContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.*;

public final class ActivityEvents {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void place(BlockEvent.EntityPlaceEvent event) {
        if(!(event.getLevel() instanceof ServerLevel world))return;
        if(event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for(var snap:multi.getReplacedBlockSnapshots()) if(mineral(world.getBlockState(snap.getPos())))ActivityGuard.mark(world,snap.getPos());
        } else if(mineral(event.getPlacedBlock())) ActivityGuard.mark(world,event.getPos());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void generated(BlockEvent.FluidPlaceBlockEvent event) {
        if(event.getLevel() instanceof ServerLevel world && mineral(event.getNewState()))ActivityGuard.mark(world,event.getPos());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void piston(PistonEvent.Pre event) {
        if(!(event.getLevel() instanceof ServerLevel world))return;
        var resolver=event.getStructureHelper();if(resolver==null || !resolver.resolve())return;
        var direction=event.getPistonMoveType().isExtend?event.getDirection():event.getDirection().getOpposite();
        // Conservatively treat moved minerals as artificial, including natural blocks moved by machines.
        for(var pos:resolver.getToPush()) if(mineral(world.getBlockState(pos))) {
            ActivityGuard.mark(world,pos);ActivityGuard.mark(world,pos.relative(direction));
        }
    }
    public static boolean mineral(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)
            || state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.ResourceLocation.parse("c:ores")))
            || state.is(BlockTags.COAL_ORES) || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.COPPER_ORES)
            || state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.REDSTONE_ORES)
            || state.is(BlockTags.LAPIS_ORES) || state.is(BlockTags.EMERALD_ORES);
    }
    public static boolean mature(BlockState state) {
        if(state.getBlock() instanceof CropBlock crop)return crop.isMaxAge(state);
        if(state.getBlock() instanceof NetherWartBlock)return state.getValue(NetherWartBlock.AGE)==3;
        if(state.getBlock() instanceof CocoaBlock)return state.getValue(CocoaBlock.AGE)==2;
        return false;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void harvest(BlockDropsEvent event) {
        if(!(event.getBreaker() instanceof ServerPlayer p) || !ActivityGuard.player(p) || event.getDrops().isEmpty())return;
        var world=event.getLevel();var state=event.getState();var pos=event.getPos();
        if(mineral(state)) {
            boolean used=ActivityGuard.marked(world,pos);ActivityGuard.mark(world,pos);
            if(!used)ProgressionAPI.activity(p,"mining",1);
        }else if(mature(state)) {
            creditHarvest(p,world,pos);
        }
    }
    private record HarvestAttempt(ServerPlayer player,ServerLevel world,BlockPos pos,BlockState before) {}
    private static final java.util.Map<java.util.UUID,HarvestAttempt> HARVEST_ATTEMPTS=new java.util.HashMap<>();
    private static boolean ripe(BlockState state) {
        return mature(state) || (state.getBlock() instanceof net.minecraft.world.level.block.SweetBerryBushBlock && state.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)>1)
            || net.minecraft.world.level.block.CaveVines.hasGlowBerries(state);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        if(!(event.getEntity() instanceof ServerPlayer p) || !ActivityGuard.player(p))return;
        var world=p.serverLevel();var before=world.getBlockState(event.getPos());
        if(ripe(before))HARVEST_ATTEMPTS.put(p.getUUID(),new HarvestAttempt(p,world,event.getPos().immutable(),before));
    }
    @SubscribeEvent public static void completedHarvests(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        for(var attempt:java.util.List.copyOf(HARVEST_ATTEMPTS.values())) {
            if(attempt.player().server!=event.getServer())continue;
            HARVEST_ATTEMPTS.remove(attempt.player().getUUID());
            var after=attempt.world().getBlockState(attempt.pos());
            if(after.getBlock()==attempt.before().getBlock() && !ripe(after))creditHarvest(attempt.player(),attempt.world(),attempt.pos());
        }
    }
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {HARVEST_ATTEMPTS.clear();}
    private static void creditHarvest(ServerPlayer p,ServerLevel world,BlockPos pos) {
        var d=ProgressionData.get(p.server);var times=d.entry("harvest_times");
        String key=world.dimension().location()+"/"+pos.asLong();long now=ActivityGuard.now(p);
        if(p.tickCount%100==0 || times.size()>4096)for(String k:java.util.Set.copyOf(times.getAllKeys()))if(ActivityRules.elapsed(now,times.getLong(k),ProgressionConfig.current.harvestCooldownTicks))times.remove(k);
        if(times.contains(key) && !ActivityRules.elapsed(now,times.getLong(key),ProgressionConfig.current.harvestCooldownTicks))return;
        times.putLong(key,now);d.setDirty();ProgressionAPI.activity(p,"farming",1);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void fish(ItemFishedEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer p) || !ActivityGuard.player(p))return;
        var hook=event.getHookEntity();
        if(event.getDrops().isEmpty() || hook.tickCount<80 || (ProgressionConfig.current.requireOpenWater && !hook.isOpenWaterFishing()))return;
        if(hook.getPersistentData().getBoolean("tnk_fishing_paid"))return;
        hook.getPersistentData().putBoolean("tnk_fishing_paid",true);
        if(ActivityGuard.ready(p,"fish",20))ProgressionAPI.activity(p,"fishing",1); // One successful catch, independent of stack size/luck.
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void craft(PlayerEvent.ItemCraftedEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer p) || !ActivityGuard.player(p) || event.getCrafting().isEmpty()
            || !(event.getInventory() instanceof CraftingContainer grid))return;
        var recipe=p.serverLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid.asCraftInput(),p.level());
        if(recipe.isEmpty())return;
        // Verify actual output, rather than trusting item acquisition or an arbitrary inventory event.
        var result=recipe.get().value().assemble(grid.asCraftInput(),p.registryAccess());
        if(!net.minecraft.world.item.ItemStack.isSameItemSameComponents(result,event.getCrafting()))return;
        String id=recipe.get().id().toString();var c=ProgressionConfig.current;
        String activity=c.engineeringRecipes.containsKey(id)?"engineering":"crafting";
        Double units=(activity.equals("engineering")?c.engineeringRecipes:c.craftingRecipes).get(id);
        if(units==null || !ActivityGuard.ready(p,"craft",1))return;
        // Credit a validated crafting action, never the inflated output stack/shift-click count.
        ProgressionAPI.activityFractional(p,activity,units);
    }
}
