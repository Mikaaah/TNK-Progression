package dev.mikaaah.tnkprogression;
import java.util.*;
import com.google.gson.*;
import java.io.InputStreamReader;
import net.minecraft.server.MinecraftServer;
public final class TreeSmoke {
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static JsonObject read(String name)throws Exception{try(var reader=java.nio.file.Files.newBufferedReader(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("puffish_skills/categories/character/"+name+".json"))){return JsonParser.parseReader(reader).getAsJsonObject();}}
 private static void texture(String id)throws Exception{String[] p=id.split(":",2);try(var in=TreeSmoke.class.getResourceAsStream("/assets/"+p[0]+"/"+p[1])){check(in!=null,"bundled texture "+id);}}
 public static void run(MinecraftServer server)throws Exception{
  var category=read("category");var skills=read("skills");var definitions=read("definitions");var connections=read("connections").getAsJsonObject("normal").getAsJsonArray("bidirectional");
  check(TreeSmoke.class.getResource("/data/tnk/puffish_skills/config.json")==null,"no embedded Pufferfish tree definition");
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
    else if(r.get("type").getAsString().equals("puffish_skills:tag"))check((TreePassives.definitions().containsKey(r.getAsJsonObject("data").get("tag").getAsString().substring(9))||SourceTreeBonuses.hasDefinition(r.getAsJsonObject("data").get("tag").getAsString().substring(9))),"native reward refers to real passive");
   }
  }
  check(roots==6,"all six source roots");texture(category.getAsJsonObject("background").get("texture").getAsString());
  check(attributes.containsAll(Set.of("irons_spellbooks:max_mana","irons_spellbooks:mana_regen","irons_spellbooks:spell_power","irons_spellbooks:cast_time_reduction","irons_spellbooks:cooldown_reduction")),"actual supported magic attribute IDs");
  for(var title:TitleConfig.current.titles())if(ProgressionConfig.current.skillCategory.equals(title.skillCategory()))for(String skill:title.requiredSkills())check(skills.has(skill),"title criteria reference converted skills");
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
   var categoryApi=((Optional<?>)api.getMethod("getCategory",net.minecraft.resources.ResourceLocation.class).invoke(null,net.minecraft.resources.ResourceLocation.parse("puffish_skills:character"))).orElseThrow();
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
   check(CharacterState.action(p,"skills",""),"character-screen action opens the actual configured Pufferfish category");
   check(!CharacterState.action(p,"skills","different:tree"),"open action cannot choose another category");
   TnkProgression.LOG.info("TNK_SKILL_TREE_OPEN_SMOKE_OK: character GUI action calls the native Pufferfish openScreen API");
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
   active.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.item.ItemStack.EMPTY);
   float baseHealth=active.getMaxHealth();
   Object life=((Optional<?>)type.getMethod("getSkill",String.class).invoke(categoryApi,"enchanter_life_1")).orElseThrow();
   skillType.getMethod("unlock",net.minecraft.server.level.ServerPlayer.class).invoke(life,active);SourceTreeBonuses.refresh(active);
   check(active.getMaxHealth()==baseHealth,"original enchanted-equipment condition does not become unconditional health");
   var helmet=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET);
   helmet.enchant(server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING),1);
   active.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,helmet);SourceTreeBonuses.refresh(active);SourceTreeBonuses.refresh(active);
   check(active.getMaxHealth()==baseHealth+2,"source +2 Health value applies exactly once while enchanted equipment is worn");
   active.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.item.ItemStack.EMPTY);SourceTreeBonuses.refresh(active);
   check(active.getMaxHealth()==baseHealth,"conditional source Health is removed when its condition ends");
   active.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,helmet);SourceTreeBonuses.refresh(active);type.getMethod("resetSkills",net.minecraft.server.level.ServerPlayer.class).invoke(categoryApi,active);SourceTreeBonuses.refresh(active);
   check(active.getMaxHealth()==baseHealth,"respec removes source attribute modifiers without granting health");
   Object poison=((Optional<?>)type.getMethod("getSkill",String.class).invoke(categoryApi,"alchemist_offensive_1")).orElseThrow();skillType.getMethod("unlock",net.minecraft.server.level.ServerPlayer.class).invoke(poison,active);
   var victim=net.minecraft.world.entity.EntityType.CREEPER.create(server.overworld());
   victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);victim.setHealth(20);victim.invulnerableTime=0;
   var source=active.damageSources().playerAttack(active);
   // Assert TNK's exact contribution at its real handler boundary. Other installed mods may
   // legitimately alter the final hurt amount; neither their modifiers nor their listeners are disabled.
   var cleanHit=new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(victim,new net.neoforged.neoforge.common.damagesource.DamageContainer(source,4));
   SourceTreeBonuses.damage(cleanHit);check(Math.abs(cleanHit.getNewDamage()-4)<.0001,"source poisoned-target bonus is inactive on a clean target");
   victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100));
   var poisonedHit=new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(victim,new net.neoforged.neoforge.common.damagesource.DamageContainer(source,4));
   SourceTreeBonuses.damage(poisonedHit);check(Math.abs(poisonedHit.getNewDamage()-4.2)<.0001,"source +5% damage applies to an actually poisoned victim");
   victim.hurt(source,4);check(Float.isFinite(victim.getHealth()),"complete native damage pipeline remains safe alongside all installed listeners");
   type.getMethod("resetSkills",net.minecraft.server.level.ServerPlayer.class).invoke(categoryApi,active);SourceTreeBonuses.refresh(active);
   check(TitleConfig.current.titles().stream().noneMatch(title->TitleCatalogRules.retiredBuiltin(title.id())),"retired mastery/subclass badges never return to automatic catalogue");
   check(TitleConfig.current.titles().stream().filter(TitleConfig.Definition::automatic).count()==10,"only ten authored purchase-based skill titles unlock automatically");
   check(TitleConfig.legacy().size()==16,"all retired definitions remain available to saved owners");
   check(SkillBridge.spentPoints(active,"puffish_skills:character").orElseThrow()==0,"cheap spent-point query reflects full native respec");
   Object alchemist=((Optional<?>)type.getMethod("getSkill",String.class).invoke(categoryApi,"alchemist_class")).orElseThrow();
   skillType.getMethod("unlock",net.minecraft.server.level.ServerPlayer.class).invoke(alchemist,active);
   check(SkillBridge.spentPoints(active,"puffish_skills:character").orElseThrow()==1,"cheap spent-point query observes purchase without cached player state");
   Origins.adminSet(active,"tnk:apothecary");active.removeAllEffects();
   var potion=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POTION);
   potion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(Optional.empty(),Optional.empty(),List.of(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,1000))));
   var originalPotion=potion.copy();var bottle=potion.finishUsingItem(server.overworld(),active);
   net.neoforged.neoforge.event.EventHooks.onItemUseFinish(active,originalPotion,0,bottle);
   check(active.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED).getDuration()==1610,"origin and purchased potion skill extend the original drink once, including Finish event");
   check(!OriginHooks.drinking(active),"potion context clears after drink");
   active.removeAllEffects();active.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,1000));
   check(active.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED).getDuration()==1000,"drink bonuses do not affect splash or unrelated effect applications");
   active.removeAllEffects();type.getMethod("resetSkills",net.minecraft.server.level.ServerPlayer.class).invoke(categoryApi,active);Origins.adminReset(active);
   TnkProgression.LOG.info("TNK_POTION_SKILL_ORIGIN_LIVE_OK: native skill purchase, 40% skill plus15% origin, one original effect application, no Finish reapply or scope leak");
   TnkProgression.LOG.info("TNK_SOURCE_VALUES_LIVE_OK: +2 conditional Health, no stacking, condition removal, respec and +5% poisoned-target damage; active title requirements validated; retired subclass badges excluded");
   TnkProgression.LOG.info("TNK_SECOND_WIND_LIVE_OK: real damage, recovery and saved cooldown through respec");
   TnkProgression.LOG.info("TNK_PUFFERFISH_LIVE_POINTS_RESPEC_OK: actual loaded mod, 175 points, no duplicates, attributes/tags and full respec verified");
  }
  TnkProgression.LOG.info("TNK_TREE_588_SMOKE_OK: config-folder tree, 588 nodes, 669 links, 6 roots, own textures, 175 points; no embedded tree");
 }
}
