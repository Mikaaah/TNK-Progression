"""Deterministic, pixel-native 26px node frames for Pufferfish's renderer."""
import json,math
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'resourcepack/assets/tnk/textures/gui'
STATES=('locked','available','affordable','unlocked','excluded')
ROLES=('minor','notable','hub','keystone')
BASE={'minor':('#927044','#e3bd74'),'notable':('#8b619f','#dfb6e7'),'hub':('#b18b41','#ffe6a2'),'keystone':('#a75f33','#ffcf7e')}
def polygon(role):
    if role=='minor':return [(8,1),(17,1),(24,8),(24,17),(17,24),(8,24),(1,17),(1,8)]
    if role=='notable':return [(5,1),(20,1),(24,5),(24,20),(20,24),(5,24),(1,20),(1,5)]
    if role=='hub':return [(8,0),(17,0),(17,2),(23,7),(25,8),(25,17),(23,17),(18,23),(17,25),(8,25),(8,23),(2,18),(0,17),(0,8),(2,8),(7,2)]
    return [(round(12.5+(12.5 if i%2==0 else 10)*math.cos(i*math.pi/12)),round(12.5+(12.5 if i%2==0 else 10)*math.sin(i*math.pi/12))) for i in range(24)]
def make_frame(role,state):
    im=Image.new('RGBA',(26,26),(0,0,0,0));d=ImageDraw.Draw(im)
    metal,light=BASE[role]
    well='#32291f';shade='#332316';rim=metal;hi=light
    if state=='locked':rim,hi,well='#716357','#a5967c','#393129'
    if state=='excluded':rim,hi,well='#625d58','#827a70','#34312d'
    if state=='affordable':rim,hi=light,'#fff1c6'
    if state=='unlocked':rim,hi=metal,'#fff0ba'
    points=polygon(role)
    d.polygon(points,fill=shade)
    # Outer bevel and inset preserve a clear 16x16 icon area.
    d.line(points+[points[0]],fill=rim,width=2)
    d.line(points[:len(points)//2+1],fill=hi,width=1)
    if role=='minor':
        d.rounded_rectangle((3,3,22,22),radius=7,fill=well,outline=metal if state not in ('locked','excluded') else rim,width=1)
    elif role=='notable':
        d.rounded_rectangle((3,3,22,22),radius=3,fill=well,outline=rim,width=1)
        d.rectangle((11,0,14,2),fill=hi)
    else:
        d.ellipse((3,3,22,22),fill=well,outline=rim,width=1)
        d.point([(12,1),(13,1),(1,12),(24,12),(12,24),(13,24)],fill=hi)
    if state=='affordable':
        d.line((7,1,18,1),fill='#fff0bb',width=1)
        d.point([(2,7),(23,18),(7,23),(18,2)],fill='#ffe1a0')
    if state=='unlocked':
        # Two inset jade pips identify purchased nodes beyond colour alone.
        d.rectangle((9,23,11,24),fill='#71b99c');d.rectangle((14,23,16,24),fill='#71b99c')
    if state=='excluded':d.line((6,23,19,23),fill='#9d7166',width=1)
    return im

def frame_config(role):
    return {'type':'texture','data':{s:f'tnk:textures/gui/frames/{role}_{s}.png' for s in STATES}}

def build():
    target=ASSETS/'frames';target.mkdir(parents=True,exist_ok=True)
    for role in ROLES:
        for state in STATES:make_frame(role,state).save(target/f'{role}_{state}.png')
    theme={
      'background':{'texture':'tnk:textures/gui/character_background.png','width':1254,'height':1254,'position':'fill'},
      'colors':{'connections':{
       'locked':{'fill':'#775c3b','stroke':'#49331f'},
       'available':{'fill':'#b38c4f','stroke':'#50371f'},
       'affordable':{'fill':'#eed18d','stroke':'#745024'},
       'unlocked':{'fill':'#f7e3a4','stroke':'#86602e'},
       'excluded':{'fill':'#857565','stroke':'#4d4339'}},
       'points':{'fill':'#f4e2bc','stroke':'#493422'}},
      'frames':{role:frame_config(role) for role in ROLES},
      'node_sizes':{'minor':1.15,'notable':1.35,'hub':1.6,'keystone':1.8},
      'background_generation':{'mode':'built-in imagegen','prompt_summary':'Nearly solid muted warm brown, exceptionally faint fine paper grain, uniform colour, no scenery, map details, ornaments, border, stains or text.'}
    }
    (ROOT/'theme.json').write_text(json.dumps(theme,indent=2)+'\n')
    return theme
if __name__=='__main__':build();print('Built 20 native 26x26 frame textures and theme configuration.')
