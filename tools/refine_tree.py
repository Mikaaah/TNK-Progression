"""Restore representable source effects, retaining their conditions, and improve every tooltip."""
import collections,textwrap
BRANCHES={'alchemist':'Alchemist','blacksmith':'Blacksmith','cook':'Cook','enchanter':'Enchanter','hunter':'Hunter','miner':'Miner'}
SUBS={'alchemist':('Assassin','Healer'),'blacksmith':('Soldier','Artisan'),'cook':('Berserker','Fisherman'),'enchanter':('Arsonist','Scholar'),'hunter':('Ranger','Fletcher'),'miner':('Explorer','Jeweler')}
ATTRS={'generic.max_health':'Maximum Health','generic.armor':'Armor','generic.attack_speed':'Attack Speed','generic.movement_speed':'Movement Speed','generic.attack_damage':'Attack Damage','generic.luck':'Luck'}
def kind(d):return d.get('type','skilltree:none').split(':')[-1]
def condition(d):
 k=kind(d)
 if k=='none':return ''
 if k=='effect_amount':return 'while affected by a status effect'
 if k=='health_percentage':return f"while Health is at most {100*d['max']:g}%"
 if k=='food_level':return f"while Food is at least {d['min']:g} / 20"
 if k=='fishing':return 'while your fishing line is cast'
 if k=='attribute_value':return f"while {d['attribute'].split('.')[-1].title()} is at least {d['min']:g}"
 if k=='has_effect':return 'against targets with '+d['effect'].split(':')[-1].replace('_',' ').title()
 if k=='burning':return 'against burning targets'
 if k in ('has_item_in_hand','has_item_equipped'):
  i=d['item_condition'];enchanted=kind(i)=='enchanted'
  if enchanted:i=i.get('item_condition',{})
  return ('while holding ' if k=='has_item_in_hand' else 'with equipped ')+('enchanted ' if enchanted else '')+i.get('equipment_type','equipment').replace('_',' ')
 raise ValueError(k)
def supported_condition(d):
 try:condition(d);return True
 except (ValueError,KeyError):return False
def supported(b):
 if not all(supported_condition(b.get(key,{})) for key in ['player_condition','target_condition']):return False
 m=b.get('player_multiplier',{});k=kind(m)
 if k not in ('none','effect_amount','food_level','attribute_value','enchants_amount','enchants_levels','distance_to_target'):return False
 if k=='attribute_value' and not m['attribute'].startswith('minecraft:'):return False
 return (kind(b)=='attribute' and b['attribute'].split(':')[-1] in ATTRS) or kind(b) in ('damage','block_break_speed')
def source_text(b):
 k=kind(b);v=b.get('amount',b.get('multiplier',0));op=b.get('operation',1)
 label=ATTRS.get(b.get('attribute','').split(':')[-1],'Block Breaking Speed' if k=='block_break_speed' else 'Projectile Damage' if kind(b.get('damage_condition',{}))=='projectile' else 'Melee Damage' if kind(b.get('damage_condition',{}))=='melee' else 'Damage')
 amount=f'{v*100:+g}%' if op else f'{v:+g}'

 t=amount+' '+label+(' (base)' if op==1 else '')
 if label=='Maximum Health' and op==0:t+=f" ({v/2:g} {'heart' if v==2 else 'hearts'})"
 m=b.get('player_multiplier',{});mk=kind(m)
 if mk=='food_level':t+=' per Food point'
 elif mk=='effect_amount':t+=' per active status effect'
 elif mk=='distance_to_target':t+=' per block to target'
 elif mk=='attribute_value':t+=' per '+m['attribute'].split('.')[-1].title()+f" / {m.get('divisor',1):g}"
 elif mk=='enchants_amount':t+=' per matching equipped enchantment'
 elif mk=='enchants_levels':t+=' per matching equipped enchantment level'
 c=[condition(b.get(key,{})) for key in ['player_condition','target_condition']]
 return t+(' — '+', '.join(x for x in c if x) if any(c) else '')
def refine(defs,rows,passives,report):
 restored={};counts=collections.Counter();reports={r['id']:r for r in report}
 for id,d in defs.items():
  source=rows[id];branch=id.split('_')[0];r=reports[id];bonuses=source['bonuses'];exact=all(supported(b) for b in bonuses)
  removed={'ars_nouveau:ars_nouveau.perk.max_mana','ars_nouveau:ars_nouveau.perk.mana_regen','ars_nouveau:ars_nouveau.perk.spell_damage'}
  rewards=[w for w in d['rewards'] if w.get('data',{}).get('attribute') not in removed]
  lines=[t for t in d['description'].splitlines() if not(t.startswith('Ars Nouveau:') and any(s in t for s in ['maximum mana','mana regeneration','spell damage']))]
  if exact:
   restored[id]=bonuses
   rewards=[w for w in rewards if w['type']=='puffish_skills:attribute' and w['data']['attribute'].startswith(('irons_spellbooks:','ars_nouveau:'))]
   lines=[source_text(b) for b in bonuses]+[t for t in lines if t.startswith(("Iron's Spells:",'Ars Nouveau:'))]
   if id in passives:
    wind=passives[id].get('second_wind');passives.pop(id)
    if wind:passives[id]={'second_wind':wind};lines.append('Second Wind: survive a hit below 25% Health to recover 10% Maximum Health. Cooldown: 180s.')
   r['modernized']=[];counts['source_faithful_nodes']+=1
  else:
   for b in bonuses:
    if kind(b) in ('crit_chance','crit_damage'):
     a='apothic_attributes:'+kind(b);v=b.get('chance',b.get('amount'))
     for w in rewards:
      if w.get('data',{}).get('attribute')==a:w['data']['value']=v
     lines=[t for t in lines if not t.startswith('Apothic Attributes:')]
     lines.append(f'+{v*100:g} percentage points '+('Critical Chance' if kind(b)=='crit_chance' else 'Critical Damage')+' (Apothic Attributes; all eligible hits).')
   # Preserve comparable magnitudes even where the old trigger/system remains an explicit adaptation.
   scalar_specs={'jump_height':('puffish_attributes:jump','multiplier','Jump Height'), 'incoming_healing':('puffish_attributes:healing','multiplier','Healing Received'), 'gained_experience':('puffish_attributes:experience','multiplier','Vanilla Orb XP (all sources; not Character XP)')}
   for b in bonuses:
    if kind(b) in scalar_specs:
     target,field,label=scalar_specs[kind(b)];v=b[field]
     for w in rewards:
      if w.get('data',{}).get('attribute')==target:w['data']['value']=v
     markers={'jump_height':['jump height'],'incoming_healing':['healing received'],'gained_experience':['vanilla-orb XP']}[kind(b)]
     lines=[t for t in lines if not any(m.lower() in t.lower() for m in markers)];lines.append(f'+{100*v:g}% '+label)
    if kind(b)=='healing' and kind(b.get('event_listener',{}))=='attack' and id in passives and 'leech' in passives[id]:
     v=b['amount'];passives[id]['leech']=v;lines=[t for t in lines if not t.startswith('Combat recovery:')];lines.append(f'Combat Recovery: +{v:g} Health per hit. Once per second; total cap 1 Health.')
    if kind(b)=='crafted_item_bonus' and kind(b['item_bonus'])=='potion_duration' and id in passives:
     v=b['item_bonus']['multiplier'];passives[id]['potion_duration']=v;lines=[t for t in lines if 'duration of non-instant beneficial potions' not in t];lines.append(f'+{100*v:g}% beneficial Potion Duration when you drink it. Total cap: +50%.')
    if kind(b)=='attribute' and b['attribute'] in ['skilltree:evasion','skilltree:blocking'] and kind(b.get('player_multiplier',{}))=='none' and id in passives:
     stat='evasion' if b['attribute'].endswith('evasion') else 'guard';v=b['amount']/100;passives[id][stat]=v
     lines=[t for t in lines if not ('chance to evade' in t or 'chance to halve' in t)]
     lines.append(f'{100*v:g}% '+('Physical Evasion chance. Total cap: 15%.' if stat=='evasion' else 'chance to halve an attack while actively using a Shield. Total cap: 25%.'))
   if not r['modernized']:r['modernized']=['Personal replacement for an old-mod attribute, condition or specialized trigger.']
   counts['adapted_nodes']+=1
  if id in restored or id in passives:rewards=[w for w in rewards if w['type']!='puffish_skills:tag']+[{'type':'puffish_skills:tag','data':{'tag':'tnk_tree.'+id}}]
  for i,t in enumerate(lines):
   t=t.replace("Iron's Spells:",'Spellcasting:').replace('base mana regeneration','Mana Regeneration (base)').replace('maximum mana','Maximum Mana').replace('base spell power','Spell Power (base)')
   lines[i]=t.rstrip('.')
  sub=next((SUBS[branch][i-1] for i in [1,2] if f'_subclass_{i}' in id),None);group=BRANCHES[branch]+(' / '+sub if sub else ' / Core')
  tier='Keystone' if source['buttonSize']==32 else 'Root' if source['isStartingPoint'] else 'Notable' if source['buttonSize']>=20 else 'Minor'
  d['description']='\n'.join(['Towns & Kingdoms',group,tier+' · 1 Skill Point'+(' · Adapted' if not exact else ''),'']+[textwrap.fill('• '+s,52,subsequent_indent='  ',break_long_words=False,break_on_hyphens=False) for s in dict.fromkeys(lines)])
  extra=['Refund: bonuses are removed on Respec.']
  if any('Maximum Mana' in t or 'Mana Regeneration' in t for t in lines):extra.insert(0,"Shared Mana · Iron's attribute only.");d['description']+="\nMana source: Iron's Spells (shared)."
  title_names={'alchemist':('Shadow','Guardian'),'blacksmith':('Soldier','Steel'),'cook':('Berserker','Tide'),'enchanter':('Blaze','Astral'),'hunter':('Ranger','Archer'),'miner':('Explorer','Amethyst')}
  for i in [1,2]:
   if id==f'{branch}_subclass_{i}_mastery':
    extra.insert(0,'Unlocks Title: '+title_names[branch][i-1]+' (60 spent points).');d['description']+='\nTitle: '+title_names[branch][i-1]+' · requires 60 spent points'
  if d.get('required_spent_points'):extra.insert(0,f"Requires {d['required_spent_points']} spent Skill Points.")
  if not exact:extra.append('Adapted effect: replaces an old-mod mechanic; see conversion guide.')
  if any(w.get('required') is False for w in rewards):extra.append('Named mod attributes require that mod to be installed.')
  d['extra_description']='\n'.join(textwrap.fill(t,52,break_long_words=False,break_on_hyphens=False) for t in extra);d['rewards']=rewards
  if source['isStartingPoint']:d['title']=BRANCHES[branch]
  r.update(title=d['title'],description=d['description'],runtime=passives.get(id,{}),original_bonuses=bonuses,value_policy='Original values and conditions' if exact else 'Explicit trigger/system adaptation; original comparable magnitudes where applicable',branch_path=group)
 return restored,dict(counts)
