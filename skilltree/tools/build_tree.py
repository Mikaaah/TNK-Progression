"""Generate the TNK 1.21.1 Pufferfish attribute-tree draft, reproducibly."""
import json, math, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / 'datapack'
OUT = PACK / 'data/tnk/puffish_skills/categories/character'
OUT.mkdir(parents=True, exist_ok=True)

def attr(name, value, operation='multiply_base'):
    return {'type':'puffish_skills:attribute','data':{'attribute':name if ':' in name else 'puffish_attributes:'+name,'value':value,'operation':operation}}

def node(title, attribute, value, description, operation='multiply_base', icon=None):
    return title, attribute, value, description, operation, icon

# Each column is an independent short path. Values are individual node bonuses.
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

defs, skills, edges, rows = {}, {}, [], []
def add(key, data, cost, x, y, branch, root=False, keystone=False):
    title,a,v,desc,op,item=data
    defs[key]={'title':title,'description':desc,'extra_description':f'{branch} | Cost: {cost} point(s). Bonuses are removed on respec.','icon':{'type':'item','data':{'item':'minecraft:'+(item or 'book')}},'frame':'challenge' if keystone else 'goal' if cost==2 else 'task','size':1.2 if keystone else 1.0,'cost':cost,'rewards':[attr(a,v,op)]}
    if keystone: defs[key]['required_spent_points']=20
    skills[key]={'x':round(x),'y':round(y),'definition':key,'root':root}
    if root: skills[key]['root']=True
    rows.append((key,branch,title,cost,desc))

core=[node('First Steps','stamina',.02,'+2% exhaustion capacity.',icon='compass'),node('Healthy Habits','minecraft:generic.max_health',1,'+1 maximum health (half a heart).','addition','apple'),node('Good Pace','sprinting_speed',.01,'+1% sprinting speed.',icon='leather_boots'),node('Careful Landing','fall_reduction',.25,'Reduce effective fall distance by 0.25 blocks.','addition','feather'),node('Basic Tools','mining_speed',.02,'+2% mining speed with the correct tool.',icon='stone_pickaxe'),node('Rest and Recover','natural_regeneration',.02,'+2% healing from natural regeneration.',icon='bread'),node('Firm Stance','minecraft:generic.knockback_resistance',.02,'+0.02 knockback resistance.','addition','shield'),node('Ready for Adventure','stamina',.04,'+4% exhaustion capacity.',icon='map')]
for i,data in enumerate(core):
    angle=-math.pi/2+(i-1)*math.pi/3
    add('foundation_'+str(i),data,[1,1,2,2,2,2,3,3][i],0 if i==0 else 40 if i==7 else 80*math.cos(angle),0 if i in (0,7) else 80*math.sin(angle),'Foundation',root=i==0)
    if i: edges.append(['foundation_0','foundation_'+str(i)])

costs=[1,1,1,1,2,2,2,2,2,4,4,2]
for b,(slug,(label,icon,nodes)) in enumerate(BRANCHES.items()):
    angle=-math.pi/2+b*math.pi/3
    for i,data in enumerate(nodes):
        depth,lane=divmod(i,3);r=144+depth*80;t=(lane-1)*48
        key=f'{slug}_{i+1:02}'
        add(key,(*data[:5],data[5] or icon),costs[i],r*math.cos(angle)-t*math.sin(angle),r*math.sin(angle)+t*math.cos(angle),label,keystone=i in (9,10))
        edges.append(['foundation_'+str(b+1) if depth==0 else f'{slug}_{i-2:02}',key])

def save(path,value):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2)+'\n')
save(PACK/'pack.mcmeta',{'pack':{'pack_format':48,'description':'TNK Character Tree — 80 nodes / 100-point limit — attribute draft'}})
save(PACK/'data/tnk/puffish_skills/config.json',{'version':2,'categories':['character']})
save(OUT/'category.json',{'title':'Towns & Kingdoms','description':'Shape your journey. Character levels award up to 100 points.','icon':{'type':'item','data':{'item':'minecraft:compass'}},'background':'minecraft:textures/block/sandstone_top.png','unlocked_by_default':True,'starting_points':0,'exclusive_root':False,'spent_points_limit':100,'erase_on_death':False})
save(OUT/'definitions.json',defs);save(OUT/'skills.json',skills)
save(OUT/'connections.json',{'normal':{'unidirectional':edges}})
assert len(skills)==80 and sum(d['cost'] for d in defs.values())==160
assert len({(s['x'],s['y']) for s in skills.values()})==80
reachable={'foundation_0'}
while True:
    more={v for u,v in edges if u in reachable}-reachable
    if not more:break
    reachable|=more
assert len(reachable)==80
for slug in BRANCHES: assert sum(d['cost'] for k,d in defs.items() if k.startswith(slug+'_'))==24
(ROOT/'NODE-CATALOGUE.md').write_text('# TNK Character Tree — attribute draft\n\n80 nodes; 160 points of choices; 100-point spending cap. Foundation costs 16; each branch costs 24. One category: `tnk:character`.\n\n| ID | Branch | Node | Cost | Effect |\n|---|---|---|---:|---|\n'+''.join('| '+' | '.join(map(str,row))+' |\n' for row in rows))
with zipfile.ZipFile(ROOT/'TNK-Character-Tree-0.1.0-draft.zip','w',zipfile.ZIP_DEFLATED) as z:
    for p in PACK.rglob('*'):
        if p.is_file():z.write(p,p.relative_to(PACK))
print('Validated: 80 distinct nodes, 79 directed connections, all reachable, 160 total points, 100-point cap.')
