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
 private static int ticks,stage,totalTicks;private static boolean seeded;private static java.util.UUID mobId;private static Double hoverX,hoverY;
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tnkprogression.uiSmoke"))return;
  var mc=Minecraft.getInstance();if(++totalTicks>2400){TnkProgression.LOG.error("TNK_CLIENT_UI_SMOKE_FAILED: timed out");mc.stop();return;}if(mc.player==null || mc.getSingleplayerServer()==null){if(totalTicks%200==0){TnkProgression.LOG.info("TNK_UI_STARTUP: {}",mc.screen==null?"none":mc.screen.getClass().getSimpleName());shot(mc,"TNK-Startup.png");}return;}
  try {
   if(hoverX!=null&&mc.screen!=null){forceMouse(mc,"xpos",hoverX);forceMouse(mc,"ypos",hoverY);}
   if(!seeded){seeded=true;var id=mc.player.getUUID();mc.options.guiScale().set(2);mc.resizeDisplay();
    mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);p.setInvulnerable(true);var n=ProgressionAPI.profile(p);n.putInt("level",42);n.putInt("ceiling",50);n.putLong("xp",(long)(ProgressionMath.levelCost(42)*.62));n.putLong("title_swap_after",0);
     Titles.grant(p,"tnk:ars_mystic");Titles.grant(p,"tnk:prepared");Titles.grant(p,"tnk:skill_delver");Titles.grant(p,"tnk:board_taskmaster");Titles.select(p,"tnk:ars_mystic");n.putLong("title_swap_after",0);
     n.putLong("mining_rank",8);n.putLong("mining_work",112);n.putLong("combat_rank",14);n.putLong("combat_work",16);n.putLong("fishing_work",3);
     var history=new net.minecraft.nbt.ListTag();history.add(net.minecraft.nbt.StringTag.valueOf("mining milestone 8  +"+ProgressionMath.taskReward(ProgressionConfig.current.activities.get("mining").initialXp(),7)+" XP"));n.put("activity_history",history);CharacterState.sendFull(p);
    });return;
   }
   var state=CharacterClientState.current;if(state==null || state.level()!=42)return;
   ticks++;
   if(stage==0 && ticks>40){
    mc.gui.getChat().clearMessages(true);
    mc.player.getRecipeBook().setOpen(net.minecraft.world.inventory.RecipeBookType.CRAFTING,true);mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
    checkInventory(mc);stage=20;ticks=0;
   }
   else if(stage==20 && ticks>15){
    shot(mc,"TNK-Inventory-Character-Button.png");
    var inventory=(net.minecraft.client.gui.screens.inventory.InventoryScreen)mc.screen;
    inventory.mouseClicked(inventory.getGuiLeft()+114,inventory.height/2-13,0);stage=1;ticks=0;
   }
   else if(stage==1 && ticks>25){check(mc.screen instanceof CharacterScreen,"keybind opens screen");shot(mc,"TNK-Character-Overview.png");click(mc,300,25);stage=2;ticks=0;}
   else if(stage==2 && ticks>20){check(mc.screen instanceof CharacterScreen,"titles page remains open");click(mc,190,143);click(mc,420,260);stage=3;ticks=0;}
   else if(stage==3 && ticks>30){check(state.selected().equals("tnk:prepared"),"title button selects through server payload");check(mc.player.getMaxHealth()==22,"selected title perk syncs to client");shot(mc,"TNK-Character-Titles.png");click(mc,420,25);stage=4;ticks=0;}
   else if(stage==4 && ticks>20){click(mc,475,102);stage=5;ticks=0;}
   else if(stage==5 && ticks>25){check(state.tracked().equals("mining"),"activity track button persists via server");shot(mc,"TNK-Character-Activities.png");mc.screen.keyPressed(CharacterClient.OPEN.getKey().getValue(),0,0);checkInventory(mc);mc.setScreen(null);stage=6;ticks=0;}
   else if(stage==6 && ticks>25){check(mc.screen==null,"keybind closes screen");shot(mc,"TNK-Character-HUD.png");stage=7;ticks=0;}
   else if(stage==7 && ticks>30){
    KeyMapping.click(CharacterClient.OPEN.getKey());stage=8;ticks=0;
   }
   else if(stage==8 && ticks>20){
    check(mc.screen instanceof CharacterScreen,"I key still opens character directly");mc.options.guiScale().set(4);mc.resizeDisplay();stage=9;ticks=0;
   }
   else if(stage==9 && ticks>20){
    click(mc,420,25);click(mc,475,102);stage=21;ticks=0;
   }
   else if(stage==21 && ticks>20){
    shot(mc,"TNK-Character-GUI-Scale-4.png");mc.screen.onClose();check(mc.screen==null,"direct-key screen closes to world");mc.options.guiScale().set(2);mc.resizeDisplay();
    var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);for(int x=-5;x<=5;x++)for(int z=-5;z<=8;z++)p.serverLevel().setBlockAndUpdate(new net.minecraft.core.BlockPos(x,99,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());p.teleportTo(0,100,0);p.setYRot(0);p.setXRot(0);
     var z=net.minecraft.world.entity.EntityType.ZOMBIE.create(p.serverLevel());z.setPos(0,100,2.5);z.setNoAi(true);z.setNoGravity(true);z.setInvulnerable(true);z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET));z.getPersistentData().putInt("tnk_level",4);mobId=z.getUUID();p.serverLevel().addFreshEntity(z);
    });stage=10;ticks=0;
   }
   else if(stage==10 && ticks>80){
    var z=mc.level.entitiesForRendering();net.minecraft.world.entity.Entity target=null;for(var candidate:z)if(candidate.getUUID().equals(mobId))target=candidate;
    check(target!=null&&target.getPersistentData().getInt("tnk_level")==4,"real tracking payload supplies mob level without a name");check(!target.hasCustomName(),"client mob is not renamed for display");
    // Center on the fixture without changing production camera or controls.
    mc.player.setYRot(0);mc.player.setXRot(0);mc.player.yRotO=0;mc.player.xRotO=0;mc.getEntityRenderDispatcher().crosshairPickEntity=target;
    stage=11;ticks=0;
   }
   else if(stage==11 && ticks>10){check(MobNameplates.verifiedLines>=2&&MobNameplates.verifiedLevel==4,"actual renderer draws both badge and name lines");shot(mc,"TNK-Mob-Badge.png");CharacterClient.action("skills","");stage=12;ticks=0;}
   else if(stage==12 && ticks>30){hoverSkill(mc,"enchanter_life_1");stage=13;ticks=0;}
   else if(stage==13 && ticks>20){shot(mc,"TNK-Skilltree-Conditional-Tooltip.png");hoverSkill(mc,"enchanter_class");stage=14;ticks=0;}
   else if(stage==14 && ticks>20){shot(mc,"TNK-Skilltree-Mana-Tooltip.png");hoverSkill(mc,"hunter_subclass_1_mastery");stage=15;ticks=0;}
   else if(stage==15 && ticks>20){shot(mc,"TNK-Skilltree-Title-Tooltip.png");stage=16;TnkProgression.LOG.info("TNK_CLIENT_UI_SMOKE_OK: inventory, character UI, native skilltree tooltips, source conditions and mastery title text rendered");mc.stop();}
  }catch(Throwable ex){TnkProgression.LOG.error("TNK_CLIENT_UI_SMOKE_FAILED",ex);mc.stop();}
 }
 private static void hoverSkill(Minecraft mc,String id)throws Exception{
  var screen=mc.screen;check(screen!=null&&screen.getClass().getName().equals("net.puffish.skillsmod.client.gui.SkillsScreen"),"native Pufferfish screen opened");
  var field=screen.getClass().getDeclaredField("optActiveCategoryData");field.setAccessible(true);Object category=((java.util.Optional<?>)field.get(screen)).orElseThrow();
  Object config=category.getClass().getMethod("getConfig").invoke(category);var skills=(java.util.Map<?,?>)config.getClass().getMethod("skills").invoke(config);var node=skills.get(id);check(node!=null,"tooltip node exists");
  int x=(int)node.getClass().getMethod("x").invoke(node),y=(int)node.getClass().getMethod("y").invoke(node);int mx=screen.width/3,my=screen.height/2;
  category.getClass().getMethod("setScale",float.class).invoke(category,1f);category.getClass().getMethod("setX",int.class).invoke(category,mx-screen.width/2-x);category.getClass().getMethod("setY",int.class).invoke(category,my-screen.height/2-y);
  var definitions=(java.util.Map<?,?>)config.getClass().getMethod("definitions").invoke(config);Object def=definitions.get(id);var description=(net.minecraft.network.chat.Component)def.getClass().getMethod("description").invoke(def);check(description.getString().contains("Towns & Kingdoms"),"branch-aware tooltip reaches native client data");
  hoverX=mx*(double)mc.getWindow().getScreenWidth()/screen.width;hoverY=my*(double)mc.getWindow().getScreenHeight()/screen.height;forceMouse(mc,"xpos",hoverX);forceMouse(mc,"ypos",hoverY);
 }
 private static void forceMouse(Minecraft mc,String name,double value)throws Exception{var f=mc.mouseHandler.getClass().getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,value);}
 private static void checkInventory(Minecraft mc){
  check(mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen,"character returns to inventory");var screen=(net.minecraft.client.gui.screens.inventory.InventoryScreen)mc.screen;
  check(!screen.getRecipeBookComponent().isVisible(),"previously open recipe book stays hidden");
  check(screen.children().stream().filter(w->w instanceof InventoryCharacterButton.Portrait).count()==1,"exactly one character replacement after init");
  check(screen.children().stream().noneMatch(w->w instanceof net.minecraft.client.gui.components.ImageButton b&&b.getX()==screen.getGuiLeft()+104&&b.getY()==screen.height/2-22),"vanilla recipe button removed");
 }
 private static void click(Minecraft mc,int x,int y){var s=mc.screen;double scale=Math.min(1.35,Math.min((s.width-12)/(double)CharacterScreen.W,(s.height-12)/(double)CharacterScreen.H));int left=(int)((s.width-CharacterScreen.W*scale)/2),top=(int)((s.height-CharacterScreen.H*scale)/2);s.mouseClicked(left+x*scale,top+y*scale,0);s.mouseReleased(left+x*scale,top+y*scale,0);}
 private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{});}
 private static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);}
}
