package dev.mikaaah.tnkprogression;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.*;

/** Additional opt-in assertions, called only by the disposable direct-XP server fixture. */
public final class DirectXpAuditSmoke {
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void run(ServerPlayer player){
  var world=player.serverLevel();var n=ProgressionAPI.profile(player);var data=ProgressionData.get(player.server);
  n.putInt("level",1);n.putLong("xp",900);n.putInt("ceiling",100);
  check(ProgressionAPI.awardXp(player,2500)==2500 && ProgressionAPI.level(player)==3 && n.getLong("xp")==400,"XP carries through successive levels");
  n.putInt("level",9);n.putLong("xp",8900);n.putInt("ceiling",10);
  check(ProgressionAPI.awardXp(player,Long.MAX_VALUE)==100 && ProgressionAPI.level(player)==10 && n.getLong("xp")==0,"huge XP grant safely stops at configured ceiling");
  n.putInt("ceiling",Integer.MAX_VALUE);check(ProgressionAPI.ceiling(player)==100,"legacy corrupt ceiling never exceeds100");
  n.putInt("level",1);n.putLong("xp",0);n.putInt("ceiling",100);
  n.putDouble("mining_xp_remainder",Double.NaN);n.putDouble("mining_xp_earned",Double.POSITIVE_INFINITY);
  check(ProgressionAPI.awardSourceXp(player,"mining",.25)==0 && n.getDouble("mining_xp_remainder")==.25 && n.getDouble("mining_xp_earned")==0,"corrupted fractional accounting is repaired without XP");
  n.putDouble("mining_xp_remainder",0);

  var first=new BlockPos(5,64,5);var second=first.above();
  world.setBlockAndUpdate(first,Blocks.PUMPKIN.defaultBlockState());world.setBlockAndUpdate(second,Blocks.MELON.defaultBlockState());
  var snapshots=List.of(BlockSnapshot.create(world.dimension(),world,first),BlockSnapshot.create(world.dimension(),world,second));
  ActivityEvents.place(new BlockEvent.EntityMultiPlaceEvent(snapshots,Blocks.STONE.defaultBlockState(),player));
  long before=n.getLong("xp");ActivityEvents.harvest(drop(player,second,Blocks.MELON.defaultBlockState()));
  check(n.getLong("xp")==before,"secondary multi-place fruit receives no farming XP");
  var piston=new BlockPos(8,64,8);world.setBlockAndUpdate(piston,Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.EAST));
  var ore=piston.east();world.setBlockAndUpdate(ore,Blocks.DIAMOND_ORE.defaultBlockState());world.setBlockAndUpdate(ore.east(),Blocks.AIR.defaultBlockState());
  ActivityEvents.piston(new PistonEvent.Pre(world,piston,Direction.EAST,PistonEvent.PistonMoveType.EXTEND));
  check(ActivityGuard.marked(world,ore)&&ActivityGuard.marked(world,ore.east()),"actual piston resolver protects source and destination mineral positions");

  var times=data.entry("harvest_times");var injected=new ArrayList<String>();
  for(int i=0;i<5000;i++){String key="audit/harvest/"+i;times.putLong(key,ActivityGuard.now(player));injected.add(key);}
  long sweeps=ActivityEvents.harvestSweeps();
  for(int i=0;i<128;i++)ActivityEvents.harvest(drop(player,new BlockPos(5000+i,80,1000),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7)));
  check(ActivityEvents.harvestSweeps()-sweeps<=1,"large cooldown ledger is not scanned per harvested block");
  check(times.contains(injected.getFirst()),"active harvest receipts are never evicted for size");
  injected.forEach(times::remove);

  var firePos=new BlockPos(6,64,6);world.setBlockAndUpdate(firePos,Blocks.CAMPFIRE.defaultBlockState());var fire=(CampfireBlockEntity)world.getBlockEntity(firePos);
  for(int i=0;i<1000;i++)check(CookingXp.beforeCampfireTick(fire,new int[4],new int[4])==null,"idle campfire has no XP recipe work");
  check(fire.placeFood(player,new ItemStack(Items.BEEF),1),"owned campfire input accepted");
  fire.clearContent();check(fire.getPersistentData().getString("tnk_cook_owner_0").isEmpty(),"clearing campfire removes stale ownership");
  double cooked=n.getDouble("cooking_xp_earned");check(fire.placeFood(null,new ItemStack(Items.BEEF),1),"non-player campfire input accepted");
  CampfireBlockEntity.cookTick(world,firePos,world.getBlockState(firePos),fire);
  check(n.getDouble("cooking_xp_earned")==cooked,"automation cannot inherit an old campfire owner");

  var oldPosition=player.position();player.setPos(1,65,1);
  player.tickCount=200;ExplorationXp.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
  player.tickCount=220;ExplorationXp.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
  data.setDirty(false);player.tickCount=240;ExplorationXp.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
  check(!data.isDirty(),"idle exploration does not dirty global SavedData");data.setDirty();player.setPos(oldPosition);
  var clone=new ServerPlayer(player.server,world,player.getGameProfile(),ClientInformation.createDefault());
  check(ProgressionAPI.profile(clone)==n,"replacement player object uses the same UUID-backed profile");
  var expected=n.copy();var saved=data.save(new net.minecraft.nbt.CompoundTag(),world.registryAccess()).copy();
  var loaded=ProgressionData.load(saved,world.registryAccess()).entry("player:"+player.getUUID());
  check(loaded.equals(expected)&&loaded.getBoolean("ascended_origins_unlocked")&&loaded.getString(Origins.KEY).equals("tnk:dreadnought"),"saved profile retains all fields, including XP, selected origin and ascension unlock");
  TnkProgression.LOG.info("TNK_XP_AUDIT_SMOKE_OK: overflow/ceilings, corrupt counters, multi-place/piston provenance, 5000-entry harvest ledger, idle/automated campfires, idle exploration and UUID persistence");
 }
 private static BlockDropsEvent drop(ServerPlayer player,BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return new BlockDropsEvent(player.serverLevel(),pos,state,null,new ArrayList<>(List.of(new ItemEntity(player.level(),pos.getX(),pos.getY(),pos.getZ(),new ItemStack(Items.WHEAT)))),player,new ItemStack(Items.DIAMOND_PICKAXE));}
}
