"""Render the exact graph and HAS textures; no game screenshot is simulated."""
import json, math
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
C=ROOT/'datapack/data/tnk/puffish_skills/categories/character'
skills=json.loads((C/'skills.json').read_text());defs=json.loads((C/'definitions.json').read_text())
edges=json.loads((C/'connections.json').read_text())['normal']['bidirectional']
meta=json.loads((ROOT/'layout-metadata.json').read_text())
font='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf';bold=font.replace('Sans.ttf','Sans-Bold.ttf')
def f(n,b=False):return ImageFont.truetype(bold if b else font,n)
COLORS={'Wayfarer':'#8bbcc0','Delver':'#d4ac6c','Homesteader':'#9fbd79','Artificer':'#d3bd86','Survivor':'#a5badd','Adventurer':'#d79886','Foundation':'#f1d392','Crossroads':'#b8a9cf'}
W,H=2900,3160;cx,cy,scale=1450,1515,1.17
im=Image.new('RGB',(W,H),'#211c19');d=ImageDraw.Draw(im)
d.rounded_rectangle((35,35,W-35,H-35),radius=28,outline='#645039',width=3)
d.text((cx,70),'TOWNS & KINGDOMS',font=f(52,True),fill='#ead7ad',anchor='mt')
d.text((cx,142),'240 SKILLS  /  EVERY NODE COSTS 1 POINT  /  SPEND UP TO 100',font=f(26),fill='#b9a78b',anchor='mt')
for radius in (290,620,910):
 r=radius*scale;d.ellipse((cx-r,cy-r,cx+r,cy+r),outline='#30271f',width=2)
pos={k:(round(cx+s['x']*scale),round(cy+s['y']*scale)) for k,s in skills.items()}
for a,b in edges:
 color='#89719b' if meta[a]['branch']=='Crossroads' or meta[b]['branch']=='Crossroads' else '#937447'
 d.line((pos[a],pos[b]),fill='#372b20',width=8);d.line((pos[a],pos[b]),fill=color,width=3)
for k,s in skills.items():
 x,y=pos[k];m=meta[k];role=m['role'];color=COLORS[m['branch']]
 r={'minor':17,'notable':22,'hub':25,'keystone':29}[role]
 if s['root']:r=30
 if role=='keystone':
  points=[]
  for i in range(24):
   a=i*math.pi/12;rad=r+6 if i%2==0 else r-1;points.append((x+rad*math.cos(a),y+rad*math.sin(a)))
  d.polygon(points,fill='#aa582e',outline='#e5ac69');d.ellipse((x-r+5,y-r+5,x+r-5,y+r-5),fill='#2c211c',outline='#efb778',width=3)
 elif role=='hub' or s['root']:
  d.ellipse((x-r-4,y-r-4,x+r+4,y+r+4),fill='#483621',outline='#e9c875',width=4)
  d.ellipse((x-r+2,y-r+2,x+r-2,y+r-2),fill='#29221b',outline='#ae8954',width=2)
 elif role=='notable':
  d.rounded_rectangle((x-r-2,y-r-2,x+r+2,y+r+2),radius=9,fill='#332638',outline='#b99ac9',width=3)
 else:d.ellipse((x-r-2,y-r-2,x+r+2,y+r+2),fill='#29231e',outline=color if m['branch']=='Crossroads' else '#8a6c43',width=2)
 size=32 if role=='minor' else 40 if role in ('hub','notable') else 44
 namespace,texture=defs[k]['icon']['data']['texture'].split(':',1)
 icon=Image.open(ROOT/'resourcepack/assets'/namespace/texture).convert('RGBA').resize((size,size),Image.Resampling.NEAREST)
 im.paste(icon,(x-size//2,y-size//2),icon)
for b,name in enumerate(['Wayfarer','Delver','Homesteader','Artificer','Survivor','Adventurer']):
 angle=-math.pi/2+b*math.pi/3;x=cx+1350*math.cos(angle);y=195 if b==0 else cy+1350*math.sin(angle)
 d.text((x,y),name.upper(),font=f(30,True),fill=COLORS[name],anchor='mm')
d.text((cx,2925),'Connected clusters • side routes • shared crossroads • 100-point build limit',font=f(25),fill='#c0b093',anchor='mt')
d.text((cx,2985),'Layout preview with actual HAS sprites — not an in-game screenshot',font=f(23),fill='#a18e75',anchor='mt')
d.text((cx,3030),'Icon artwork: Aleksandr Makarov / HAS IconPack',font=f(21),fill='#a18e75',anchor='mt')
im.save(ROOT/'TNK-Character-Web-Preview.png')
im.crop((1000,200,1900,1395)).save(ROOT/'TNK-Wayfarer-Closeup.png')
print('Rendered overview and Wayfarer close-up from the real graph.')
