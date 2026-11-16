package dev.mikaaah.tnkprogression;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
public final class TeamBridge {
 private static boolean attempted,warned;
 private static Binding binding;
 private record Binding(java.lang.reflect.Method api,java.lang.reflect.Method loaded,java.lang.reflect.Method manager,java.lang.reflect.Method forPlayer,java.lang.reflect.Method id){}
 private static Binding binding(){
  if(!attempted){
   attempted=true;
   try{
    Class<?> api=Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI"),type=Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
    binding=new Binding(api.getMethod("api"),type.getMethod("isManagerLoaded"),type.getMethod("getManager"),
     Class.forName("dev.ftb.mods.ftbteams.api.TeamManager").getMethod("getTeamForPlayerID",UUID.class),Class.forName("dev.ftb.mods.ftbteams.api.Team").getMethod("getId"));
   }catch(ReflectiveOperationException|LinkageError|RuntimeException e){warn(e);}
  }
  return binding;
 }
 private static void warn(Throwable e){if(!warned){warned=true;TnkProgression.LOG.warn("FTB Teams lookup failed; personal progression remains active",e);}}
 public static Optional<String> key(ServerPlayer p) {
  if(!ProgressionConfig.current.shareTeamUnlocks || !ModList.get().isLoaded("ftbteams"))return Optional.empty();
  try {
   var methods=binding();if(methods==null)return Optional.empty();
   Object api=methods.api.invoke(null);
   if(api==null || !(boolean)methods.loaded.invoke(api))return Optional.empty();
   Object manager=methods.manager.invoke(api);
   Optional<?> team=(Optional<?>)methods.forPlayer.invoke(manager,p.getUUID());
   if(team.isEmpty())return Optional.empty();
   // Manager and membership are deliberately queried live: joins, leaves and server restarts change them.
   return Optional.of("team:"+methods.id.invoke(team.get()));
  }catch(ReflectiveOperationException|LinkageError|RuntimeException e){warn(e);return Optional.empty();}
 }
}
