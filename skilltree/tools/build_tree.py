"""Generate the TNK 1.21.1 Pufferfish attribute-tree draft, reproducibly."""
import json, math, zipfile, re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / 'datapack'
OUT = PACK / 'data/tnk/puffish_skills/categories/character'
OUT.mkdir(parents=True, exist_ok=True)
ICON_MAP = json.loads((ROOT / 'icon-map.json').read_text())

def icon_for(key):
    texture = ICON_MAP[key]['texture']
    namespace, path = texture.split(':', 1)
    assert (ROOT / 'resourcepack/assets' / namespace / path).is_file(), texture
    return {'type': 'texture', 'data': {'texture': texture}}

def attr(name, value, operation='multiply_base'):
    return {'type':'puffish_skills:attribute','data':{'attribute':name if ':' in name else 'puffish_attributes:'+name,'value':value,'operation':operation}}

def node(title, attribute, value, description, operation='multiply_base', icon=None):
    return title, attribute, value, description, operation, icon

# Original passives retained as anchors in the expanded web. Values are individual node bonuses.
BRANCHES = {
 'wayfarer': ('Wayfarer','leather_boots',[
  node('Quick Step','sprinting_speed',.02,'+2% sprinting speed.'), node('Sure Footing','fall_reduction',.5,'Reduce effective fall distance by 0.5 blocks.','addition'), node('Trail Rations','stamina',.04,'+4% exhaustion capacity before food or saturation is consumed.'),
  node('Open Road','sprinting_speed',.02,'+2% sprinting speed.'), node('Soft Landing','fall_reduction',.5,'Reduce effective fall distance by 0.5 blocks.','addition'), node('Long Journey','stamina',.06,'+6% exhaustion capacity.'),
  node('Quiet Passage','stealth',1,'Reduce enemy detection distance by 1 block.','addition'), node('Spring in Your Step','jump',.03,'+3% jump height.'), node('Travelling Light','minecraft:generic.movement_speed',.02,'+2% movement speed.'),
  node('Pathfinder','sprinting_speed',.05,'+5% sprinting speed.'), node('Featherfoot','fall_reduction',1,'Reduce effective fall distance by 1 block.','addition'), node('Wanderer','stamina',.08,'+8% exhaustion capacity.')]),
 'delver': ('Delver','iron_pickaxe',[
  node('Stone Sense','pickaxe_speed',.03,'+3% pickaxe mining speed.'), node('Earth Mover','shovel_speed',.04,'+4% shovel mining speed.'), node('Steady Swing','axe_speed',.03,'+3% axe mining speed.'),
  node('Clean Cut','pickaxe_speed',.03,'+3% pickaxe mining speed.'), node('Excavator','shovel_speed',.06,'+6% shovel mining speed.'), node('Timber Hand','axe_speed',.05,'+5% axe mining speed.'),
  node('Deep Work','mining_speed',.04,'+4% mining speed when using the correct tool.'), node('Miner\'s Luck','fortune',.1,'+0.10 Fortune for block drops; fractional levels are rolled by Pufferfish.','addition'), node('Tunnel Legs','stamina',.06,'+6% exhaustion capacity.'),
  node('Master Delver','pickaxe_speed',.08,'+8% pickaxe mining speed.'), node('Prospector','fortune',.15,'+0.15 Fortune for block drops. Does not affect equipment loot.','addition'), node('Practical Tools','mining_speed',.04,'+4% mining speed with the correct tool.')]),
 'homesteader': ('Homesteader','wheat',[
  node('Packed Lunch','stamina',.04,'+4% exhaustion capacity.'), node('Fresh Air','natural_regeneration',.03,'+3% healing from natural regeneration.'), node('Faithful Friend','tamed_damage',.03,'+3% damage dealt by your tamed creatures.'),
  node('Hearty Meal','stamina',.04,'+4% exhaustion capacity.'), node('Home Remedies','healing',.04,'+4% healing received.'), node('Animal Care','tamed_resistance',.03,'Reduce damage taken by your tamed creatures by 3% of base damage.'),
  node('A Good Rest','natural_regeneration',.05,'+5% healing from natural regeneration.'), node('Well Fed','stamina',.08,'+8% exhaustion capacity.'), node('Working Hands','axe_speed',.05,'+5% axe mining speed.'),
  node('Comforts of Home','healing',.08,'+8% healing received.'), node('Trusted Companion','tamed_resistance',.06,'Reduce damage taken by your tamed creatures by 6% of base damage.'), node('Garden Paths','shovel_speed',.06,'+6% shovel mining speed.')]),
 'artificer': ('Artificer','anvil',[
  node('Apprentice Smith','repair_cost',-.02,'-2% anvil XP cost. Does not change identification costs.'), node('Keen Student','experience',.02,'+2% vanilla XP from collected orbs. No character XP bonus.'), node('Workshop Hands','axe_speed',.03,'+3% axe mining speed.'),
  node('Measured Repairs','repair_cost',-.03,'-3% anvil XP cost.'), node('Patient Study','experience',.03,'+3% vanilla XP from collected orbs.'), node('Prepared Workspace','stamina',.06,'+6% exhaustion capacity.'),
  node('Efficient Smith','repair_cost',-.05,'-5% anvil XP cost.'), node('Applied Knowledge','experience',.04,'+4% vanilla XP from collected orbs.'), node('Material Handling','mining_speed',.04,'+4% mining speed with the correct tool.'),
  node('Master Smith','repair_cost',-.10,'-10% anvil XP cost. The full branch totals -20% before other modifiers.'), node('Lifelong Learner','experience',.06,'+6% vanilla XP from collected orbs. The full branch totals +15%.'), node('Fine Tools','pickaxe_speed',.05,'+5% pickaxe mining speed.')]),
 'survivor': ('Survivor','shield',[
  node('Hardy','minecraft:generic.max_health',1,'+1 maximum health (half a heart).','addition'), node('Recovery','natural_regeneration',.03,'+3% healing from natural regeneration.'), node('Hold Ground','minecraft:generic.knockback_resistance',.02,'+0.02 knockback resistance.','addition'),
  node('Second Breath','healing',.03,'+3% healing received; this is a permanent bonus, not an emergency trigger.'), node('Weathered','resistance',.01,'Reduce incoming damage by 1% of base damage.'), node('Brace','minecraft:generic.knockback_resistance',.03,'+0.03 knockback resistance.','addition'),
  node('Close Guard','melee_resistance',.02,'Reduce incoming melee damage by 2% of base damage.'), node('Arrow Guard','ranged_resistance',.02,'Reduce incoming ranged damage by 2% of base damage.'), node('Warding','magic_resistance',.02,'Reduce qualifying magic damage by 2% of base damage.'),
  node('Unbroken','minecraft:generic.max_health',2,'+2 maximum health (one heart).','addition'), node('Resilient','resistance',.02,'Reduce incoming damage by 2% of base damage.'), node('Steady Recovery','natural_regeneration',.05,'+5% healing from natural regeneration.')]),
 'adventurer': ('Adventurer','iron_sword',[
  node('Practised Blade','melee_damage',.02,'+2% melee damage.'), node('Steady Aim','ranged_damage',.02,'+2% qualifying ranged damage.'), node('Arcane Basics','magic_damage',.02,'+2% qualifying magic damage. Coverage depends on damage-type tags.'),
  node('Balanced Grip','minecraft:generic.attack_speed',.01,'+1% attack speed.'), node('True Flight','bow_projectile_speed',.04,'+4% bow projectile speed.'), node('Focused Casting','magic_damage',.03,'+3% qualifying magic damage.'),
  node('Sword Familiarity','sword_damage',.03,'+3% sword damage.'), node('Crossbow Drill','crossbow_projectile_speed',.05,'+5% crossbow projectile speed.'), node('Arcane Ward','magic_resistance',.02,'Reduce qualifying incoming magic damage by 2% of base damage.'),
  node('Seasoned Fighter','melee_damage',.04,'+4% melee damage.'), node('Seasoned Marksman','ranged_damage',.04,'+4% qualifying ranged damage.'), node('Arcane Familiarity','magic_damage',.03,'+3% qualifying magic damage.')])
}

core=[node('First Steps','stamina',.02,'+2% exhaustion capacity.',icon='compass'),node('Healthy Habits','minecraft:generic.max_health',1,'+1 maximum health (half a heart).','addition','apple'),node('Good Pace','sprinting_speed',.01,'+1% sprinting speed.',icon='leather_boots'),node('Careful Landing','fall_reduction',.25,'Reduce effective fall distance by 0.25 blocks.','addition','feather'),node('Basic Tools','mining_speed',.02,'+2% mining speed with the correct tool.',icon='stone_pickaxe'),node('Rest and Recover','natural_regeneration',.02,'+2% healing from natural regeneration.',icon='bread'),node('Firm Stance','minecraft:generic.knockback_resistance',.02,'+0.02 knockback resistance.','addition','shield'),node('Ready for Adventure','stamina',.04,'+4% exhaustion capacity.',icon='map')]

# The old reward IDs remain stable; this generator expands the layout around them.
from web_rewards import EXTRA
from collections import deque

from build_theme import build as build_theme
THEME=build_theme()
VERSION='0.2.2'
defs,skills,layout,edges,rows,used_icons={},{},{},set(),[],{}

def link(a,b):
    assert a!=b
    edges.add(tuple(sorted((a,b))))

def add(key,data,x,y,branch,alias,role='minor',root=False):
    title,a,v,desc,op,*_=data
    # Original descriptions referring to the old 12-node branch totals are obsolete.
    desc=desc.split(' The full branch totals')[0]
    defs[key]={'title':title,'description':desc,'extra_description':f'{branch} | Costs 1 point. Removed on respec.','icon':icon_for(alias),'frame':THEME['frames'][role],'size':THEME['node_sizes'][role],'cost':1,'rewards':[attr(a,v,op)]}
    if role=='keystone':defs[key]['required_spent_points']=40
    skills[key]={'x':round(x),'y':round(y),'definition':key,'root':root}
    layout[key]={'branch':branch,'role':role,'icon_alias':alias}
    used_icons[key]=ICON_MAP[alias]
    rows.append((key,branch,title,1,desc))

# Centre plus a small inner constellation, with six independent entry hubs.
for i,data in enumerate(core):
    angle=-math.pi/2+(i-1)*math.pi/3
    x,y=(0,0) if i==0 else (72*math.cos(-math.pi/3),72*math.sin(-math.pi/3)) if i==7 else (120*math.cos(angle),120*math.sin(angle))
    add(f'foundation_{i}',data,x,y,'Foundation',f'foundation_{i}',role='hub' if i<=6 else 'minor',root=i==0)
    if i:link('foundation_0',f'foundation_{i}')
for i,(title,a,v,desc,alias) in enumerate([
 ('Fresh Start','natural_regeneration',.01,'+1% healing from natural regeneration.','foundation_5'),
 ('Prepared Traveller','stamina',.02,'+2% exhaustion capacity.','foundation_0'),
 ('Work Rhythm','mining_speed',.01,'+1% mining speed with the correct tool.','foundation_4'),
 ('Measured Pace','sprinting_speed',.005,'+0.5% sprinting speed.','foundation_2')],8):
    angle=-math.pi/3+(i-7)*math.pi/3
    add(f'foundation_{i}',node(title,a,v,desc),72*math.cos(angle),72*math.sin(angle),'Foundation',alias)
    link('foundation_0',f'foundation_{i}')
for i in range(6):
    # Small inner loop and routes into the two nearest branch hubs.
    mid=7+i if i<5 else 7
    if i<5:link(f'foundation_{mid}',f'foundation_{mid+1}' if i<4 else 'foundation_7')
    if i<5:
        link(f'foundation_{mid}',f'foundation_{i+1}')
        link(f'foundation_{mid}',f'foundation_{(i+1)%6+1}')

# Local coordinates are radial distance and lateral offset. Each sector has
# multiple diamonds, two side loops, two outer loops and two distinct tips.
POINTS=[(210,0),(285,-65),(285,65),(365,-100),(365,0),(365,100),(445,-65),(445,65),(520,0),
 (515,-150),(600,-190),(685,-150),(690,-65),(605,-60),
 (515,150),(600,190),(685,150),(690,65),(605,60),(760,0),
 (780,-160),(855,-200),(930,-160),(930,-60),(855,-40),
 (780,160),(855,200),(930,160),(930,60),(855,40),
 (1015,-130),(1080,-180),(1015,130),(1080,180)]
LOCAL_EDGES=[(0,1),(0,2),(1,3),(1,4),(2,4),(2,5),(3,6),(4,6),(4,7),(5,7),(6,8),(7,8),
 (3,9),(9,10),(10,11),(11,12),(12,13),(13,9),(5,14),(14,15),(15,16),(16,17),(17,18),(18,14),
 (8,13),(8,18),(12,19),(17,19),(19,20),(19,25),(12,20),(17,25),
 (20,21),(21,22),(22,23),(23,24),(24,20),(25,26),(26,27),(27,28),(28,29),(29,25),
 (24,19),(29,19),(24,29),(23,28),(22,30),(30,31),(27,32),(32,33)]
ANCHORS=[0,4,8,9,14,19,20,25,24,31,33,29]
SECTORS=list(BRANCHES)
ids={}
for b,(slug,(label,_,nodes)) in enumerate(BRANCHES.items()):
    assert len(EXTRA[slug])==22
    angle=-math.pi/2+b*math.pi/3
    remaining=iter(EXTRA[slug]);extra_i=13
    for p,(r,t) in enumerate(POINTS):
        if p in ANCHORS:
            i=ANCHORS.index(p);key=f'{slug}_{i+1:02}';data=nodes[i];alias=key
        else:
            data=next(remaining);alias=data[5];key=f'{slug}_{extra_i:02}';extra_i+=1
        role='keystone' if p in (31,33) else 'hub' if p in (8,19) else 'notable' if p in (9,14,20,25,24,29) else 'minor'
        add(key,data,r*math.cos(angle)-t*math.sin(angle),r*math.sin(angle)+t*math.cos(angle),label,alias,role)
        ids[b,p]=key
    link(f'foundation_{b+1}',ids[b,0])
    for p,q in LOCAL_EDGES:link(ids[b,p],ids[b,q])

# Four-node hybrid routes join adjacent sectors away from the central hub.
# They form a traversable ring: builds can move sideways, not only outwards.
for b,slug in enumerate(SECTORS):
    nxt=(b+1)%6;other=SECTORS[nxt]
    source_a=EXTRA[slug][0];source_b=EXTRA[other][0]
    prev=ids[b,15]
    for j,offset in enumerate((23,28,33,38)):
        key=f'bridge_{slug}_{other}_{j+1}'
        angle=-math.pi/2+b*math.pi/3+math.radians(offset)
        data=source_a if j%2==0 else source_b
        title=f'{BRANCHES[slug][0]} / {BRANCHES[other][0]}: '+['Shared Practice','Cross Training','Fieldcraft','Common Ground'][j]
        v=data[2]/2
        desc='Hybrid route: '+re.sub(r'[+-]?\d+(?:\.\d+)?',lambda m:f'{float(m.group())/2:+g}' if m.group().startswith(('+','-')) else f'{float(m.group())/2:g}',data[3],count=1)
        actual=(title,data[1],v,desc,data[4],data[5])
        add(key,actual,630*math.cos(angle),630*math.sin(angle),'Crossroads',data[5],role='minor')
        link(prev,key);prev=key
    link(prev,ids[nxt,10])

def save(path,value):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2)+'\n')

save(PACK/'pack.mcmeta',{'pack':{'pack_format':48,'description':'TNK Character Web — 240 nodes / every node costs 1 / spend up to 100'}})
save(PACK/'data/tnk/puffish_skills/config.json',{'version':2,'categories':['character']})
save(OUT/'category.json',{'title':'Towns & Kingdoms','description':'240 skills. Every skill costs 1 point. Spend up to 100.','icon':icon_for('foundation_0'),'background':THEME['background'],'colors':THEME['colors'],'unlocked_by_default':True,'starting_points':0,'exclusive_root':False,'spent_points_limit':100,'erase_on_death':False})
save(OUT/'definitions.json',defs);save(OUT/'skills.json',skills)
save(OUT/'connections.json',{'normal':{'bidirectional':[list(e) for e in sorted(edges)]}})
save(ROOT/'layout-metadata.json',layout);save(ROOT/'icon-map.json',used_icons)
assert len(skills)==240 and len(defs)==240
assert all(d['cost']==1 for d in defs.values())
assert len({(s['x'],s['y']) for s in skills.values()})==240
neighbors={k:set() for k in skills}
for a,b in edges:neighbors[a].add(b);neighbors[b].add(a)
dist={'foundation_0':0};queue=deque(dist)
while queue:
    for v in neighbors[queue.popleft()]:
        if v not in dist:dist[v]=min(dist[n]+1 for n in neighbors[v] if n in dist);queue.append(v)
assert len(dist)==240 and max(dist.values())<100
assert sum(s['root'] for s in skills.values())==1
assert all(sum(k.startswith(slug+'_') for k in skills)==34 for slug in SECTORS)
cycles=len(edges)-len(skills)+1
assert cycles>=60
minimum=min(math.hypot(a['x']-b['x'],a['y']-b['y']) for i,a in enumerate(skills.values()) for b in list(skills.values())[i+1:])
assert minimum>=35,minimum
summary={'nodes':240,'edges':len(edges),'independent_cycles':cycles,'root_count':1,'maximum_shortest_path':max(dist.values()),'minimum_node_distance':round(minimum,2),'node_cost':1,'spending_limit':100,'foundation_nodes':12,'nodes_per_branch':34,'crossroad_nodes':24}
save(ROOT/'validation-summary.json',summary)
(ROOT/'NODE-CATALOGUE.md').write_text('# TNK Character Web — 0.2.2 simple theme draft\n\n240 nodes. Every node costs 1 point. Spend up to 100. Six branches of 34 nodes, 12 foundation nodes and 24 hybrid crossroad nodes.\n\n| ID | Branch | Node | Cost | Effect |\n|---|---|---|---:|---|\n'+''.join('| '+' | '.join(map(str,row))+' |\n' for row in rows))
for source,name in [(PACK,f'TNK-Character-Web-{VERSION}-draft.zip'),(ROOT/'resourcepack',f'TNK-Skilltree-Icons-{VERSION}.zip')]:
    with zipfile.ZipFile(ROOT/name,'w',zipfile.ZIP_DEFLATED) as z:
        for p in source.rglob('*'):
            if p.is_file():z.write(p,p.relative_to(source))
print(json.dumps(summary))
