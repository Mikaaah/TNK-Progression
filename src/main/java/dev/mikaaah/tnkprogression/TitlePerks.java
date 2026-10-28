package dev.mikaaah.tnkprogression;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** One equipped title, one stable modifier per attribute; collection entries never stack. */
public final class TitlePerks {
 public record Perk(String attribute,double amount,String operation,String description) {}
 public record Config(int swapCooldownSeconds,Map<String,List<Perk>> titles) {}
 public static Config current=new Config(30,Map.of());
 public static boolean reload(){try{
  var path=FMLPaths.CONFIGDIR.get().resolve("tnk-title-perks.json");
  if(!Files.exists(path))try(var in=TitlePerks.class.getResourceAsStream("/data/tnkprogression/default_title_perks.json")){Files.copy(Objects.requireNonNull(in),path);}
  var c=new Gson().fromJson(Files.readString(path),Config.class);
  if(c.swapCooldownSeconds()<0 || c.swapCooldownSeconds()>300 || c.titles()==null || c.titles().size()>256)throw new IllegalArgumentException("Invalid perk settings");
  for(var list:c.titles().values()){
   if(list==null || list.size()>3)throw new IllegalArgumentException("At most three modest bonuses per title");
   Set<String> used=new HashSet<>();
   for(var p:list)if(p==null || ResourceLocation.tryParse(p.attribute())==null || !used.add(p.attribute()) || !Double.isFinite(p.amount()) || Math.abs(p.amount())>4 || (!p.operation().equals("add")&&!p.operation().equals("multiply_base")) || (p.operation().equals("multiply_base")&&Math.abs(p.amount())>.10) || !TitleConfig.validLabel(p.description(),100) || p.attribute().contains("identif"))throw new IllegalArgumentException("Invalid title perk");
  }
  current=c;return true;
 }catch(Exception ex){TnkProgression.LOG.error("Keeping previous title perks",ex);return false;}}
 private static ResourceLocation modifier(String attribute){return ResourceLocation.parse("tnkprogression:title/"+attribute.replace(':','/'));}
 public static boolean available(ServerPlayer p,Perk perk){return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(perk.attribute())).map(h->p.getAttribute(h)!=null).orElse(false);}
 public static List<Perk> forTitle(String id){return current.titles().getOrDefault(id,List.of());}
 public static void apply(ServerPlayer p){
  var n=ProgressionAPI.profile(p);var previous=n.getList("title_perk_attributes",Tag.TAG_STRING);Set<String> attributes=new HashSet<>();
  for(var tag:previous)attributes.add(tag.getAsString());
  for(var list:current.titles().values())for(var perk:list)attributes.add(perk.attribute());
  for(String id:attributes)BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(id)).ifPresent(h->{var a=p.getAttribute(h);if(a!=null)a.removeModifier(modifier(id));});
  ListTag active=new ListTag();
  for(var perk:forTitle(Titles.selected(p)))BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(perk.attribute())).ifPresent(h->{
   var a=p.getAttribute(h);if(a==null)return;
   a.addTransientModifier(new AttributeModifier(modifier(perk.attribute()),perk.amount(),perk.operation().equals("add")?AttributeModifier.Operation.ADD_VALUE:AttributeModifier.Operation.ADD_MULTIPLIED_BASE));active.add(StringTag.valueOf(perk.attribute()));
  });
  n.put("title_perk_attributes",active);ProgressionData.get(p.server).setDirty();
  if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth()); // Swapping never heals.
 }
 public static long cooldown(ServerPlayer p){return Math.max(0,ProgressionAPI.profile(p).getLong("title_swap_after")-p.server.overworld().getGameTime());}
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)apply(p);}
 @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)apply(p);}
}
