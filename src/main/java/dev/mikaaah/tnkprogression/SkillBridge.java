package dev.mikaaah.tnkprogression;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import java.util.Optional;
public final class SkillBridge {
 private static boolean warned;
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
   type.getMethod("setPoints",ServerPlayer.class,ResourceLocation.class,int.class).invoke(category.get(),p,ResourceLocation.parse("tnkprogression:character_level"),ProgressionAPI.level(p));
  }catch(ReflectiveOperationException|RuntimeException e){if(!warned){warned=true;TnkProgression.LOG.warn("Pufferfish API unavailable: check installed version/category; no command fallback or duplicate awards",e);}}
 }
}
