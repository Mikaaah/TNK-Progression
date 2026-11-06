package dev.mikaaah.tnkprogression.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** Slate, copper and pixel keylines shared by the character sheet and HUD. */
public final class CharacterTheme {
 public static final int BROWN=0xFF446077,EDGE=0xFF729AA1,LIGHT=0xFFF4EBE5,PAPER=0xFF905C49,CARD=0xFF7A4841,INK=0xFFF4EBE5,MUTED=0xFFA9C3C8,GREEN=0xFFA9C887,TEAL=0xFF729AA1,GOLD=0xFFC09473,ROSE=0xFFCF8D7C,PURPLE=0xFF9BAFC6;
 // Small clipped corners, a warm keyline and a single highlight replace heavy square outlines.
 public static void rounded(GuiGraphics g,int x,int y,int w,int h,int colour){g.fill(x+2,y,x+w-2,y+h,colour);g.fill(x+1,y+1,x+w-1,y+h-1,colour);g.fill(x,y+2,x+w,y+h-2,colour);}
 private static int tint(int colour,int target,double amount){int value=colour&0xFF000000;for(int shift=16;shift>=0;shift-=8)value|=(int)Math.round(((colour>>shift)&255)*(1-amount)+((target>>shift)&255)*amount)<<shift;return value;}
 /** Opaque overlapping strips share the same vertical ramp and keep the clipped pixel corners. */
 public static void gradient(GuiGraphics g,int x,int y,int w,int h,int from,int to){g.fillGradient(x+2,y,x+w-2,y+h,from,to);g.fillGradient(x+1,y+1,x+w-1,y+h-1,tint(from,to,1d/h),tint(from,to,(h-1d)/h));g.fillGradient(x,y+2,x+w,y+h-2,tint(from,to,2d/h),tint(from,to,(h-2d)/h));}
 public static void panel(GuiGraphics g,int x,int y,int w,int h,int colour){CleanLookDraw.panel(g,colour==PAPER||colour==CARD||colour==LIGHT?"panel_copper":"button",x,y,w,h);}
 public static void section(GuiGraphics g,Font f,String title,int x,int y,int w,int colour){panel(g,x,y,w,19,BROWN);heading(g,f,title,x+8,y+5,LIGHT);}
 public static void bar(GuiGraphics g,int x,int y,int w,int h,double ratio){bar(g,x,y,w,h,ratio,GREEN);}
 public static void bar(GuiGraphics g,int x,int y,int w,int h,double ratio,int colour){rounded(g,x,y,w,h,BROWN);int filled=(int)((w-2)*Math.clamp(ratio,0,1));g.fill(x+1,y+1,x+w-1,y+h-1,0xFF20293F);if(filled>0){g.fillGradient(x+1,y+1,x+1+filled,y+h-1,tint(colour,0xFFFFFF,.20),tint(colour,0,.10));if(h>4)g.fill(x+1,y+1,x+1+filled,y+2,0x40FFF5CD);}}
 public static int activityColour(String id){return switch(id){case "combat"->ROSE;case "mining"->GOLD;case "fishing"->PURPLE;case "exploration"->TEAL;case "farming"->GREEN;case "crafting"->0xFFA7754D;default->0xFF88739A;};}
 public static net.minecraft.world.item.ItemStack activityIcon(String id){return new net.minecraft.world.item.ItemStack(switch(id){case "combat"->net.minecraft.world.item.Items.IRON_SWORD;case "mining"->net.minecraft.world.item.Items.IRON_PICKAXE;case "fishing"->net.minecraft.world.item.Items.FISHING_ROD;case "exploration"->net.minecraft.world.item.Items.COMPASS;case "farming"->net.minecraft.world.item.Items.WHEAT;case "crafting"->net.minecraft.world.item.Items.CRAFTING_TABLE;default->net.minecraft.world.item.Items.REDSTONE;});}
 public static String name(String id){return id.isEmpty()?"None":Character.toUpperCase(id.charAt(0))+id.substring(1);}
 public static String number(double value){return String.format(java.util.Locale.ROOT,value==Math.floor(value)?"%,.0f":"%,.2f",value);}
 public static void text(GuiGraphics g,Font f,String text,int x,int y,int colour){g.drawString(f,text,x,y,colour,false);}
 public static void heading(GuiGraphics g,Font f,String text,int x,int y,int colour){g.drawString(f,Component.literal(text).withStyle(s->s.withFont(net.minecraft.resources.ResourceLocation.parse("tnkprogression:menu"))),x,y,colour,false);}
 public static void centerHeading(GuiGraphics g,Font f,String text,int center,int y,int colour){var label=Component.literal(text).withStyle(s->s.withFont(net.minecraft.resources.ResourceLocation.parse("tnkprogression:menu")));g.drawString(f,label,center-f.width(label)/2+1,y,colour,false);}
 public static void background(GuiGraphics g,String name,int x,int y,int w,int h){g.blit(net.minecraft.resources.ResourceLocation.parse("tnkprogression:textures/gui/"+name+".png"),x-8,y-16,0f,0f,w+16,h+28,w+16,h+28);}
 public static String journal(String text){return name(text).replace("milestone","Milestone");}
}
