package dev.mikaaah.tnkprogression.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** Warm leather, bronze and restrained parchment, shared by the screen and HUD. */
public final class CharacterTheme {
 public static final int BROWN=0xFF543A32,EDGE=0xFFAD8261,LIGHT=0xFFFFE7BC,PAPER=0xFFE6CAA1,CARD=0xFFF0D8B2,INK=0xFF49342D,MUTED=0xFF7A5B49,GREEN=0xFF69865D,TEAL=0xFF437C79,GOLD=0xFFD3A555,ROSE=0xFF9F5B50,BLUE=0xFF607F99;
 // Small clipped corners, a warm keyline and a single highlight replace heavy square outlines.
 public static void rounded(GuiGraphics g,int x,int y,int w,int h,int colour){g.fill(x+2,y,x+w-2,y+h,colour);g.fill(x+1,y+1,x+w-1,y+h-1,colour);g.fill(x,y+2,x+w,y+h-2,colour);}
 public static void panel(GuiGraphics g,int x,int y,int w,int h,int colour){rounded(g,x+1,y+2,w,h,0x40261C19);rounded(g,x,y,w,h,BROWN);rounded(g,x+1,y+1,w-2,h-2,EDGE);rounded(g,x+2,y+2,w-4,h-4,colour);g.fill(x+4,y+2,x+w-4,y+3,0x40FFF0CE);}
 public static void section(GuiGraphics g,Font f,String title,int x,int y,int w,int colour){rounded(g,x,y,w,19,colour);heading(g,f,title,x+8,y+5,LIGHT);}
 public static void bar(GuiGraphics g,int x,int y,int w,int h,double ratio){bar(g,x,y,w,h,ratio,GREEN);}
 public static void bar(GuiGraphics g,int x,int y,int w,int h,double ratio,int colour){rounded(g,x,y,w,h,BROWN);int filled=(int)((w-2)*Math.clamp(ratio,0,1));g.fill(x+1,y+1,x+w-1,y+h-1,0xFFB79A76);if(filled>0){g.fill(x+1,y+1,x+1+filled,y+h-1,colour);if(h>4)g.fill(x+1,y+1,x+1+filled,y+2,0x60FFF5CD);}}
 public static int activityColour(String id){return switch(id){case "combat"->ROSE;case "mining"->GOLD;case "fishing"->BLUE;case "exploration"->TEAL;case "farming"->GREEN;case "crafting"->0xFFA7754D;default->0xFF88739A;};}
 public static net.minecraft.world.item.ItemStack activityIcon(String id){return new net.minecraft.world.item.ItemStack(switch(id){case "combat"->net.minecraft.world.item.Items.IRON_SWORD;case "mining"->net.minecraft.world.item.Items.IRON_PICKAXE;case "fishing"->net.minecraft.world.item.Items.FISHING_ROD;case "exploration"->net.minecraft.world.item.Items.COMPASS;case "farming"->net.minecraft.world.item.Items.WHEAT;case "crafting"->net.minecraft.world.item.Items.CRAFTING_TABLE;default->net.minecraft.world.item.Items.REDSTONE;});}
 public static String name(String id){return id.isEmpty()?"None":Character.toUpperCase(id.charAt(0))+id.substring(1);}
 public static String number(double value){return String.format(java.util.Locale.ROOT,value==Math.floor(value)?"%,.0f":"%,.2f",value);}
 public static void text(GuiGraphics g,Font f,String text,int x,int y,int colour){g.drawString(f,text,x,y,colour,false);}
 public static void heading(GuiGraphics g,Font f,String text,int x,int y,int colour){g.drawString(f,Component.literal(text).withStyle(s->s.withBold(true)),x,y,colour,false);}
 public static String journal(String text){return name(text).replace("milestone","Milestone");}
}
