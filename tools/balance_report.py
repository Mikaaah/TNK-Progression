#!/usr/bin/env python3
"""Deterministic pacing model, not a claim of measured playtime. Gates assumed open."""
import json,math,csv
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];d=json.loads((ROOT/'docs/BALANCE-PRESET.json').read_text())
level_cost=lambda level:int(d['levelBase']+d['levelFactor']*level**d['levelExponent'])
total=sum(level_cost(level) for level in range(1,100))
rows=[]
for key,(base,reward,rate) in d['activities'].items():
 xp=0;rank=0;work=0
 while xp<total:
  work+=int(base*(1+rank*d['activityGrowth'])**d['requirementExponent']+rank)
  xp+=int(reward*(1+rank*d['activityGrowth'])**d['rewardExponent']);rank+=1
 rows.append([key,rate,work,rank,round(work/rate,1)])
with (ROOT/'docs/BALANCE-PACING.csv').open('w') as f:
 w=csv.writer(f,lineterminator="\n");w.writerow(['Activity','Assumed eligible units/hour','Total units to level 100','Milestones','Modelled hours without quest XP']);w.writerows(rows)
print('XP from 1 to 100:',total)
for row in rows:print(row)
# Whole-tree coefficients, including restored source attributes; conditions/multipliers are not a build bound.
defs=json.loads((ROOT/'skilltree/rebuilt588/config/puffish_skills/categories/character/definitions.json').read_text());agg={}
for node in defs.values():
 assert node['cost']==1
 for r in node.get('rewards',[]):
  if r['type']=='puffish_skills:attribute':
   a=r['data'];key=(a['attribute'],a['operation']);agg[key]=agg.get(key,0)+a['value']
source=json.loads((ROOT/'skilltree/rebuilt588/config/puffish_skills/tnk_passives.json').read_text())
for bonuses in source.get('source_bonuses',{}).values():
 for b in bonuses:
  if b['type']=='skilltree:attribute':
   key=(b['attribute'],{0:'addition',1:'multiply_base',2:'multiply_total'}[b['operation']]);agg[key]=agg.get(key,0)+b['amount']
  elif b['type']=='skilltree:block_break_speed':
   key=('minecraft:player.block_break_speed','multiply_base');agg[key]=agg.get(key,0)+b['multiplier']
with (ROOT/'docs/BALANCE-PASSIVES.csv').open('w') as f:
 w=csv.writer(f,lineterminator="\n");w.writerow(['Attribute','Operation','Whole-tree coefficient sum (conditional multipliers excluded)']);w.writerows([k[0],k[1],round(v,5)] for k,v in sorted(agg.items()))
assert len(defs)==588
