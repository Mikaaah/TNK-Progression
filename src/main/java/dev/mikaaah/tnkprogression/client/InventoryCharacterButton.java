package dev.mikaaah.tnkprogression.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.RecipeBookType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Replace only the survival inventory's vanilla recipe button; JEI and other screen buttons stay independent. */
@EventBusSubscriber(modid="tnkprogression",value=Dist.CLIENT)
public final class InventoryCharacterButton {
 @SubscribeEvent public static void before(ScreenEvent.Init.Pre e){
  if(e.getScreen() instanceof InventoryScreen && Minecraft.getInstance().player!=null){
   // Before init computes leftPos: a previously open book must not shift the inventory.
   Minecraft.getInstance().player.getRecipeBook().setOpen(RecipeBookType.CRAFTING,false);
  }
 }
 @SubscribeEvent public static void after(ScreenEvent.Init.Post e){
  if(!(e.getScreen() instanceof InventoryScreen inventory))return;
  var book=inventory.getRecipeBookComponent();if(book.isVisible())book.toggleVisibility();
  e.removeListener(book);
  int x=inventory.getGuiLeft()+104,y=inventory.height/2-22;
  for(var listener:java.util.List.copyOf(e.getListenersList()))if(listener instanceof ImageButton b && b.getX()==x && b.getY()==y && b.getWidth()==20 && b.getHeight()==18)e.removeListener(b);
  var button=new Portrait(x,y,b->{CharacterClient.action("refresh","");Minecraft.getInstance().setScreen(new CharacterScreen(inventory));});
  button.setTooltip(Tooltip.create(Component.literal("Character · Level, Titles & Activities")));e.addListener(button);
 }
 public static final class Portrait extends Button {
  Portrait(int x,int y,OnPress action){super(x,y,20,18,Component.literal("Open Character"),action,DEFAULT_NARRATION);}
  @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt){
   CharacterTheme.panel(g,getX(),getY(),width,height,isHoveredOrFocused()?CharacterTheme.GOLD:CharacterTheme.BROWN);
   var player=Minecraft.getInstance().player;if(player!=null)PlayerFaceRenderer.draw(g,player.getSkin(),getX()+5,getY()+4,10);
  }
 }
}
