package dev.mikaaah.tnkprogression.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class ParchmentButton extends Button {
 private boolean selected;private int accent=CharacterTheme.PURPLE;
 public ParchmentButton(int x,int y,int w,int h,Component title,OnPress action){super(x,y,w,h,title,action,DEFAULT_NARRATION);}
 public ParchmentButton selected(boolean value){selected=value;return this;}
 public ParchmentButton accent(int colour){accent=colour;return this;}
 @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
  CharacterTheme.panel(g,getX(),getY(),getWidth(),getHeight(),selected?accent:!active?CharacterTheme.PAPER:isHoveredOrFocused()?CharacterTheme.LIGHT:CharacterTheme.CARD);
  if(isHoveredOrFocused()&&active)g.fill(getX()+5,getY()+getHeight()-3,getX()+getWidth()-5,getY()+getHeight()-2,accent);
  var f=Minecraft.getInstance().font;String text=getMessage().getString();if(f.width(Component.literal(text).withStyle(s->s.withBold(true)))>width-10)text=f.plainSubstrByWidth(text,width-19)+"…";
  var label=Component.literal(text).withStyle(s->s.withBold(true));g.drawString(f,label,getX()+(getWidth()-f.width(label))/2,getY()+(getHeight()-8)/2,active?selected?CharacterTheme.LIGHT:CharacterTheme.INK:CharacterTheme.MUTED,false);
 }
}
