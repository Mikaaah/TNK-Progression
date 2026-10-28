package dev.mikaaah.tnkprogression;

import com.google.gson.*;
import java.util.*;
import java.io.InputStreamReader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.fml.ModList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Reversible Pufferfish tag rewards drive these server-owned effects; no client grants or item affix writes. */
public final class TreePassives {
 private record Cached(Set<String> tags,Map<String,Double> values){}
 private static final Map<ServerPlayer,Cached> CACHE=new WeakHashMap<>();
 private static final Map<String,Map<String,Double>> DEFINITIONS=load();
 private static Map<String,Map<String,Double>> load(){
  var path=net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("puffish_skills/tnk_passives.json");
  if(!java.nio.file.Files.exists(path))return Map.of();
  try(var reader=java.nio.file.Files.newBufferedReader(path)){
   var root=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("skills");
   Map<String,Map<String,Double>> result=new HashMap<>();
   for(var skill:root.entrySet()){Map<String,Double> values=new HashMap<>();for(var value:skill.getValue().getAsJsonObject().entrySet())values.put(value.getKey(),value.getValue().getAsDouble());result.put(skill.getKey(),Map.copyOf(values));}
   return Map.copyOf(result);
  }catch(Exception ex){TnkProgression.LOG.error("Invalid Pufferfish passive configuration; conditional effects disabled",ex);return Map.of();}
 }
 static Map<String,Map<String,Double>> definitions(){return DEFINITIONS;}
 public static Map<String,Double> stats(ServerPlayer p){
  if(!ModList.get().isLoaded("puffish_skills") || !ActivityGuard.player(p))return Map.of();
  var tags=p.getTags();var previous=CACHE.get(p);
  if(previous!=null && previous.tags.equals(tags))return previous.values;
  var values=TreePassiveRules.aggregate(tags,DEFINITIONS);CACHE.put(p,new Cached(Set.copyOf(tags),values));return values;
 }
 @SubscribeEvent public static void before(LivingDamageEvent.Pre event){
  if(event.getEntity().level().isClientSide)return;
  var source=event.getSource();
  if(source.getEntity() instanceof ServerPlayer attacker && event.getEntity() instanceof Enemy){
   var stats=stats(attacker);boolean beneficial=stats.getOrDefault("effect_damage",0.0)>0 && attacker.getActiveEffects().stream().anyMatch(e->e.getEffect().value().isBeneficial());
   double multiplier=TreePassiveRules.damageMultiplier(stats,event.getEntity().isOnFire(),attacker.getFoodData().getFoodLevel()>=18,beneficial,source.is(DamageTypeTags.IS_PROJECTILE),attacker.distanceTo(event.getEntity()));
   event.setNewDamage((float)(event.getNewDamage()*multiplier));
  }
  if(event.getEntity() instanceof ServerPlayer defender && source.getEntity() instanceof Enemy && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)){
   var stats=stats(defender);
   if(!source.is(DamageTypeTags.BYPASSES_ARMOR) && stats.getOrDefault("evasion",0.0)>0 && defender.getRandom().nextDouble()<stats.get("evasion"))event.setNewDamage(0);
   else if(!source.is(DamageTypeTags.BYPASSES_ARMOR) && stats.getOrDefault("guard",0.0)>0 && defender.isUsingItem() && defender.getUseItem().getItem() instanceof ShieldItem && defender.getRandom().nextDouble()<stats.get("guard"))event.setNewDamage(event.getNewDamage()*.5f);
  }
 }
 @SubscribeEvent public static void after(LivingDamageEvent.Post event){
  if(event.getEntity().level().isClientSide || event.getNewDamage()<=0)return;
  long now=((net.minecraft.server.level.ServerLevel)event.getEntity().level()).getServer().overworld().getGameTime();
  if(event.getSource().getEntity() instanceof ServerPlayer attacker && event.getEntity() instanceof Enemy){
   var stats=stats(attacker);var profile=ProgressionAPI.profile(attacker);
   if(stats.getOrDefault("leech",0.0)>0 && ActivityRules.elapsed(now,profile.contains("tree_leech_last")?profile.getLong("tree_leech_last"):-1,20)){
    attacker.heal((float)(double)stats.get("leech"));profile.putLong("tree_leech_last",now);ProgressionData.get(attacker.server).setDirty();
   }
   if(stats.getOrDefault("ignite",0.0)>0 && event.getSource().getDirectEntity()==attacker && attacker.getRandom().nextDouble()<stats.get("ignite"))event.getEntity().igniteForSeconds(3);
  }
  if(event.getEntity() instanceof ServerPlayer p && p.isAlive() && p.getHealth()>0 && p.getHealth()<p.getMaxHealth()*.25 && stats(p).getOrDefault("second_wind",0.0)>0){
   var profile=ProgressionAPI.profile(p);long last=profile.contains("tree_second_wind_last")?profile.getLong("tree_second_wind_last"):-1;
   if(ActivityRules.elapsed(now,last,3600)){
    profile.putLong("tree_second_wind_last",now);ProgressionData.get(p.server).setDirty();p.heal(p.getMaxHealth()*.10f);
    p.displayClientMessage(net.minecraft.network.chat.Component.literal("Second Wind"),true);
   }
  }
 }
 @SubscribeEvent public static void consumed(LivingEntityUseItemEvent.Finish event){
  if(!(event.getEntity() instanceof ServerPlayer p))return;
  var stats=stats(p);if(stats.isEmpty())return;
  var food=event.getItem().get(DataComponents.FOOD);
  if(food!=null && food.nutrition()>0)p.heal((float)(double)stats.getOrDefault("food_recovery",0.0));
  if(event.getItem().getItem() instanceof PotionItem && stats.getOrDefault("potion_duration",0.0)>0){
   var contents=event.getItem().get(DataComponents.POTION_CONTENTS);
   if(contents!=null)for(var effect:contents.getAllEffects())if(effect.getEffect().value().isBeneficial() && !effect.getEffect().value().isInstantenous() && effect.getDuration()>0){
    p.addEffect(new MobEffectInstance(effect.getEffect(),TreePassiveRules.potionDuration(effect.getDuration(),stats.get("potion_duration")),effect.getAmplifier(),effect.isAmbient(),effect.isVisible(),effect.showIcon()));
   }
  }
 }
 // Native tag rewards remove effects immediately on respec. Also reconcile tags left by a removed/replaced category.
 @SubscribeEvent public static void reconcile(PlayerTickEvent.Post event){
  if(!(event.getEntity() instanceof ServerPlayer p) || p.tickCount%100!=0 || p.getTags().stream().noneMatch(t->t.startsWith("tnk_tree.")))return;
  SkillBridge.titleSnapshot(p,ProgressionConfig.current.skillCategory).ifPresent(snapshot->{
   for(String tag:Set.copyOf(p.getTags()))if(tag.startsWith("tnk_tree.") && !snapshot.unlocked().contains(tag.substring(9)))p.removeTag(tag);
  });
 }
}
