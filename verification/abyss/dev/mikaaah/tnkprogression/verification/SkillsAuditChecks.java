package dev.mikaaah.tnkprogression.verification;

import com.google.gson.JsonObject;
import dev.mikaaah.tnkprogression.*;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import static dev.mikaaah.tnkprogression.verification.AbyssSmoke.check;

/** Disposable dedicated-server assertions. Included only in the opt-in verification source set. */
public final class SkillsAuditChecks {
 private SkillsAuditChecks() {}
 private static Map<ResourceLocation,Double> attributes(ServerPlayer player){
  Map<ResourceLocation,Double> values=new HashMap<>();
  for(var holder:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.holders().toList()){
   var attribute=player.getAttribute(holder);if(attribute==null)continue;
   values.put(holder.key().location(),attribute.getValue());
   var originIds=attribute.getModifiers().stream().filter(m->m.id().getNamespace().equals("tnkprogression")&&m.id().getPath().startsWith("origin/")).map(AttributeModifier::id).toList();
   check(originIds.size()==new HashSet<>(originIds).size(),"origin modifier IDs never duplicate");
  }
  return values;
 }
 private static ServerPlayer reconnect(ServerPlayer original){
  var server=original.server;
  var player=new ServerPlayer(server,original.serverLevel(),original.getGameProfile(),ClientInformation.createDefault());
  player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player,CommonListenerCookie.createInitial(player.getGameProfile(),false)){
   @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
   @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
  };
  player.restoreFrom(original,false); // Real transformed clone boundary.
  for(String tag:original.getTags())player.addTag(tag); // The verified PlayerList.respawn ordering.
  return player;
 }
 private static void sigil(ServerPlayer player){
  player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AscensionItem.SIGIL.get()));
  var result=AscensionItem.SIGIL.get().use(player.serverLevel(),player,InteractionHand.MAIN_HAND);
  check(result.getResult().consumesAction()&&player.getMainHandItem().isEmpty(),"actual sigil consumes exactly one item");
 }
 private static void removePlayer(ServerPlayer player) throws Exception {
  player.serverLevel().players().remove(player);
  var list=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");list.setAccessible(true);((List<?>)list.get(player.server.getPlayerList())).remove(player);
  var ids=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");ids.setAccessible(true);((Map<?,?>)ids.get(player.server.getPlayerList())).remove(player.getUUID());
 }
 private static void ascension(MinecraftServer server,boolean skills,JsonObject report) throws Exception {
  var player=AbyssSmoke.player(server,server.overworld(),"SkillsAscensionAudit");
  try{
   var n=ProgressionAPI.profile(player);n.putInt("level",55);n.putInt("ceiling",100);n.putLong("xp",321);
   check(Titles.selectPassive(player,""),"initial explicit passive clear succeeds");
   check(n.contains("title_passive")&&n.getString("title_passive").isEmpty(),"initial empty passive choice is saved");
   check(Titles.grant(player,"tnk:prepared")&&Titles.passive(player).isEmpty(),"first title does not override explicit empty passive");
   check(Titles.grant(player,"tnk:owner")&&Titles.select(player,"tnk:owner")&&Titles.selectPassive(player,"tnk:prepared"),"independent staff cosmetic and ordinary passive");
   long titleLock=n.getLong("title_swap_after");var healthModifier=player.getAttribute(Attributes.MAX_HEALTH).getModifier(ResourceLocation.parse("tnkprogression:title/minecraft/generic.max_health"));
   check(healthModifier!=null,"selected health passive has one stable modifier");
   check(Titles.select(player,"tnk:owner")&&Titles.selectPassive(player,"tnk:prepared")&&n.getLong("title_swap_after")==titleLock,"repeated selection neither resets cooldown nor changes ownership");
   TitlePerks.apply(player);check(player.getAttribute(Attributes.MAX_HEALTH).getModifier(healthModifier.id())==healthModifier,"unchanged title refresh keeps its modifier instance");
   Origins.open(player);
   for(var origin:OriginRules.ascended())check(!Origins.available(player,origin.id())&&!Origins.choose(player,origin.id()),"ascended choice locked before sigil: "+origin.id());
   check(Origins.choose(player,"tnk:deepdelver"),"normal origin available before ascension");
   var earned=n.getCompound("titles_unlocked").copy();Object category=null;Class<?> categoryType=null;int spent=0;
   if(skills){
    SkillBridge.sync(player);categoryType=Class.forName("net.puffish.skillsmod.api.Category");
    category=((Optional<?>)Class.forName("net.puffish.skillsmod.api.SkillsAPI").getMethod("getCategory",ResourceLocation.class).invoke(null,ResourceLocation.parse(ProgressionConfig.current.skillCategory))).orElseThrow();
    var skill=((Optional<?>)categoryType.getMethod("getSkill",String.class).invoke(category,"alchemist_class")).orElseThrow();
    Class.forName("net.puffish.skillsmod.api.Skill").getMethod("unlock",ServerPlayer.class).invoke(skill,player);
    spent=SkillBridge.spentPoints(player,ProgressionConfig.current.skillCategory).orElseThrow();check(spent==1,"native pre-ascension skill is purchased");
   }
   var tags=Set.copyOf(player.getTags());int request=0,available=0;
   check(OriginRules.ascended().size()==15,"all fifteen authored ascended origins exist");
   for(var origin:OriginRules.ascended()){
    sigil(player);request++;
    check(Origins.ascendedUnlocked(player)&&Origins.selected(player).isEmpty()&&n.getLong("origin_selection_request")==request,"sigil unlocks and requests one new selection");
    check(n.getInt("level")==55&&n.getLong("xp")==321&&n.getCompound("titles_unlocked").equals(earned)&&player.getTags().equals(tags),"sigil preserves level, XP, titles and native skill ownership");
    if(Origins.available(player,origin.id())){check(Origins.choose(player,origin.id()),"available ascended origin can be chosen: "+origin.id());available++;}
    else{
     check(!Origins.choose(player,origin.id()),"missing optional mana attributes cannot consume selection: "+origin.id());
     // Authorized force-assignment remains supported even without optional attributes.
     check(server.getCommands().getDispatcher().execute("tnkprogress origin set @s "+origin.id(),player.createCommandSourceStack().withPermission(2))==1,"admin may assign any known ascended origin safely");
    }
    check(!Origins.choose(player,origin.id()),"chosen ascended origin cannot be replayed");
    player.setHealth(5);var before=attributes(player);Origins.apply(player);Origins.apply(player);
    Origins.respawn(new PlayerEvent.PlayerRespawnEvent(player,false));Origins.dimension(new PlayerEvent.PlayerChangedDimensionEvent(player,net.minecraft.world.level.Level.NETHER,net.minecraft.world.level.Level.OVERWORLD));
    check(before.equals(attributes(player))&&player.getHealth()==5,"ascended lifecycle refresh never stacks or heals: "+origin.id());
    check(Titles.selected(player).equals("tnk:owner")&&Titles.passive(player).equals("tnk:prepared")&&n.getLong("title_swap_after")==titleLock,"ascension preserves both title slots and cooldown");
    if(skills)check(SkillBridge.spentPoints(player,ProgressionConfig.current.skillCategory).orElseThrow()==spent,"ascension does not refund or duplicate purchased skill points");
   }
   var saved=ProgressionData.get(server).save(new CompoundTag(),player.registryAccess());
   var loaded=ProgressionData.load(saved,player.registryAccess()).entry("player:"+player.getUUID());
   check(loaded.getString(Origins.KEY).equals(Origins.selected(player))&&loaded.getInt("origin_revision")==2&&loaded.getBoolean("ascended_origins_unlocked")&&loaded.getLong("origin_selection_request")==15,"selected ascension and reselection count survive SavedData roundtrip");
   check(loaded.getInt("level")==55&&loaded.getLong("xp")==321&&loaded.getCompound("titles_unlocked").equals(earned)&&loaded.getString("title_selected").equals("tnk:owner")&&loaded.getString("title_passive").equals("tnk:prepared"),"save retains progression, ownership and independent selections");
   var replacement=reconnect(player);Origins.respawn(new PlayerEvent.PlayerRespawnEvent(replacement,false));TitlePerks.respawn(new PlayerEvent.PlayerRespawnEvent(replacement,false));
   check(Origins.selected(replacement).equals(Origins.selected(player))&&Origins.ascendedUnlocked(replacement)&&Titles.selected(replacement).equals("tnk:owner")&&Titles.passive(replacement).equals("tnk:prepared"),"replacement player entity sees same UUID-owned origin, ascension and title selections");
   check(attributes(replacement).equals(attributes(player))&&replacement.getTags().equals(player.getTags()),"clone/respawn reapplies the same origin/passive attributes once and retains native tags");
   if(skills)check(SkillBridge.spentPoints(replacement,ProgressionConfig.current.skillCategory).orElseThrow()==spent,"replacement UUID retains native purchased point costs");
   report.addProperty("ascended_origins_checked",15);report.addProperty("ascended_origins_selectable_with_present_attributes",available);report.addProperty("sigil_persistence_and_idempotence",true);
   if(skills)categoryType.getMethod("resetSkills",ServerPlayer.class).invoke(category,player);
  }finally{removePlayer(player);}
 }
 public static void run(MinecraftServer server,JsonObject report) throws Exception {
  boolean skills=ModList.get().isLoaded("puffish_skills");
  // Neither suite stops its server. OriginSmoke already invokes all expanded current-origin checks.
  OriginSmoke.run(server);TitleSmoke.run(server);TitleCleanupSmoke.verify(server);
  if(skills)TreeSmoke.run(server);
  else{
   var player=AbyssSmoke.player(server,server.overworld(),"SkillsAbsentAudit");
   try{
    var before=ProgressionAPI.profile(player).copy();SkillBridge.sync(player);
    check(SkillBridge.spentPoints(player,ProgressionConfig.current.skillCategory).isEmpty()&&SkillBridge.titleSnapshot(player,ProgressionConfig.current.skillCategory).isEmpty()&&!SkillBridge.open(player),"absent optional Skills returns unavailable safely");
    check(ProgressionAPI.profile(player).equals(before),"absent optional Skills invents no points, unlocks or character progress");
   }finally{removePlayer(player);}
  }
  ascension(server,skills,report);
  report.addProperty("puffish_skills",skills);report.addProperty("origin_legacy_and_current_runtime_checks",true);report.addProperty("title_manual_rewards_and_legacy_migration",true);report.addProperty("optional_skills_absence_checked",!skills);
  TnkProgression.LOG.info("TNK_SKILLS_ORIGINS_TITLES_AUDIT_OK: {}",report);
 }
}
