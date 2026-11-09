package dev.mikaaah.tnkprogression;

import net.minecraft.server.level.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.*;

public final class ActivityEvents {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void place(BlockEvent.EntityPlaceEvent event) {
        if(!(event.getLevel() instanceof ServerLevel world))return;
        if(event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for(var snap:multi.getReplacedBlockSnapshots()) if(protectedBlock(world.getBlockState(snap.getPos())))ActivityGuard.mark(world,snap.getPos());
        } else if(protectedBlock(event.getPlacedBlock())) ActivityGuard.mark(world,event.getPos());
        if(fruit(event.getPlacedBlock())){var data=ProgressionData.get(world.getServer());data.entry("placed_fruit_v3").putBoolean(world.dimension().location()+"/"+event.getPos().asLong(),true);data.setDirty();}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void generated(BlockEvent.FluidPlaceBlockEvent event) {
        if(event.getLevel() instanceof ServerLevel world && protectedBlock(event.getNewState()))ActivityGuard.mark(world,event.getPos());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void piston(PistonEvent.Pre event) {
        if(!(event.getLevel() instanceof ServerLevel world))return;
        var resolver=event.getStructureHelper();if(resolver==null || !resolver.resolve())return;
        var direction=event.getPistonMoveType().isExtend?event.getDirection():event.getDirection().getOpposite();
        // Conservatively treat moved minerals as artificial, including natural blocks moved by machines.
        for(var pos:resolver.getToPush()) if(protectedBlock(world.getBlockState(pos))) {
            ActivityGuard.mark(world,pos);ActivityGuard.mark(world,pos.relative(direction));
        }
    }
    private static boolean fruit(BlockState s){return s.is(net.minecraft.world.level.block.Blocks.PUMPKIN)||s.is(net.minecraft.world.level.block.Blocks.MELON);}
    public static boolean protectedBlock(BlockState state){return mineral(state)||DirectXpConfig.current.mining(state)>0||state.is(net.minecraft.world.level.block.Blocks.PUMPKIN)||state.is(net.minecraft.world.level.block.Blocks.MELON);}
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
        if(DirectXpConfig.current.mining(state)>0) {
            boolean used=ActivityGuard.marked(world,pos);ActivityGuard.mark(world,pos);
            if(!used)ProgressionAPI.awardSourceXp(p,"mining",DirectXpConfig.current.mining(state));
        }else if(ripe(state)) {
            creditHarvest(p,world,pos,state);
        }else if(state.is(net.minecraft.world.level.block.Blocks.PUMPKIN)||state.is(net.minecraft.world.level.block.Blocks.MELON)){
            var data=ProgressionData.get(p.server);var ledger=data.entry("placed_fruit_v3");String key=world.dimension().location()+"/"+pos.asLong();boolean used=ledger.getBoolean(key);ledger.remove(key);data.setDirty();
            if(!used)creditHarvest(p,world,pos,state);
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
            if(after.getBlock()==attempt.before().getBlock() && !ripe(after))creditHarvest(attempt.player(),attempt.world(),attempt.pos(),attempt.before());
        }
    }
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {HARVEST_ATTEMPTS.clear();}
    private static void creditHarvest(ServerPlayer p,ServerLevel world,BlockPos pos,BlockState state) {
        var d=ProgressionData.get(p.server);var times=d.entry("harvest_times");
        String key=world.dimension().location()+"/"+pos.asLong();long now=ActivityGuard.now(p);
        if(p.tickCount%100==0 || times.size()>4096)for(String k:java.util.Set.copyOf(times.getAllKeys()))if(ActivityRules.elapsed(now,times.getLong(k),ProgressionConfig.current.harvestCooldownTicks))times.remove(k);
        if(times.contains(key) && !ActivityRules.elapsed(now,times.getLong(key),ProgressionConfig.current.harvestCooldownTicks))return;
        times.putLong(key,now);d.setDirty();ProgressionAPI.awardSourceXp(p,"farming",DirectXpConfig.current.farming(state));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
    public static void fish(ItemFishedEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer p)||!ActivityGuard.player(p) )return;
        if(StarcatcherFishing.capture(event))return;
        if(event.isCanceled()||event.getDrops().isEmpty())return;
        if(StarcatcherFishing.eventFromStarcatcher()){
            var caught=StarcatcherFishing.resolve(p,event.getDrops());if(caught==null)return;
            var n=caught.bob().getPersistentData();if(n.getBoolean("tnk_fishing_paid"))return;
            n.putBoolean("tnk_fishing_paid",true);ProgressionAPI.awardSourceXp(p,"fishing",caught.xp());return;
        }
        var hook=event.getHookEntity();
        if(hook.tickCount<80||hook.getOwner()!=p||(ProgressionConfig.current.requireOpenWater&&!hook.isOpenWaterFishing())||hook.getPersistentData().getBoolean("tnk_fishing_paid"))return;
        hook.getPersistentData().putBoolean("tnk_fishing_paid",true);
        if(ActivityGuard.ready(p,"fish",20))ProgressionAPI.awardSourceXp(p,"fishing",DirectXpConfig.current.vanillaFishing);
    }
    // Crafting and engineering intentionally have no automatic XP handler.
}
