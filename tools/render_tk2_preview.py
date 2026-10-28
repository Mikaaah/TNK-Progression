#!/usr/bin/env python3
"""Render the actual converted data with the existing TNK assets; not an in-game screenshot."""
import json,math
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont,ImageOps
ROOT=Path(__file__).resolve().parents[1];DATA=ROOT/'skilltree/rebuilt588/data/tnk/puffish_skills/categories/character'
nodes=json.loads((DATA/'skills.json').read_text());defs=json.loads((DATA/'definitions.json').read_text());cat=json.loads((DATA/'category.json').read_text());edges=json.loads((DATA/'connections.json').read_text())['normal']['bidirectional']
def asset(id):
 ns,path=id.split(':',1);return ROOT/'skilltree/resourcepack/assets'/ns/path
def font(n):return ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',n)
W,H=2400,2600;cx,cy,scale=1200,1265,1.10
im=ImageOps.fit(Image.open(asset(cat['background']['texture'])).convert('RGB'),(W,H));draw=ImageDraw.Draw(im)
draw.text((cx,45),'TOWNS & KINGDOMS',fill='#ecd4a7',font=font(44),anchor='mt')
draw.text((cx,105),'588 NODES   /   6 DISCIPLINES   /   175 POINTS AT LEVEL 100',fill='#d5bd91',font=font(26),anchor='mt')
pos={id:(round(cx+n['x']*scale),round(cy+n['y']*scale)) for id,n in nodes.items()}
for a,b in edges:
 draw.line((pos[a],pos[b]),fill='#49331f',width=5);draw.line((pos[a],pos[b]),fill='#b38c4f',width=2)
for id,n in nodes.items():
 d=defs[id];x,y=pos[id];size=d['size'];fs=round(26*size*scale);isize=round(16*size*scale)
 frame=Image.open(asset(d['frame']['data']['available'])).convert('RGBA').resize((fs,fs),Image.Resampling.NEAREST)
 icon=Image.open(asset(d['icon']['data']['texture'])).convert('RGBA').resize((isize,isize),Image.Resampling.NEAREST)
 im.paste(frame,(x-fs//2,y-fs//2),frame);im.paste(icon,(x-isize//2,y-isize//2),icon)
for id,n in nodes.items():
 if n['root']:
  angle=math.atan2(n['y'],n['x']);draw.text((cx+190*math.cos(angle),cy+190*math.sin(angle)),id.split('_')[0].title(),fill='#ebd4ac',font=font(19),anchor='mm')
draw.text((cx,2430),'Original 588-node routing • Existing TNK icons, frames and warm background',fill='#dcc39a',font=font(24),anchor='mt')
draw.text((cx,2478),'Rendered from the actual bundled configuration and assets — not a game capture',fill='#c8af89',font=font(22),anchor='mt')
target=ROOT.parent/'deliverables';target.mkdir(exist_ok=True)
im.save(target/'TNK-588-Skilltree-Preview.jpg',quality=94,subsampling=0)
root=nodes['enchanter_class'];px,py=pos['enchanter_class']
im.crop((max(0,px-440),max(160,py-650),min(W,px+660),min(H,py+450))).save(target/'TNK-588-Enchanter-Detail.jpg',quality=95,subsampling=0)
print('Actual-layout previews rendered.')
