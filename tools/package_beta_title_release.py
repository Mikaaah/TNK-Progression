#!/usr/bin/env python3
"""Reproduce the 0.6.7 resource update without recompiling/changing Java classes.

Usage: python tools/package_beta_title_release.py accepted-0.6.6.jar advancedpack.zip output.jar
"""
import copy
import json
import os
from pathlib import Path
import sys
import zipfile
from embed_prefix_artwork import embed

ROOT = Path(__file__).resolve().parents[1]


def build(baseline, artwork, target):
    baseline, artwork, target = map(Path, (baseline, artwork, target))
    if baseline.resolve() == target.resolve():
        raise ValueError('Keep the accepted baseline JAR as a separate file')
    with zipfile.ZipFile(baseline) as archive:
        assert archive.testzip() is None
        assert b'version="0.6.6-beta.1"' in archive.read('META-INF/neoforge.mods.toml')
    embed(baseline, artwork, target, '0.6.7-beta.1')
    catalog = ROOT / 'src/main/resources/data/tnkprogression/default_titles.json'
    temporary = target.with_suffix('.tmp')
    with zipfile.ZipFile(target) as source, zipfile.ZipFile(temporary, 'w', zipfile.ZIP_DEFLATED) as output:
        for item in source.infolist():
            data = catalog.read_bytes() if item.filename == 'data/tnkprogression/default_titles.json' else source.read(item.filename)
            output.writestr(copy.copy(item), data)
    os.replace(temporary, target)
    with zipfile.ZipFile(baseline) as original, zipfile.ZipFile(target) as release:
        assert release.testzip() is None
        for name in original.namelist():
            if name.endswith(('.class', '.png')):
                assert release.read(name) == original.read(name), name
        old = json.loads(original.read('data/tnkprogression/default_titles.json'))
        new = json.loads(release.read('data/tnkprogression/default_titles.json'))
        assert new['titles'][:-1] == old['titles']
        assert new['titles'][-1]['id'] == 'tnk:beta_tester'
    print('BETA_TITLE_RESOURCE_RELEASE_OK', target)


if __name__ == '__main__':
    build(*sys.argv[1:])
