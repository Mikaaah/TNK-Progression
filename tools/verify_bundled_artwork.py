#!/usr/bin/env python3
"""Check that the embedded TNK badges match the recorded original PNG bytes."""

import hashlib
import json
from pathlib import Path


def main():
    resources = Path(__file__).resolve().parents[1] / 'src/main/resources'
    assets = resources / 'assets/tnkprogression'
    manifest = json.loads((assets / 'prefix-artwork-manifest.json').read_text())
    catalog = json.loads((resources / 'data/tnkprogression/default_titles.json').read_text())
    if len(manifest['numbers']) != 100 or len(manifest['titles']) != len(catalog['titles']):
        raise ValueError('Incomplete number/title artwork manifest')
    targets = set()
    for rows in manifest.values():
        for row in rows:
            target = (resources / row['target']).resolve()
            if not target.is_relative_to(resources.resolve()) or target in targets:
                raise ValueError('Invalid or duplicate artwork target')
            targets.add(target)
            data = target.read_bytes()
            if not data.startswith(b'\x89PNG\r\n\x1a\n'):
                raise ValueError(f'Invalid PNG: {row["target"]}')
            if hashlib.sha256(data).hexdigest() != row['sha256']:
                raise ValueError(f'Artwork hash mismatch: {row["target"]}')
    for name in ['level_badges', 'title_badges']:
        font = json.loads((assets / 'font' / f'{name}.json').read_text())
        for provider in font['providers']:
            if provider['type'] != 'bitmap':
                continue
            namespace, path = provider['file'].split(':', 1)
            if not (resources / 'assets' / namespace / 'textures' / path).is_file():
                raise ValueError(f'Missing font image: {provider["file"]}')
    print(f'TNK_BUNDLED_ARTWORK_OK: 100 number badges, {len(manifest["titles"])} title badges; original PNG hashes verified.')


if __name__ == '__main__':
    main()
