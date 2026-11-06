package dev.mikaaah.tnkprogression;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class CharacterSmoke {
 private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
 public static void run(MinecraftServer server){
  var p=FakePlayerFactory.get(server.overworld(),new com.mojang.authlib.GameProfile(UUID.fromString("d2797f44-2f39-40e7-a71c-908c6bb918da"),"CharacterTest"));
  var n=ProgressionAPI.profile(p);for(String k:Set.copyOf(n.getAllKeys()))n.remove(k);
  check(TitlePerks.current.titles().size()==56 && TitlePerks.current.founderPassives(),"existing title perks plus Founder migration loaded");
  Titles.grant(p,"tnk:prepared");
  double baseHealth=p.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
  check(p.getMaxHealth()==baseHealth+2,"equipped title health perk applied");
  for(int i=0;i<3;i++)TitlePerks.apply(p);
  check(p.getMaxHealth()==baseHealth+2,"perk refresh never stacks");
  p.setHealth(10);Titles.grant(p,"tnk:skill_delver");
  check(CharacterState.action(p,"title","tnk:skill_delver"),"owned cosmetic selectable through GUI action");
  check(Titles.passive(p).equals("tnk:prepared") && p.getMaxHealth()==baseHealth+2,"cosmetic choice preserves passive benefits");
  check(CharacterState.action(p,"passive","tnk:skill_delver"),"owned passive selectable independently");
  check(p.getMaxHealth()==baseHealth && p.getHealth()==10,"unequipped title removed without healing");
  long lock=n.getLong("title_swap_after");
  check(!CharacterState.action(p,"passive","tnk:prepared"),"passive swap cooldown server enforced");
  check(CharacterState.action(p,"title","tnk:prepared") && n.getLong("title_swap_after")==lock,"cosmetic changes ignore and preserve passive cooldown");
  check(CharacterState.action(p,"title",""),"hiding always allowed");
  check(Titles.passive(p).equals("tnk:skill_delver"),"hiding cosmetic preserves passive selection");
  check(CharacterState.action(p,"passive","") && n.getLong("title_swap_after")==lock,"clearing passive never clears cooldown");
  check(!CharacterState.action(p,"passive","tnk:prepared"),"passive clear cannot bypass cooldown");
  n.putLong("title_swap_after",0);
  check(CharacterState.action(p,"passive","tnk:prepared") && p.getHealth()==10,"re-equipping cannot refill health");
  check(Titles.selected(p).isEmpty(),"passive change preserves hidden cosmetic");
  check(!CharacterState.action(p,"title","tnk:monarch"),"GUI cannot equip locked title");
  check(!CharacterState.action(p,"passive","tnk:monarch"),"GUI cannot equip locked passive");
  check(!Titles.earn(p,"tnk:owner") && !Titles.earn(p,"tnk:admin"),"staff titles cannot be earned automatically");
  check(Titles.grant(p,"tnk:owner") && Titles.grant(p,"tnk:admin"),"staff titles accept explicit grants");
  check(CharacterState.action(p,"title","tnk:owner"),"staff badge may be cosmetic during passive cooldown");
  check(Titles.passive(p).equals("tnk:prepared") && p.getMaxHealth()==baseHealth+2,"owner badge neither replaces nor stacks benefits");
  check(!CharacterState.action(p,"passive","tnk:owner") && TitlePerks.forTitle("tnk:owner").isEmpty(),"owner title is always cosmetic only");
  check(!CharacterState.action(p,"passive","tnk:admin") && TitlePerks.forTitle("tnk:admin").isEmpty(),"admin is cosmetic only");
  check(TitlePerks.forTitle("tnk:beta_tester").size()==2,"Founder has exactly two modest benefits");
  check(!CharacterState.action(p,"xp","99999"),"GUI cannot submit XP");
  check(!CharacterState.action(p,"skills","other:category"),"skill-tree button cannot submit a different category or target");
  check(!CharacterState.action(p,"track","invented"),"unknown activity rejected");
  check(CharacterState.action(p,"track","mining") && CharacterState.tracked(p).equals("mining"),"tracking selection persists");
  check(CharacterState.action(p,"hud","off") && !CharacterState.snapshot(p).hud(),"HUD visibility server preference");
  var view=CharacterState.snapshot(p);check(view.activities().size()==7 && view.titles().size()==58 && view.passive().equals("tnk:prepared"),"complete screen snapshot has independent selections");
  var json=CharacterState.JSON.toJson(view);check(CharacterState.JSON.fromJson(json,CharacterState.Snapshot.class).equals(view),"snapshot serialization roundtrip");
  var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
  try{var payload=new CharacterPayload(json);CharacterPayload.CODEC.encode(buffer,payload);check(payload.equals(CharacterPayload.CODEC.decode(buffer)),"screen payload codec");}finally{buffer.release();}
  buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
  try{var action=new CharacterAction("track","fishing");CharacterAction.CODEC.encode(buffer,action);check(action.equals(CharacterAction.CODEC.decode(buffer)),"bounded client action codec");}finally{buffer.release();}
  var saved=ProgressionData.get(server).save(new CompoundTag(),p.registryAccess());var stored=ProgressionData.load(saved,p.registryAccess()).entry("player:"+p.getUUID());
  check(stored.getString("tracked_activity").equals("mining") && stored.getBoolean("hud_hidden"),"tracking and HUD survive reload");
  check(stored.getString("title_selected").equals("tnk:owner") && stored.getString("title_passive").equals("tnk:prepared"),"both slots survive save reload");
  Titles.revoke(p,"tnk:prepared");check(p.getMaxHealth()==baseHealth && Titles.selected(p).equals("tnk:owner") && Titles.passive(p).isEmpty(),"passive revocation removes benefit and preserves cosmetic");
  Titles.revoke(p,"tnk:owner");check(Titles.selected(p).isEmpty(),"cosmetic revocation clears the badge");
  // A pre-split profile retains its old visible title and exactly one source of benefits.
  n.remove("title_slots_split");n.remove("title_passive");n.putString("title_selected","tnk:skill_delver");
  check(Titles.passive(p).equals("tnk:skill_delver") && Titles.selected(p).equals("tnk:skill_delver"),"legacy title migrates into both slots");
  Titles.select(p,"");check(Titles.passive(p).equals("tnk:skill_delver"),"migration is not repeated after hiding");
  Titles.grant(p,"tnk:beta_tester");n.putLong("title_swap_after",0);
  check(Titles.selectPassive(p,"tnk:beta_tester"),"owned Founder can be selected as a passive");
  var movement=p.getAttribute(Attributes.MOVEMENT_SPEED);var breaking=p.getAttribute(Attributes.BLOCK_BREAK_SPEED);
  double baseMove=movement.getBaseValue(),baseBreak=breaking.getBaseValue();
  check(Math.abs(movement.getValue()-baseMove-.003)<1e-8 && Math.abs(breaking.getValue()-baseBreak-.05)<1e-8,"Founder supplies flat vanilla-base 3% movement and 5% breaking benefits");
  var moveId=net.minecraft.resources.ResourceLocation.parse("tnkprogression:test/late_movement");var breakId=net.minecraft.resources.ResourceLocation.parse("tnkprogression:test/late_breaking");
  movement.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(moveId,.2,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
  breaking.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(breakId,4,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
  for(int i=0;i<3;i++)TitlePerks.apply(p);
  check(Math.abs(movement.getValue()-(baseMove+.2+.003))<1e-8 && Math.abs(breaking.getValue()-(baseBreak+4+.05))<1e-8,"late additive bonuses do not grow Founder's flat contribution and refresh does not stack");
  check(Titles.select(p,"tnk:admin") && Titles.passive(p).equals("tnk:beta_tester"),"Founder passive works beneath a staff cosmetic");
  check(Titles.selectPassive(p,"") && Math.abs(movement.getValue()-(baseMove+.2))<1e-8 && Math.abs(breaking.getValue()-(baseBreak+4))<1e-8,"clearing Founder removes exactly its own contributions");
  movement.removeModifier(moveId);breaking.removeModifier(breakId);
  TnkProgression.LOG.info("TNK_FOUNDER_PASSIVES_SMOKE_OK: early bonuses, constant late additive contribution, no stacking and independent staff cosmetic");
  // Migration is proportional: preserve earned level and bar completion, rather than granting levels from a new curve.
  n.putInt("level",50);n.putLong("xp",(100+40L*50*50)/2);n.putLong("mining_rank",10);n.putLong("mining_work",(long)(100*(1+10*.65+10*10*.08)/2));n.putLong("combat_rank",10);n.putLong("combat_work",10);n.putDouble("combat_fraction",.5);n.remove("balance_curve_version");
  ProgressionAPI.profile(p);check(n.getInt("level")==50 && Math.abs(n.getLong("xp")-ProgressionMath.levelCost(50)/2)<=1,"XP curve migration preserves percentage");
  check(Math.abs(n.getLong("mining_work")-ProgressionMath.taskCost(100,10)/2)<=1,"activity curve migration preserves percentage");
  double expected=10.5/(10*(1+10*.65+10*10*.08))*ProgressionMath.taskCost(10,10);
  check(Math.abs(n.getLong("combat_work")+n.getDouble("combat_fraction")-expected)<.00001,"fractional contribution migration preserves percentage");
  TnkProgression.LOG.info("TNK_CHARACTER_SMOKE_OK");
 }
}
