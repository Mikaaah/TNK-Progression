package dev.mikaaah.tnkprogression.client;

import dev.mikaaah.tnkprogression.LevelBadges;
import dev.mikaaah.tnkprogression.ProgressionAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

/** Two centered, depth-tested lines using the same unmodified number artwork as chat. */
@EventBusSubscriber(modid="tnkprogression",value=Dist.CLIENT)
public final class MobNameplates {
 private static final boolean VERIFY=Boolean.getBoolean("tnkprogression.uiSmoke");
 static int verifiedLines,verifiedLevel;
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void render(RenderNameTagEvent e){
  if(!(e.getEntity() instanceof Mob mob) || !mob.getPersistentData().contains("tnk_level") || e.canRender()==TriState.FALSE)return;
  var mc=Minecraft.getInstance();var dispatcher=mc.getEntityRenderDispatcher();
  if(e.canRender()==TriState.DEFAULT&&!mob.shouldShowName()&&mob!=dispatcher.crosshairPickEntity)return;
  // Match vanilla's mob name visibility, including invisibility, passengers and team rules.
  if(mc.player==null || !Minecraft.renderNames() || mob==mc.getCameraEntity() || mob.isInvisibleTo(mc.player) || mob.isVehicle())return;
  double range=mob.isDiscrete()?32:64;if(dispatcher.distanceToSqr(mob)>=range*range)return;
  var team=mob.getTeam();var own=mc.player.getTeam();
  if(team!=null)switch(team.getNameTagVisibility()){
   case NEVER->{return;}
   case HIDE_FOR_OTHER_TEAMS->{if(own!=null&&!team.isAlliedTo(own))return;}
   case HIDE_FOR_OWN_TEAM->{if(own!=null&&team.isAlliedTo(own))return;}
   default->{}
  }
  var anchor=mob.getAttachments().getNullable(EntityAttachment.NAME_TAG,0,mob.getViewYRot(e.getPartialTick()));if(anchor==null)return;
  var pose=e.getPoseStack();pose.pushPose();pose.translate(anchor.x,anchor.y+.5,anchor.z);pose.mulPose(dispatcher.cameraOrientation());pose.scale(.025f,-.025f,.025f);
  var font=e.getEntityRenderer().getFont();int background=(int)(mc.options.getBackgroundOpacity(.25f)*255)<<24;
  line(e,font,LevelBadges.badge(ProgressionAPI.mobLevel(mob)),-12,0);
  line(e,font,e.getContent(),0,background);
  if(VERIFY){verifiedLines+=2;verifiedLevel=ProgressionAPI.mobLevel(mob);}
  pose.popPose();e.setCanRender(TriState.FALSE);
 }
 private static void line(RenderNameTagEvent e,Font font,Component text,int y,int background){
  font.drawInBatch(text,-font.width(text)/2f,y,0xFFFFFFFF,false,e.getPoseStack().last().pose(),e.getMultiBufferSource(),Font.DisplayMode.NORMAL,background,e.getPackedLight());
 }
}
