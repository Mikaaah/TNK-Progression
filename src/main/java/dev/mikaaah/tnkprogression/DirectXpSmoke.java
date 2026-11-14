package dev.mikaaah.tnkprogression;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.network.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in integration tests in a disposable dedicated server, including the transformed cooking methods. */
public final class DirectXpSmoke {
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 @SuppressWarnings("unchecked") public static void run(ServerStartedEvent event){
  if(!Boolean.getBoolean("tnkprogression.directXpSmoke")&&!Boolean.getBoolean("tnkprogression.smoke"))return;
  var server=event.getServer();var world=server.overworld();var gp=new GameProfile(UUID.randomUUID(),"DirectXpTest");
  var p=new ServerPlayer(server,world,gp,ClientInformation.createDefault());
  p.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),p,CommonListenerCookie.createInitial(gp,false)){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){} @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}};p.setGameMode(GameType.SURVIVAL);p.setPos(0,65,0);
  try{
   var field=PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);((Map<UUID,ServerPlayer>)field.get(server.getPlayerList())).put(p.getUUID(),p);
   var players=PlayerList.class.getDeclaredField("players");players.setAccessible(true);((List<ServerPlayer>)players.get(server.getPlayerList())).add(p);world.players().add(p);
   var n=ProgressionAPI.profile(p);n.putInt("ceiling",100);n.putInt("level",55);n.putInt("balance_curve_version",2);n.remove("direct_xp_version");n.putLong("xp",BalanceConfig.current.previousCost(55)/2);
   n=ProgressionAPI.profile(p);check(ProgressionAPI.level(p)==55&&Math.abs(n.getLong("xp")-27500)<20,"migration keeps level and within-level fraction");
   // Missing preference and explicit false are different: pack defaults must not erase player choices.
   var config=ProgressionConfig.current;
   var configJson=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("tnkprogression.json"))).getAsJsonObject();
   check(configJson.has("hudEnabledByDefault")&&!configJson.get("hudEnabledByDefault").getAsBoolean(),"existing config gains disabled HUD default");
   check(!n.contains("hud_hidden")&&!CharacterState.snapshot(p).hud(),"new and untouched profiles use hidden HUD default");
   check(CharacterState.action(p,"hud","on")&&CharacterState.snapshot(p).hud(),"player can opt in over disabled pack default");
   var hudSaved=ProgressionData.get(server).save(new net.minecraft.nbt.CompoundTag(),p.registryAccess());
   var hudStored=ProgressionData.load(hudSaved,p.registryAccess()).entry("player:"+p.getUUID());
   check(hudStored.contains("hud_hidden")&&!hudStored.getBoolean("hud_hidden"),"explicit HUD opt-in survives saved-data roundtrip");
   config.hudEnabledByDefault=true;
   check(CharacterState.action(p,"hud","off")&&!CharacterState.snapshot(p).hud(),"explicit hide wins over enabled pack default");
   n.remove("hud_hidden");check(CharacterState.snapshot(p).hud(),"pack can still choose an enabled default");
   config.hudEnabledByDefault=false;
   check(!CharacterState.snapshot(p).hud()&&ProgressionAPI.level(p)==55&&Math.abs(n.getLong("xp")-27500)<20,"changing HUD defaults preserves character level and XP");
   TnkProgression.LOG.info("TNK_HUD_DEFAULT_SMOKE_OK: config migration, default hidden, explicit opt-in/out, saved preference and unchanged progress");
   long preserved=n.getLong("xp");Origins.open(p);check(Origins.choose(p,"tnk:deepdelver"),"base origin selectable");check(!Origins.choose(p,"tnk:dreadnought"),"ascended origin starts locked");
   p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,AscensionItem.SIGIL.toStack());AscensionItem.SIGIL.get().use(world,p,net.minecraft.world.InteractionHand.MAIN_HAND);
   check(p.getMainHandItem().isEmpty()&&Origins.ascendedUnlocked(p)&&Origins.selected(p).isEmpty(),"1.4 sigil consumes and unlocks reselection");
   check(ProgressionAPI.level(p)==55&&n.getLong("xp")==preserved,"ascension preserves character progress");
   check(Origins.choose(p,"tnk:dreadnought")&&!Origins.choose(p,"tnk:berserker"),"ascended origin one choice");
   n.putInt("level",1);n.putLong("xp",0);
   check(ProgressionAPI.awardSourceXp(p,"mining",.25)==0,"fractional reward carries");ProgressionAPI.awardSourceXp(p,"mining",.25);ProgressionAPI.awardSourceXp(p,"mining",.25);check(ProgressionAPI.awardSourceXp(p,"mining",.25)==1,"four quarter rewards pay one XP");
   long before=n.getLong("xp");ProgressionAPI.activity(p,"crafting",999999);ProgressionAPI.activity(p,"engineering",999999);check(n.getLong("xp")==before,"removed activities award zero");
   check(DirectXpConfig.current.mining(Blocks.COAL_ORE.defaultBlockState())==8,"coal reward not overridden by generic ore tag");
   check(DirectXpConfig.current.mining(Blocks.DIAMOND_ORE.defaultBlockState())==50,"diamond reward");
   var pos=new BlockPos(1024+(p.getUUID().hashCode()&0xFFFFFF),64,4);ActivityEvents.harvest(drop(p,pos,Blocks.DIAMOND_ORE.defaultBlockState()));check(n.getLong("xp")==before+50,"natural ore pays direct XP");
   ActivityEvents.harvest(drop(p,pos,Blocks.DIAMOND_ORE.defaultBlockState()));check(n.getLong("xp")==before+50,"mined position cannot pay twice");
   ActivityGuard.mark(world,pos.above());ActivityEvents.harvest(drop(p,pos.above(),Blocks.DIAMOND_ORE.defaultBlockState()));check(n.getLong("xp")==before+50,"placed ore pays zero");
   var zombie=EntityType.ZOMBIE.create(world);zombie.getPersistentData().putInt("tnk_level",20);check(DirectXpConfig.current.combat(zombie)==50,"combat reward uses mob level");
   var cow=EntityType.COW.create(world);cow.getPersistentData().putInt("tnk_level",20);check(DirectXpConfig.current.combat(cow)==5,"passive reward reduced");
   long quest=n.getLong("xp");server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tnkprogress xp DirectXpTest 150");check(n.getLong("xp")==quest+150&&n.getDouble("quest_xp_earned")==150,"reusable command records quest XP");
   server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tnkprogress xp DirectXpTest 150");check(n.getDouble("quest_xp_earned")==300,"command needs no unique reward ID");
   // Actual furnace tick/mixins: manually loaded food pays, hopper-style inventory loading does not.
   var furnacePos=new BlockPos(2,64,2);world.setBlockAndUpdate(furnacePos,Blocks.FURNACE.defaultBlockState());var be=(AbstractFurnaceBlockEntity)world.getBlockEntity(furnacePos);
   var menu=new FurnaceMenu(77,p.getInventory(),be,new SimpleContainerData(4));CookingXp.begin(p,menu);be.setItem(0,new ItemStack(Items.BEEF));CookingXp.end();be.setItem(1,new ItemStack(Items.COAL));
   double cooked=n.getDouble("cooking_xp_earned");for(int i=0;i<210;i++)AbstractFurnaceBlockEntity.serverTick(world,furnacePos,world.getBlockState(furnacePos),be);
   check(be.getItem(2).is(Items.COOKED_BEEF)&&n.getDouble("cooking_xp_earned")==cooked+2,"manual cooking pays once at actual completion");
   be.setItem(0,new ItemStack(Items.BEEF));for(int i=0;i<210;i++)AbstractFurnaceBlockEntity.serverTick(world,furnacePos,world.getBlockState(furnacePos),be);
   check(n.getDouble("cooking_xp_earned")==cooked+2,"automation input produces no character XP");
   var firePos=new BlockPos(3,64,3);world.setBlockAndUpdate(firePos,Blocks.CAMPFIRE.defaultBlockState());var fire=(net.minecraft.world.level.block.entity.CampfireBlockEntity)world.getBlockEntity(firePos);
   double fireBefore=n.getDouble("cooking_xp_earned");check(fire.placeFood(p,new ItemStack(Items.BEEF),1),"campfire accepts food");net.minecraft.world.level.block.entity.CampfireBlockEntity.cookTick(world,firePos,world.getBlockState(firePos),fire);check(n.getDouble("cooking_xp_earned")==fireBefore+2,"campfire pays on actual cooking");
   var cropPos=pos.above(3);long farm=n.getLong("xp");ActivityEvents.harvest(drop(p,cropPos,Blocks.WHEAT.defaultBlockState()));check(n.getLong("xp")==farm,"immature crop gives zero");ActivityEvents.harvest(drop(p,cropPos,((CropBlock)Blocks.WHEAT).getStateForAge(7)));check(n.getLong("xp")==farm+1,"mature crop pays per harvest");ActivityEvents.harvest(drop(p,cropPos,((CropBlock)Blocks.WHEAT).getStateForAge(7)));check(n.getLong("xp")==farm+1,"same harvest cannot pay twice");
   p.setPos(16,65,16);p.tickCount=20;ExplorationXp.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));long explore=n.getLong("xp");p.setPos(32,65,16);p.tickCount=40;ExplorationXp.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));check(n.getLong("xp")>=explore+1,"physically visited new chunk pays");long exploreAfter=n.getLong("xp");p.tickCount=60;ExplorationXp.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));check(n.getLong("xp")==exploreAfter,"repeat visit pays nothing");p.setPos(320,65,16);p.tickCount=80;ExplorationXp.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));check(n.getLong("xp")==exploreAfter,"teleport does not pay skipped travel XP");
   StarcatcherSmoke.run(p);
   long total=0;for(int l=1;l<100;l++)total+=ProgressionMath.levelCost(l);check(total==4_950_000,"level 100 total");
   var snapshot=CharacterState.snapshot(p);check(snapshot.activities().size()==7&&snapshot.activities().stream().allMatch(a->!a.info().isEmpty()),"seven direct sources have live help");
   check(snapshot.ascendedOriginsUnlocked()&&snapshot.originSelectionRequest()==1,"snapshot preserves ascension state");
   check(CharacterState.JSON.fromJson(CharacterState.JSON.toJson(snapshot),CharacterState.Snapshot.class).equals(snapshot),"snapshot JSON roundtrip");
   n.putInt("level",10);n.putInt("ceiling",10);n.putLong("xp",0);check(ProgressionAPI.awardSourceXp(p,"quest",500)==0&&n.getLong("xp")==0,"chapter ceiling retained");
   TnkProgression.LOG.info("TNK_DIRECT_XP_SMOKE_OK: migration, Ascension, fractional XP, natural/placed ores, level rewards, quest commands, real cooking mixins, automation exclusion, curve, UI payload and ceiling");
  }catch(Throwable failure){TnkProgression.LOG.error("TNK_DIRECT_XP_SMOKE_FAILED",failure);}
  finally{world.players().remove(p);try{var list=PlayerList.class.getDeclaredField("players");list.setAccessible(true);((List<?>)list.get(server.getPlayerList())).remove(p);var f=PlayerList.class.getDeclaredField("playersByUUID");f.setAccessible(true);((Map<?,?>)f.get(server.getPlayerList())).remove(p.getUUID());}catch(Exception ignored){}server.halt(false);}
 }
 private static BlockDropsEvent drop(ServerPlayer p,BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return new BlockDropsEvent(p.serverLevel(),pos,state,null,new ArrayList<>(List.of(new ItemEntity(p.level(),pos.getX(),pos.getY(),pos.getZ(),new ItemStack(Items.DIAMOND)))),p,new ItemStack(Items.DIAMOND_PICKAXE));}
}
