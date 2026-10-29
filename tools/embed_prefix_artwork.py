#!/usr/bin/env python3
"""Embed user-owned original number/title PNGs into a private release JAR.

No artwork is stored in the public repository. PNG bytes are copied unchanged.
Usage: python tools/embed_prefix_artwork.py base.jar advancedpack.zip output.jar [--version VERSION]
The output may be the input JAR, for the Gradle release-artwork hook.
"""
import argparse
import hashlib
import json
import os
import re
import tempfile
import zipfile
from pathlib import Path
from package_title_prefixes import build

ROOT = Path(__file__).resolve().parents[1]
LEGACY_SHEET = 'assets/tnkprogression/textures/font/level_badges.png'
LANG = 'assets/tnkprogression/lang/en_us.json'
MANIFEST = 'assets/tnkprogression/prefix-artwork-manifest.json'


def embed(base, source, target, version=None):
    base, source, target = map(Path, (base, source, target))
    target.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=target.parent) as tmp:
        pack, output = Path(tmp) / 'prefixes.zip', Path(tmp) / 'release.jar'
        build(source, pack, ROOT / 'src/main/resources/data/tnkprogression/default_titles.json')
        with zipfile.ZipFile(base) as original, zipfile.ZipFile(pack) as art:
            overlay = {n: art.read(n) for n in art.namelist() if n.startswith('assets/')}
            # Keep the character key labels; the optional pack alone contains only title translations.
            lang = json.loads(original.read(LANG))
            lang.update(json.loads(overlay[LANG]))
            overlay[LANG] = (json.dumps(lang, indent=2, ensure_ascii=False) + '\n').encode()
            manifest = {'numbers': json.loads(art.read('number-prefix-map.json')),
                        'titles': json.loads(art.read('title-prefix-map.json'))}
            assert len(manifest['numbers']) == 100 and len(manifest['titles']) == 55
            overlay[MANIFEST] = (json.dumps(manifest, indent=2, ensure_ascii=False) + '\n').encode()
            with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as out:
                for info in original.infolist():
                    if info.filename in overlay or info.filename == LEGACY_SHEET:
                        continue
                    data = original.read(info.filename)
                    if info.filename == 'META-INF/neoforge.mods.toml' and version:
                        data, count = re.subn(rb'(?m)^version="[^"]+"$',
                                              ('version="' + version + '"').encode(), data)
                        assert count == 1, 'Expected one mod version field'
                    out.writestr(info, data)
                for name, data in overlay.items():
                    out.writestr(name, data)
            with zipfile.ZipFile(output) as out, zipfile.ZipFile(source) as src:
                assert out.testzip() is None and LEGACY_SHEET not in out.namelist()
                assert len(out.namelist()) == len(set(out.namelist())), 'Duplicate JAR resources'
                for section in manifest.values():
                    for row in section:
                        data = out.read(row['target'])
                        assert data == src.read(row['source'])
                        assert hashlib.sha256(data).hexdigest() == row['sha256']
                for name in original.namelist():
                    if name.endswith('.class'):
                        assert original.read(name) == out.read(name), 'Artwork packaging must not modify code'
                assert json.loads(out.read(LANG))['key.tnkprogression.character'] == lang['key.tnkprogression.character']
                providers = json.loads(out.read('assets/tnkprogression/font/level_badges.json'))['providers']
                assert [p['chars'] for p in providers] == [[chr(0xE000 + n)] for n in range(1, 101)]
        os.replace(output, target)
    print('TNK_EMBEDDED_PREFIX_ARTWORK_OK: 100 original numbers, 55 original titles; all PNG and class bytes verified.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('base', type=Path)
    parser.add_argument('source', type=Path)
    parser.add_argument('target', type=Path)
    parser.add_argument('--version')
    args = parser.parse_args()
    if args.version and not re.fullmatch(r'[A-Za-z0-9.+_-]+', args.version):
        parser.error('Invalid version')
    embed(args.base, args.source, args.target, args.version)
