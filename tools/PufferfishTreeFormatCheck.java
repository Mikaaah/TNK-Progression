import java.nio.file.*;
import java.util.*;
import net.puffish.skillsmod.api.json.*;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.config.*;
import net.puffish.skillsmod.reward.BuiltinRewards;
import net.minecraft.resources.ResourceLocation;
public class PufferfishTreeFormatCheck {
 static int warnings;
 static net.minecraft.server.MinecraftServer server;
 static ConfigContext context=new ConfigContext(){public net.minecraft.server.MinecraftServer getServer(){return server;} public void emitWarning(String text){warnings++;}};
 static JsonElement read(Path root,String name)throws Exception{return JsonElement.parseFile(root.resolve(name+".json"),JsonPath.create(name)).getSuccess().orElseThrow();}
 public static void run(net.minecraft.server.MinecraftServer running,String directory)throws Exception{
  server=running;
  // Isolated parser fixture only: supply the platform queried even for an empty required_mods list.
  var constructor=net.puffish.skillsmod.SkillsMod.class.getDeclaredConstructors()[0];constructor.setAccessible(true);
  var platform=new net.puffish.skillsmod.server.setup.ServerPlatform(){public boolean isFakePlayer(net.minecraft.server.level.ServerPlayer p){return true;}public boolean isModLoaded(String id){return net.neoforged.fml.ModList.get().isLoaded(id);}};
  var fixture=constructor.newInstance(Path.of(directory),(net.puffish.skillsmod.server.network.ServerPacketSender)(p,packet)->{},platform,null);
  var instance=net.puffish.skillsmod.SkillsMod.class.getDeclaredField("instance");instance.setAccessible(true);instance.set(null,fixture);
  BuiltinRewards.register();Path root=Path.of(directory);
  var result=CategoryConfig.parse(ResourceLocation.parse("puffish_skills:character"),2,read(root,"category"),read(root,"definitions"),read(root,"skills"),read(root,"connections"),Optional.empty(),Optional.empty(),context);
  if(result.getSuccess().isEmpty())throw new AssertionError(result.getFailure().orElse(null));
  var category=result.getSuccess().orElseThrow();
  if(category.skills().getAll().size()!=588)throw new AssertionError("Missing nodes");
  var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(server.overworld(),new com.mojang.authlib.GameProfile(java.util.UUID.fromString("f6279536-72a7-44fd-bac6-c5fa8cd43c08"),"TreeRewardCheck"));
  var tag=category.definitions().getById("blacksmith_defensive_keystone_1").orElseThrow().rewards().stream().filter(r->r.type().getPath().equals("tag")).findFirst().orElseThrow().instance();
  tag.update(update(player,1));if(!player.getTags().contains("tnk_tree.blacksmith_defensive_keystone_1"))throw new AssertionError("Native tag reward not applied");
  tag.update(update(player,0));if(player.getTags().contains("tnk_tree.blacksmith_defensive_keystone_1"))throw new AssertionError("Native respec did not remove passive tag");
  var life=category.definitions().getById("miner_life_1").orElseThrow().rewards().getFirst().instance();float health=player.getMaxHealth();
  life.update(update(player,1));float raised=player.getMaxHealth();if(raised<=health)throw new AssertionError("Native attribute not applied");
  life.update(update(player,1));if(player.getMaxHealth()!=raised)throw new AssertionError("Native refresh stacked");
  life.update(update(player,0));if(player.getMaxHealth()!=health)throw new AssertionError("Native respec did not remove attribute");
  System.out.println("PUFFERFISH_588_NATIVE_PARSE_OK: 588 nodes; optional-mod warnings="+warnings);
  System.out.println("PUFFERFISH_NATIVE_REWARD_RESPEC_OK: tag and attribute apply/remove; repeat update does not stack");
 }
 static net.puffish.skillsmod.api.reward.RewardUpdateContext update(net.minecraft.server.level.ServerPlayer p,int count){return new net.puffish.skillsmod.api.reward.RewardUpdateContext(){public net.minecraft.server.level.ServerPlayer getPlayer(){return p;}public int getCount(){return count;}public boolean isAction(){return false;}};}
}
