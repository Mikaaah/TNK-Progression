package dev.mikaaah.tnkprogression.client;

import dev.mikaaah.tnkprogression.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in integrated-client regression test, exclusively in its disposable test world. */
@EventBusSubscriber(modid="tnkprogression",value=Dist.CLIENT)
public final class TitleSlotsUiSmoke {
 private static int ticks,stage,total;private static boolean seeded;
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tnkprogression.titleSlotsSmoke"))return;
  var mc=Minecraft.getInstance();if(++total>3000){TnkProgression.LOG.error("TNK_TITLE_SLOTS_CLIENT_FAILED: timeout");mc.stop();return;}
  if(mc.player==null || mc.getSingleplayerServer()==null)return;
  try{
   if(!seeded){seeded=true;mc.options.guiScale().set(2);mc.resizeDisplay();
    var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{
     var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);p.setInvulnerable(true);var n=ProgressionAPI.profile(p);
     for(String key:java.util.Set.copyOf(n.getAllKeys()))n.remove(key);
     Origins.open(p);Origins.choose(p,"tnk:librarian");n.putInt("level",100);n.putInt("ceiling",100);n.putLong("xp",0);
     Titles.grant(p,"tnk:prepared");Titles.grant(p,"tnk:owner");Titles.grant(p,"tnk:admin");Titles.grant(p,"tnk:beta_tester");Titles.grant(p,"tnk:ars_mystic");
     Titles.select(p,"tnk:owner");n.putLong("title_swap_after",0);CharacterState.sendFull(p);
    });return;
   }
   var s=CharacterClientState.current;if(s==null || s.level()!=100)return;ticks++;
   if(stage==0 && ticks>30){mc.gui.getChat().clearMessages(true);mc.setScreen(new CharacterScreen());stage=1;ticks=0;}
   else if(stage==1 && ticks>20){check(s.selected().equals("tnk:owner") && s.passive().equals("tnk:prepared"),"independent initial slots");check(mc.player.getMaxHealth()==22,"cosmetic owner retains Survivor health passive");shot(mc,"TNK-Title-Slots-Overview.png");click(mc,300,25);stage=2;ticks=0;}
   else if(stage==2 && ticks>20){shot(mc,"TNK-Title-Slots-Owner.png");click(mc,200,144);click(mc,405,286);stage=3;ticks=0;}
   else if(stage==3 && ticks>30){check(s.passive().equals("tnk:ars_mystic") && s.selected().equals("tnk:owner"),"passive button changes benefits without badge");check(mc.player.getMaxHealth()==21,"Survivor benefit replaced by Mystic's existing +1 health without stacking");click(mc,200,90);click(mc,405,259);stage=4;ticks=0;}
   else if(stage==4 && ticks>30){check(s.selected().equals("tnk:admin") && s.passive().equals("tnk:ars_mystic") && s.swapTicks()>0,"cosmetic button works during passive cooldown");shot(mc,"TNK-Title-Slots-Admin.png");click(mc,489,259);stage=5;ticks=0;}
   else if(stage==5 && ticks>20){check(s.selected().isEmpty() && s.passive().equals("tnk:ars_mystic"),"cosmetic clear keeps passive");click(mc,200,117);click(mc,405,259);stage=6;ticks=0;}
   else if(stage==6 && ticks>20){check(s.selected().equals("tnk:beta_tester") && s.passive().equals("tnk:ars_mystic"),"Founder cosmetic keeps a different passive");mc.options.guiScale().set(4);mc.resizeDisplay();stage=7;ticks=0;}
   else if(stage==7 && ticks>20){check(mc.getWindow().getGuiScale()==4,"effective GUI scale is four, not a window-size clamp");shot(mc,"TNK-Title-Slots-Founder-Scale4.png");click(mc,489,286);stage=8;ticks=0;}
   else if(stage==8 && ticks>20){check(s.passive().isEmpty() && s.selected().equals("tnk:beta_tester") && s.swapTicks()>0,"passive clear keeps cosmetic and lockout");var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);ProgressionAPI.profile(p).putLong("title_swap_after",0);CharacterState.sendFull(p);});stage=9;ticks=0;}
   else if(stage==9 && ticks>20 && s.swapTicks()==0){click(mc,405,286);stage=10;ticks=0;}
   else if(stage==10 && ticks>30){check(s.passive().equals("tnk:beta_tester"),"Founder is selectable in the actual passive UI");check(Math.abs(mc.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-.103)<1e-6 && Math.abs(mc.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED)-1.05)<1e-6,"Founder movement and breaking bonuses reach the client");click(mc,200,171);click(mc,405,259);stage=11;ticks=0;}
   else if(stage==11 && ticks>20){check(s.selected().equals("tnk:owner") && s.passive().equals("tnk:beta_tester"),"Owner badge remains independent of Founder passive");var founder=s.titles().stream().filter(t->t.id().equals("tnk:beta_tester")).findFirst().orElseThrow();check(founder.perks().size()==2 && founder.perks().stream().allMatch(CharacterState.Perk::available),"both Founder perks are available in the menu data");click(mc,200,117);stage=12;ticks=0;}
   else if(stage==12 && ticks>10){shot(mc,"TNK-Founder-Passives.png");TnkProgression.LOG.info("TNK_TITLE_SLOTS_CLIENT_OK: real UI selections, cosmetic during cooldown, stat changes, independent clears and scales 2/4");TnkProgression.LOG.info("TNK_FOUNDER_PASSIVES_CLIENT_OK: two available perks and actual movement/breaking attributes beneath an independent Owner cosmetic");mc.stop();}
  }catch(Throwable ex){TnkProgression.LOG.error("TNK_TITLE_SLOTS_CLIENT_FAILED",ex);mc.stop();}
 }
 private static void click(Minecraft mc,int x,int y){var screen=mc.screen;double scale=Math.min(1.35,Math.min((screen.width-28)/(double)CharacterScreen.W,(screen.height-40)/(double)CharacterScreen.H));int left=(int)((screen.width-CharacterScreen.W*scale)/2),top=(int)((screen.height-CharacterScreen.H*scale)/2);screen.mouseClicked(left+x*scale,top+y*scale,0);screen.mouseReleased(left+x*scale,top+y*scale,0);}
 private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{});}
 private static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);}
}
