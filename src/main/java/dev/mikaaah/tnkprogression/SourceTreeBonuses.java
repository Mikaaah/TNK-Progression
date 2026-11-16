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
 private record Expression(String kind,double min,double max,double divisor,ResourceLocation attribute,ResourceLocation effect,String equipment,Expression item){}
 private static final Expression NONE=new Expression("none",0,1,1,null,null,"any",null);
 private record Bonus(String id,String kind,ResourceLocation modifier,ResourceLocation attribute,double amount,AttributeModifier.Operation operation,Expression playerCondition,Expression playerMultiplier,Expression targetCondition,String damage){}
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
   for(var e:root.getAsJsonObject("source_bonuses").entrySet()){List<Bonus> list=new ArrayList<>();int i=0;for(var b:e.getValue().getAsJsonArray())list.add(compile(e.getKey()+"/"+(i++),b.getAsJsonObject()));result.put(e.getKey(),List.copyOf(list));}return Map.copyOf(result);
  }catch(Exception ex){TnkProgression.LOG.error("Invalid original skill bonus table; source effects disabled",ex);return Map.of();}
 }
 static boolean hasDefinition(String id){return DEFINITIONS.containsKey(id);}
 private static String text(JsonObject d,String key,String fallback){return d.has(key)?d.get(key).getAsString():fallback;}
 private static double number(JsonObject d,String key,double fallback){return d.has(key)?d.get(key).getAsDouble():fallback;}
 private static String kind(JsonObject d){return text(d,"type","skilltree:none").replace("skilltree:","");}
 private static Expression expression(JsonObject parent,String key){return parent.has(key)?expression(parent.getAsJsonObject(key)):NONE;}
 private static Expression expression(JsonObject d){
  String kind=kind(d);boolean attribute=kind.equals("attribute_value"),effect=kind.equals("has_effect");
  boolean nested=Set.of("enchanted","has_item_in_hand","has_item_equipped","enchants_amount","enchants_levels").contains(kind);
  return new Expression(kind,number(d,"min",0),number(d,"max",1),number(d,"divisor",1),
    attribute?ResourceLocation.parse(text(d,"attribute","minecraft:generic.armor")):null,
    effect?ResourceLocation.parse(text(d,"effect","minecraft:poison")):null,text(d,"equipment_type","any"),nested?expression(d,"item_condition"):NONE);
 }
 /** Parse configuration once. Player ticks evaluate only live conditions, never JSON or identifiers. */
 private static Bonus compile(String id,JsonObject d){
  String kind=kind(d);int op=(int)number(d,"operation",1);
  ResourceLocation attribute=kind.equals("block_break_speed")?ResourceLocation.parse("minecraft:player.block_break_speed"):
    kind.equals("attribute")?ResourceLocation.parse(text(d,"attribute","")):null;
  double amount=number(d,"amount",kind.equals("damage")?0:number(d,"multiplier",0));
  if(!Double.isFinite(amount))throw new IllegalArgumentException("Non-finite source bonus "+id);
  return new Bonus(id,kind,ResourceLocation.parse("tnkprogression:source/"+id),attribute,amount,
    op==0?AttributeModifier.Operation.ADD_VALUE:op==1?AttributeModifier.Operation.ADD_MULTIPLIED_BASE:AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
    expression(d,"player_condition"),expression(d,"player_multiplier"),expression(d,"target_condition"),expression(d,"damage_condition").kind);
 }
 private static List<Bonus> owned(ServerPlayer p){
  if(!ActivityGuard.player(p)||!net.neoforged.fml.ModList.get().isLoaded("puffish_skills")){OWNED.remove(p);return List.of();}
  var tags=p.getTags();var old=OWNED.get(p);if(old!=null&&old.tags.equals(tags))return old.bonuses;
  List<Bonus> bonuses=new ArrayList<>();if(net.neoforged.fml.ModList.get().isLoaded("puffish_skills")&&ActivityGuard.player(p))for(String tag:tags)if(tag.startsWith("tnk_tree."))bonuses.addAll(DEFINITIONS.getOrDefault(tag.substring(9),List.of()));
  OWNED.put(p,new Owned(Set.copyOf(tags),List.copyOf(bonuses)));return bonuses;
 }
 private static boolean item(ItemStack stack,Expression d){
  if(stack.isEmpty())return false;
  if(d.kind.equals("enchanted"))return stack.isEnchanted()&&item(stack,d.item);
  String type=d.equipment;var i=stack.getItem();
  return switch(type){case "any"->true;case "shield"->i instanceof ShieldItem||stack.is(Tags.Items.TOOLS_SHIELD);case "weapon"->i instanceof SwordItem||i instanceof AxeItem||i instanceof TridentItem||i instanceof ProjectileWeaponItem||stack.is(Tags.Items.MELEE_WEAPON_TOOLS)||stack.is(Tags.Items.RANGED_WEAPON_TOOLS);case "ranged_weapon"->i instanceof ProjectileWeaponItem||stack.is(Tags.Items.RANGED_WEAPON_TOOLS);case "pickaxe"->i instanceof PickaxeItem||stack.is(ItemTags.PICKAXES);case "armor"->i instanceof ArmorItem||stack.is(Tags.Items.ARMORS);case "chestplate"->i instanceof ArmorItem a&&a.getEquipmentSlot()==EquipmentSlot.CHEST;case "helmet"->i instanceof ArmorItem a&&a.getEquipmentSlot()==EquipmentSlot.HEAD;case "boots"->i instanceof ArmorItem a&&a.getEquipmentSlot()==EquipmentSlot.FEET;default->false;};
 }
 /** Compute one attribute without its own conditional modifier, preventing threshold/self-scaling feedback. */
 private static double attribute(ServerPlayer p,ResourceLocation name,ResourceLocation excluded){
  var holder=BuiltInRegistries.ATTRIBUTE.getHolder(name);if(holder.isEmpty())return 0;var a=p.getAttribute(holder.get());if(a==null)return 0;
  double base=a.getBaseValue(),multiply=0,total=1;for(var m:a.getModifiers())if(!m.id().equals(excluded))switch(m.operation()){case ADD_VALUE->base+=m.amount();case ADD_MULTIPLIED_BASE->multiply+=m.amount();case ADD_MULTIPLIED_TOTAL->total*=1+m.amount();}
  return (base+base*multiply)*total;
 }
 private static boolean condition(ServerPlayer p,LivingEntity target,Expression d,ResourceLocation excluded){
  return switch(d.kind){
   case "none"->true;
   case "effect_amount"->p.getActiveEffects().size()>=d.min;
   case "health_percentage"->p.getHealth()/p.getMaxHealth()<=d.max;
   case "food_level"->p.getFoodData().getFoodLevel()>=d.min;
   case "fishing"->p.fishing!=null;
   case "attribute_value"->attribute(p,d.attribute,excluded)>=d.min;
   case "has_item_in_hand"->item(p.getMainHandItem(),d.item)||item(p.getOffhandItem(),d.item);
   case "has_item_equipped"->{boolean found=false;for(var stack:p.getAllSlots())if(item(stack,d.item))found=true;yield found;}
   case "burning"->target!=null&&target.isOnFire();
   case "has_effect"->target!=null&&BuiltInRegistries.MOB_EFFECT.getHolder(d.effect).map(target::hasEffect).orElse(false);
   default->false;
  };
 }
 private static double multiplier(ServerPlayer p,LivingEntity target,Expression d,ResourceLocation excluded){
  return switch(d.kind){
   case "none"->1;case "effect_amount"->p.getActiveEffects().size();case "food_level"->p.getFoodData().getFoodLevel();
   case "attribute_value"->attribute(p,d.attribute,excluded)/Math.max(.0001,d.divisor);
   case "distance_to_target"->target==null?0:p.distanceTo(target);
   case "enchants_amount","enchants_levels"->{int count=0;for(var stack:p.getAllSlots())if(item(stack,d.item)){var ench=stack.get(DataComponents.ENCHANTMENTS);if(ench!=null)for(var e:ench.entrySet())count+=d.kind.equals("enchants_amount")?1:e.getIntValue();}yield count;}
   default->0;
  };
 }
 static void refresh(ServerPlayer p){
  var bonuses=owned(p);var previous=APPLIED.getOrDefault(p,Map.of());if(bonuses.isEmpty()&&previous.isEmpty())return;
  Map<String,Applied> next=new HashMap<>();
  for(var b:bonuses){
   if(b.attribute==null||!condition(p,null,b.playerCondition,b.modifier))continue;
   var holder=BuiltInRegistries.ATTRIBUTE.getHolder(b.attribute);if(holder.isEmpty())continue;var a=p.getAttribute(holder.get());if(a==null)continue;
   double amount=b.amount*multiplier(p,null,b.playerMultiplier,b.modifier);if(!Double.isFinite(amount)||amount==0)continue;
   next.put(b.id,new Applied(a,b.modifier,amount,b.operation));
  }
  for(var e:previous.entrySet())if(!next.containsKey(e.getKey()))e.getValue().attribute.removeModifier(e.getValue().id);
  for(var e:next.entrySet()){var n=e.getValue();if(n.equals(previous.get(e.getKey()))&&n.attribute.hasModifier(n.id))continue;n.attribute.removeModifier(n.id);n.attribute.addTransientModifier(new AttributeModifier(n.id,n.amount,n.operation));}
  if(!next.equals(previous))APPLIED.put(p,Map.copyOf(next));if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
 }
 @SubscribeEvent public static void tick(PlayerTickEvent.Post e){if(e.getEntity() instanceof ServerPlayer p&&p.tickCount%5==0)refresh(p);}
 @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){OWNED.remove(p);APPLIED.remove(p);}}
 @SubscribeEvent public static void clone(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone e){if(e.getOriginal() instanceof ServerPlayer p){OWNED.remove(p);APPLIED.remove(p);}}
 @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e){OWNED.clear();APPLIED.clear();}
 @SubscribeEvent public static void damage(LivingDamageEvent.Pre e){
  if(!(e.getSource().getEntity() instanceof ServerPlayer p)||e.getEntity().level().isClientSide)return;
  double added=0;for(var b:owned(p)){if(!b.kind.equals("damage"))continue;
   boolean projectile=e.getSource().is(DamageTypeTags.IS_PROJECTILE);
   if(b.damage.equals("projectile")&&!projectile||b.damage.equals("melee")&&(projectile||e.getSource().getDirectEntity()!=p))continue;
   if(condition(p,e.getEntity(),b.playerCondition,b.modifier)&&condition(p,e.getEntity(),b.targetCondition,b.modifier))added+=b.amount*multiplier(p,e.getEntity(),b.playerMultiplier,b.modifier);
  }
  if(Double.isFinite(added)&&added!=0)e.setNewDamage((float)Math.max(0,e.getNewDamage()*(1+added)));
 }
}
