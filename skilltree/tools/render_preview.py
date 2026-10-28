"""Draw an exact layout preview with the packaged sprites, not a game screenshot."""
import json, math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
C=ROOT/'datapack/data/tnk/puffish_skills/categories/character'
skills=json.loads((C/'skills.json').read_text())
defs=json.loads((C/'definitions.json').read_text())
edges=json.loads((C/'connections.json').read_text())['normal']['unidirectional']
font='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
bold='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
def f(n,b=False):return ImageFont.truetype(bold if b else font,n)
im=Image.new('RGB',(1600,1800),'#e9dcc1');d=ImageDraw.Draw(im)
d.rounded_rectangle((30,30,1570,1770),radius=24,outline='#9d8159',width=3)
d.text((800,70),'TOWNS & KINGDOMS',font=f(38,True),fill='#3f3023',anchor='mt')
d.text((800,125),'80 skills  /  100 points to spend',font=f(24),fill='#68513b',anchor='mt')
cx,cy,scale=800,855,1.55
pos={k:(round(cx+s['x']*scale),round(cy+s['y']*scale)) for k,s in skills.items()}
colors={'wayfarer':'#607e7f','delver':'#956e38','homesteader':'#65804b','artificer':'#927240','survivor':'#63758c','adventurer':'#955d51','foundation':'#826944'}
for a,b in edges:
    d.line((pos[a],pos[b]),fill='#a28d6b',width=3)
    ax,ay=pos[a];bx,by=pos[b];ang=math.atan2(by-ay,bx-ax);x=ax+(bx-ax)*.65;y=ay+(by-ay)*.65
    d.polygon([(x+6*math.cos(ang),y+6*math.sin(ang)),(x+5*math.cos(ang+2.5),y+5*math.sin(ang+2.5)),(x+5*math.cos(ang-2.5),y+5*math.sin(ang-2.5))],fill='#8b7657')
for k,s in skills.items():
    x,y=pos[k];v=defs[k];cost=v['cost'];group=k.rsplit('_',1)[0];r=30 if cost==4 else 26
    d.rounded_rectangle((x-r,y-r,x+r,y+r),radius=10 if cost==4 else 5,fill='#4a3b2b',outline=colors[group],width=4)
    path=ROOT/'resourcepack/assets/tnk/textures/skilltree'/f'{k}.png'
    sprite=Image.open(path).convert('RGBA').resize((40,40),Image.Resampling.NEAREST)
    im.paste(sprite,(x-20,y-20),sprite)
    d.ellipse((x+r-14,y+r-14,x+r+9,y+r+9),fill='#f5e8c9',outline=colors[group],width=2)
    d.text((x+r-2,y+r-3),str(cost),font=f(14,True),fill='#463523',anchor='mm')
names=['WAYFARER','DELVER','HOMESTEADER','ARTIFICER','SURVIVOR','ADVENTURER']
labels=[(800,185),(1300,410),(1300,1340),(800,1530),(300,1340),(300,410)]
for i,label in enumerate(names):
    x,y=labels[i]
    d.text((x,y),label,font=f(23,True),fill=colors[label.lower()],anchor='mm')
d.text((800,1630),'Icon artwork: Aleksandr Makarov  /  HAS IconPack',font=f(19),fill='#66513b',anchor='mt')
d.text((800,1670),'Layout preview with actual sprites — not an in-game screenshot',font=f(17),fill='#78644a',anchor='mt')
im.save(ROOT/'TNK-Skilltree-HAS-Preview.png')
print(ROOT/'TNK-Skilltree-HAS-Preview.png')
