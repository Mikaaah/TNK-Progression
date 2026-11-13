package dev.mikaaah.tnkprogression.verification;

import dev.mikaaah.tnkprogression.*;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.dungeon.*;
import dev.mikaaah.tnkloot.verification.DungeonRewardSmoke;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import static dev.mikaaah.tnkprogression.verification.AbyssSmoke.check;

public final class LootChecks {
 public static void run(MinecraftServer server,ServerPlayer player,com.google.gson.JsonObject report,boolean spiral) throws Exception {
  var n=ProgressionAPI.profile(player);n.putInt("level",1);n.putInt("ceiling",10);
  var boss=EntityType.ZOMBIE.create(server.overworld());boss.getPersistentData().putInt("tnk_level",110);
  check(TnkLevelBridge.mobLevel(boss)==110,"Loot sees true saved level 110");
  for(int i=0;i<100;i++){
   var token=TnkLootRolls.roll(player,110,"compat_test");
   check(UnidentifiedWeaponItem.tier(token)<=1,"ordinary generated tier cap");
   check(java.util.List.of(TnkLootRolls.RARITIES).indexOf(UnidentifiedWeaponItem.data(token).getString("tnk_rarity"))<=1,"ordinary generated rarity cap");
   var earlier=token.copy();TnkLootRolls.cap(token,player,110);check(ItemStack.matches(earlier,token),"legacy token capped twice");
  }
  report.addProperty("ordinary_loot_cap_and_legacy_cases",100);
  if(spiral){
   var table=DungeonData.table(net.minecraft.resources.ResourceLocation.parse("tnk_spiral:chests/floor_6")).orElseThrow();
   check(table.settings().sourceLevel()==100,"floor six authored source is 100 despite combat level 110");
   var equipment=table.availableNormal().stream().filter(e->e.template().getItem() instanceof ArmorItem && !e.settings().identified()).findFirst().orElseThrow();
   var token=DungeonEquipment.reward(equipment,server.overworld(),new java.util.Random(4321));
   check(UnidentifiedWeaponItem.data(token).getInt("tnk_source_level")==100,"saved reward retains authored source 100");
   var saved=token.copy();TnkLootRolls.cap(token,player,110);check(ItemStack.matches(token,saved),"low recipient cannot re-cap authored reward");
   var result=UnidentifiedWeaponItem.baseResult(server.overworld(),token);
   check(ItemStack.matches(result,ApotheosisCompat.apply(result.copy(),0,player,55)),"low recipient identification preserves authored floor-six result");
   report.addProperty("combat_110_chest_source_100_low_recipient",true);
   report.add("authored_dungeon_regression",DungeonRewardSmoke.verify(server));
  }
 }
}
