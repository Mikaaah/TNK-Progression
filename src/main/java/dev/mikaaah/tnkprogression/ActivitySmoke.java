package dev.mikaaah.tnkprogression;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Integration assertions against real server registries/events, in a disposable smoke world only. */
public final class ActivitySmoke {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void run(MinecraftServer server) {
        var world=server.overworld();
        // Reset only this disposable smoke world's known fixture coordinates between runs.
        var fixtures=ProgressionData.get(server).entry("mining_provenance_v2");
        fixtures.remove(world.dimension().location()+"/7/1/28");fixtures.remove(world.dimension().location()+"/8/3/28");
        ProgressionData.get(server).entry("harvest_times").remove(world.dimension().location()+"/"+new BlockPos(133,50,456).asLong());
        var p=new ServerPlayer(server,world,new com.mojang.authlib.GameProfile(UUID.fromString("1931fcb0-c81e-4e68-b142-0406d8bfb402"),"ActivityTest"),ClientInformation.createDefault()) {
            @Override public void sendSystemMessage(Component message) {}
        };
        var n=ProgressionAPI.profile(p);for(String k:Set.copyOf(n.getAllKeys()))n.remove(k);
        n.putInt("level",90);n.putInt("ceiling",100);
        check(ActivityGuard.player(p),"real survival player eligible");
        ActivityGuard.mark(world,new BlockPos(123,30,456));
        check(ActivityGuard.marked(world,new BlockPos(123,30,456)),"placement provenance");
        check(!ActivityGuard.marked(world,new BlockPos(124,30,456)),"adjacent block remains natural");
        var saved=ProgressionData.get(server).save(new CompoundTag(),world.registryAccess());
        check(ProgressionData.load(saved,world.registryAccess()).entry("mining_provenance_v2").size()>0,"provenance survives save reload");
        var pos=new BlockPos(132,50,456);
        var drops=new ArrayList<ItemEntity>();drops.add(new ItemEntity(world,0,0,0,new ItemStack(Items.COBBLESTONE)));
        NeoForge.EVENT_BUS.post(new BlockDropsEvent(world,pos,Blocks.STONE.defaultBlockState(),null,drops,p,ItemStack.EMPTY));
        long mined=n.getLong("mining_work");check(mined==1,"natural mineral counts once");
        NeoForge.EVENT_BUS.post(new BlockDropsEvent(world,pos,Blocks.STONE.defaultBlockState(),null,drops,p,ItemStack.EMPTY));
        check(n.getLong("mining_work")==mined,"same position cannot regenerate mining credit");
        var cancelled=new BlockDropsEvent(world,new BlockPos(134,50,456),Blocks.STONE.defaultBlockState(),null,drops,p,ItemStack.EMPTY);cancelled.setCanceled(true);NeoForge.EVENT_BUS.post(cancelled);
        check(n.getLong("mining_work")==mined,"cancelled drops give no work");
        var crop=Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7);
        check(ActivityEvents.mature(crop),"mature wheat detected");check(!ActivityEvents.mature(Blocks.WHEAT.defaultBlockState()),"immature wheat excluded");
        var farmPos=new BlockPos(133,50,456);
        NeoForge.EVENT_BUS.post(new BlockDropsEvent(world,farmPos,crop,null,drops,p,ItemStack.EMPTY));
        check(n.getLong("farming_work")==1,"mature harvest counts");
        NeoForge.EVENT_BUS.post(new BlockDropsEvent(world,farmPos,crop,null,drops,p,ItemStack.EMPTY));
        check(n.getLong("farming_work")==1,"bonemeal instant repeat excluded");
        ProgressionAPI.activityFractional(p,"combat",.25);ProgressionAPI.activityFractional(p,"combat",.75);
        check(n.getLong("combat_work")==1 && n.getDouble("combat_fraction")==0,"fractional contributions accumulate exactly");
        check(ProgressionAPI.engineeringObjective(p,"tnk:first_workshop"),"engineering commission accepted");
        check(!ProgressionAPI.engineeringObjective(p,"tnk:first_workshop"),"engineering receipt cannot replay");
        check(!ProgressionAPI.engineeringObjective(p,"tnk:invented"),"unknown commission rejected");
        var rod=new FishingHook(p,world,0,0);rod.tickCount=100;
        var fish=new ItemFishedEvent(List.of(new ItemStack(Items.COD)),1,rod);
        // Fresh test hook has no open-water certificate: narrow AFK pool must not pay.
        rod.tickCount=0;NeoForge.EVENT_BUS.post(fish);check(n.getLong("fishing_rank")==0,"invalid/instant catch rejected");
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,-1) {
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
        };
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
        grid.setItem(0,new ItemStack(Items.WHEAT));grid.setItem(1,new ItemStack(Items.WHEAT));grid.setItem(2,new ItemStack(Items.WHEAT));
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(p,new ItemStack(Items.BREAD,64),grid));
        check(n.getLong("crafting_work")==1,"craft credits one validated action, not output stack count");
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(p,new ItemStack(Items.BREAD,64),grid));
        check(n.getLong("crafting_work")==1,"duplicate same-tick craft callback rejected");
        var zombie=new net.minecraft.world.entity.monster.Zombie(world);
        float healthBefore=zombie.getHealth();zombie.hurt(world.damageSources().playerAttack(p),5);
        double actual=healthBefore-zombie.getHealth();
        double recorded=zombie.getPersistentData().getCompound("tnk_contributions").getCompound(p.getUUID().toString()).getDouble("damage");
        check(actual>0 && Math.abs(recorded-actual)<0.00001,"actual post-armor health damage recorded by server event");
        var projectile=new net.minecraft.world.entity.projectile.Snowball(world,p);
        check(CombatProgression.owner(projectile)==p,"projectile resolves to its player");
        class OwnedZombie extends net.minecraft.world.entity.monster.Zombie implements OwnableEntity {
            OwnedZombie(){super(world);}
            @Override public UUID getOwnerUUID(){return p.getUUID();}
            @Override public LivingEntity getOwner(){return p;}
        }
        check(CombatProgression.owner(new OwnedZombie())==p,"owned summon resolves to player");
        n.putInt("level",100);double before=n.getDouble("combat_fraction");ProgressionAPI.activityFractional(p,"combat",.5);
        check(n.getDouble("combat_fraction")==before,"fractional work not banked at cap");
        TnkProgression.LOG.info("TNK_ACTIVITY_SMOKE_OK");
    }
}
