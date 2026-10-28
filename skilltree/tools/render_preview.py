"""Render the real graph with the exact packaged background, icons and frames."""
import json,math
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont,ImageOps
ROOT=Path(__file__).resolve().parents[1]
C=ROOT/'datapack/data/tnk/puffish_skills/categories/character'
skills=json.loads((C/'skills.json').read_text());defs=json.loads((C/'definitions.json').read_text())
edges=json.loads((C/'connections.json').read_text())['normal']['bidirectional']
meta=json.loads((ROOT/'layout-metadata.json').read_text());theme=json.loads((ROOT/'theme.json').read_text())
font='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf';bold=font.replace('Sans.ttf','Sans-Bold.ttf')
def f(n,b=False):return ImageFont.truetype(bold if b else font,n)
def asset(identifier):
 ns,path=identifier.split(':',1);return ROOT/'resourcepack/assets'/ns/path
background=Image.open(asset(theme['background']['texture'])).convert('RGB')
W,H=2900,3160;cx,cy,scale=1450,1515,1.17
im=ImageOps.fit(background,(W,H),method=Image.Resampling.LANCZOS);d=ImageDraw.Draw(im)
d.rounded_rectangle((35,35,W-35,H-35),radius=28,outline='#6b4829',width=3)
d.text((cx,70),'TOWNS & KINGDOMS',font=f(52,True),fill='#49301d',anchor='mt',stroke_width=1,stroke_fill='#d5ad71')
d.text((cx,142),'240 SKILLS  /  EVERY NODE COSTS 1 POINT  /  SPEND UP TO 100',font=f(26),fill='#553b24',anchor='mt')
pos={k:(round(cx+s['x']*scale),round(cy+s['y']*scale)) for k,s in skills.items()}
for a,b in edges:
 d.line((pos[a],pos[b]),fill=theme['colors']['connections']['available']['stroke'],width=6)
 d.line((pos[a],pos[b]),fill=theme['colors']['connections']['available']['fill'],width=3)
for k,s in skills.items():
 x,y=pos[k];definition=defs[k];size=definition['size']
 # Same 26px frame / 16px icon ratio observed in the uploaded JAR's renderer.
 frame_px=round(26*size*scale);icon_px=round(16*size*scale)
 frame=Image.open(asset(definition['frame']['data']['available'])).convert('RGBA').resize((frame_px,frame_px),Image.Resampling.NEAREST)
 im.paste(frame,(x-frame_px//2,y-frame_px//2),frame)
 icon=Image.open(asset(definition['icon']['data']['texture'])).convert('RGBA').resize((icon_px,icon_px),Image.Resampling.NEAREST)
 im.paste(icon,(x-icon_px//2,y-icon_px//2),icon)
for b,name in enumerate(['Wayfarer','Delver','Homesteader','Artificer','Survivor','Adventurer']):
 angle=-math.pi/2+b*math.pi/3;x=cx+1350*math.cos(angle);y=195 if b==0 else cy+1350*math.sin(angle)
 d.text((x,y),name.upper(),font=f(30,True),fill='#4c321d',anchor='mm',stroke_width=2,stroke_fill='#e5c590')
d.text((cx,2925),'Custom atlas background • native pixel frames • all nodes shown in available style',font=f(25),fill='#4f3521',anchor='mt',stroke_width=1,stroke_fill='#dbb77e')
d.text((cx,2985),'Preview rendered from the packaged assets — not an in-game screenshot',font=f(23),fill='#5a3d26',anchor='mt',stroke_width=1,stroke_fill='#dbb77e')
d.text((cx,3030),'Icons: Aleksandr Makarov / HAS IconPack',font=f(21),fill='#5a3d26',anchor='mt',stroke_width=1,stroke_fill='#dbb77e')
im.save(ROOT/'TNK-Character-Web-Preview.jpg',quality=94,subsampling=0)
im.crop((1000,200,1900,1395)).resize((1350,1793),Image.Resampling.NEAREST).save(ROOT/'TNK-Wayfarer-Closeup.jpg',quality=95,subsampling=0)
# Literal contact sheet of all 20 client frame sprites, with HAS icons overlaid.
canvas=ImageOps.fit(background,(1660,1060),method=Image.Resampling.LANCZOS);draw=ImageDraw.Draw(canvas)
draw.text((830,35),'TNK NODE FRAMES',font=f(38,True),fill='#4c321d',anchor='mt')
draw.text((830,86),'Actual 26px textures, enlarged with nearest-neighbour scaling',font=f(20),fill='#5a3d26',anchor='mt')
roles=['minor','notable','hub','keystone'];states=['locked','available','affordable','unlocked','excluded']
examples=['delver_01','adventurer_03','foundation_0','wayfarer_10']
for j,state in enumerate(states):draw.text((350+j*265,150),state.upper(),font=f(21,True),fill='#4c321d',anchor='mm')
for i,role in enumerate(roles):
 y=270+i*190;draw.text((65,y),role.upper(),font=f(24,True),fill='#4c321d',anchor='lm')
 for j,state in enumerate(states):
  x=350+j*265;frame=Image.open(asset(theme['frames'][role]['data'][state])).convert('RGBA').resize((130,130),Image.Resampling.NEAREST)
  canvas.paste(frame,(x-65,y-65),frame)
  icon=Image.open(asset(defs[examples[i]]['icon']['data']['texture'])).convert('RGBA').resize((80,80),Image.Resampling.NEAREST)
  canvas.paste(icon,(x-40,y-40),icon)
draw.text((830,1010),'Every frame uses the same 1-point cost; shapes indicate role, not price.',font=f(20),fill='#4c321d',anchor='mm')
canvas.save(ROOT/'TNK-Frame-States.jpg',quality=95,subsampling=0)
print('Rendered exact-asset overview, close-up and 20-state frame sheet.')
