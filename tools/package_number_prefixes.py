#!/usr/bin/env python3
"""Package original licensed number images byte-for-byte. Never recolours or edits assets.
Usage: python tools/package_number_prefixes.py /path/advancedpack.zip /path/output.zip
The input artwork is user-supplied and is not stored in this public repository.
"""
import sys,json,zipfile,hashlib
from pathlib import Path
COLOURS=['ŻELAZO','MIEDŹ','ZŁOTO','SZMARAGD','PRYZMARYN','DIAMENT','LAPIS','AMETYST','RUBIN','OBSYDIAN','KWARC']
def package(source,target):
    manifest=[];providers=[]
    with zipfile.ZipFile(source) as src,zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as out:
        for level in range(1,101):
            colour=COLOURS[min(level//10,10)]
            name=f'nexo/pack/assets/generated/textures/glyphs/NUSSE-{level}-{colour}.png'
            data=src.read(name);texture=f'assets/tnkprogression/textures/font/numbers/level_{level}.png'
            out.writestr(texture,data)
            providers.append({'type':'bitmap','file':f'tnkprogression:font/numbers/level_{level}.png','height':9,'ascent':8,'chars':[chr(0xE000+level)]})
            manifest.append({'level':level,'colour':colour,'source':name,'target':texture,'sha256':hashlib.sha256(data).hexdigest()})
        out.writestr('assets/tnkprogression/font/level_badges.json',json.dumps({'providers':providers},indent=2))
        out.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':34,'description':'TNK number prefixes 1–100 · original Advanced Pack artwork'}}))
        out.writestr('number-prefix-map.json',json.dumps(manifest,indent=2,ensure_ascii=False))
        out.writestr('README.txt','Enable this client resource pack above older prefix packs. Requires TNK Progression 0.2.0. Original artwork is unchanged. Level zero uses plain text; the supplied pack has no zero image.\n')
    with zipfile.ZipFile(target) as out,zipfile.ZipFile(source) as src:
        for row in manifest: assert out.read(row['target'])==src.read(row['source'])
    print('Packaged and byte-verified 100 unmodified number images in 11 colour brackets.')
if __name__=='__main__':package(sys.argv[1],sys.argv[2])
