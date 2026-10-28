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
  check(TitlePerks.current.titles().size()==43,"all title perks loaded");
  Titles.grant(p,"tnk:prepared");
  double baseHealth=p.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
  check(p.getMaxHealth()==baseHealth+2,"equipped title health perk applied");
  for(int i=0;i<3;i++)TitlePerks.apply(p);
  check(p.getMaxHealth()==baseHealth+2,"perk refresh never stacks");
  p.setHealth(10);Titles.grant(p,"tnk:skill_delver");
  check(CharacterState.action(p,"title","tnk:skill_delver"),"owned title selectable through GUI action");
  check(p.getMaxHealth()==baseHealth && p.getHealth()==10,"unequipped title removed without healing");
  check(!CharacterState.action(p,"title","tnk:prepared"),"title swap cooldown server enforced");
  check(CharacterState.action(p,"title",""),"hiding always allowed");
  check(!CharacterState.action(p,"title","tnk:prepared"),"hiding does not erase cooldown");
  n.putLong("title_swap_after",0);
  check(CharacterState.action(p,"title","tnk:prepared") && p.getHealth()==10,"re-equipping cannot refill health");
  check(!CharacterState.action(p,"title","tnk:monarch"),"GUI cannot equip locked title");
  check(!CharacterState.action(p,"xp","99999"),"GUI cannot submit XP");
  check(!CharacterState.action(p,"track","invented"),"unknown activity rejected");
  check(CharacterState.action(p,"track","mining") && CharacterState.tracked(p).equals("mining"),"tracking selection persists");
  check(CharacterState.action(p,"hud","off") && !CharacterState.snapshot(p).hud(),"HUD visibility server preference");
  var view=CharacterState.snapshot(p);check(view.activities().size()==7 && view.titles().size()==43,"complete screen snapshot");
  var json=CharacterState.JSON.toJson(view);check(CharacterState.JSON.fromJson(json,CharacterState.Snapshot.class).equals(view),"snapshot serialization roundtrip");
  var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
  try{var payload=new CharacterPayload(json);CharacterPayload.CODEC.encode(buffer,payload);check(payload.equals(CharacterPayload.CODEC.decode(buffer)),"screen payload codec");}finally{buffer.release();}
  buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
  try{var action=new CharacterAction("track","fishing");CharacterAction.CODEC.encode(buffer,action);check(action.equals(CharacterAction.CODEC.decode(buffer)),"bounded client action codec");}finally{buffer.release();}
  var saved=ProgressionData.get(server).save(new CompoundTag(),p.registryAccess());var stored=ProgressionData.load(saved,p.registryAccess()).entry("player:"+p.getUUID());
  check(stored.getString("tracked_activity").equals("mining") && stored.getBoolean("hud_hidden"),"tracking and HUD survive reload");
  Titles.revoke(p,"tnk:prepared");check(p.getMaxHealth()==baseHealth,"revocation removes active perk");
  // Migration is proportional: preserve earned level and bar completion, rather than granting levels from a new curve.
  n.putInt("level",50);n.putLong("xp",(100+40L*50*50)/2);n.putLong("mining_rank",10);n.putLong("mining_work",(long)(100*(1+10*.65+10*10*.08)/2));n.putLong("combat_rank",10);n.putLong("combat_work",10);n.putDouble("combat_fraction",.5);n.remove("balance_curve_version");
  ProgressionAPI.profile(p);check(n.getInt("level")==50 && Math.abs(n.getLong("xp")-ProgressionMath.levelCost(50)/2)<=1,"XP curve migration preserves percentage");
  check(Math.abs(n.getLong("mining_work")-ProgressionMath.taskCost(100,10)/2)<=1,"activity curve migration preserves percentage");
  double expected=10.5/(10*(1+10*.65+10*10*.08))*ProgressionMath.taskCost(10,10);
  check(Math.abs(n.getLong("combat_work")+n.getDouble("combat_fraction")-expected)<.00001,"fractional contribution migration preserves percentage");
  TnkProgression.LOG.info("TNK_CHARACTER_SMOKE_OK");
 }
}
