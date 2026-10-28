package dev.mikaaah.tnkprogression.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/** Warm leather, bronze and restrained parchment, shared by the screen and HUD. */
public final class CharacterTheme {
 public static final int BROWN=0xFF493426,EDGE=0xFF9C7848,LIGHT=0xFFE8D8B6,PAPER=0xFFD4BE93,CARD=0xFFC5AA7D,INK=0xFF3E2D22,MUTED=0xFF756043,GREEN=0xFF65815B;
 public static void panel(GuiGraphics g,int x,int y,int w,int h,int colour){g.fill(x,y,x+w,y+h,BROWN);g.fill(x+1,y+1,x+w-1,y+h-1,EDGE);g.fill(x+2,y+2,x+w-2,y+h-2,colour);}
 public static void bar(GuiGraphics g,int x,int y,int w,int h,double ratio){g.fill(x,y,x+w,y+h,BROWN);g.fill(x+1,y+1,x+w-1,y+h-1,0xFF9C8768);g.fill(x+1,y+1,x+1+(int)((w-2)*Math.clamp(ratio,0,1)),y+h-1,GREEN);}
 public static String name(String id){return id.isEmpty()?"None":Character.toUpperCase(id.charAt(0))+id.substring(1);}
 public static String number(double value){return String.format(java.util.Locale.ROOT,value==Math.floor(value)?"%,.0f":"%,.2f",value);}
 public static void text(GuiGraphics g,Font f,String text,int x,int y,int colour){g.drawString(f,text,x,y,colour,false);}
}
