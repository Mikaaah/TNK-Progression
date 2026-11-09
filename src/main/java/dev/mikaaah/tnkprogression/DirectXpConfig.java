package dev.mikaaah.tnkprogression;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;

/** Direct rewards stay constant as the character levels. Optional mods use tags/IDs, never hard dependencies. */
public final class DirectXpConfig {
 public double combatBase=10,combatPerLevel=2,passiveMultiplier=.1,toughMultiplier=2,minibossMultiplier=5,bossMultiplier=12;
 public double crop=1,specialCrop=2,fruit=2,berries=1,cookedFood=2,vanillaFishing=25;
 public double chunk=1,biome=150,structure=250,dimension=750;
 public Map<String,Double> miningBlocks=new LinkedHashMap<>(Map.ofEntries(
  Map.entry("minecraft:stone",1d),Map.entry("minecraft:deepslate",1d),Map.entry("minecraft:blackstone",1d),Map.entry("minecraft:end_stone",1d),
  Map.entry("minecraft:netherrack",.25),Map.entry("minecraft:dirt",.25),Map.entry("minecraft:coarse_dirt",.25),Map.entry("minecraft:sand",.25),Map.entry("minecraft:red_sand",.25),Map.entry("minecraft:gravel",.25),
  Map.entry("minecraft:ancient_debris",100d),Map.entry("minecraft:nether_quartz_ore",20d),Map.entry("minecraft:nether_gold_ore",20d)));
 public Map<String,Double> miningTags=new LinkedHashMap<>(Map.ofEntries(
  Map.entry("c:ores/coal",8d),Map.entry("c:ores/copper",8d),Map.entry("c:ores/iron",12d),Map.entry("c:ores/zinc",12d),
  Map.entry("c:ores/gold",20d),Map.entry("c:ores/redstone",20d),Map.entry("c:ores/lapis",20d),Map.entry("c:ores/quartz",20d),
  Map.entry("c:ores/diamond",50d),Map.entry("c:ores/emerald",50d),Map.entry("c:ores/netherite_scrap",100d),Map.entry("c:ores",12d),
  Map.entry("minecraft:base_stone_overworld",1d),Map.entry("minecraft:base_stone_nether",1d)));
 public Map<String,Double> starcatcher=new LinkedHashMap<>(Map.of("common",40d,"uncommon",75d,"rare",125d,"epic",250d,"legendary",500d,"golden",750d));
 public Map<String,Double> cookingItems=new LinkedHashMap<>();
 public Map<String,Double> mobMultipliers=new LinkedHashMap<>();
 public Set<String> minibosses=new HashSet<>(Set.of("deeperdarker:stalker"));
 public static DirectXpConfig current=new DirectXpConfig();
 public static boolean reload(){try{
  var path=FMLPaths.CONFIGDIR.get().resolve("tnk-xp.json");var gson=new GsonBuilder().setPrettyPrinting().create();
  if(!Files.exists(path))Files.writeString(path,gson.toJson(new DirectXpConfig()));
  var c=gson.fromJson(Files.readString(path),DirectXpConfig.class);if(c==null)throw new IllegalArgumentException("Empty XP config");
  for(var field:DirectXpConfig.class.getFields())if(field.getType()==double.class)valid(field.getDouble(c));
  for(var map:List.of(c.miningBlocks,c.miningTags,c.starcatcher,c.cookingItems,c.mobMultipliers)){
   if(map==null||map.size()>4096)throw new IllegalArgumentException("Invalid XP map");
   for(var e:map.entrySet()){if(e.getKey()==null||e.getValue()==null)throw new IllegalArgumentException("Null reward");valid(e.getValue());}
  }
  if(c.minibosses==null)throw new IllegalArgumentException("Null miniboss list");
  for(var map:List.of(c.miningBlocks,c.miningTags,c.cookingItems,c.mobMultipliers))for(String id:map.keySet())ResourceLocation.parse(id);
  for(String id:c.minibosses)ResourceLocation.parse(id);
  current=c;return true;
 }catch(Exception ex){TnkProgression.LOG.error("Keeping previous direct XP config",ex);return false;}}
 private static void valid(double value){if(!Double.isFinite(value)||value<0||value>1_000_000)throw new IllegalArgumentException("XP values must be finite, 0..1000000");}
 public double mining(BlockState state){
  Double exact=miningBlocks.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());if(exact!=null)return exact;
  double reward=0;boolean matched=false;
  for(var e:miningTags.entrySet())if(!e.getKey().equals("c:ores")&&state.is(TagKey.create(Registries.BLOCK,ResourceLocation.parse(e.getKey())))){matched=true;reward=Math.max(reward,e.getValue());}
  if(!matched && state.is(TagKey.create(Registries.BLOCK,ResourceLocation.parse("c:ores"))))return miningTags.getOrDefault("c:ores",0d);
  return reward;
 }
 public double farming(BlockState state){
  if(state.is(Blocks.PUMPKIN)||state.is(Blocks.MELON))return fruit;
  if(state.getBlock() instanceof NetherWartBlock || state.getBlock() instanceof CocoaBlock)return specialCrop;
  if(state.getBlock() instanceof SweetBerryBushBlock || state.getBlock() instanceof CaveVines || state.getBlock() instanceof CaveVinesPlantBlock)return berries;
  return crop;
 }
 public double cooking(ItemStack stack){return stack.has(DataComponents.FOOD)?cookingItems.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),cookedFood):0;}
 public double combat(net.minecraft.world.entity.Mob mob){
  String id=BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
  double multiplier=mobMultipliers.getOrDefault(id,minibosses.contains(id)?minibossMultiplier:BossLevels.baseLevel(mob)>0?bossMultiplier:mob.getMaxHealth()>=60?toughMultiplier:mob instanceof Enemy?1:passiveMultiplier);
  return (combatBase+combatPerLevel*ProgressionAPI.mobLevel(mob))*multiplier;
 }
}
