package dev.mikaaah.tnkprogression.client;

import dev.mikaaah.tnkprogression.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="tnkprogression",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class CharacterClient {
 public static final KeyMapping OPEN=new KeyMapping("key.tnkprogression.character",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_I,"key.categories.tnkprogression");
 @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(OPEN);}
 @SubscribeEvent public static void layers(RegisterGuiLayersEvent e){e.registerAboveAll(ResourceLocation.parse("tnkprogression:character_hud"),(g,delta)->{
  var mc=Minecraft.getInstance();var s=CharacterClientState.current;
  if(mc.player==null || s==null || !s.hud() || mc.options.hideGui || mc.screen!=null || mc.player.isSpectator())return;
  int x=8,y=g.guiHeight()-118,w=194;var f=mc.font;
  CharacterTheme.panel(g,x,y,w,68,CharacterTheme.BROWN);
  net.minecraft.client.gui.components.PlayerFaceRenderer.draw(g,mc.player.getSkin(),x+8,y+6,16);
  g.drawString(f,LevelBadges.badge(s.level()),x+29,y+7,0xFFFFFF,false);CharacterTheme.heading(g,f,f.plainSubstrByWidth(mc.player.getName().getString(),105),x+34+f.width(LevelBadges.badge(s.level())),y+7,CharacterTheme.LIGHT);
  String xp=s.level()>=100?"Maximum level":s.level()>=s.ceiling()?"Capped · complete next chapter":CharacterTheme.number(s.xp())+" / "+CharacterTheme.number(s.required())+" XP";
  CharacterTheme.text(g,f,xp,x+8,y+25,CharacterTheme.LIGHT);CharacterTheme.bar(g,x+8,y+38,w-16,4,s.level()>=s.ceiling()?1:s.xp()/(double)s.required(),CharacterTheme.PURPLE);
  s.activities().stream().filter(a->a.id().equals(s.tracked())).findFirst().ifPresent(a->{CharacterTheme.text(g,f,CharacterTheme.name(a.id())+"  "+CharacterTheme.number(a.work())+" / "+CharacterTheme.number(a.required()),x+8,y+47,CharacterTheme.LIGHT);CharacterTheme.bar(g,x+8,y+59,w-16,4,a.work()/a.required(),CharacterTheme.activityColour(a.id()));});
 });}
 public static void action(String action,String value){PacketDistributor.sendToServer(new CharacterAction(action,value));}
 @EventBusSubscriber(modid="tnkprogression",value=Dist.CLIENT)
 public static final class Ticks {
  @SubscribeEvent public static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();while(OPEN.consumeClick())if(mc.player!=null){if(mc.screen instanceof CharacterScreen)mc.setScreen(null);else if(mc.screen==null){action("refresh","");mc.setScreen(new CharacterScreen());}}}
 }
}
