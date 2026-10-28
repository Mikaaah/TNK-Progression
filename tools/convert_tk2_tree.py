#!/usr/bin/env python3
"""Port the supplied 588-node topology to Pufferfish, using TNK's own existing art.
Usage: python tools/convert_tk2_tree.py TK2_Original_Skill_Tree_588_Nodes_Extracted.zip
No source PNGs or original space background are copied.
"""
import collections,csv,hashlib,json,math,sys,zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'skilltree/rebuilt588'
CATEGORY=OUT/'data/tnk/puffish_skills/categories/character'
OLD=ROOT/'skilltree/datapack/data/tnk/puffish_skills/categories/character'
SCALE=2.0
BRANCH_ICONS={'alchemist':'homesteader','blacksmith':'survivor','cook':'homesteader','enchanter':'adventurer','hunter':'wayfarer','miner':'delver'}
LABELS={'alchemist':'Alchemist','blacksmith':'Blacksmith','cook':'Cook','enchanter':'Enchanter','hunter':'Hunter','miner':'Miner'}
OPTIONAL_PREFIXES=('puffish_attributes:','irons_spellbooks:','ars_nouveau:','apothic_attributes:')

def write(path,data):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n')

def attr(attribute,value,text,operation='multiply_base'):
 return {'type':'puffish_skills:attribute','required':not attribute.startswith(OPTIONAL_PREFIXES),'data':{'attribute':attribute,'value':round(value,5),'operation':operation}},text

def convert(source):
 original=json.loads((OLD/'definitions.json').read_text())
 with zipfile.ZipFile(source) as src:
  rows={Path(n).stem:json.loads(src.read(n)) for n in src.namelist() if n.startswith('data/skilltree/skills/') and n.endswith('.json')}
  names=json.loads(src.read('assets/skilltree/lang/en_us.json'))
 assert len(rows)==588
 defs={};skills={};edges=set();passives={};report=[];bounds=collections.defaultdict(list)
 def frame(role):return {'type':'texture','data':{s:f'tnk:textures/gui/frames/{role}_{s}.png' for s in ['locked','available','affordable','unlocked','excluded']}}
 for id,d in sorted(rows.items()):
  branch=id.split('_')[0];large=d['buttonSize']>=24;factor=2.0 if large else 1.0
  rewards=[];texts=[];runtime={};changes=[]
  def add(a,v,t,op='multiply_base'):
   reward,text=attr(a,v,t,op)
   if reward not in rewards:rewards.append(reward);texts.append(text)
  def passive(key,value,text):runtime[key]=runtime.get(key,0)+value;texts.append(text)
  def magic(kind):
   if kind=='mana':
    add('irons_spellbooks:max_mana',8*factor,f"Iron's Spells: +{8*factor:g} maximum mana.",'addition')
    add('ars_nouveau:ars_nouveau.perk.max_mana',8*factor,f'Ars Nouveau: +{8*factor:g} maximum mana.','addition')
   elif kind=='regen':
    add('irons_spellbooks:mana_regen',.015*factor,f"Iron's Spells: +{1.5*factor:g}% base mana regeneration.")
    add('ars_nouveau:ars_nouveau.perk.mana_regen',.15*factor,f'Ars Nouveau: +{.15*factor:g} mana regeneration perk.','addition')
   elif kind=='power':
    add('irons_spellbooks:spell_power',.01*factor,f"Iron's Spells: +{factor:g}% base spell power.")
    add('ars_nouveau:ars_nouveau.perk.spell_damage',.1*factor,f'Ars Nouveau: +{.1*factor:g} spell damage perk.','addition')
   elif kind=='haste':
    add('irons_spellbooks:cast_time_reduction',.01*factor,f"Iron's Spells: +{factor:g}% base cast-time-reduction attribute.")
   elif kind=='cooldown':add('irons_spellbooks:cooldown_reduction',.01*factor,f"Iron's Spells: +{factor:g}% base cooldown-reduction attribute.")
   elif kind=='ward':
    add('irons_spellbooks:spell_resist',.0125*factor,f"Iron's Spells: +{1.25*factor:g}% base spell resistance.")
    add('ars_nouveau:ars_nouveau.perk.warding',.1*factor,f'Ars Nouveau: +{.1*factor:g} warding perk.','addition')
  for b in d['bonuses']:
   kind=b['type'].split(':')[-1]
   if kind=='attribute':
    a=b['attribute'].split(':')[-1]
    if a=='generic.max_health':add('minecraft:generic.max_health',.5*factor,f'+{.5*factor:g} maximum health.','addition')
    elif a=='generic.armor':add('minecraft:generic.armor',.25*factor,f'+{.25*factor:g} armor.','addition')
    elif a=='generic.attack_speed':add('minecraft:generic.attack_speed',.01*factor,f'+{factor:g}% base attack speed.')
    elif a=='generic.movement_speed':add('minecraft:generic.movement_speed',.01*factor,f'+{factor:g}% base movement speed.')
    elif a=='generic.attack_damage':add('minecraft:generic.attack_damage',.02*factor,f'+{2*factor:g}% base attack damage.')
    elif a=='blocking':passive('guard',.005*factor,f'{.5*factor:g}% chance to halve a non-bypassing attack while actively using a shield.')
    elif a=='evasion':passive('evasion',.0025*factor,f'{.25*factor:g}% chance to evade a direct physical attack; total capped at 15%.')
    elif a=='regeneration':add('puffish_attributes:natural_regeneration',.02*factor,f'+{2*factor:g}% natural regeneration.')
    elif a=='stealth':add('puffish_attributes:stealth',.25*factor,f'+{.25*factor:g} stealth distance.','addition')
    elif a=='exp_per_minute':
     magic('regen');changes.append('Passive XP per minute replaced with mana regeneration; no AFK XP generator.')
    elif a=='ring':
     magic('mana');changes.append('Extra Curios slot replaced with mana capacity; no inventory-slot injection.')
    elif a=='generic.luck':add('minecraft:generic.luck',.1*factor,f'+{.1*factor:g} luck.','addition')
    else:raise ValueError((id,a))
   elif kind=='damage':
    condition=b.get('player_condition',{}).get('type','').split(':')[-1]
    target=b.get('target_condition',{}).get('type','').split(':')[-1]
    mult=b.get('player_multiplier',{}).get('type','').split(':')[-1]
    dtype=b.get('damage_condition',{}).get('type','').split(':')[-1]
    if target=='burning':passive('burning_damage',.025*factor,f'+{2.5*factor:g}% damage against burning enemies.')
    elif mult=='distance_to_target':passive('longshot',.06*factor,f'Up to +{6*factor:g}% projectile damage at 16+ blocks.')
    elif mult=='food_level':passive('well_fed_damage',.015*factor,f'+{1.5*factor:g}% damage while food is 18 or higher.')
    elif condition=='has_effect':passive('effect_damage',.015*factor,f'+{1.5*factor:g}% damage while a beneficial effect is active.')
    elif branch=='enchanter':magic('power')
    elif dtype=='projectile' or branch=='hunter':add('puffish_attributes:ranged_damage',.015*factor,f'+{1.5*factor:g}% ranged damage.')
    else:add('minecraft:generic.attack_damage',.01*factor,f'+{factor:g}% base attack damage.')
   elif kind=='crafted_item_bonus':
    sub=b['item_bonus']['type'].split(':')[-1]
    changes.append('Crafted-item-only '+sub+' modernized into a personal bonus; no permanent affixes are written onto crafted items.')
    if branch=='enchanter':magic(['mana','regen','haste','cooldown','power'][sum(id.encode())%5])
    elif sub in ['potion_duration','potion_amplification']:
     passive('potion_duration',.025*factor,f'+{2.5*factor:g}% duration of non-instant beneficial potions you drink; capped at +50%.')
     if large:magic('regen')
    elif sub in ['food_saturation','food_effect','food_healing']:
     passive('food_recovery',.15*factor,f'Restore +{.15*factor:g} health when eating nutritious food; total capped at 3 health per meal.')
     add('puffish_attributes:stamina',.015*factor,f'+{1.5*factor:g}% exhaustion capacity.')
    elif sub=='durability':add('puffish_attributes:repair_cost',-.01*factor,f'-{factor:g}% base anvil XP cost.')
    elif sub=='quiver_capacity':add('puffish_attributes:bow_projectile_speed',.015*factor,f'+{1.5*factor:g}% bow projectile velocity.')
    elif sub=='sockets':
     add('minecraft:generic.armor_toughness',.25*factor,f'+{.25*factor:g} armor toughness.','addition');changes.append('Old socket count replaced; Apotheosis sockets are not modified.')
    elif branch=='miner':add('puffish_attributes:pickaxe_speed',.015*factor,f'+{1.5*factor:g}% pickaxe speed.')
    elif branch=='blacksmith':add('minecraft:generic.attack_speed',.01*factor,f'+{factor:g}% base attack speed.')
    elif branch=='hunter':add('puffish_attributes:ranged_damage',.0125*factor,f'+{1.25*factor:g}% ranged damage.')
    elif branch=='alchemist':magic('power')
    else:add('puffish_attributes:healing',.015*factor,f'+{1.5*factor:g}% healing received.')
   elif kind in ['crit_chance','crit_damage']:
    a='crit_chance' if kind=='crit_chance' else 'crit_damage';v=.005*factor if a=='crit_chance' else .015*factor
    add('apothic_attributes:'+a,v,f'Apothic Attributes: +{100*v:g} percentage points '+('critical chance.' if a=='crit_chance' else 'critical damage.'),'addition')
    if branch=='enchanter':magic('power')
   elif kind in ['healing','incoming_healing']:
    add('puffish_attributes:healing',.015*factor,f'+{1.5*factor:g}% healing received.')
    if kind=='healing':passive('leech',.025*factor,f'Combat recovery: +{.025*factor:g} health on a successful hit, at most once per second and capped at 1 health.')
   elif kind in ['gem_power','player_sockets']:
    changes.append(kind+' replaced; does not alter Apotheosis gems or sockets.')
    if branch=='enchanter':magic('ward')
    elif branch=='miner':add('puffish_attributes:fortune',.05*factor,f'+{.05*factor:g} block Fortune attribute.','addition')
    elif branch=='hunter':add('puffish_attributes:ranged_damage',.02*factor,f'+{2*factor:g}% ranged damage.')
    else:add('minecraft:generic.armor_toughness',.25*factor,f'+{.25*factor:g} armor toughness.','addition')
   elif kind=='block_break_speed':add('minecraft:player.block_break_speed',.025*factor,f'+{2.5*factor:g}% base block-breaking speed.')
   elif kind=='repair_efficiency':add('puffish_attributes:repair_cost',-.015*factor,f'-{1.5*factor:g}% base anvil XP cost.')
   elif kind in ['enchantment_requirement','free_enchantment','enchantment_amplification']:
    magic('mana' if kind=='enchantment_requirement' else 'cooldown' if kind=='free_enchantment' else 'power');changes.append('Old enchantment manipulation replaced with spellcasting progression.')
   elif kind=='gained_experience':add('puffish_attributes:experience',.02*factor,f'+{2*factor:g}% vanilla-orb XP; does not grant character XP.')
   elif kind=='loot_duplication':
    add('puffish_attributes:fortune',.025*factor,f'+{.025*factor:g} block Fortune attribute.','addition');changes.append('Loot duplication replaced; equipment roll/tier/rarity is unaffected.')
   elif kind=='recipe_unlock':
    magic('regen');changes.append('Unavailable old-mod recipe replaced with supported mana-regeneration progression.')
   elif kind=='ignite':passive('ignite',.025*factor,f'{2.5*factor:g}% chance on a direct hit to ignite a hostile enemy for 3 seconds; capped at 20%.')
   elif kind=='jump_height':add('puffish_attributes:jump',.02*factor,f'+{2*factor:g}% jump height.')
   elif kind=='arrow_retrieval':
    add('puffish_attributes:crossbow_projectile_speed',.015*factor,f'+{1.5*factor:g}% crossbow projectile velocity.');changes.append('Arrow retrieval replaced with projectile velocity; no arrow duplication.')
   else:raise ValueError((id,kind))
  # Expand the caster cluster with actual supported mod stats instead of a generic magic-damage substitute.
  if branch=='enchanter':
   if 'life' in id:magic('mana')
   elif 'defensive' in id:magic('ward')
   elif 'speed' in id:magic('haste')
   if id.endswith('_class'):magic('mana');magic('regen')
   if id=='enchanter_subclass_1':
    add('irons_spellbooks:fire_spell_power',.06,'Iron\'s Spells: +6% base fire-school spell power.')
   if id=='enchanter_subclass_2':
    add('irons_spellbooks:summon_damage',.06,"Iron's Spells: +6% base summon damage.")
   if id.endswith('mastery'):magic('power');magic('cooldown')
   if 'offensive' in id and 'crafting' not in id:
    school=['fire','ice','lightning','ender','blood','evocation'][sum(id.encode())%6]
    add('irons_spellbooks:'+school+'_spell_power',.0075*factor,f"Iron's Spells: +{.75*factor:g}% base {school}-school spell power.")
  if branch=='alchemist' and 'subclass_2' in id and ('crafting' in id or id.endswith('_2')):
   add('irons_spellbooks:holy_spell_power',.01*factor,f"Iron's Spells: +{factor:g}% base holy-school spell power.")
  if branch=='hunter' and id in ['hunter_subclass_2','hunter_subclass_2_mastery']:
   add('irons_spellbooks:nature_spell_power',.04,"Iron's Spells: +4% base nature-school spell power.")
   add('puffish_attributes:tamed_damage',.04,'+4% damage dealt by tamed creatures.')
  if id=='blacksmith_defensive_keystone_1':
   passive('second_wind',1,'Second Wind: below 25% health after surviving a hit, recover 10% maximum health. 180-second saved cooldown; does not prevent lethal hits.')
  if id.endswith('_gateway') or not rewards and not runtime:
   add('minecraft:generic.movement_speed',.002,'+0.2% base movement speed.')
  # Optional integrations never leave a node wholly inert in a pack without that mod.
  if not any(r['required'] for r in rewards) and not runtime:
   add('minecraft:generic.max_health',.05,'+0.05 maximum health (always active).','addition')
  if runtime:rewards.append({'type':'puffish_skills:tag','data':{'tag':'tnk_tree.'+id}})
  tier='keystone' if d['buttonSize']==32 else 'hub' if d['isStartingPoint'] or d['buttonSize']==30 else 'notable' if d['buttonSize']>=20 else 'minor'
  if tier=='minor':tier='small'
  # TNK uses 'small' or 'minor' depending on the existing frame generator; resolve the actual asset.
  if not (ROOT/f'skilltree/resourcepack/assets/tnk/textures/gui/frames/{tier}_locked.png').exists():tier='small' if (ROOT/'skilltree/resourcepack/assets/tnk/textures/gui/frames/small_locked.png').exists() else 'minor'
  family=BRANCH_ICONS[branch];suffix=10 if large else 1+(sum(id.encode())%12)
  icon_id=f'{family}_{suffix:02d}'
  icon=original[icon_id]['icon'] if icon_id in original else original[f'{family}_01']['icon']
  title=names.get('skill.skilltree.'+id+'.name',id.replace('_',' ').title())
  if changes:
   if branch=='enchanter' and ('crafting' in id or id.endswith('mastery')):title='Arcane '+('Mastery' if large else 'Study')
   elif 'loot_duplication' in [b['type'].split(':')[-1] for b in d['bonuses']]:title='Prospector\'s Fortune'
   elif 'gateway' in id:title=LABELS[branch]+' Crossroad'
   elif 'crafting' in id or id.endswith('_class'):
    title={'alchemist':'Potion Longevity','blacksmith':'Tempered Craft','cook':'Restorative Cuisine','hunter':'Fletcher\'s Discipline','miner':'Deep Delving','enchanter':'Arcane Study'}[branch]
  if id=='blacksmith_defensive_keystone_1':title='Second Wind'
  definition={'title':title,'description':'\n'.join(dict.fromkeys(texts)),
    'extra_description':LABELS[branch]+' | 1 skill point. Removed on respec. Optional mod bonuses apply only when that mod is installed. '+('Modernized from the old tree; see the conversion catalogue.' if changes else ''),
    'icon':icon,'frame':frame(tier),'size':round(d['buttonSize']/16,3),'cost':1,'rewards':rewards}
  if d['buttonSize']==32:definition['required_spent_points']=40 if not id.endswith('mastery') else 60
  defs[id]=definition;skills[id]={'x':round(d['positionX']*SCALE),'y':round(d['positionY']*SCALE),'definition':id,'root':d['isStartingPoint']}
  for c in d['directConnections']+d['longConnections']:
   other=c.split(':')[-1];assert other in rows;edges.add(tuple(sorted((id,other))))
  assert not d['oneWayConnections']
  if runtime:passives[id]=runtime
  report.append({'id':id,'branch':branch,'title':title,'original_bonus_types':[b['type'] for b in d['bonuses']],
                 'description':definition['description'],'modernized':changes,'runtime':runtime})
  for reward in rewards:
   if reward['type']=='puffish_skills:attribute':
    a=reward['data'];bounds[(a['attribute'],a['operation'])].append(a['value'])
 cat=json.loads((OLD/'category.json').read_text());cat.update(title='Towns & Kingdoms',description='588 skills across six connected disciplines. 175 points earned by character level 100.',spent_points_limit=175,exclusive_root=False,starting_points=0)
 write(CATEGORY/'category.json',cat);write(CATEGORY/'definitions.json',defs);write(CATEGORY/'skills.json',skills)
 write(CATEGORY/'connections.json',{'normal':{'bidirectional':[list(e) for e in sorted(edges)]}})
 write(OUT/'data/tnk/puffish_skills/config.json',{'version':2,'categories':['character']})
 write(OUT/'data/tnkprogression/tree_passives.json',{'version':1,'skills':passives})
 write(OUT/'conversion-catalogue.json',report)
 # All source vertices and edges must be preserved and reachable; no hidden missing-mod bridge nodes.
 graph={n:set() for n in rows}
 for a,b in edges:graph[a].add(b);graph[b].add(a)
 roots=[n for n,d in skills.items() if d['root']];distance={n:0 for n in roots};queue=collections.deque(roots)
 while queue:
  n=queue.popleft()
  for other in graph[n]:
   if other not in distance:distance[other]=distance[n]+1;queue.append(other)
 assert len(distance)==588 and len(roots)==6 and all(d['cost']==1 for d in defs.values())
 assert len({(d['x'],d['y']) for d in skills.values()})==588
 assert all(len(r['description'])>0 for r in report)
 assert not any('identif' in r['description'].lower() for r in report)
 summary={'nodes':588,'branches':dict(collections.Counter(r['branch'] for r in report)),'roots':roots,'connections':len(edges),'max_shortest_path':max(distance.values()),'points_at_100':175,'all_cost_one':True,'runtime_passive_nodes':len(passives),'modernized_nodes':sum(bool(r['modernized']) for r in report),'source_sha256':hashlib.sha256(Path(source).read_bytes()).hexdigest(),'source_position_scale':SCALE,'art':'Existing TNK HAS icons, frames and warm background; no original-source art copied.'}
 write(OUT/'validation-summary.json',summary)
 with (OUT/'ATTRIBUTE-BOUNDS.csv').open('w') as f:
  w=csv.writer(f,lineterminator="\n");w.writerow(['Attribute','Operation','Whole-tree sum','175-point optimistic bound ignoring routing'])
  for (a,op),values in sorted(bounds.items()):
   ordered=sorted(values,reverse=sum(values)>=0);w.writerow([a,op,round(sum(values),5),round(sum(ordered[:175]),5)])
 print(json.dumps(summary,indent=2))

if __name__=='__main__':convert(sys.argv[1])
