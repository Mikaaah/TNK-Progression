package dev.mikaaah.tnkprogression.client;

import dev.mikaaah.tnkprogression.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Screenshot;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in disposable integrated-client test; never runs during ordinary gameplay. */
@EventBusSubscriber(modid="tnkprogression",value=Dist.CLIENT)
public final class ClientUiSmoke {
 private static int ticks,stage,totalTicks;private static boolean seeded;
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tnkprogression.uiSmoke"))return;
  var mc=Minecraft.getInstance();if(++totalTicks>2400){TnkProgression.LOG.error("TNK_CLIENT_UI_SMOKE_FAILED: timed out");mc.stop();return;}if(mc.player==null || mc.getSingleplayerServer()==null)return;
  try {
   if(!seeded){seeded=true;var id=mc.player.getUUID();mc.options.guiScale().set(2);mc.resizeDisplay();
    mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);p.setInvulnerable(true);var n=ProgressionAPI.profile(p);n.putInt("level",42);n.putInt("ceiling",50);n.putLong("xp",(long)(ProgressionMath.levelCost(42)*.62));n.putLong("title_swap_after",0);
     Titles.grant(p,"tnk:ars_mystic");Titles.grant(p,"tnk:prepared");Titles.grant(p,"tnk:skill_delver");Titles.grant(p,"tnk:board_taskmaster");Titles.select(p,"tnk:ars_mystic");n.putLong("title_swap_after",0);
     n.putLong("mining_rank",8);n.putLong("mining_work",112);n.putLong("combat_rank",14);n.putLong("combat_work",16);n.putLong("fishing_work",3);
     var history=new net.minecraft.nbt.ListTag();history.add(net.minecraft.nbt.StringTag.valueOf("mining milestone 8  +"+ProgressionMath.taskReward(ProgressionConfig.current.activities.get("mining").initialXp(),7)+" XP"));n.put("activity_history",history);CharacterState.sendFull(p);
    });return;
   }
   var state=CharacterClientState.current;if(state==null || state.level()!=42)return;
   ticks++;
   if(stage==0 && ticks>40){KeyMapping.click(CharacterClient.OPEN.getKey());stage=1;ticks=0;}
   else if(stage==1 && ticks>25){check(mc.screen instanceof CharacterScreen,"keybind opens screen");shot(mc,"TNK-Character-Overview.png");click(mc,300,25);stage=2;ticks=0;}
   else if(stage==2 && ticks>20){check(mc.screen instanceof CharacterScreen,"titles page remains open");click(mc,190,143);click(mc,420,260);stage=3;ticks=0;}
   else if(stage==3 && ticks>30){check(state.selected().equals("tnk:prepared"),"title button selects through server payload");check(mc.player.getMaxHealth()==22,"selected title perk syncs to client");shot(mc,"TNK-Character-Titles.png");click(mc,420,25);stage=4;ticks=0;}
   else if(stage==4 && ticks>20){click(mc,475,102);stage=5;ticks=0;}
   else if(stage==5 && ticks>25){check(state.tracked().equals("mining"),"activity track button persists via server");shot(mc,"TNK-Character-Activities.png");mc.screen.keyPressed(CharacterClient.OPEN.getKey().getValue(),0,0);stage=6;ticks=0;}
   else if(stage==6 && ticks>25){check(mc.screen==null,"keybind closes screen");shot(mc,"TNK-Character-HUD.png");stage=7;ticks=0;}
   else if(stage==7 && ticks>30){TnkProgression.LOG.info("TNK_CLIENT_UI_SMOKE_OK");mc.stop();}
  }catch(Throwable ex){TnkProgression.LOG.error("TNK_CLIENT_UI_SMOKE_FAILED",ex);mc.stop();}
 }
 private static void click(Minecraft mc,int x,int y){var s=mc.screen;double scale=Math.min(1.35,Math.min((s.width-12)/(double)CharacterScreen.W,(s.height-12)/(double)CharacterScreen.H));int left=(int)((s.width-CharacterScreen.W*scale)/2),top=(int)((s.height-CharacterScreen.H*scale)/2);s.mouseClicked(left+x*scale,top+y*scale,0);s.mouseReleased(left+x*scale,top+y*scale,0);}
 private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{});}
 private static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);}
}
