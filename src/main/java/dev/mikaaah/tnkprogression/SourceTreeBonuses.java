package dev.mikaaah.tnkprogression;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Original source magnitudes and conditions, owned exclusively through reversible native skill tags. */
public final class SourceTreeBonuses {
 private record Bonus(String id,JsonObject data){}
 private record Owned(Set<String> tags,List<Bonus> bonuses){}
 private record Applied(AttributeInstance attribute,ResourceLocation id,double amount,AttributeModifier.Operation operation){}
 private static final Map<String,List<Bonus>> DEFINITIONS=load();
 private static final Map<ServerPlayer,Owned> OWNED=new WeakHashMap<>();
 private static final Map<ServerPlayer,Map<String,Applied>> APPLIED=new WeakHashMap<>();
 private static Map<String,List<Bonus>> load(){
  var path=net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("puffish_skills/tnk_passives.json");
  if(!java.nio.file.Files.exists(path))return Map.of();
  try(var reader=java.nio.file.Files.newBufferedReader(path)){
   var root=JsonParser.parseReader(reader).getAsJsonObject();if(!root.has("source_bonuses"))return Map.of();Map<String,List<Bonus>> result=new HashMap<>();
   for(var e:root.getAsJsonObject("source_bonuses").entrySet()){List<Bonus> list=new ArrayList<>();int i=0;for(var b:e.getValue().getAsJsonArray())list.add(new Bonus(e.getKey()+"/"+(i++),b.getAsJsonObject()));result.put(e.getKey(),List.copyOf(list));}return Map.copyOf(result);
  }catch(Exception ex){TnkProgression.LOG.error("Invalid original skill bonus table; source effects disabled",ex);return Map.of();}
 }
 static boolean hasDefinition(String id){return DEFINITIONS.containsKey(id);}
 private static String text(JsonObject d,String key,String fallback){return d.has(key)?d.get(key).getAsString():fallback;}
 private static double number(JsonObject d,String key,double fallback){return d.has(key)?d.get(key).getAsDouble():fallback;}
 private static JsonObject object(JsonObject d,String key){return d.has(key)?d.getAsJsonObject(key):new JsonObject();}
 private static String kind(JsonObject d){return text(d,"type","skilltree:none").replace("skilltree:","");}
 private static List<Bonus> owned(ServerPlayer p){
  if(!ActivityGuard.player(p)||!net.neoforged.fml.ModList.get().isLoaded("puffish_skills")){OWNED.remove(p);return List.of();}
  var tags=p.getTags();var old=OWNED.get(p);if(old!=null&&old.tags.equals(tags))return old.bonuses;
  List<Bonus> bonuses=new ArrayList<>();if(net.neoforged.fml.ModList.get().isLoaded("puffish_skills")&&ActivityGuard.player(p))for(String tag:tags)if(tag.startsWith("tnk_tree."))bonuses.addAll(DEFINITIONS.getOrDefault(tag.substring(9),List.of()));
  OWNED.put(p,new Owned(Set.copyOf(tags),List.copyOf(bonuses)));return bonuses;
 }
 private static boolean item(ItemStack stack,JsonObject d){
  if(stack.isEmpty())return false;
  if(kind(d).equals("enchanted"))return stack.isEnchanted()&&item(stack,object(d,"item_condition"));
  String type=text(d,"equipment_type","any");var i=stack.getItem();
  return switch(type){case "any"->true;case "shield"->i instanceof ShieldItem||stack.is(Tags.Items.TOOLS_SHIELD);case "weapon"->i instanceof SwordItem||i instanceof AxeItem||i instanceof TridentItem||i instanceof ProjectileWeaponItem||stack.is(Tags.Items.MELEE_WEAPON_TOOLS)||stack.is(Tags.Items.RANGED_WEAPON_TOOLS);case "ranged_weapon"->i instanceof ProjectileWeaponItem||stack.is(Tags.Items.RANGED_WEAPON_TOOLS);case "pickaxe"->i instanceof PickaxeItem||stack.is(ItemTags.PICKAXES);case "armor"->i instanceof ArmorItem||stack.is(Tags.Items.ARMORS);case "chestplate"->i instanceof ArmorItem a&&a.getEquipmentSlot()==EquipmentSlot.CHEST;case "helmet"->i instanceof ArmorItem a&&a.getEquipmentSlot()==EquipmentSlot.HEAD;case "boots"->i instanceof ArmorItem a&&a.getEquipmentSlot()==EquipmentSlot.FEET;default->false;};
 }
 /** Compute one attribute without its own conditional modifier, preventing threshold/self-scaling feedback. */
 private static double attribute(ServerPlayer p,String name,ResourceLocation excluded){
  var holder=BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(name));if(holder.isEmpty())return 0;var a=p.getAttribute(holder.get());if(a==null)return 0;
  double base=a.getBaseValue(),multiply=0,total=1;for(var m:a.getModifiers())if(!m.id().equals(excluded))switch(m.operation()){case ADD_VALUE->base+=m.amount();case ADD_MULTIPLIED_BASE->multiply+=m.amount();case ADD_MULTIPLIED_TOTAL->total*=1+m.amount();}
  return (base+base*multiply)*total;
 }
 private static boolean condition(ServerPlayer p,LivingEntity target,JsonObject d,ResourceLocation excluded){
  return switch(kind(d)){
   case "none"->true;
   case "effect_amount"->p.getActiveEffects().size()>=number(d,"min",0);
   case "health_percentage"->p.getHealth()/p.getMaxHealth()<=number(d,"max",1);
   case "food_level"->p.getFoodData().getFoodLevel()>=number(d,"min",0);
   case "fishing"->p.fishing!=null;
   case "attribute_value"->attribute(p,text(d,"attribute","minecraft:generic.armor"),excluded)>=number(d,"min",0);
   case "has_item_in_hand"->item(p.getMainHandItem(),object(d,"item_condition"))||item(p.getOffhandItem(),object(d,"item_condition"));
   case "has_item_equipped"->{boolean found=false;for(var stack:p.getAllSlots())if(item(stack,object(d,"item_condition")))found=true;yield found;}
   case "burning"->target!=null&&target.isOnFire();
   case "has_effect"->target!=null&&BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(text(d,"effect","minecraft:poison"))).map(target::hasEffect).orElse(false);
   default->false;
  };
 }
 private static double multiplier(ServerPlayer p,LivingEntity target,JsonObject d,ResourceLocation excluded){
  return switch(kind(d)){
   case "none"->1;case "effect_amount"->p.getActiveEffects().size();case "food_level"->p.getFoodData().getFoodLevel();
   case "attribute_value"->attribute(p,text(d,"attribute","minecraft:generic.armor"),excluded)/Math.max(.0001,number(d,"divisor",1));
   case "distance_to_target"->target==null?0:p.distanceTo(target);
   case "enchants_amount","enchants_levels"->{int count=0;for(var stack:p.getAllSlots())if(item(stack,object(d,"item_condition"))){var ench=stack.get(DataComponents.ENCHANTMENTS);if(ench!=null)for(var e:ench.entrySet())count+=kind(d).equals("enchants_amount")?1:e.getIntValue();}yield count;}
   default->0;
  };
 }
 static void refresh(ServerPlayer p){
  Map<String,Applied> next=new HashMap<>();
  for(var bonus:owned(p)){
   var b=bonus.data;String k=kind(b);if(!k.equals("attribute")&&!k.equals("block_break_speed"))continue;
   var id=ResourceLocation.parse("tnkprogression:source/"+bonus.id);if(!condition(p,null,object(b,"player_condition"),id))continue;
   String key=k.equals("block_break_speed")?"minecraft:player.block_break_speed":text(b,"attribute","");var holder=BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(key));if(holder.isEmpty())continue;var a=p.getAttribute(holder.get());if(a==null)continue;
   double amount=number(b,"amount",number(b,"multiplier",0))*multiplier(p,null,object(b,"player_multiplier"),id);if(!Double.isFinite(amount)||amount==0)continue;
   int op=(int)number(b,"operation",1);next.put(bonus.id,new Applied(a,id,amount,op==0?AttributeModifier.Operation.ADD_VALUE:op==1?AttributeModifier.Operation.ADD_MULTIPLIED_BASE:AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
  }
  var previous=APPLIED.getOrDefault(p,Map.of());for(var e:previous.entrySet())if(!next.containsKey(e.getKey()))e.getValue().attribute.removeModifier(e.getValue().id);
  for(var e:next.entrySet()){var n=e.getValue();if(n.equals(previous.get(e.getKey()))&&n.attribute.hasModifier(n.id))continue;n.attribute.removeModifier(n.id);n.attribute.addTransientModifier(new AttributeModifier(n.id,n.amount,n.operation));}
  APPLIED.put(p,Map.copyOf(next));if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
 }
 @SubscribeEvent public static void tick(PlayerTickEvent.Post e){if(e.getEntity() instanceof ServerPlayer p&&p.tickCount%5==0)refresh(p);}
 @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){OWNED.remove(p);APPLIED.remove(p);}}
 @SubscribeEvent public static void clone(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone e){if(e.getOriginal() instanceof ServerPlayer p){OWNED.remove(p);APPLIED.remove(p);}}
 @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e){OWNED.clear();APPLIED.clear();}
 @SubscribeEvent public static void damage(LivingDamageEvent.Pre e){
  if(!(e.getSource().getEntity() instanceof ServerPlayer p)||e.getEntity().level().isClientSide)return;
  double added=0;for(var bonus:owned(p)){var b=bonus.data;if(!kind(b).equals("damage"))continue;var id=ResourceLocation.parse("tnkprogression:source/"+bonus.id);
   String damage=kind(object(b,"damage_condition"));boolean projectile=e.getSource().is(DamageTypeTags.IS_PROJECTILE);
   if(damage.equals("projectile")&&!projectile||damage.equals("melee")&&(projectile||e.getSource().getDirectEntity()!=p))continue;
   if(condition(p,e.getEntity(),object(b,"player_condition"),id)&&condition(p,e.getEntity(),object(b,"target_condition"),id))added+=number(b,"amount",0)*multiplier(p,e.getEntity(),object(b,"player_multiplier"),id);
  }
  if(Double.isFinite(added)&&added!=0)e.setNewDamage((float)Math.max(0,e.getNewDamage()*(1+added)));
 }
}
