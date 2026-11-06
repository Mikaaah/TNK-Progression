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
  CleanLookDraw.panel(g,selected?"panel_copper":!active?"button_disabled":isHoveredOrFocused()?"button_hover":"button",getX(),getY(),getWidth(),getHeight());
  var f=Minecraft.getInstance().font;String text=getMessage().getString();if(f.width(Component.literal(text).withStyle(s->s.withBold(false)))>width-10)text=f.plainSubstrByWidth(text,width-19)+"…";
  var label=Component.literal(text).withStyle(s->s.withBold(false));g.drawString(f,label,getX()+(getWidth()-f.width(label))/2,getY()+(getHeight()-8)/2,active?selected?CharacterTheme.LIGHT:CharacterTheme.INK:CharacterTheme.MUTED,false);
 }
}
