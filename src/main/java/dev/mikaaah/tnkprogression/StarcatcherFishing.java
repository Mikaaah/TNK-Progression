package dev.mikaaah.tnkprogression;

import java.util.*;
import java.util.function.Supplier;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.AttachmentType;

/** Optional bridge verified against Starcatcher 3.1.4.1's actual catch event and attachment API. */
public final class StarcatcherFishing {
 private static final StackWalker WALKER=StackWalker.getInstance();
 private static boolean warned;
 private static final ThreadLocal<Pending> PENDING=new ThreadLocal<>();
 private record Api(AttachmentType<Object> fishingBob,Class<?> bobType,Field player,Field properties,DataComponentType<Object> fishInfo){}
 private static final ClassValue<Method> UUID_ACCESS=new ClassValue<>(){protected Method computeValue(Class<?> type){try{return type.getMethod("getUuid");}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}};
 private static final ClassValue<Method> RARITY_ACCESS=new ClassValue<>(){protected Method computeValue(Class<?> type){try{return type.getMethod("rarity");}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}};
 private static final class Binding {private static final Api API=bind();}
 @SuppressWarnings("unchecked") private static Api bind(){try{
  var attachments=Class.forName("com.wdiscute.starcatcher.registry.SCDataAttachments");
  var supplier=(Supplier<?>)attachments.getField("FISHING_BOB").get(null);
  var bob=Class.forName("com.wdiscute.starcatcher.bobentity.FishingBobEntity");
  return new Api((AttachmentType<Object>)supplier.get(),bob,bob.getField("player"),bob.getField("fpToFish"),
    (DataComponentType<Object>)BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("starcatcher:caught_fish_info")));
 }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn("Starcatcher catch XP could not bind this API; no XP credited",ex);return null;}}
 private static void warn(String message,Throwable ex){if(!warned){warned=true;TnkProgression.LOG.warn(message,ex);}}
 private static final class Pending {final ServerPlayer player;final Catch caught;net.neoforged.neoforge.event.entity.player.ItemFishedEvent event;Pending(ServerPlayer p,Catch c){player=p;caught=c;}}
 public static void begin(ServerPlayer player,boolean completed){PENDING.remove();if(completed&&ActivityGuard.player(player)){var c=resolve(player,List.of());if(c!=null)PENDING.set(new Pending(player,c));}}
 public static void clear(){PENDING.remove();}
 public static boolean capture(net.neoforged.neoforge.event.entity.player.ItemFishedEvent event){var pending=PENDING.get();if(pending==null||pending.player!=event.getEntity())return false;pending.event=event;return true;}
 public static void finish(ServerPlayer player){var pending=PENDING.get();PENDING.remove();if(pending==null||pending.player!=player||!pending.caught.bob().isRemoved())return;
  double xp=pending.caught.xp();if(pending.event!=null){if(pending.event.isCanceled()||pending.event.getDrops().isEmpty())return;var caught=resolveDrops(pending.caught,pending.event.getDrops());xp=caught.xp();}
  var n=pending.caught.bob().getPersistentData();if(n.getBoolean("tnk_fishing_paid"))return;n.putBoolean("tnk_fishing_paid",true);ProgressionAPI.awardSourceXp(player,"fishing",xp);
 }
 public record Catch(Entity bob,double xp){}
 public static boolean eventFromStarcatcher(){return WALKER.walk(frames->frames.anyMatch(f->f.getClassName().equals("com.wdiscute.starcatcher.fish.FishApi")&&f.getMethodName().equals("spawnFishFromPlayerFishing")));}
 public static Catch resolve(ServerPlayer player,List<ItemStack> drops){try{
  var api=Binding.API;if(api==null)return null;
  Object data=player.getData(api.fishingBob());
  UUID uuid=(UUID)UUID_ACCESS.get(data.getClass()).invoke(data);if(uuid==null)return null;
  var bob=player.serverLevel().getEntity(uuid);
  if(bob==null||!api.bobType().isInstance(bob)||api.player().get(bob)!=player)return null;
  Object fp=api.properties().get(bob);if(fp==null)return null;
  String rarity=rarity(RARITY_ACCESS.get(fp.getClass()).invoke(fp));
  return resolveDrops(new Catch(bob,DirectXpConfig.current.starcatcher.getOrDefault(rarity,0d)),drops);
 }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn("Starcatcher catch XP could not resolve this API; no XP credited",ex);return null;}}
 private static Catch resolveDrops(Catch caught,List<ItemStack> drops){try{
  var api=Binding.API;var component=api==null?null:api.fishInfo();
  if(component!=null)for(var stack:drops){Object info=stack.get(component);if(info!=null&&rarity(RARITY_ACCESS.get(info.getClass()).invoke(info)).equals("golden"))return new Catch(caught.bob(),DirectXpConfig.current.starcatcher.getOrDefault("golden",0d));}
 }catch(ReflectiveOperationException|RuntimeException ex){warn("Starcatcher golden catch metadata unavailable",ex);}return caught;}
 private static String rarity(Object rarity){return ((Enum<?>)rarity).name().toLowerCase(Locale.ROOT);}
}
