package dev.mikaaah.tnkprogression.client;

import dev.mikaaah.tnkprogression.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Virtual coordinates keep the full character sheet usable at different GUI scales. */
public final class CharacterScreen extends Screen {
 public static final int W=526,H=312;
 private int tab,page;private boolean ownedOnly;private String focus="";private double scale;private int left,top;
 private String widgetSignature="";
 public CharacterScreen(){super(Component.literal("Character"));}
 private CharacterState.Snapshot state(){return CharacterClientState.current;}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void renderBackground(GuiGraphics g,int mouseX,int mouseY,float delta){}
 @Override protected void init(){layout();rebuild();}
 private void layout(){scale=Math.min(1.35,Math.min((width-12)/(double)W,(height-12)/(double)H));left=(int)((width-W*scale)/2);top=(int)((height-H*scale)/2);}
 private ParchmentButton button(String label,int x,int y,int w,int h,net.minecraft.client.gui.components.Button.OnPress action){return addRenderableWidget(new ParchmentButton(x,y,w,h,Component.literal(label),action));}
 private List<CharacterState.Title> choices(){var s=state();return s==null?List.of():s.titles().stream().filter(t->!ownedOnly||t.earned()).sorted(Comparator.comparing(CharacterState.Title::earned).reversed().thenComparing(CharacterState.Title::label)).toList();}
 private void rebuild(){
  clearWidgets();var s=state();
  button("Character",142,14,116,22,b->{tab=0;rebuild();});button("Titles",262,14,116,22,b->{tab=1;rebuild();});button("Activities",382,14,128,22,b->{tab=2;rebuild();});
  button("X",W-24,0,24,12,b->onClose());
  if(s==null)return;
  button(s.hud()?"HUD: shown":"HUD: hidden",20,274,104,20,b->CharacterClient.action("hud",s.hud()?"off":"on"));
  if(tab==1){
   var list=choices();if(focus.isEmpty())focus=!s.selected().isEmpty()?s.selected():list.isEmpty()?"":list.get(0).id();int pages=Math.max(1,(list.size()+5)/6);page=Math.clamp(page,0,pages-1);
   button(ownedOnly?"Owned titles":"All titles",148,52,174,19,b->{ownedOnly=!ownedOnly;page=0;rebuild();});
   for(int i=page*6;i<Math.min(list.size(),page*6+6);i++){
    var t=list.get(i);int y=78+(i%6)*27;
    button((t.earned()?"":"Locked · ")+t.label(),148,y,174,24,b->{focus=t.id();rebuild();});
   }
   button("<",148,248,30,20,b->{page=Math.max(0,page-1);rebuild();});button(">",292,248,30,20,b->{page=Math.min(pages-1,page+1);rebuild();});
   var t=s.titles().stream().filter(v->v.id().equals(focus)).findFirst().orElse(null);
   var equip=button(t!=null&&s.selected().equals(t.id())?"Equipped":"Equip title",340,250,164,22,b->{if(t!=null)CharacterClient.action("title",t.id());});equip.active=t!=null&&t.earned()&&(s.swapTicks()==0||s.selected().equals(t.id()));
   button("Hide title",340,277,164,19,b->CharacterClient.action("title",""));
  }else if(tab==2){for(int i=0;i<s.activities().size();i++){var a=s.activities().get(i);button(a.id().equals(s.tracked())?"Pinned":"Track",444,58+i*32,62,25,b->CharacterClient.action("track",a.id()));}}
 }
 @Override public void tick(){
  var s=state();String signature=s==null?"":s.selected()+s.hud()+s.tracked()+s.titles().hashCode()+(s.swapTicks()==0);
  if(!widgetSignature.equals(signature)){widgetSignature=signature;rebuild();}
 }
 private void text(GuiGraphics g,String s,int x,int y,int colour){CharacterTheme.text(g,font,s,x,y,colour);}
 private void wrap(GuiGraphics g,String s,int x,int y,int w,int colour){g.drawWordWrap(font,Component.literal(s),x,y,w,colour);}
 private Component titleBadge(CharacterState.Title t){return Component.translatableWithFallback(t.translation(),"["+t.label()+"]").withStyle(style->style.withFont(Titles.FONT).withColor(0xFFFFFF).withBold(false).withItalic(false));}
 @Override public void render(GuiGraphics g,int mx,int my,float delta){
  g.fill(0,0,width,height,0xA0251D16);layout();int vx=(int)((mx-left)/scale),vy=(int)((my-top)/scale);
  g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale((float)scale,(float)scale,1);
  CharacterTheme.panel(g,0,0,W,H,CharacterTheme.PAPER);CharacterTheme.panel(g,6,6,126,296,CharacterTheme.BROWN);
  text(g,"CHARACTER",22,22,CharacterTheme.LIGHT);
  var s=state();
  if(s==null){text(g,"Loading server progression...",150,80,CharacterTheme.INK);super.render(g,vx,vy,delta);g.pose().popPose();return;}
  CharacterTheme.panel(g,16,48,108,162,0xFF6C5038);
  if(minecraft.player!=null){
   // Inventory's scissor rectangle uses absolute GUI coordinates, so render outside our virtual pose.
   g.pose().popPose();
   InventoryScreen.renderEntityInInventoryFollowsMouse(g,left+(int)(20*scale),top+(int)(52*scale),left+(int)(120*scale),top+(int)(205*scale),(int)(60*scale),.0625f,mx,my,minecraft.player);
   g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale((float)scale,(float)scale,1);
  }
  text(g,minecraft.player==null?"Adventurer":font.plainSubstrByWidth(minecraft.player.getName().getString(),102),20,218,CharacterTheme.LIGHT);
  text(g,"Level "+s.level()+" / 100",20,233,CharacterTheme.LIGHT);
  s.titles().stream().filter(t->t.id().equals(s.selected())).findFirst().ifPresentOrElse(t->g.drawString(font,titleBadge(t),20,250,0xFFFFFF,false),()->text(g,"No title equipped",20,250,CharacterTheme.LIGHT));
  if(tab==0)overview(g,s);else if(tab==1)titles(g,s);else activities(g,s);
  super.render(g,vx,vy,delta);g.pose().popPose();
 }
 private void overview(GuiGraphics g,CharacterState.Snapshot s){
  text(g,"YOUR JOURNEY",150,56,CharacterTheme.INK);text(g,"Character level "+s.level(),150,77,CharacterTheme.INK);text(g,"Ceiling "+s.ceiling(),422,77,CharacterTheme.MUTED);
  CharacterTheme.bar(g,150,94,354,12,s.level()>=s.ceiling()?1:s.xp()/(double)s.required());
  text(g,s.level()>=s.ceiling()?s.level()==100?"Maximum level reached":"XP paused at your campaign ceiling":CharacterTheme.number(s.xp())+" / "+CharacterTheme.number(s.required())+" character XP",150,113,CharacterTheme.INK);
  text(g,"Skill points: "+s.pointsEarned()+" earned"+(s.pointsSpent()<0?"":" · "+s.pointsSpent()+" spent"),150,133,CharacterTheme.INK);
  wrap(g,"Next unlock: "+s.gate(),150,151,350,CharacterTheme.MUTED);
  CharacterTheme.panel(g,146,182,362,53,CharacterTheme.CARD);
  s.activities().stream().filter(a->a.id().equals(s.tracked())).findFirst().ifPresent(a->{text(g,"TRACKING · "+CharacterTheme.name(a.id()),155,190,CharacterTheme.INK);text(g,CharacterTheme.number(a.work())+" / "+CharacterTheme.number(a.required())+" work  ·  +"+CharacterTheme.number(a.reward())+" XP",155,204,CharacterTheme.INK);CharacterTheme.bar(g,155,220,342,5,a.work()/a.required());});
  text(g,"RECENT MILESTONES",150,245,CharacterTheme.MUTED);
  if(s.recent().isEmpty())text(g,"Complete an activity to begin your journal.",150,263,CharacterTheme.INK);
  for(int i=0;i<Math.min(3,s.recent().size());i++)text(g,font.plainSubstrByWidth(CharacterTheme.name(s.recent().get(i)),350),150,261+i*13,CharacterTheme.INK);
 }
 private void titles(GuiGraphics g,CharacterState.Snapshot s){
  CharacterTheme.panel(g,334,50,174,193,CharacterTheme.CARD);
  var title=s.titles().stream().filter(t->t.id().equals(focus)).findFirst().orElse(null);
  text(g,(page+1)+" / "+Math.max(1,(choices().size()+5)/6),218,254,CharacterTheme.INK);
  if(title==null){wrap(g,"Choose a title to view its perks and requirements.",344,66,152,CharacterTheme.INK);return;}
  g.drawString(font,titleBadge(title),344,63,0xFFFFFF,false);text(g,title.earned()?"UNLOCKED":"LOCKED",344,83,CharacterTheme.MUTED);
  var desc=font.split(Component.literal(title.description()),152);for(int i=0;i<Math.min(4,desc.size());i++)g.drawString(font,desc.get(i),344,101+i*10,CharacterTheme.INK,false);
  text(g,"WHILE EQUIPPED",344,151,CharacterTheme.MUTED);
  for(int i=0;i<title.perks().size();i++){var p=title.perks().get(i);var lines=font.split(Component.literal(p.text()+(p.available()?"":" (unavailable)")),152);for(int j=0;j<Math.min(2,lines.size());j++)g.drawString(font,lines.get(j),344,166+i*20+j*9,p.available()?CharacterTheme.INK:CharacterTheme.MUTED,false);}
  if(s.swapTicks()>0)text(g,"Swap ready in "+((s.swapTicks()+19)/20)+"s",344,231,CharacterTheme.MUTED);
 }
 private void activities(GuiGraphics g,CharacterState.Snapshot s){
  for(int i=0;i<s.activities().size();i++){var a=s.activities().get(i);int y=55+i*32;
   text(g,CharacterTheme.name(a.id())+"  ·  milestone "+(a.rank()+1),150,y,CharacterTheme.INK);
   text(g,CharacterTheme.number(a.work())+" / "+CharacterTheme.number(a.required())+"   +"+CharacterTheme.number(a.reward())+" XP",150,y+11,CharacterTheme.MUTED);
   CharacterTheme.bar(g,150,y+24,280,4,a.work()/a.required());
  }
  text(g,"Activities remain repeatable. Campaign ceilings still apply.",150,288,CharacterTheme.MUTED);
 }
 @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked((x-left)/scale,(y-top)/scale,button);}
 @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased((x-left)/scale,(y-top)/scale,button);}
 @Override public boolean mouseScrolled(double x,double y,double dx,double dy){if(tab==1){page=Math.max(0,page+(dy<0?1:-1));rebuild();return true;}return super.mouseScrolled((x-left)/scale,(y-top)/scale,dx,dy);}
 @Override public boolean keyPressed(int key,int scan,int modifiers){if(CharacterClient.OPEN.matches(key,scan)){onClose();return true;}return super.keyPressed(key,scan,modifiers);}
}
