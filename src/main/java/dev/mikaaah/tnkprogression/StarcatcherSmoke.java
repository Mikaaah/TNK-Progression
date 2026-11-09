package dev.mikaaah.tnkprogression;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.attachment.AttachmentType;

/** Exercises the real optional mod's complete catch method; no API stubs or simulated catch event. */
public final class StarcatcherSmoke {
 @SuppressWarnings("unchecked") public static void run(ServerPlayer p)throws Exception{
  if(!net.neoforged.fml.ModList.get().isLoaded("starcatcher"))return;
  var world=p.serverLevel();var api=Class.forName("com.wdiscute.starcatcher.fish.FishApi");var bobClass=Class.forName("com.wdiscute.starcatcher.bobentity.FishingBobEntity");
  var skinClass=Class.forName("com.wdiscute.starcatcher.registry.tackleskin.AbstractTackleSkin");
  var skin=((Supplier<?>)Class.forName("com.wdiscute.starcatcher.registry.tackleskin.SCTackleSkins").getField("BASE_TACKLE_SKIN").get(null)).get();
  var fishes=(List<?>)api.getMethod("getFishes",Level.class).invoke(null,world);
  var attachment=(AttachmentType<Object>)((Supplier<?>)Class.forName("com.wdiscute.starcatcher.registry.SCDataAttachments").getField("FISHING_BOB").get(null)).get();
  var attachmentClass=Class.forName("com.wdiscute.starcatcher.data.attachments.FishingBobAttachment");
  for(String rarity:List.of("common","rare","epic","legendary")){
   Object fp=null;for(Object fish:fishes)if(((Enum<?>)fish.getClass().getMethod("rarity").invoke(fish)).name().equalsIgnoreCase(rarity)){fp=fish;break;}
   if(fp==null)throw new AssertionError("No real Starcatcher fish for "+rarity);
   Entity bob=(Entity)bobClass.getConstructor(Level.class,Player.class,ItemStack.class,skinClass).newInstance(world,p,new ItemStack(Items.FISHING_ROD),skin);
   ((List<?>)bobClass.getField("modifiers").get(bob)).clear();bobClass.getField("fpToFish").set(bob,fp);
   bobClass.getField("rlToAwardUponFishingComplete").set(bob,api.getMethod("getKey",Level.class,fp.getClass()).invoke(null,world,fp));world.addFreshEntity(bob);
   p.setData(attachment,attachmentClass.getConstructor(String.class).newInstance(bob.getUUID().toString()));
   double before=ProgressionAPI.profile(p).getDouble("fishing_xp_earned");
   api.getMethod("spawnFishFromPlayerFishing",ServerPlayer.class,boolean.class,int.class,boolean.class,boolean.class,int.class).invoke(null,p,true,100,false,false,0);
   double gained=ProgressionAPI.profile(p).getDouble("fishing_xp_earned")-before;
   if(gained!=DirectXpConfig.current.starcatcher.get(rarity))throw new AssertionError(rarity+" catch gave "+gained+" XP");
  }
  TnkProgression.LOG.info("TNK_STARCATCHER_SMOKE_OK: real Starcatcher 3.1.4.1 common, rare, epic and legendary catches pay exactly once");
 }
}
