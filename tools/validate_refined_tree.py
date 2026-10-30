"""Validate the shipped update against the user's actual source archive, not its own generated labels."""
import json,zipfile,sys,csv
from style_tree_tooltips import plain
from pathlib import Path
root=Path(__file__).resolve().parents[1];base=root/'skilltree/rebuilt588';category=base/'config/puffish_skills/categories/character'
with zipfile.ZipFile(sys.argv[1]) as z:source={Path(n).stem:json.loads(z.read(n)) for n in z.namelist() if n.startswith('data/skilltree/skills/') and n.endswith('.json')}
defs=json.loads((category/'definitions.json').read_text());skills=json.loads((category/'skills.json').read_text());tables=json.loads((base/'config/puffish_skills/tnk_passives.json').read_text());catalog=json.loads((root/'src/main/resources/data/tnkprogression/default_titles.json').read_text())
assert len(defs)==588 and set(defs)==set(source)==set(skills)
assert len(tables['source_bonuses'])==141
for id,bonuses in tables['source_bonuses'].items():assert bonuses==source[id]['bonuses'],id
blocked={'ars_nouveau:ars_nouveau.perk.max_mana','ars_nouveau:ars_nouveau.perk.mana_regen','ars_nouveau:ars_nouveau.perk.spell_damage'}
for id,d in defs.items():
 assert d['cost']==1 and 'Towns & Kingdoms' in plain(d['description']) and 'Skill Point' in plain(d['description'])
 assert max(map(len,plain(d['description']).splitlines()))<=52,id
 for r in d['rewards']:
  assert r.get('data',{}).get('attribute') not in blocked,id
  if r['type']=='puffish_skills:tag':assert id in tables['skills'] or id in tables['source_bonuses']
 if id not in tables['source_bonuses']:assert 'Adapted effect' in plain(d['extra_description']),id
assert len(catalog['titles'])==55
new=[d for d in catalog['titles'] if d['id'].startswith('tnk:mastery_')];assert len(new)==12
for d in new:
 assert d['minSpentPoints']==60 and all(n in skills for n in d['requiredSkills'])
 assert d['label'] in plain(defs[d['requiredSkills'][0]]['extra_description'])
report=json.loads((base/'conversion-catalogue.json').read_text())
with (base/'ORIGINAL-VALUE-AUDIT.csv').open('w') as f:
 w=csv.writer(f);w.writerow(['Node','Branch / Subclass','Title','Value policy','Original bonus JSON','Implemented tooltip','Adaptations'])
 for r in report:w.writerow([r['id'],r['branch_path'],r['title'],r['value_policy'],json.dumps(r['original_bonuses'],ensure_ascii=False),r['description'],'; '.join(r['modernized'])])
print('TNK_SOURCE_AUDIT_OK: 588 nodes; 141 complete source bonus arrays identical; no duplicate Ars mana/power route; wrapped tooltips; 12 valid mastery titles / 55 total.')
