#!/usr/bin/env python3
"""Combine unchanged number/title artwork into one private client resource pack.
Usage: python tools/package_title_prefixes.py advancedpack.zip output.zip [tnk-titles.json]
Only copies the supplied PNG bytes. Labels without original art remain readable text.
"""
import sys,json,zipfile,tempfile,hashlib
from pathlib import Path
from package_number_prefixes import package
ROOT=Path(__file__).resolve().parents[1]
def build(source,target,catalog):
    defs=json.loads(Path(catalog).read_text())['titles'];manifest=[];providers=[];lang={};missing=[]
    assert len(defs)<=256
    with tempfile.TemporaryDirectory() as tmp:
        numbers=Path(tmp)/'numbers.zip';package(source,numbers)
        with zipfile.ZipFile(numbers) as base,zipfile.ZipFile(source) as src,zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as out:
            for name in base.namelist():
                if name not in ('pack.mcmeta','README.txt'):out.writestr(name,base.read(name))
            for index,d in enumerate(defs):
                name=f"nexo/pack/assets/generated/textures/glyphs/NUSSE-{d['artwork']}-{d['style']}.png"
                if name not in src.namelist():missing.append(d['id']);continue
                data=src.read(name);texture=f'assets/tnkprogression/textures/font/titles/title_{index}.png';out.writestr(texture,data)
                glyph=chr(0xE100+index);providers.append({'type':'bitmap','file':f'tnkprogression:font/titles/title_{index}.png','height':9,'ascent':8,'chars':[glyph]})
                key='tnkprogression.title.'+d['id'].replace(':','.').replace('/','.');assert key not in lang;lang[key]=glyph
                manifest.append({'id':d['id'],'source':name,'target':texture,'sha256':hashlib.sha256(data).hexdigest()})
            providers.append({'type':'reference','id':'minecraft:default'})
            out.writestr('assets/tnkprogression/font/title_badges.json',json.dumps({'providers':providers},indent=2))
            out.writestr('assets/tnkprogression/lang/en_us.json',json.dumps(lang,indent=2))
            out.writestr('title-prefix-map.json',json.dumps(manifest,indent=2,ensure_ascii=False))
            out.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':34,'description':'TNK number and earned-title prefixes · original supplied artwork'}}))
            out.writestr('README.txt','Enable above older TNK prefix packs; replaces the earlier number-only pack. Requires TNK Progression 0.3.0-beta.1 on server and clients. Number and title PNG files are unchanged originals. Missing custom title artwork uses readable text.\n')
        with zipfile.ZipFile(target) as out,zipfile.ZipFile(source) as src:
            for row in manifest:assert out.read(row['target'])==src.read(row['source'])
    print(f'Packaged {len(manifest)} unmodified title sprites; text-only custom titles: {missing}')
    return manifest
if __name__=='__main__':build(sys.argv[1],sys.argv[2],sys.argv[3] if len(sys.argv)>3 else ROOT/'src/main/resources/data/tnkprogression/default_titles.json')
