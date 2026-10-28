package dev.mikaaah.tnkprogression;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import java.util.Optional;
public final class SkillBridge {
 private static boolean warned;
 /** Open only the requesting player's configured native Pufferfish category. */
 public static boolean open(ServerPlayer p){
  if(!ModList.get().isLoaded("puffish_skills"))return false;
  try{
   Class<?> api=Class.forName("net.puffish.skillsmod.api.SkillsAPI"),type=Class.forName("net.puffish.skillsmod.api.Category");
   Optional<?> category=(Optional<?>)api.getMethod("getCategory",ResourceLocation.class).invoke(null,ResourceLocation.parse(ProgressionConfig.current.skillCategory));
   if(category.isEmpty())return false;
   if(ProgressionConfig.current.skillCategory.equals("puffish_skills:character"))try(var skills=(java.util.stream.Stream<?>)type.getMethod("streamSkills").invoke(category.get())){
    long count=skills.count();if(count!=588)p.sendSystemMessage(net.minecraft.network.chat.Component.literal("The loaded character tree contains "+count+" nodes; the supplied config has 588. Check config/puffish_skills/categories/character."));
   }
   sync(p);type.getMethod("openScreen",ServerPlayer.class).invoke(category.get(),p);return true;
  }catch(ReflectiveOperationException|RuntimeException e){TnkProgression.LOG.warn("Could not open the configured Pufferfish skill tree",e);return false;}
 }
 public record TitleSnapshot(int spent,java.util.Set<String> unlocked) {}
 public static Optional<TitleSnapshot> titleSnapshot(ServerPlayer p,String id){
  if(!ModList.get().isLoaded("puffish_skills"))return Optional.empty();
  try{
   Class<?> api=Class.forName("net.puffish.skillsmod.api.SkillsAPI"),type=Class.forName("net.puffish.skillsmod.api.Category"),skill=Class.forName("net.puffish.skillsmod.api.Skill");
   Optional<?> category=(Optional<?>)api.getMethod("getCategory",ResourceLocation.class).invoke(null,ResourceLocation.parse(id));
   if(category.isEmpty())return Optional.empty();
   int spent=(int)type.getMethod("getSpentPoints",ServerPlayer.class).invoke(category.get(),p);
   var ids=new java.util.HashSet<String>();
   try(var stream=(java.util.stream.Stream<?>)type.getMethod("streamUnlockedSkills",ServerPlayer.class).invoke(category.get(),p)){
    for(var node:stream.toList())ids.add((String)skill.getMethod("getId").invoke(node));
   }
   return Optional.of(new TitleSnapshot(spent,java.util.Set.copyOf(ids)));
  }catch(ReflectiveOperationException|RuntimeException e){if(!warned){warned=true;TnkProgression.LOG.warn("Pufferfish title conditions unavailable; no level-based substitute",e);}return Optional.empty();}
 }

 public static void sync(ServerPlayer p){
  if(!ModList.get().isLoaded("puffish_skills"))return;
  try{
   Class<?> api=Class.forName("net.puffish.skillsmod.api.SkillsAPI");
   Optional<?> category=(Optional<?>)api.getMethod("getCategory",ResourceLocation.class).invoke(null,ResourceLocation.parse(ProgressionConfig.current.skillCategory));
   if(category.isEmpty())return;
   Class<?> type=Class.forName("net.puffish.skillsmod.api.Category");
   type.getMethod("setPoints",ServerPlayer.class,ResourceLocation.class,int.class).invoke(category.get(),p,ResourceLocation.parse("tnkprogression:character_level"),ProgressionMath.skillPoints(ProgressionAPI.level(p)));
  }catch(ReflectiveOperationException|RuntimeException e){if(!warned){warned=true;TnkProgression.LOG.warn("Pufferfish API unavailable: check installed version/category; no command fallback or duplicate awards",e);}}
 }
}
