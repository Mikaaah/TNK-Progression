package dev.mikaaah.tnkprogression;
import java.util.*;
import com.google.gson.*;
import java.io.InputStreamReader;
import net.minecraft.server.MinecraftServer;
public final class TreeSmoke {
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static JsonObject read(String name)throws Exception{try(var in=TreeSmoke.class.getResourceAsStream("/data/tnk/puffish_skills/categories/character/"+name+".json")){check(in!=null,"bundled "+name);return JsonParser.parseReader(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 private static void texture(String id)throws Exception{String[] p=id.split(":",2);try(var in=TreeSmoke.class.getResourceAsStream("/assets/"+p[0]+"/"+p[1])){check(in!=null,"bundled texture "+id);}}
 public static void run(MinecraftServer server)throws Exception{
  var category=read("category");var skills=read("skills");var definitions=read("definitions");var connections=read("connections").getAsJsonObject("normal").getAsJsonArray("bidirectional");
  check(skills.size()==588 && definitions.size()==588 && connections.size()==669,"complete converted graph");
  check(category.get("spent_points_limit").getAsInt()==175 && category.get("starting_points").getAsInt()==0,"no extra starting-point source");
  int roots=0;Set<String> attributes=new HashSet<>();
  for(var entry:skills.entrySet()){
   if(entry.getValue().getAsJsonObject().get("root").getAsBoolean())roots++;
   var d=definitions.getAsJsonObject(entry.getKey());check(d.get("cost").getAsInt()==1,"one point per skill");
   texture(d.getAsJsonObject("icon").getAsJsonObject("data").get("texture").getAsString());
   for(var tex:d.getAsJsonObject("frame").getAsJsonObject("data").entrySet())texture(tex.getValue().getAsString());
   for(var reward:d.getAsJsonArray("rewards")){
    var r=reward.getAsJsonObject();if(r.get("type").getAsString().equals("puffish_skills:attribute"))attributes.add(r.getAsJsonObject("data").get("attribute").getAsString());
    else if(r.get("type").getAsString().equals("puffish_skills:tag"))check(TreePassives.definitions().containsKey(r.getAsJsonObject("data").get("tag").getAsString().substring(9)),"native reward refers to real passive");
   }
  }
  check(roots==6,"all six source roots");texture(category.getAsJsonObject("background").get("texture").getAsString());
  check(attributes.containsAll(Set.of("irons_spellbooks:max_mana","irons_spellbooks:mana_regen","irons_spellbooks:spell_power","irons_spellbooks:cast_time_reduction","irons_spellbooks:cooldown_reduction","ars_nouveau:ars_nouveau.perk.max_mana")),"actual supported magic attribute IDs");
  for(var title:TitleConfig.current.titles())if("tnk:character".equals(title.skillCategory()))for(String skill:title.requiredSkills())check(skills.has(skill),"title criteria reference converted skills");
  var effects=TreePassiveRules.aggregate(Set.of("tnk_tree.blacksmith_defensive_keystone_1"),TreePassives.definitions());check(effects.get("second_wind")==1,"Second Wind definition loaded");
  check(TreePassiveRules.aggregate(Set.of(),TreePassives.definitions()).isEmpty(),"respec tags remove every runtime passive");
  String checker=System.getProperty("tnkprogression.pufferfishCheckJar");
  if(checker!=null){
   var urls=new java.net.URL[]{java.nio.file.Path.of(checker).toUri().toURL(),java.nio.file.Path.of(System.getProperty("tnkprogression.pufferfishCheckClasses")).toUri().toURL()};
   try(var loader=new java.net.URLClassLoader(urls,TreeSmoke.class.getClassLoader())){
    Class.forName("PufferfishTreeFormatCheck",true,loader).getMethod("run",MinecraftServer.class,String.class).invoke(null,server,System.getProperty("tnkprogression.pufferfishCheckData"));
   }
  }
  if(net.neoforged.fml.ModList.get().isLoaded("puffish_skills")){
   Class<?> api=Class.forName("net.puffish.skillsmod.api.SkillsAPI"),type=Class.forName("net.puffish.skillsmod.api.Category"),skillType=Class.forName("net.puffish.skillsmod.api.Skill");
   var categoryApi=((Optional<?>)api.getMethod("getCategory",net.minecraft.resources.ResourceLocation.class).invoke(null,net.minecraft.resources.ResourceLocation.parse("tnk:character"))).orElseThrow();
   var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(server.overworld(),new com.mojang.authlib.GameProfile(UUID.fromString("85cb6e51-0884-4bb0-92ba-8777249f6c40"),"TreeLiveCheck"));
   type.getMethod("resetSkills",net.minecraft.server.level.ServerPlayer.class).invoke(categoryApi,p);
   ProgressionAPI.profile(p).putInt("level",100);ProgressionAPI.profile(p).putInt("ceiling",100);
   SkillBridge.sync(p);SkillBridge.sync(p);
   check((int)type.getMethod("getPointsTotal",net.minecraft.server.level.ServerPlayer.class).invoke(categoryApi,p)==175,"live Pufferfish grants exactly 175 points without duplicates");
   float health=p.getMaxHealth();
   for(String id:List.of("enchanter_class","alchemist_class")){
    Object skill=((Optional<?>)type.getMethod("getSkill",String.class).invoke(categoryApi,id)).orElseThrow();skillType.getMethod("unlock",net.minecraft.server.level.ServerPlayer.class).invoke(skill,p);
   }
   check(p.getMaxHealth()>health && p.getTags().contains("tnk_tree.alchemist_class"),"live native attribute and passive reward application");
   type.getMethod("resetSkills",net.minecraft.server.level.ServerPlayer.class).invoke(categoryApi,p);
   check(p.getMaxHealth()==health && !p.getTags().contains("tnk_tree.alchemist_class"),"live respec removes attributes and runtime markers");
   check((int)type.getMethod("getPointsLeft",net.minecraft.server.level.ServerPlayer.class).invoke(categoryApi,p)==175,"respec retains earned 175-point budget");
   var profile=new com.mojang.authlib.GameProfile(UUID.fromString("2bc4264d-dbb6-40fc-9b09-364ae30f71e2"),"SecondWindLive");
   var active=new net.minecraft.server.level.ServerPlayer(server,server.overworld(),profile,net.minecraft.server.level.ClientInformation.createDefault());
   active.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),active,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
    @Override public void send(net.minecraft.network.protocol.Packet<?> packet){} // Disposable server fixture has no client socket.
   };
   active.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);active.setPos(0,64,0);
   ProgressionAPI.profile(active).putInt("level",100);ProgressionAPI.profile(active).putInt("ceiling",100);ProgressionAPI.profile(active).remove("tree_second_wind_last");SkillBridge.sync(active);
   type.getMethod("resetSkills",net.minecraft.server.level.ServerPlayer.class).invoke(categoryApi,active);
   Object wind=((Optional<?>)type.getMethod("getSkill",String.class).invoke(categoryApi,"blacksmith_defensive_keystone_1")).orElseThrow();
   skillType.getMethod("unlock",net.minecraft.server.level.ServerPlayer.class).invoke(wind,active);
   check(active.getTags().contains("tnk_tree.blacksmith_defensive_keystone_1"),"live Second Wind ownership tag");
   var spawnImmunity=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");spawnImmunity.setAccessible(true);spawnImmunity.setInt(active,0);
   var enemy=net.minecraft.world.entity.EntityType.ZOMBIE.create(server.overworld());enemy.setPos(1,64,0);
   active.setHealth(4);active.invulnerableTime=0;active.hurt(active.damageSources().mobAttack(enemy),1);
   check(active.getHealth()>4 && ProgressionAPI.profile(active).contains("tree_second_wind_last"),"Second Wind responds to actual surviving damage and saves cooldown");
   type.getMethod("resetSkills",net.minecraft.server.level.ServerPlayer.class).invoke(categoryApi,active);skillType.getMethod("unlock",net.minecraft.server.level.ServerPlayer.class).invoke(wind,active);
   active.setHealth(4);active.invulnerableTime=0;active.hurt(active.damageSources().mobAttack(enemy),1);
   check(active.getHealth()<4,"respec and re-unlock cannot reset Second Wind cooldown");
   type.getMethod("resetSkills",net.minecraft.server.level.ServerPlayer.class).invoke(categoryApi,active);
   TnkProgression.LOG.info("TNK_SECOND_WIND_LIVE_OK: real damage, recovery and saved cooldown through respec");
   TnkProgression.LOG.info("TNK_PUFFERFISH_LIVE_POINTS_RESPEC_OK: actual loaded mod, 175 points, no duplicates, attributes/tags and full respec verified");
  }
  TnkProgression.LOG.info("TNK_TREE_588_SMOKE_OK: 588 nodes, 669 links, 6 roots, own textures, 175 points, native tag passives and migrated title IDs");
 }
}
