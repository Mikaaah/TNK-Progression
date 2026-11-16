#!/usr/bin/env python3
"""Default direct-XP curve and passive coefficients; never a playtime estimate."""
import json, math, csv, re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
# Read the authoritative default Java fields. Legacy presets are migration history.
source=(ROOT/'src/main/java/dev/mikaaah/tnkprogression/BalanceConfig.java').read_text()
match=re.search(r'public double levelBase=([0-9.]+),levelFactor=([0-9.]+),levelExponent=([0-9.]+);',source)
if not match: raise SystemExit('Default curve declarations changed; update this report reader')
base,factor,exponent=map(float,match.groups())
rows=[];total=0
for level in range(1,100):
 cost=max(1,int(base+factor*math.pow(level,exponent)));total+=cost
 rows.append([level,level+1,cost,total])
with (ROOT/'docs/BALANCE-PACING.csv').open('w') as f:
 w=csv.writer(f,lineterminator="\n");w.writerow(['From level','To level','Default character XP cost','Cumulative XP from level 1']);w.writerows(rows)
print('Default direct XP from 1 to 100:',total)
print('Activity mastery and automatic crafting/engineering rewards are retired; no activity-rank playtime model is generated.')
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
