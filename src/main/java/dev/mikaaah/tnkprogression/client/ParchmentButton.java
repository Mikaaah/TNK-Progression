package dev.mikaaah.tnkprogression.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class ParchmentButton extends Button {
 public ParchmentButton(int x,int y,int w,int h,Component title,OnPress action){super(x,y,w,h,title,action,DEFAULT_NARRATION);}
 @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
  CharacterTheme.panel(g,getX(),getY(),getWidth(),getHeight(),isHoveredOrFocused()?CharacterTheme.LIGHT:CharacterTheme.CARD);
  var f=Minecraft.getInstance().font;g.drawString(f,getMessage(),getX()+(getWidth()-f.width(getMessage()))/2,getY()+(getHeight()-8)/2,active?CharacterTheme.INK:CharacterTheme.MUTED,false);
 }
}
