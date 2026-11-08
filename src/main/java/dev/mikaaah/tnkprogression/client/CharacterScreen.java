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
 private final Screen parent;
 public CharacterScreen(){this(null);}
 public CharacterScreen(Screen parent){super(Component.literal("Character"));this.parent=parent;}
 @Override public void onClose(){minecraft.setScreen(parent);}
 private CharacterState.Snapshot state(){return CharacterClientState.current;}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void renderBackground(GuiGraphics g,int mouseX,int mouseY,float delta){}
 @Override protected void init(){layout();rebuild();}
 private void layout(){scale=Math.min(1.35,Math.min((width-28)/(double)W,(height-40)/(double)H));left=(int)((width-W*scale)/2);top=(int)((height-H*scale)/2);}
 private ParchmentButton button(String label,int x,int y,int w,int h,net.minecraft.client.gui.components.Button.OnPress action){return addRenderableWidget(new ParchmentButton(x,y,w,h,Component.literal(label),action));}
 private List<CharacterState.Title> choices(){var s=state();return s==null?List.of():s.titles().stream().filter(t->!ownedOnly||t.earned()).sorted(Comparator.comparing(CharacterState.Title::earned).reversed().thenComparing(CharacterState.Title::label)).toList();}
 private void rebuild(){
  clearWidgets();var s=state();
  button("Character",142,14,88,22,b->{tab=0;rebuild();}).selected(tab==0);button("Titles",234,14,88,22,b->{tab=1;rebuild();}).accent(CharacterTheme.ROSE).selected(tab==1);button("Activities",326,14,88,22,b->{tab=2;rebuild();}).accent(CharacterTheme.PURPLE).selected(tab==2);button("Origin",418,14,92,22,b->{tab=3;rebuild();}).selected(tab==3);
  button("X",W-26,-7,21,18,b->onClose());
  if(s==null)return;
  button(s.hud()?"HUD: Shown":"HUD: Hidden",20,274,104,20,b->CharacterClient.action("hud",s.hud()?"off":"on"));
  if(tab==1){
   var list=choices();if(focus.isEmpty())focus=!s.selected().isEmpty()?s.selected():list.isEmpty()?"":list.get(0).id();int pages=Math.max(1,(list.size()+5)/6);page=Math.clamp(page,0,pages-1);
   button(ownedOnly?"Owned Titles":"All Titles",148,52,174,19,b->{ownedOnly=!ownedOnly;page=0;rebuild();});
   for(int i=page*6;i<Math.min(list.size(),page*6+6);i++){
    var t=list.get(i);int y=78+(i%6)*27;
    button((t.earned()?"":"Locked · ")+t.label(),148,y,174,24,b->{focus=t.id();rebuild();}).accent(t.earned()?CharacterTheme.PURPLE:CharacterTheme.MUTED).selected(focus.equals(t.id()));
   }
   button("<",148,248,30,20,b->{page=Math.max(0,page-1);rebuild();});button(">",292,248,30,20,b->{page=Math.min(pages-1,page+1);rebuild();});
   var t=s.titles().stream().filter(v->v.id().equals(focus)).findFirst().orElse(null);
   var cosmetic=button(t!=null&&s.selected().equals(t.id())?"Cosmetic: Active":"Use Cosmetic",340,248,130,22,b->{if(t!=null)CharacterClient.action("title",t.id());}).selected(t!=null&&s.selected().equals(t.id()));cosmetic.active=t!=null&&t.earned();
   button("X",474,248,30,22,b->CharacterClient.action("title",""));
   boolean cosmeticOnly=t!=null&&Titles.cosmeticOnly(t.id());
   var passive=button(cosmeticOnly?"Cosmetic Only":t!=null&&s.passive().equals(t.id())?"Passive: Active":"Use Passive",340,275,130,22,b->{if(t!=null)CharacterClient.action("passive",t.id());}).selected(t!=null&&s.passive().equals(t.id()));passive.active=t!=null&&t.earned()&&!cosmeticOnly&&(s.swapTicks()==0||s.passive().equals(t.id()));
   button("X",474,275,30,22,b->CharacterClient.action("passive",""));
  }else if(tab==2){for(int i=0;i<s.activities().size();i++){var a=s.activities().get(i);button("Info",394,59+i*33,44,21,b->minecraft.setScreen(new ActivityInfoScreen(this,a.id()))).accent(CharacterTheme.activityColour(a.id()));button(a.id().equals(s.tracked())?"Pinned":"Track",444,59+i*33,62,21,b->CharacterClient.action("track",a.id())).accent(CharacterTheme.activityColour(a.id())).selected(a.id().equals(s.tracked()));}}
  else if(tab==0)button("Skill Tree",394,126,110,22,b->CharacterClient.action("skills","")).accent(CharacterTheme.PURPLE);
 }
 @Override public void tick(){
  var s=state();if(s!=null && s.origin().isEmpty()){minecraft.setScreen(new OriginScreen(this,parent));return;}String signature=s==null?"":s.selected()+s.passive()+s.hud()+s.tracked()+s.origin()+s.titles().hashCode()+(s.swapTicks()==0);
  if(!widgetSignature.equals(signature)){widgetSignature=signature;rebuild();}
 }
 private void text(GuiGraphics g,String s,int x,int y,int colour){CharacterTheme.text(g,font,s,x,y,colour);}
 private void heading(GuiGraphics g,String s,int x,int y,int colour){CharacterTheme.heading(g,font,s,x,y,colour);}
 private void wrap(GuiGraphics g,String s,int x,int y,int w,int colour){g.drawWordWrap(font,Component.literal(s),x,y,w,colour);}
 private Component titleBadge(CharacterState.Title t){return Component.translatableWithFallback(t.translation(),"["+t.label()+"]").withStyle(style->style.withFont(Titles.FONT).withColor(0xFFFFFF).withBold(false).withItalic(false));}
 private void identity(GuiGraphics g,CharacterState.Snapshot s){
  var line=Component.empty().append(LevelBadges.badge(s.level()));
  s.titles().stream().filter(t->t.id().equals(s.selected())).findFirst().ifPresent(t->line.append(Component.literal(" ")).append(titleBadge(t)));
  float fit=Math.min(1f,96f/Math.max(1,font.width(line)));
  g.pose().pushPose();g.pose().translate(20,236,0);g.pose().scale(fit,fit,1);g.drawString(font,line,0,0,0xFFFFFF,false);g.pose().popPose();
  String passive=s.titles().stream().filter(t->t.id().equals(s.passive())).map(CharacterState.Title::label).findFirst().orElse("None");
  text(g,"Passive",20,251,CharacterTheme.LIGHT);text(g,font.plainSubstrByWidth(passive,96),20,262,CharacterTheme.LIGHT);
 }
 @Override public void render(GuiGraphics g,int mx,int my,float delta){
  g.fill(0,0,width,height,0xA020293F);layout();int vx=(int)((mx-left)/scale),vy=(int)((my-top)/scale);
  g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale((float)scale,(float)scale,1);
  CharacterTheme.background(g,"character",0,0,W,H);
  StationHeader.draw(g,font,"Character",14,114);g.fill(21,37,117,38,CharacterTheme.EDGE);
  var s=state();
  if(s==null){text(g,"Loading server progression...",150,80,CharacterTheme.INK);super.render(g,vx,vy,delta);g.pose().popPose();return;}
  g.fillGradient(20,52,120,206,0xFF293349,0xFF20293F);
  if(minecraft.player!=null){
   // Inventory's scissor rectangle uses absolute GUI coordinates, so render outside our virtual pose.
   g.pose().popPose();
   InventoryScreen.renderEntityInInventoryFollowsMouse(g,left+(int)(20*scale),top+(int)(52*scale),left+(int)(120*scale),top+(int)(205*scale),(int)(60*scale),.0625f,mx,my,minecraft.player);
   g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale((float)scale,(float)scale,1);
  }
  heading(g,minecraft.player==null?"Adventurer":font.plainSubstrByWidth(minecraft.player.getName().getString(),94),20,218,CharacterTheme.LIGHT);
  identity(g,s);
  if(tab==0)overview(g,s);else if(tab==1)titles(g,s);else if(tab==2)activities(g,s);else origin(g,s);
  super.render(g,vx,vy,delta);
  if(tab==1 && vx>=474 && vx<504 && vy>=248 && vy<297)g.renderTooltip(font,Component.literal(vy<273?"Hide cosmetic title; keep passive benefits":"Clear passive benefits; keep cosmetic title"),vx,vy);
  g.pose().popPose();
 }
 private void overview(GuiGraphics g,CharacterState.Snapshot s){
  CharacterTheme.section(g,font,"Your Journey",146,50,362,CharacterTheme.PURPLE);heading(g,"Character Level "+s.level(),150,77,CharacterTheme.INK);text(g,"Ceiling "+s.ceiling(),422,77,CharacterTheme.MUTED);
  CharacterTheme.bar(g,150,94,354,12,s.level()>=s.ceiling()?1:s.xp()/(double)s.required(),CharacterTheme.PURPLE);
  text(g,s.level()>=s.ceiling()?s.level()==100?"Maximum level reached":"XP paused at your campaign ceiling":CharacterTheme.number(s.xp())+" / "+CharacterTheme.number(s.required())+" character XP",150,113,CharacterTheme.INK);
  heading(g,"Skill Points",150,130,CharacterTheme.INK);text(g,s.pointsEarned()+" Earned"+(s.pointsSpent()<0?"":" · "+s.pointsSpent()+" Spent"),150,141,CharacterTheme.MUTED);
  wrap(g,"Next Milestone: "+s.gate(),150,159,350,CharacterTheme.MUTED);
  CharacterTheme.panel(g,146,182,362,53,CharacterTheme.CARD);
  s.activities().stream().filter(a->a.id().equals(s.tracked())).findFirst().ifPresent(a->{g.renderItem(CharacterTheme.activityIcon(a.id()),155,191);heading(g,"Tracking · "+CharacterTheme.name(a.id()),178,190,CharacterTheme.INK);text(g,CharacterTheme.number(a.work())+" / "+CharacterTheme.number(a.required())+" Work  ·  +"+CharacterTheme.number(a.reward())+" XP",178,204,CharacterTheme.MUTED);CharacterTheme.bar(g,155,220,342,6,a.work()/a.required(),CharacterTheme.activityColour(a.id()));});
  heading(g,"Recent Milestones",150,245,CharacterTheme.INK);g.fill(150,256,504,257,0x509D7657);
  if(s.recent().isEmpty())text(g,"Complete an activity to begin your journal.",150,263,CharacterTheme.INK);
  for(int i=0;i<Math.min(3,s.recent().size());i++){CharacterTheme.rounded(g,150,264+i*13,3,3,CharacterTheme.PURPLE);text(g,font.plainSubstrByWidth(CharacterTheme.journal(s.recent().get(i)),342),159,261+i*13,CharacterTheme.INK);}
 }
 private void titles(GuiGraphics g,CharacterState.Snapshot s){
  CharacterTheme.panel(g,334,50,174,193,CharacterTheme.CARD);
  var title=s.titles().stream().filter(t->t.id().equals(focus)).findFirst().orElse(null);
  text(g,(page+1)+" / "+Math.max(1,(choices().size()+5)/6),218,254,CharacterTheme.INK);
  if(title==null){wrap(g,"Choose a title to view its perks and requirements.",344,66,152,CharacterTheme.INK);return;}
  CharacterTheme.rounded(g,339,55,164,21,CharacterTheme.BROWN);g.drawString(font,titleBadge(title),344,63,0xFFFFFF,false);heading(g,title.earned()?"Unlocked":"Locked",344,83,title.earned()?CharacterTheme.PURPLE:CharacterTheme.ROSE);
  var desc=font.split(Component.literal(title.description()),152);for(int i=0;i<Math.min(4,desc.size());i++)g.drawString(font,desc.get(i),344,101+i*10,CharacterTheme.INK,false);
  heading(g,"Passive Benefits",344,151,CharacterTheme.PURPLE);
  if(title.perks().isEmpty())wrap(g,Titles.cosmeticOnly(title.id())?"Cosmetic only. Choose another earned title for passive benefits.":"No passive benefits configured.",344,166,152,CharacterTheme.MUTED);
  for(int i=0;i<title.perks().size();i++){var p=title.perks().get(i);var lines=font.split(Component.literal(p.text()+(p.available()?"":" (unavailable)")),152);for(int j=0;j<Math.min(2,lines.size());j++)g.drawString(font,lines.get(j),344,166+i*20+j*9,p.available()?CharacterTheme.INK:CharacterTheme.MUTED,false);}
  if(s.swapTicks()>0)text(g,"Passive ready in "+((s.swapTicks()+19)/20)+"s",344,231,CharacterTheme.MUTED);
 }
 private void activities(GuiGraphics g,CharacterState.Snapshot s){
  for(int i=0;i<s.activities().size();i++){var a=s.activities().get(i);int y=54+i*33;
   CharacterTheme.panel(g,146,y,362,31,i%2==0?CharacterTheme.CARD:0xFF805044);g.fill(148,y+4,150,y+27,CharacterTheme.activityColour(a.id()));g.renderItem(CharacterTheme.activityIcon(a.id()),155,y+6);
   heading(g,font.plainSubstrByWidth(CharacterTheme.name(a.id())+"  ·  Milestone "+(a.rank()+1),208),178,y+4,CharacterTheme.INK);
   text(g,font.plainSubstrByWidth(CharacterTheme.number(a.work())+" / "+CharacterTheme.number(a.required())+"   +"+CharacterTheme.number(a.reward())+" XP",208),178,y+15,CharacterTheme.MUTED);
   CharacterTheme.bar(g,178,y+26,208,2,a.work()/a.required(),CharacterTheme.activityColour(a.id()));
  }
  text(g,"Info shows qualifying actions, recipes and limits.",150,288,CharacterTheme.MUTED);
 }
 private void origin(GuiGraphics g,CharacterState.Snapshot s){
  CharacterTheme.section(g,font,"Your Origin",146,50,362,CharacterTheme.PURPLE);
  var origin=OriginRules.find(s.origin(),s.originRevision()).orElse(null);
  if(origin==null){wrap(g,"This character's saved origin is unavailable. Ask an administrator to check it.",160,90,334,CharacterTheme.MUTED);return;}
  CharacterTheme.panel(g,150,76,354,37,CharacterTheme.CARD);OriginScreen.drawIcon(g,origin,160,86);heading(g,origin.label(),185,87,CharacterTheme.INK);
  wrap(g,origin.story(),160,124,334,CharacterTheme.INK);heading(g,"Origin Perks",160,174,CharacterTheme.PURPLE);
  int y=193;for(String perk:origin.perks()){wrap(g,perk,160,y,334,CharacterTheme.INK);y+=Math.max(26,font.split(Component.literal(perk),334).size()*9+6);}
 }
 @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked((x-left)/scale,(y-top)/scale,button);}
 @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased((x-left)/scale,(y-top)/scale,button);}
 @Override public boolean mouseScrolled(double x,double y,double dx,double dy){if(tab==1){page=Math.max(0,page+(dy<0?1:-1));rebuild();return true;}return super.mouseScrolled((x-left)/scale,(y-top)/scale,dx,dy);}
 @Override public boolean keyPressed(int key,int scan,int modifiers){if(CharacterClient.OPEN.matches(key,scan)){onClose();return true;}return super.keyPressed(key,scan,modifiers);}
}
