package dev.mikaaah.tnkprogression.verification;

import com.pla.spiral_abyss.encounter.*;
import com.pla.spiral_abyss.encounter.data.*;
import com.pla.spiral_abyss.encounter.data.MobCategory;
import com.pla.spiral_abyss.config.GearConfig;
import com.pla.spiral_abyss.world.*;
import dev.mikaaah.tnkprogression.*;
import java.util.*;
import java.lang.reflect.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import static dev.mikaaah.tnkprogression.verification.AbyssSmoke.check;

public final class SpiralChecks {
 static Object field(Object obj,String name) throws Exception {var f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
 static void field(Object obj,String name,Object value) throws Exception {var f=obj.getClass().getDeclaredField(name);f.setAccessible(true);f.set(obj,value);}
 static ArenaSession session(MinecraftServer server,ServerLevel level,int slot) throws Exception {
  var parties=AbyssParties.get(server);var id=UUID.randomUUID();var profile=parties.profile(id);profile.slot=slot;
  var session=new ArenaSession(parties,level,profile);
  ((Map<UUID,ArenaSession>)field(parties,"sessions")).put(id,session);return session;
 }
 static UUID context(ArenaSession s,int floor,int wave) throws Exception {
  var run=UUID.randomUUID();field(s.profile().progress,"floor",floor);field(s.controller(),"waveIndex",wave-1);field(s.controller(),"runId",run);return run;
 }
 static Mob spawn(ArenaSession s,ServerPlayer player,int floor,MobCategory category,EntityType<?> type,double party) throws Exception {
  var id=EntityType.getKey(type);var plans=plans(category,id,floor);
  var equipment=new WaveEquipment(MobCatalog.equipment(),GearConfig.load(floor),s.level().random);
  var list=new WaveSpawner().spawn(s.level(),s.bounds().center(),player,List.of(new FloorDefinition.Enemy(id,1,category)),s.controller().runId(),equipment,floor,party,plans);
  return list.getFirst();
 }
 static MobCatalog.ScalingSnapshot plans(MobCategory cat,ResourceLocation id,int floor) {
  return new MobCatalog.ScalingSnapshot(Map.of(cat,Map.of(id,new MobScaling.Plan(Map.of(floor,new MobScaling.Floor(new MobScaling.Health(cat==MobCategory.BOSS,cat==MobCategory.BOSS?10000:10),5))))),Map.of());
 }
 static void near(double actual,double expected,String message){check(Math.abs(actual-expected)<.02,message+": "+actual+" vs "+expected);}
 static float hit(Mob attacker,LivingEntity victim,DamageSource source) {
  var neo=new LivingIncomingDamageEvent(victim,new DamageContainer(source,10));
  // Exercise Spiral's actual server adapter and TNK's listener; multiplicative ordering is irrelevant.
  com.pla.spiral_abyss.neoforge.event.NeoForgeGameplayEvents.AbyssEncounters_scaleDamage(neo);
  AbyssIntegration.damage(neo);return neo.getAmount();
 }
 public static void run(MinecraftServer server,com.google.gson.JsonObject report) throws Exception {
  var level=server.getLevel(AbyssDimension.LEVEL);check(level!=null,"Spiral dimension exists");
  var low=AbyssSmoke.player(server,level,"AbyssLow");var high=AbyssSmoke.player(server,level,"AbyssHigh");
  ProgressionAPI.profile(low).putInt("level",1);ProgressionAPI.profile(high).putInt("level",100);
  var a=session(server,level,0);var b=session(server,level,1);
  var originConfig=ProgressionConfig.current;
  boolean bridge=ModList.get().isLoaded("tnk_abyss_rotations");
  if(bridge)loadBridge();
  Integer oldMinimum=originConfig.bossBaseLevels.put("minecraft:zombie",105);
  int checks=0;
  try {
   for(int floor=1;floor<=6;floor++){
    context(a,floor,1);low.setPos(a.bounds().center());high.setPos(a.bounds().center());
    for(ServerPlayer player:List.of(low,high)){
     // Place only this level's player close to the wave; arena levels must not use nearest player.
     low.setPos(player==low?a.bounds().center():b.bounds().center());high.setPos(player==high?a.bounds().center():b.bounds().center());MobLevels.invalidate(level);
     var mob=spawn(a,player,floor,MobCategory.COMMON_MOB,EntityType.ZOMBIE,1.75);
     int target=bridge?45+floor*5:40+floor*10;
     check(ProgressionAPI.mobLevel(mob)==target,"fixed floor independent of player "+floor);
     double expectedHealth=mob.getAttribute(Attributes.MAX_HEALTH).getBaseValue()*(1+originConfig.healthPerLevel*(target-1))*1.75;
     near(mob.getMaxHealth(),expectedHealth,"TNK health baseline and party once");
     near(mob.getPersistentData().getDouble(WaveSpawner.DAMAGE_TAG),1.75,"floor damage multiplier removed");
     check(mob.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(ResourceLocation.parse("tnkprogression:damage"))==null,"arena attribute has no duplicate outgoing level factor");
     check(!AbyssIntegration.fullBoss(mob),"registry minimum cannot promote regular encounter to boss");
     mob.setHealth(7);var encoded=new net.minecraft.nbt.CompoundTag();mob.save(encoded);
     var restored=(Mob)EntityType.loadEntityRecursive(encoded,level,e->e);check(restored!=null,"entity deserializes");
     // Change both player/config/rotation context: existing assignment must remain untouched.
     int fallback=originConfig.abyss.floorLevels[floor-1];originConfig.abyss.floorLevels[floor-1]=1;
     ProgressionEvents.join(new EntityJoinLevelEvent(restored,level));ProgressionEvents.join(new EntityJoinLevelEvent(restored,level));
     originConfig.abyss.floorLevels[floor-1]=fallback;
     check(ProgressionAPI.mobLevel(restored)==target&&restored.getHealth()==7,"reload preserves level and damage");
     near(restored.getMaxHealth(),expectedHealth,"reload never stacks modifiers");
     check(restored.getAttribute(Attributes.MAX_HEALTH).getModifiers().stream().filter(m->m.id().toString().equals("tnkprogression:health")).count()==1,"one saved TNK modifier");
     check(LevelBadges.mobBadge(target).getString().equals(String.valueOf((char)(0xE000+target))),"badge glyph matches saved combat level");
     // Attribute melee, fixed projectiles, spells and area damage all get party scaling once.
     near(hit(mob,low,level.damageSources().mobAttack(mob)),17.5*(1+originConfig.damagePerLevel*(target-1)),"melee and fixed mob-attack area paths scale once");
     var arrow=EntityType.ARROW.create(level);arrow.setOwner(mob);
     near(hit(mob,low,level.damageSources().arrow(arrow,mob)),17.5*(1+originConfig.damagePerLevel*(target-1)),"projectile damage");
     near(hit(mob,low,new DamageSource(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC),mob)),17.5*(1+originConfig.damagePerLevel*(target-1)),"spell damage");
     var cloud=EntityType.AREA_EFFECT_CLOUD.create(level);cloud.setOwner(mob);
     near(hit(mob,low,new DamageSource(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.INDIRECT_MAGIC),cloud,mob)),17.5*(1+originConfig.damagePerLevel*(target-1)),"area damage");
     mob.discard();checks++;
    }
   }
   for(int[] slot:new int[][]{{5,3},{6,2},{6,3}}){
    context(a,slot[0],slot[1]);low.setPos(a.bounds().center());
    var boss=spawn(a,low,slot[0],MobCategory.BOSS,EntityType.ZOMBIE,1.5);
    int target=bridge?45+slot[0]*5:40+slot[0]*10;
    check(ProgressionAPI.mobLevel(boss)==Math.min(110,Math.max(target+10,105)),"boss base/+10/cap");
    check(AbyssIntegration.fullBoss(boss),"actual BOSS category");
    near(boss.getMaxHealth(),boss.getAttribute(Attributes.MAX_HEALTH).getBaseValue()*(1+.03*(ProgressionAPI.mobLevel(boss)-1))*1.5,"absolute native boss HP is not stacked");boss.discard();
   }
   originConfig.bossBaseLevels.put("minecraft:zombie",110);context(a,6,3);var max=spawn(a,low,6,MobCategory.BOSS,EntityType.ZOMBIE,1);
   check(ProgressionAPI.mobLevel(max)==110,"level 110 combat");
   var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
   var packet=new MobLevelPayload(max.getId(),max.getUUID(),110);MobLevelPayload.CODEC.encode(buffer,packet);check(MobLevelPayload.CODEC.decode(buffer).level()==110,"badge wire level 110");buffer.release();
   context(b,2,1);high.setPos(b.bounds().center());var other=spawn(b,high,2,MobCategory.MINI_BOSS,EntityType.VILLAGER,1);
   check(ProgressionAPI.mobLevel(other)==(bridge?55:60)&&!AbyssIntegration.fullBoss(other),"independent second party and non-Enemy miniboss");
   check(ProgressionAPI.mobLevel(max)==110,"second party cannot rewrite first party");
   // A real native scaling call honors SkipRaisingHealth, including TNK's own health modifier.
   context(a,5,3);var skip=EntityType.ZOMBIE.create(level);skip.setPos(a.bounds().center());skip.getPersistentData().putUUID(WaveSpawner.RUN_TAG,a.controller().runId());skip.getPersistentData().putBoolean(WaveSpawner.BOSS_TAG,true);skip.getPersistentData().putBoolean("SkipRaisingHealth",true);
   WaveScaling.apply(skip,5,MobCategory.BOSS,EntityType.getKey(skip.getType()),2,plans(MobCategory.BOSS,EntityType.getKey(skip.getType()),5));level.addFreshEntity(skip);
   near(skip.getMaxHealth(),skip.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),"SkipRaisingHealth skips TNK and party HP");
   // Adopt an already-present reinforcement through Spiral's real discover method.
   context(a,3,1);var root=spawn(a,low,3,MobCategory.COMMON_MOB,EntityType.ZOMBIE,1.5);
   var stages=(WaveStages<Mob>)field(a.controller(),"stages");stages.prepare(List.of(new FloorDefinition.Enemy(EntityType.getKey(root.getType()),1,MobCategory.COMMON_MOB)));stages.advance(entries->List.of(root));
   var reinforcement=EntityType.HUSK.create(level);reinforcement.setPos(a.bounds().center().add(3,0,0));level.addFreshEntity(reinforcement);reinforcement.setHealth(8);
   var reinforcements=new EncounterReinforcements(a);reinforcements.begin(List.of(root));reinforcements.discover(a.controller().runId(),stages::add);
   check(AbyssIntegration.assigned(reinforcement)&&ProgressionAPI.mobLevel(reinforcement)==ProgressionAPI.mobLevel(root)&&reinforcement.getHealth()<=8,"native reinforcement adoption without healing");
   // Actual damage/death pipeline, not an injected XP command.
   low.setPos(a.bounds().center());ProgressionAPI.profile(low).putInt("level",1);ProgressionAPI.profile(low).putInt("ceiling",100);double before=ProgressionAPI.profile(low).getDouble("combat_xp_earned");
   root.hurt(level.damageSources().playerAttack(low),root.getMaxHealth()*2);
   check(ProgressionAPI.profile(low).getDouble("combat_xp_earned")>before,"verified COMMAND arena kill awards combat XP");
   double after=ProgressionAPI.profile(low).getDouble("combat_xp_earned");CombatProgression.death(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(root,level.damageSources().playerAttack(low)));check(ProgressionAPI.profile(low).getDouble("combat_xp_earned")==after,"duplicate death never pays twice");
   // Native phase handling inherits the root snapshot after its own health transfer.
   stages.clear(m->m.discard());context(a,6,3);var phaseRoot=spawn(a,low,6,MobCategory.BOSS,EntityType.ZOMBIE,1.5);
   stages.prepare(List.of(new FloorDefinition.Enemy(EntityType.getKey(phaseRoot.getType()),1,MobCategory.BOSS)));stages.advance(entries->List.of(phaseRoot));
   var phase=EntityType.HUSK.create(level);phase.setPos(a.bounds().center().add(5,0,0));level.addFreshEntity(phase);phase.setHealth(8);
   var phases=new EncounterReinforcements(a);phases.begin(List.of(phaseRoot));((Map<ResourceLocation,Mob>)field(phases,"phases")).put(EntityType.getKey(phase.getType()),phaseRoot);
   phases.discover(a.controller().runId(),stages::add);
   check(AbyssIntegration.fullBoss(phase)&&ProgressionAPI.mobLevel(phase)==110&&phase.getHealth()<=12.01,"native boss phase inherits level without extra healing");
   var converted=EntityType.HUSK.create(level);converted.setPos(a.bounds().center().add(7,0,0));AbyssIntegration.inherit(phaseRoot,converted);
   check(!converted.getPersistentData().hasUUID(WaveSpawner.RUN_TAG),"inherited child leaves native stage adoption to Spiral");level.addFreshEntity(converted);phases.discover(a.controller().runId(),stages::add);
   check(AbyssIntegration.assigned(converted)&&stages.enemies().contains(converted),"inherited child remains in native stage roster");
   // Both compatibility switches preserve native Spiral stats without TNK counting them again.
   context(b,5,3);originConfig.abyss.tnkHealthBaseline=false;originConfig.abyss.tnkDamageBaseline=false;
   var nativeStats=spawn(b,high,5,MobCategory.BOSS,EntityType.ZOMBIE,1.5);
   near(nativeStats.getMaxHealth(),15000,"native absolute health compatibility option");near(nativeStats.getPersistentData().getDouble(WaveSpawner.DAMAGE_TAG),7.5,"native damage compatibility option");
   check(nativeStats.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(ResourceLocation.parse("tnkprogression:damage"))==null,"no second TNK damage modifier in native mode");
   originConfig.abyss.tnkHealthBaseline=true;originConfig.abyss.tnkDamageBaseline=true;
   // No native run, even inside a real arena: forged bridge fields cannot create fixed levels.
   var forged=EntityType.ZOMBIE.create(level);forged.setPos(a.bounds().center());forged.getPersistentData().putInt("tnk_abyss_floor",6);forged.getPersistentData().putInt("tnk_abyss_target_level",100);level.addFreshEntity(forged);check(!AbyssIntegration.assigned(forged),"unowned in-dimension mob not arena");
   // Endless entry point is untouched and cannot capture a normal-floor context.
   var endless=EntityType.ZOMBIE.create(level);endless.setPos(a.bounds().center());endless.getPersistentData().putUUID(WaveSpawner.RUN_TAG,UUID.randomUUID());WaveScaling.applyEndless(endless,20,MobCategory.COMMON_MOB,2);check(!AbyssIntegration.assigned(endless),"Endless excluded");
   report.addProperty("floor_player_cases",checks);report.addProperty("boss_slots",3);report.addProperty("reload_damage_scaling_reinforcements_XP",true);
  } finally {if(oldMinimum==null)originConfig.bossBaseLevels.remove("minecraft:zombie");else originConfig.bossBaseLevels.put("minecraft:zombie",oldMinimum);}
 }
 static void loadBridge() throws Exception {
  // Parse the supplied rotation with changed targets and vanilla boss candidates for isolated checks.
  var gson=new com.google.gson.Gson();var definition=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of("bridge-fixture/rotation.json"))).getAsJsonObject();
  for(var value:definition.getAsJsonArray("floors")){var f=value.getAsJsonObject();f.addProperty("target_level",45+f.get("floor").getAsInt()*5);}
  Map<String,com.google.gson.JsonObject> themes=new TreeMap<>();
  try(var paths=java.nio.file.Files.list(java.nio.file.Path.of("bridge-fixture/themes"))){for(var path:paths.toList())themes.put(path.getFileName().toString().replace(".json",""),com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(path)).getAsJsonObject());}
  var candidates=com.google.gson.JsonParser.parseString("[\"minecraft:wither\",\"minecraft:ender_dragon\",\"minecraft:elder_guardian\",\"minecraft:warden\"]").getAsJsonArray();
  for(var theme:themes.values())theme.add("bosses",candidates.deepCopy());
  for(var value:definition.getAsJsonArray("floors")){var f=value.getAsJsonObject();if(f.has("bosses"))for(String slot:Set.copyOf(f.getAsJsonObject("bosses").keySet()))f.getAsJsonObject("bosses").add(slot,candidates.deepCopy());}
  var data=Class.forName("dev.mikaaah.tnkabyss.RotationData").getMethod("parse",com.google.gson.JsonObject.class,SortedMap.class).invoke(null,definition,themes);
  Class.forName("dev.mikaaah.tnkabyss.RotationCatalog").getField("current").set(null,data);
 }
}
