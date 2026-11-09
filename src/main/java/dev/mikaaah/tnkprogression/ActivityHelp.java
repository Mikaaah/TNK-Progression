package dev.mikaaah.tnkprogression;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;

/** Sent from the server so the player sees the active rewards, not a stale client preset. */
public final class ActivityHelp {
 public static List<String> describe(ServerPlayer player,String key){var c=DirectXpConfig.current;var lines=new ArrayList<String>();switch(key){
  case "combat"->{
   lines.add("Character XP = "+n(c.combatBase)+" + "+n(c.combatPerLevel)+" x mob level, multiplied by enemy category and your damage contribution.");
   lines.add("Ordinary hostile x1; passive x"+n(c.passiveMultiplier)+"; tough (60+ maximum health) x"+n(c.toughMultiplier)+"; configured miniboss x"+n(c.minibossMultiplier)+"; configured boss x"+n(c.bossMultiplier)+". Per-entity overrides are available in tnk-xp.json.");
   lines.add("Deal damage personally or through supported owned summons. Be alive, nearby and in the same dimension when it dies. Recent damage expires after "+ProgressionConfig.current.contributionTicks/20+" seconds.");
   lines.add("Environmental and machine damage cannot turn a token hit into a full reward. Pets, owned enemies, spawn eggs, commands and split offspring give no XP. Legitimate dungeon, spawner and triggered bosses qualify.");
  }
  case "mining"->{
   lines.add("Mine a natural block by hand and produce a block drop. XP depends on block type, never Fortune drops or character level.");
   c.miningBlocks.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->lines.add(e.getKey()+": "+n(e.getValue())+" XP per block"));
   lines.add("Compatible modded blocks also match these tags (the highest matching reward applies; an explicit block override wins):");
   c.miningTags.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->lines.add("#"+e.getKey()+": "+n(e.getValue())+" XP"));
   lines.add("Placed, piston-moved, fluid-generated and already mined positions give zero XP. Provenance persists through restart. Fake players and machine mining give zero XP.");
  }
  case "farming"->{
   lines.add("Harvest a mature crop by hand: "+n(c.crop)+" XP. Mature nether wart/cocoa: "+n(c.specialCrop)+" XP. Naturally grown pumpkin/melon: "+n(c.fruit)+" XP. Ripe berries: "+n(c.berries)+" XP.");
   lines.add("One reward per harvest, independent of crop yield. Breaking mature crops and supported right-click harvesting count. Planted crops can grow and earn XP; placing fruit or breaking immature crops cannot.");
   lines.add("Each crop position has a shared "+ProgressionConfig.current.harvestCooldownTicks/20+" second cooldown. Automated harvesting gives zero XP.");
  }
  case "cooking"->{
   lines.add("Manually insert raw ingredients into a furnace, smoker or campfire. XP is awarded when an actual cooking recipe produces food: normally "+n(c.cookedFood)+" XP per cooked item.");
   lines.add("Remain online, in the same dimension and within 64 blocks when cooking finishes. Ingredient ownership persists in the cooking block through restart.");
   lines.add("Hopper/pipe ingredients, taking stored output, crafting food and non-food smelting give zero XP. External removal or replacement of raw input clears its ownership to prevent automated loops.");
   c.cookingItems.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->lines.add(e.getKey()+": "+n(e.getValue())+" XP per cooked item"));
  }
  case "fishing"->{
   lines.add("Complete a real fishing catch. Vanilla rod catches: "+n(c.vanillaFishing)+" XP each, independent of stack size or luck.");
   lines.add("Starcatcher catches use fish rarity, including compatible fish from other mods. Complete Starcatcher's catch or minigame. Lava catches are supported.");
   c.starcatcher.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(e->lines.add("Starcatcher "+e.getKey()+": "+n(e.getValue())+" XP per catch"));
   lines.add("Trash/none give zero XP unless configured. Empty reels, dropped/inventory fish and automatic fishing do not count. A catch pays once.");
   lines.add(ProgressionConfig.current.requireOpenWater?"Vanilla fishing requires open water and a hook at least 4 seconds old. Starcatcher uses its own valid catch rules.":"Vanilla fishing allows catches without open water; the hook must be at least 4 seconds old.");
  }
  case "exploration"->{
   lines.add("Physically enter a chunk new to your character: "+n(c.chunk)+" XP. First biome type: "+n(c.biome)+" XP. First visit to each structure instance: "+n(c.structure)+" XP. First entry into a dimension: "+n(c.dimension)+" XP.");
   lines.add("Each discovery pays once per character. Position is sampled once per second; crossing more than 64 blocks between samples skips destination chunk/biome/structure XP for that sample. Merely loading or pregenerating chunks gives nothing.");
   lines.add("Login does not pay dimension XP. The first sample records your chunk without paying; exploration starts as you move. Existing visited chunks stay visited after migration.");
  }
  case "quest"->{
   lines.add("Quest XP comes only from a server command reward. FTB Quests command: tnkprogress xp {p} 150 (permission level 2).");
   lines.add("Use the same command for every quest in a chapter. Change only the XP amount. FTB Quests controls claims/repeatability; no unique mod reward ID is required.");
   lines.add("Suggested rewards: small 50, normal 150, major 400, boss 1200+, chapter finale 2000-5000 XP. Quest XP is recorded separately and fills the same character bar.");
  }
  default->lines.add("This activity has no XP rewards.");
 }
 lines.add("Direct XP stays constant as you level. Only the character level requirement grows. Crafting and engineering award no automatic XP.");
 lines.add(key.equals("quest")?"XP pauses at your campaign ceiling; unlock the next chapter to continue.":"Survival/Adventure players only. XP pauses at your campaign ceiling; unlock the next chapter to continue.");return List.copyOf(lines);}
 private static String n(double value){return value==(long)value?Long.toString((long)value):String.format(Locale.ROOT,"%.2f",value);}
}
