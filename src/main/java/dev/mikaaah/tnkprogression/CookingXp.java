package dev.mikaaah.tnkprogression;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;

/** XP belongs to the player who inserted raw ingredients by hand, paid when a recipe actually finishes. */
public final class CookingXp {
 private static final ThreadLocal<ServerPlayer> MANUAL=new ThreadLocal<>();
 private record Input(AbstractFurnaceBlockEntity furnace,ItemStack stack){}
 private static final ThreadLocal<Input> BEFORE=new ThreadLocal<>();
 private static final String QUEUE="tnk_cooking_inputs_v1";
 private static ListTag queue(AbstractFurnaceBlockEntity be){return be.getPersistentData().getList(QUEUE,Tag.TAG_COMPOUND);}
 public static void begin(Player player,AbstractContainerMenu menu){
  MANUAL.remove();BEFORE.remove();
  if(!(player instanceof ServerPlayer p)||!ActivityGuard.player(p)||!(menu instanceof AbstractFurnaceMenu)||!(menu.getSlot(0).container instanceof AbstractFurnaceBlockEntity be))return;
  normalize(be);BEFORE.set(new Input(be,be.getItem(0).copy()));MANUAL.set(p);
 }
 public static void end(){try{
  var before=BEFORE.get();var player=MANUAL.get();if(before==null||player==null)return;
  var be=before.furnace();var old=before.stack();var now=be.getItem(0);var q=queue(be);
  if(!ItemStack.isSameItemSameComponents(old,now)){
   q=new ListTag();append(q,player.getUUID().toString(),now.getCount());
  }else if(now.getCount()>old.getCount())append(q,player.getUUID().toString(),now.getCount()-old.getCount());
  else if(now.getCount()<old.getCount())take(q,old.getCount()-now.getCount());
  be.getPersistentData().put(QUEUE,q);be.setChanged();
 }finally{MANUAL.remove();BEFORE.remove();}}
 /** Hoppers/pipes cannot preserve a manual owner's receipt when replacing input. */
 public static void externalChange(BlockEntity entity,int slot){if(slot==0&&entity instanceof AbstractFurnaceBlockEntity be&&MANUAL.get()==null){be.getPersistentData().remove(QUEUE);be.setChanged();}}
 public static void normalize(AbstractFurnaceBlockEntity be){var q=queue(be);int total=0;for(var row:q)total+=((CompoundTag)row).getInt("count");int actual=be.getItem(0).getCount();if(total>actual)take(q,total-actual);else if(total<actual)append(q,"",actual-total);be.getPersistentData().put(QUEUE,q);}
 private static void append(ListTag q,String owner,int count){if(count<=0)return;if(!q.isEmpty()&&q.getCompound(q.size()-1).getString("owner").equals(owner)){var row=q.getCompound(q.size()-1);row.putInt("count",row.getInt("count")+count);}else if(q.size()<64){var row=new CompoundTag();row.putString("owner",owner);row.putInt("count",count);q.add(row);}else{q.clear();var row=new CompoundTag();row.putString("owner","");row.putInt("count",64);q.add(row);}}
 private static Map<String,Integer> take(ListTag q,int count){var owners=new HashMap<String,Integer>();while(count>0&&!q.isEmpty()){var row=q.getCompound(0);int used=Math.min(count,row.getInt("count"));if(used<=0){q.remove(0);continue;}owners.merge(row.getString("owner"),used,Integer::sum);count-=used;int left=row.getInt("count")-used;if(left==0)q.remove(0);else row.putInt("count",left);}return owners;}
 public record Cook(int input,int output,ItemStack result){}
 public static Cook beforeCook(AbstractFurnaceBlockEntity be){if(!be.getPersistentData().contains(QUEUE))return null;normalize(be);return new Cook(be.getItem(0).getCount(),be.getItem(2).getCount(),be.getItem(2).copy());}
 public static void afterCook(AbstractFurnaceBlockEntity be,Cook before){if(before==null||!(be.getLevel() instanceof ServerLevel world))return;
  int consumed=before.input()-be.getItem(0).getCount(),produced=be.getItem(2).getCount()-before.output();
  if(consumed<=0)return;var q=queue(be);var owners=take(q,consumed);be.getPersistentData().put(QUEUE,q);be.setChanged();
  if(produced<=0)return;double xp=DirectXpConfig.current.cooking(be.getItem(2));
  for(var owner:owners.entrySet())pay(world,be,owner.getKey(),xp*produced*owner.getValue()/consumed);
 }
 private static void pay(ServerLevel world,BlockEntity be,String owner,double xp){if(owner.isEmpty()||xp<=0)return;try{var p=world.getServer().getPlayerList().getPlayer(UUID.fromString(owner));if(p!=null&&p.level()==world&&p.distanceToSqr(be.getBlockPos().getCenter())<=64*64)ProgressionAPI.awardSourceXp(p,"cooking",xp);}catch(IllegalArgumentException ignored){}}
 public static void campfirePlaced(CampfireBlockEntity be,Player player,ItemStack[] before){if(!(player instanceof ServerPlayer p)||!ActivityGuard.player(p))return;for(int i=0;i<4;i++)if(before[i].isEmpty()&&!be.getItems().get(i).isEmpty()){be.getPersistentData().putString("tnk_cook_owner_"+i,p.getUUID().toString());be.setChanged();}}
 public static ItemStack[] campfireItems(CampfireBlockEntity be){return be.getItems().stream().map(ItemStack::copy).toArray(ItemStack[]::new);}
 public static ItemStack[] campfireResults(CampfireBlockEntity be){var result=new ItemStack[4];for(int i=0;i<4;i++){var input=new SingleRecipeInput(be.getItems().get(i));result[i]=be.getLevel().getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING,input,be.getLevel()).map(r->r.value().assemble(input,be.getLevel().registryAccess())).orElse(ItemStack.EMPTY);}return result;}
 public static void campfireDone(CampfireBlockEntity be,ItemStack[] before,ItemStack[] results){if(!(be.getLevel() instanceof ServerLevel world))return;for(int i=0;i<4;i++)if(!before[i].isEmpty()&&be.getItems().get(i).isEmpty()){
  String key="tnk_cook_owner_"+i;pay(world,be,be.getPersistentData().getString(key),DirectXpConfig.current.cooking(results[i])*results[i].getCount());be.getPersistentData().remove(key);be.setChanged();
 }}
 @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event){MANUAL.remove();BEFORE.remove();}
}
