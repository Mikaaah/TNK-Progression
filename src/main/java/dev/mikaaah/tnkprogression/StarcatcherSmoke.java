package dev.mikaaah.tnkprogression;
import java.util.*;
import java.util.function.Supplier;
import java.lang.reflect.InvocationTargetException;
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
  var world=p.serverLevel();var oldPosition=p.position();
  // The preceding teleport exclusion test leaves this fixture player in an unticketed chunk.
  // Real connected players ticket their fishing area; use the already prepared spawn area here.
  p.setPos(world.getSharedSpawnPos().getCenter());
  try{
  var api=Class.forName("com.wdiscute.starcatcher.fish.FishApi");var bobClass=Class.forName("com.wdiscute.starcatcher.bobentity.FishingBobEntity");
  var skinClass=Class.forName("com.wdiscute.starcatcher.registry.tackleskin.AbstractTackleSkin");
  var skin=((Supplier<?>)Class.forName("com.wdiscute.starcatcher.registry.tackleskin.SCTackleSkins").getField("BASE_TACKLE_SKIN").get(null)).get();
  var fishes=(List<?>)api.getMethod("getFishes",Level.class).invoke(null,world);
  var attachment=(AttachmentType<Object>)((Supplier<?>)Class.forName("com.wdiscute.starcatcher.registry.SCDataAttachments").getField("FISHING_BOB").get(null)).get();
  var attachmentClass=Class.forName("com.wdiscute.starcatcher.data.attachments.FishingBobAttachment");
  var complete=api.getMethod("spawnFishFromPlayerFishing",ServerPlayer.class,boolean.class,int.class,boolean.class,boolean.class,int.class);
  Object common=null;
  for(String rarity:List.of("common","rare","epic","legendary")){
   Object fp=null;for(Object fish:fishes)if(((Enum<?>)fish.getClass().getMethod("rarity").invoke(fish)).name().equalsIgnoreCase(rarity)){fp=fish;break;}
   if(fp==null)throw new AssertionError("No real Starcatcher fish for "+rarity);
   if(rarity.equals("common"))common=fp;
   Entity bob=makeBob(p,api,bobClass,skinClass,skin,fp);
   p.setData(attachment,attachmentClass.getConstructor(String.class).newInstance(bob.getUUID().toString()));
   double before=ProgressionAPI.profile(p).getDouble("fishing_xp_earned");
   complete.invoke(null,p,true,100,false,false,0);
   double gained=ProgressionAPI.profile(p).getDouble("fishing_xp_earned")-before;
   if(gained!=DirectXpConfig.current.starcatcher.get(rarity))throw new AssertionError(rarity+" catch gave "+gained+" XP");
   complete.invoke(null,p,true,100,false,false,0);
   if(ProgressionAPI.profile(p).getDouble("fishing_xp_earned")!=before+gained)throw new AssertionError("Repeated completed catch paid twice");
  }
  Entity missed=makeBob(p,api,bobClass,skinClass,skin,common);
  p.setData(attachment,attachmentClass.getConstructor(String.class).newInstance(missed.getUUID().toString()));
  double before=ProgressionAPI.profile(p).getDouble("fishing_xp_earned");complete.invoke(null,p,false,100,false,false,0);
  if(ProgressionAPI.profile(p).getDouble("fishing_xp_earned")!=before)throw new AssertionError("Failed minigame paid XP");
  Entity paid=makeBob(p,api,bobClass,skinClass,skin,common);paid.getPersistentData().putBoolean("tnk_fishing_paid",true);
  p.setData(attachment,attachmentClass.getConstructor(String.class).newInstance(paid.getUUID().toString()));complete.invoke(null,p,true,100,false,false,0);
  if(ProgressionAPI.profile(p).getDouble("fishing_xp_earned")!=before)throw new AssertionError("Already-paid bob paid again");
  // Seed a real pending catch before making the original method throw. The transformed wrapper must
  // clear it in finally, and it must not pay for the partially executed original catch.
  Entity fault=makeBob(p,api,bobClass,skinClass,skin,common);
  p.setData(attachment,attachmentClass.getConstructor(String.class).newInstance(fault.getUUID().toString()));
  StarcatcherFishing.begin(p,true);var pending=StarcatcherFishing.class.getDeclaredField("PENDING");pending.setAccessible(true);var scope=(ThreadLocal<?>)pending.get(null);
  if(scope.get()==null)throw new AssertionError("Actual Starcatcher attachment/bob API failed to bind");
  bobClass.getField("tackleSkin").set(fault,null);boolean failed=false;
  try{complete.invoke(null,p,true,100,false,false,0);}catch(InvocationTargetException ex){if(!(ex.getCause() instanceof NullPointerException))throw ex;failed=true;}
  finally{bobClass.getField("tackleSkin").set(fault,skin);fault.discard();p.removeData(attachment);}
  if(!failed||scope.get()!=null||ProgressionAPI.profile(p).getDouble("fishing_xp_earned")!=before)throw new AssertionError("Throwing catch retained context or paid XP");
  TnkProgression.LOG.info("TNK_STARCATCHER_SMOKE_OK: real Starcatcher 3.1.4.1 common/rare/epic/legendary catches, failed minigame, repeat/already-paid rejection and transformed exception cleanup");
  }finally{p.setPos(oldPosition);}
 }
 private static Entity makeBob(ServerPlayer p,Class<?> api,Class<?> bobClass,Class<?> skinClass,Object skin,Object fp)throws Exception{
  Entity bob=(Entity)bobClass.getConstructor(Level.class,Player.class,ItemStack.class,skinClass).newInstance(p.serverLevel(),p,new ItemStack(Items.FISHING_ROD),skin);
  ((List<?>)bobClass.getField("modifiers").get(bob)).clear();bobClass.getField("fpToFish").set(bob,fp);
  bobClass.getField("rlToAwardUponFishingComplete").set(bob,api.getMethod("getKey",Level.class,fp.getClass()).invoke(null,p.serverLevel(),fp));
  if(!p.serverLevel().addFreshEntity(bob)||p.serverLevel().getEntity(bob.getUUID())!=bob)throw new AssertionError("Starcatcher fixture bob is not visible through the world's live entity lookup");
  if(StarcatcherFishing.resolve(p,List.of())==null)throw new AssertionError("Starcatcher real attachment/owner/rarity API did not resolve the fixture bob");
  return bob;
 }
}
