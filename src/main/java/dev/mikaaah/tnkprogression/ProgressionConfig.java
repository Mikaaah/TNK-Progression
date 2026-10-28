package dev.mikaaah.tnkprogression;
import com.google.gson.*;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.*;
import java.util.*;
public final class ProgressionConfig {
 public int[] ceilings = {10,30,50,70,80,94,100};
 public int[] rarityCaps = {1,2,3,4,4,5,5};
 public boolean shareTeamUnlocks = true;
 public String skillCategory = "puffish_skills:character";
 public boolean scaleMobs = true;
 public int mobLevelVariance = 4;
 public int balanceVersion=2;
 public Map<String,Long> legacyActivityRequirements=new HashMap<>(Map.of("mining",100L,"combat",10L,"fishing",1L,"exploration",10L,"farming",40L,"crafting",30L,"engineering",10L));
 public double healthPerLevel = 0.03, damagePerLevel = 0.0125;
 public Map<String,Activity> activities = defaults();
 public static Map<String,Activity> defaults() { return new HashMap<>(Map.of("mining",new Activity(100,800),"combat",new Activity(10,2000),"fishing",new Activity(20,3000),"exploration",new Activity(30,1500),"farming",new Activity(40,1000),"crafting",new Activity(30,1800),"engineering",new Activity(10,4500))); }
 public int contributionTicks = 1200, combatWindowTicks = 12000, harvestCooldownTicks = 1200;
 public double contributionRange = 128;
 public boolean requireOpenWater = true;
 public Set<String> excludedSpawns = new HashSet<>(Set.of("SPAWNER","TRIAL_SPAWNER","SPAWN_EGG","DISPENSER","COMMAND","BREEDING","MOB_SUMMONED","TRIGGERED","SPLIT"));
 // Only audited, material-consuming recipe IDs earn repeatable crafting work. No packing/unpacking/dye loops.
 public Map<String,Double> craftingRecipes = new HashMap<>(Map.of("minecraft:bread",1.0,"minecraft:paper",1.0,"minecraft:torch",1.0,"minecraft:iron_pickaxe",3.0,"minecraft:iron_sword",3.0));
 public Map<String,Double> engineeringRecipes = new HashMap<>(Map.of("create:crafting/kinetics/mechanical_press",3.0,"create:crafting/kinetics/mechanical_mixer",3.0,"create:crafting/kinetics/millstone",3.0,"kubejs:tk3/frames/rotation_manual",3.0,"kubejs:tk3/frames/create_millstone",3.0));
 // Commission IDs are a server-authorized API for FTB Quests/KubeJS; each pays once per player.
 public Map<String,Double> engineeringObjectives = engineeringDefaults();
 public static Map<String,Double> engineeringDefaults(){var map=new HashMap<String,Double>();map.put("tnk:first_workshop",10.0);map.put("tnk:first_processing_line",20.0);for(int i=1;i<=10;i++)map.put("tnk:chapter_"+i+"_complete",10.0*i);return map;}
 public record Activity(long initialRequirement,long initialXp) {}
 public static ProgressionConfig current = new ProgressionConfig();
 public static boolean reload() {
  Path path=FMLPaths.CONFIGDIR.get().resolve("tnkprogression.json");
  try {
   Gson gson=new GsonBuilder().setPrettyPrinting().create();
   if (!Files.exists(path)) Files.writeString(path,gson.toJson(new ProgressionConfig()));
   JsonObject raw=gson.fromJson(Files.readString(path),JsonObject.class);
   ProgressionConfig c=gson.fromJson(raw,ProgressionConfig.class);
   boolean treeMigration="tnk:character".equals(c.skillCategory);
   if(treeMigration)c.skillCategory="puffish_skills:character";
   boolean migrate=!raw.has("balanceVersion");
   if(migrate){
    for(var entry:c.activities.entrySet())c.legacyActivityRequirements.put(entry.getKey(),entry.getValue().initialRequirement());
    var legacy=Map.of("mining",new Activity(100,25),"combat",new Activity(10,35),"fishing",new Activity(1,15),"exploration",new Activity(10,30),"farming",new Activity(40,25),"crafting",new Activity(30,25),"engineering",new Activity(10,40));
    for(var entry:legacy.entrySet())if(entry.getValue().equals(c.activities.get(entry.getKey())))c.activities.put(entry.getKey(),defaults().get(entry.getKey()));
    if(c.healthPerLevel==0.035)c.healthPerLevel=0.03;if(c.damagePerLevel==0.015)c.damagePerLevel=0.0125;
   }
   if(c.ceilings.length!=c.rarityCaps.length || c.ceilings.length==0 || c.ceilings[c.ceilings.length-1]!=100 || c.mobLevelVariance<0 || c.mobLevelVariance>99 || !Double.isFinite(c.healthPerLevel) || !Double.isFinite(c.damagePerLevel) || c.healthPerLevel<0 || c.damagePerLevel<0) throw new IllegalArgumentException("Invalid balance settings");
   int prev=0;
   for(int i=0;i<c.ceilings.length;i++){ if(c.ceilings[i]<=prev || c.rarityCaps[i]<0 || c.rarityCaps[i]>5) throw new IllegalArgumentException("Invalid milestones"); prev=c.ceilings[i]; }
   for (var entry:defaults().entrySet()) c.activities.putIfAbsent(entry.getKey(),entry.getValue());
   if(c.contributionTicks<20 || c.combatWindowTicks<20 || c.harvestCooldownTicks<20 || !Double.isFinite(c.contributionRange) || c.contributionRange<1 || c.contributionRange>1024 || c.excludedSpawns==null)throw new IllegalArgumentException("Invalid activity guards");
   for(var rules:List.of(c.craftingRecipes,c.engineeringRecipes,c.engineeringObjectives))for(var rule:rules.entrySet())if(net.minecraft.resources.ResourceLocation.tryParse(rule.getKey())==null || rule.getValue()==null || !Double.isFinite(rule.getValue()) || rule.getValue()<=0 || rule.getValue()>1000)throw new IllegalArgumentException("Invalid activity recipe/objective");
   for(String key:defaults().keySet()) { Activity a=c.activities.get(key); if(a==null || a.initialRequirement<1 || a.initialXp<1)throw new IllegalArgumentException("Invalid activity "+key); }
   if(c.skillCategory==null || net.minecraft.resources.ResourceLocation.tryParse(c.skillCategory)==null)throw new IllegalArgumentException("Invalid skill category");
   if(migrate || treeMigration || raw.has("blocksPerMobLevel") || raw.has("dimensionBaseLevels"))Files.writeString(path,gson.toJson(c));current=c; return true;
  } catch(Exception e){ TnkProgression.LOG.error("Keeping previous progression config",e); return false; }
 }
}
