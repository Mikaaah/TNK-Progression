package dev.mikaaah.tnkprogression;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** Optional API binding. Methods are cached; category and player data stay live across reload/respec. */
public final class SkillBridge {
 private static final ResourceLocation LEVEL_SOURCE=ResourceLocation.parse("tnkprogression:character_level");
 private static boolean attempted,warned;
 private static Binding binding;
 private record Binding(Method category,Method spent,Method unlocked,Method skillId,Method points,Method skills,Method open) {}
 private SkillBridge() {}
 private static Binding api(){
  if(!ModList.get().isLoaded("puffish_skills"))return null;
  if(!attempted){
   attempted=true;
   try{
    Class<?> api=Class.forName("net.puffish.skillsmod.api.SkillsAPI"),category=Class.forName("net.puffish.skillsmod.api.Category"),skill=Class.forName("net.puffish.skillsmod.api.Skill");
    binding=new Binding(api.getMethod("getCategory",ResourceLocation.class),category.getMethod("getSpentPoints",ServerPlayer.class),
      category.getMethod("streamUnlockedSkills",ServerPlayer.class),skill.getMethod("getId"),
      category.getMethod("setPoints",ServerPlayer.class,ResourceLocation.class,int.class),category.getMethod("streamSkills"),category.getMethod("openScreen",ServerPlayer.class));
   }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn("Pufferfish API unavailable: check installed version/category; no duplicate points or substitute unlocks",ex);}
  }
  return binding;
 }
 private static void warn(String message,Throwable failure){if(!warned){warned=true;TnkProgression.LOG.warn(message,failure);}}
 private static Optional<?> category(Binding api,String id)throws ReflectiveOperationException{
  // Category wrappers delegate to the current live config/data; do not retain them across reloads.
  return (Optional<?>)api.category.invoke(null,ResourceLocation.parse(id));
 }
 /** Read the count for periodic summaries without enumerating unlocked nodes. */
 public static OptionalInt spentPoints(ServerPlayer p,String id){
  var api=api();if(api==null)return OptionalInt.empty();
  try{var category=category(api,id);return category.isEmpty()?OptionalInt.empty():OptionalInt.of((int)api.spent.invoke(category.get(),p));}
  catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn("Pufferfish spent-point query unavailable",ex);return OptionalInt.empty();}
 }
 /** Open only the requesting player's configured native Pufferfish category. */
 public static boolean open(ServerPlayer p){
  var api=api();if(api==null)return false;
  try{
   var category=category(api,ProgressionConfig.current.skillCategory);if(category.isEmpty())return false;
   if(ProgressionConfig.current.skillCategory.equals("puffish_skills:character"))try(var skills=(Stream<?>)api.skills.invoke(category.get())){
    long count=skills.count();if(count!=588)p.sendSystemMessage(net.minecraft.network.chat.Component.literal("The loaded character tree contains "+count+" nodes; the supplied config has 588. Check config/puffish_skills/categories/character."));
   }
   sync(p);api.open.invoke(category.get(),p);return true;
  }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn("Could not open the configured Pufferfish skill tree",ex);return false;}
 }
 public record TitleSnapshot(int spent,Set<String> unlocked) {}
 public static Optional<TitleSnapshot> titleSnapshot(ServerPlayer p,String id){
  var api=api();if(api==null)return Optional.empty();
  try{
   var category=category(api,id);if(category.isEmpty())return Optional.empty();
   int spent=(int)api.spent.invoke(category.get(),p);var ids=new HashSet<String>();
   try(var stream=(Stream<?>)api.unlocked.invoke(category.get(),p)){
    var iterator=stream.iterator();while(iterator.hasNext())ids.add((String)api.skillId.invoke(iterator.next()));
   }
   return Optional.of(new TitleSnapshot(spent,Set.copyOf(ids)));
  }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn("Pufferfish title conditions unavailable; no level-based substitute",ex);return Optional.empty();}
 }
 public static void sync(ServerPlayer p){
  var api=api();if(api==null)return;
  try{
   var category=category(api,ProgressionConfig.current.skillCategory);if(category.isEmpty())return;
   // Replace only TNK's total entitlement. Actual skill costs remain Pufferfish-owned,
   // so reconnects/refunds and other point sources do not duplicate TNK's contribution.
   api.points.invoke(category.get(),p,LEVEL_SOURCE,ProgressionMath.skillPoints(ProgressionAPI.level(p)));
  }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn("Pufferfish point synchronization unavailable; no command fallback or duplicate awards",ex);}
 }
}
