package dev.mikaaah.tnkprogression;

import static dev.mikaaah.tnkprogression.OriginRules.Perk.*;
import java.util.*;
import java.lang.reflect.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.GrindstoneEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.*;

/** Real transformed vanilla calls and public event boundaries in an opt-in disposable server. */
final class OriginExpandedSmoke {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void equal(double expected, double actual, String message) { check(Math.abs(expected-actual)<1e-5, message+": expected "+expected+", got "+actual); }
    private static void choose(ServerPlayer player, String id) { check(Origins.adminSet(player, "tnk:"+id), "known origin "+id); }
    private static float speed(ServerPlayer player, Block block, Item item) {
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
        var event = new PlayerEvent.BreakSpeed(player, block.defaultBlockState(), 10, player.blockPosition());
        OriginPerks.breakSpeed(event); return event.getNewSpeed();
    }
    private static Object invoke(Object object, String method, Class<?>[] types, Object... args) throws Exception {
        Method call=object.getClass().getDeclaredMethod(method, types); call.setAccessible(true); return call.invoke(object,args);
    }
    private static void field(Object object, String name, int value) throws Exception {
        Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.setInt(object,value);
    }
    private static int field(Object object, String name) throws Exception {
        Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.getInt(object);
    }
    private static int orbXp(ServerPlayer p) {
        return p.serverLevel().getEntitiesOfClass(ExperienceOrb.class,p.getBoundingBox().inflate(8)).stream().mapToInt(ExperienceOrb::getValue).sum();
    }
    private static void drink(ServerPlayer p, MobEffectInstance effect) {
        var potion=new ItemStack(Items.POTION);potion.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.empty(),List.of(effect)));
        potion.finishUsingItem(p.serverLevel(),p);
    }
    private static LivingIncomingDamageEvent damage(LivingEntity target, net.minecraft.world.damagesource.DamageSource source) {
        var event=new LivingIncomingDamageEvent(target,new DamageContainer(source,10));OriginGameplay.damage(event);OriginPerks.fall(event);return event;
    }

    static void run(MinecraftServer server) throws Exception {
        var p=OriginSmoke.player(server,UUID.fromString("8c672337-b052-42d0-9422-32a51ab7881b"));
        var n=ProgressionAPI.profile(p);for(String key:Set.copyOf(n.getAllKeys()))n.remove(key);ProgressionAPI.profile(p);
        n.putInt("level",42);n.putInt("ceiling",50);
        var world=p.serverLevel();var spawn=world.getSharedSpawnPos();p.setPos(spawn.getX()+.5,spawn.getY()+1,spawn.getZ()+.5);
        check(OriginRules.all().size()==20,"20 current origins");
        for(var origin:OriginRules.all()) {
            Origins.adminSet(p,origin.id());check(Origins.revision(p)==2,"new profile revision");check(origin.bonuses().size()==2,"two implemented perks per origin");
            check(OriginExpandedSmoke.class.getResourceAsStream("/assets/tnkprogression/"+origin.icon().split(":",2)[1])!=null,"packaged custom icon "+origin.id());
        }
        Origins.adminReset(p);Origins.open(p);
        if(!Origins.manaAvailable(p))check(!Origins.choose(p,"tnk:hedge_mage"),"missing mana integration cannot consume permanent selection");
        check(Origins.choose(p,"tnk:deepdelver"),"new origin can be chosen");
        check(!Origins.choose(p,"tnk:woodcutter"),"new roster still enforces the permanent server lock");
        equal(11,speed(p,Blocks.IRON_ORE,Items.IRON_PICKAXE),"Deepdelver ore speed");
        equal(10,speed(p,Blocks.STONE,Items.IRON_PICKAXE),"Deepdelver does not speed ordinary stone");
        choose(p,"woodcutter");equal(11.5,speed(p,Blocks.OAK_LOG,Items.IRON_AXE),"Woodcutter logs");equal(10,speed(p,Blocks.OAK_PLANKS,Items.IRON_AXE),"Woodcutter excludes planks");
        choose(p,"stonemason");equal(11.5,speed(p,Blocks.STONE,Items.IRON_PICKAXE),"Stonemason stone speed");equal(10,speed(p,Blocks.IRON_ORE,Items.IRON_PICKAXE),"Stonemason excludes ores");
        double reach=p.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE));equal(reach+.5,p.blockInteractionRange(),"placement reach");
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));equal(reach,p.blockInteractionRange(),"placement reach requires block");
        choose(p,"millwright");equal(1,OriginRules.bonus("tnk:millwright",2,WRENCH_REACH),"wrench magnitude");equal(reach,p.blockInteractionRange(),"empty wrench condition does not extend reach");
        n.putInt("level",1);n.putInt("ceiling",100);n.putLong("xp",0);n.putLong("engineering_rank",0);n.putLong("engineering_work",0);
        var activity=ProgressionConfig.current.activities.get("engineering");long reward=ProgressionMath.taskReward(activity.initialXp(),0);
        ProgressionAPI.activity(p,"engineering",ProgressionMath.taskCost(activity.initialRequirement(),0));
        long earned=n.getLong("xp");for(int level=1;level<ProgressionAPI.level(p);level++)earned+=ProgressionMath.levelCost(level);
        equal(reward+(long)Math.floor(reward*.10),earned,"engineering bonus applies to earned activity XP");
        n.putInt("level",42);n.putInt("ceiling",50);n.putLong("xp",0);

        long seed=0;while(net.minecraft.util.RandomSource.create(seed).nextDouble()>=.05)seed++;
        BlockPos pos=new BlockPos(992,64,992);
        for(String ledger:List.of("mining_provenance_v2","origin_log_provenance_v1"))ProgressionData.get(server).entry(ledger).remove(world.dimension().location()+"/62/4/62");
        choose(p,"deepdelver");var ore=OriginSmoke.drop(p,pos,Blocks.IRON_ORE.defaultBlockState(),new ItemStack(Items.RAW_IRON,3),new ItemStack(Items.IRON_PICKAXE));p.getRandom().setSeed(seed);NeoForge.EVENT_BUS.post(ore);check(ore.getDrops().size()==2&&ore.getDrops().get(1).getItem().getCount()==1,"Deepdelver extra raw ore");
        var diamond=OriginSmoke.drop(p,pos.east(),Blocks.DIAMOND_ORE.defaultBlockState(),new ItemStack(Items.DIAMOND),new ItemStack(Items.IRON_PICKAXE));p.getRandom().setSeed(seed);NeoForge.EVENT_BUS.post(diamond);check(diamond.getDrops().size()==1,"Deepdelver excludes gems");
        choose(p,"woodcutter");var log=OriginSmoke.drop(p,pos.east(2),Blocks.OAK_LOG.defaultBlockState(),new ItemStack(Items.OAK_LOG),new ItemStack(Items.IRON_AXE));p.getRandom().setSeed(seed);NeoForge.EVENT_BUS.post(log);check(log.getDrops().size()==2,"Woodcutter extra log");
        choose(p,"homesteader");var crop=OriginSmoke.drop(p,pos.east(3),Blocks.CARROTS.defaultBlockState().setValue(CarrotBlock.AGE,7),new ItemStack(Items.CARROT,10),ItemStack.EMPTY);NeoForge.EVENT_BUS.post(crop);check(crop.getDrops().size()==2&&crop.getDrops().get(1).getItem().getCount()==1,"ten percent crop yield");
        p.getFoodData().setExhaustion(0);p.causeFoodExhaustion(1);equal(.9,p.getFoodData().getExhaustionLevel(),"Homesteader exhaustion");
        choose(p,"hedge_herbalist");var herb=OriginSmoke.drop(p,pos.east(4),Blocks.POPPY.defaultBlockState(),new ItemStack(Items.POPPY),ItemStack.EMPTY);p.getRandom().setSeed(seed);NeoForge.EVENT_BUS.post(herb);check(herb.getDrops().size()==2,"Herbalist extra flower");
        p.setHealth(10);p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(6);p.getFoodData().setExhaustion(0);
        for(int i=0;i<10;i++)p.getFoodData().tick(p);equal(11.1,p.getHealth(),"Herbalist natural regeneration");
        p.heal(1);equal(12.1,p.getHealth(),"ordinary healing is not natural regeneration");

        choose(p,"stablehand");var a=new Cow(EntityType.COW,world) { @Override public ServerPlayer getLoveCause() { return p; } };var b=new Cow(EntityType.COW,world);a.setPos(p.position());b.setPos(p.position());a.setInLove(p);b.setInLove(p);a.spawnChildFromBreeding(world,b);
        OriginGameplay.afterBreeding(new ServerTickEvent.Post(()->true,server));check(a.getAge()==5400&&b.getAge()==5400,"Stablehand real breeding cooldown: " + a.getAge() + "/" + b.getAge());
        a.setAge(6000);b.setAge(6000);var canceledBreed=new BabyEntitySpawnEvent(a,b,new Cow(EntityType.COW,world));NeoForge.EVENT_BUS.post(canceledBreed);canceledBreed.setCanceled(true);OriginGameplay.afterBreeding(new ServerTickEvent.Post(()->true,server));check(a.getAge()==6000&&b.getAge()==6000,"canceled breeding cannot shorten cooldowns");
        var horse=new Horse(EntityType.HORSE,world);horse.setPos(p.position());horse.setTamed(true);horse.equipSaddle(new ItemStack(Items.SADDLE),null);check(p.startRiding(horse,true),"mount fixture");
        double baseMount=horse.getAttributeValue(Attributes.MOVEMENT_SPEED);OriginGameplay.mount(new PlayerTickEvent.Post(p));equal(baseMount*1.15,horse.getAttributeValue(Attributes.MOVEMENT_SPEED),"mounted movement");
        p.stopRiding();OriginGameplay.mount(new PlayerTickEvent.Post(p));equal(baseMount,horse.getAttributeValue(Attributes.MOVEMENT_SPEED),"mount modifier removed on dismount");

        choose(p,"riverfolk");p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.FISHING_ROD));var hook=new FishingHook(p,world,0,0);field(hook,"timeUntilLured",0);hook.getRandom().setSeed(17);invoke(hook,"catchingFish",new Class[]{BlockPos.class},p.blockPosition());int wait=field(hook,"timeUntilLured");
        choose(p,"deepdelver");var normalHook=new FishingHook(p,world,0,0);normalHook.getRandom().setSeed(17);invoke(normalHook,"catchingFish",new Class[]{BlockPos.class},p.blockPosition());equal(Math.round(field(normalHook,"timeUntilLured")*.9),wait,"Riverfolk wait initialization");
        choose(p,"riverfolk");n.putDouble("origin_credit/fishing_xp",.99);field(hook,"nibble",1);hook.tickCount=100;int xpBefore=orbXp(p);check(hook.retrieve(p.getMainHandItem())>0,"real fishing retrieval");check(orbXp(p)>xpBefore,"real fishing XP spawned");check(n.getDouble("origin_credit/fishing_xp")<.99,"fishing XP bonus hook used");

        choose(p,"huntsman");var zombie=new Zombie(EntityType.ZOMBIE,world);var arrow=new Arrow(world,p,new ItemStack(Items.ARROW),new ItemStack(Items.BOW));equal(10.5,damage(zombie,p.damageSources().arrow(arrow,p)).getAmount(),"Huntsman ranged damage");
        var drops=new ArrayList<ItemEntity>();drops.add(new ItemEntity(world,p.getX(),p.getY(),p.getZ(),new ItemStack(Items.BEEF,10)));
        OriginGameplay.hunt(new LivingDropsEvent(zombie,p.damageSources().playerAttack(p),drops,false));check(drops.size()==2&&drops.get(1).getItem().getCount()==1,"Huntsman meat yield");

        choose(p,"forgehand");var sword=new ItemStack(Items.IRON_SWORD);sword.hurtAndBreak(10,world,p,item->{});check(sword.getDamageValue()==9,"Forgehand actual weapon wear");
        var pick=new ItemStack(Items.IRON_PICKAXE);pick.hurtAndBreak(10,world,p,item->{});check(pick.getDamageValue()==10,"Forgehand weapon perk excludes tools");
        var anvilPos=p.blockPosition().east(3);world.setBlockAndUpdate(anvilPos,Blocks.ANVIL.defaultBlockState());var anvil=new AnvilMenu(1,p.getInventory(),ContainerLevelAccess.create(world,anvilPos));var broken=new ItemStack(Items.IRON_SWORD);broken.setDamageValue(100);anvil.getSlot(0).set(broken);anvil.getSlot(1).set(new ItemStack(Items.IRON_INGOT));anvil.createResult();OriginSmoke.experience(p,30);long before=Origins.experiencePoints(p),baseAfter=OriginRules.pointsAtLevel(30-anvil.getCost());anvil.getSlot(2).onTake(p,anvil.getSlot(2).getItem().copy());equal(baseAfter+(long)Math.floor((before-baseAfter)*.10),Origins.experiencePoints(p),"Forgehand repair XP");
        choose(p,"librarian");OriginSmoke.experience(p,30);before=Origins.experiencePoints(p);p.onEnchantmentPerformed(new ItemStack(Items.IRON_PICKAXE),3);baseAfter=OriginRules.pointsAtLevel(27);equal(baseAfter+(long)Math.floor((before-baseAfter)*.10),Origins.experiencePoints(p),"new Librarian ten percent enchanting");
        var grind=new GrindstoneEvent.OnTakeItem(ContainerLevelAccess.create(world,anvilPos),p,new ItemStack(Items.ENCHANTED_BOOK),ItemStack.EMPTY,20);NeoForge.EVENT_BUS.post(grind);check(grind.getXp()==23,"Librarian disenchanting XP");
        check(Origins.captureAnvilXp(p,broken,new ItemStack(Items.IRON_SWORD))==-1,"new Librarian has no old anvil perk");

        choose(p,"apothecary");p.removeAllEffects();drink(p,new MobEffectInstance(MobEffects.MOVEMENT_SPEED,100));check(p.getEffect(MobEffects.MOVEMENT_SPEED).getDuration()==115,"drunk beneficial potion duration");
        p.setHealth(5);drink(p,new MobEffectInstance(MobEffects.HEAL,1));equal(9.4,p.getHealth(),"instant health potion ten percent healing");
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,100));check(p.getEffect(MobEffects.DIG_SPEED).getDuration()==100,"non-drunk effects unchanged");
        choose(p,"caravan_trader");var villager=new Villager(EntityType.VILLAGER,world);villager.setPos(p.position());var offer=new MerchantOffer(new ItemCost(Items.EMERALD,20),new ItemStack(Items.BREAD),20,5,0);villager.getOffers().clear();villager.getOffers().add(offer);invoke(villager,"updateSpecialPrices",new Class[]{net.minecraft.world.entity.player.Player.class},p);check(offer.getCostA().getCount()==18,"Caravan Trader buying price");invoke(villager,"updateSpecialPrices",new Class[]{net.minecraft.world.entity.player.Player.class},p);check(offer.getCostA().getCount()==18,"origin discount does not stack on repeated refresh");
        villager.setTradingPlayer(p);n.putDouble("origin_credit/trading_xp",.99);xpBefore=orbXp(p);villager.notifyTrade(offer);check(orbXp(p)>xpBefore&&n.getDouble("origin_credit/trading_xp")<.99,"trade XP hook");villager.setTradingPlayer(null);check(offer.getCostA().getCount()==20,"discount removed after trading");

        p.removeAllEffects();choose(p,"wayfarer");equal(p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()*1.05,p.getAttributeValue(Attributes.MOVEMENT_SPEED),"Wayfarer speed");equal(8,damage(p,p.damageSources().fall()).getAmount(),"Wayfarer fall damage");
        choose(p,"deckhand");equal(400,p.getMaxAirSupply(),"Deckhand air supply");equal(1.15,p.getAttributeValue(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED),"Deckhand swimming");
        choose(p,"town_watch");equal(1,p.getAttributeValue(Attributes.ARMOR),"Town Watch armour");equal(.1,p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),"Town Watch knockback resistance");
        choose(p,"street_urchin");equal(p.getAttribute(Attributes.SNEAKING_SPEED).getBaseValue()*1.15,p.getAttributeValue(Attributes.SNEAKING_SPEED),"Urchin sneak speed");p.setShiftKeyDown(true);equal(.8*.9,p.getVisibilityPercent(zombie),"Urchin detection while sneaking");p.setShiftKeyDown(false);equal(1,p.getVisibilityPercent(zombie),"standing detection unchanged");
        choose(p,"gravekeeper");equal(9,damage(p,p.damageSources().mobAttack(zombie)).getAmount(),"Gravekeeper undead defence");var kill=new LivingExperienceDropEvent(zombie,p,20);NeoForge.EVENT_BUS.post(kill);check(kill.getDroppedExperience()==23,"Gravekeeper undead XP");
        choose(p,"temple_acolyte");equal(22,p.getMaxHealth(),"Acolyte one heart");p.addEffect(new MobEffectInstance(MobEffects.POISON,100));check(p.getEffect(MobEffects.POISON).getDuration()==90,"Acolyte harmful effect duration");p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,100));check(p.getEffect(MobEffects.DIG_SPEED).getDuration()==100,"Acolyte leaves beneficial effects alone");
        choose(p,"hedge_mage");equal(.05,Origins.bonus(p,MANA),"Hedge Mage mana rule");equal(.05,Origins.bonus(p,MANA_REGEN),"Hedge Mage regen rule");
        Origins.adminReset(p);equal(20,p.getMaxHealth(),"all new attribute modifiers removed on reset");equal(300,p.getMaxAirSupply(),"air perk removed");
        TnkProgression.LOG.info("TNK_EXPANDED_ORIGINS_SMOKE_OK: 20 definitions/icons, versioned profiles, action-scoped bonuses, real potion/food/weapon/fishing/trading/anvil hooks and optional mana gating");
    }
}
