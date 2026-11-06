#!/usr/bin/env python3
"""Edit pack-wide UUID title grants. Ship the edited config to clients and servers."""
import argparse
import json
from pathlib import Path
import re
import uuid

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, default=ROOT / 'integration/kubejs/config/tnk-title-defaults.json')
    commands = parser.add_subparsers(dest='action', required=True)
    commands.add_parser('list')
    for action in ('add', 'remove'):
        sub = commands.add_parser(action)
        sub.add_argument('uuid', type=lambda value: str(uuid.UUID(value)))
        sub.add_argument('title', nargs='?', default='tnk:beta_tester')
        if action == 'add':
            sub.add_argument('--name', help='Optional name note; UUID is the identity')
    args = parser.parse_args()
    data = json.loads(args.config.read_text(encoding='utf-8'))
    if data.get('schemaVersion') != 1 or not isinstance(data.get('grants'), dict):
        parser.error('Unsupported title roster')
    if args.action == 'list':
        print(json.dumps(data['grants'], indent=2))
        return
    if not re.fullmatch(r'[a-z0-9_.-]+:[a-z0-9_./-]+', args.title) or len(args.title) > 100:
        parser.error('Invalid title ID')
    # Normalize existing entries and reject duplicate spellings of the same UUID.
    grants = {}
    for key, titles in data['grants'].items():
        canonical = str(uuid.UUID(key))
        if canonical in grants or not isinstance(titles, list):
            parser.error('Invalid or duplicate UUID entry')
        grants[canonical] = titles
    titles = grants.setdefault(args.uuid, [])
    if args.action == 'add':
        if args.title not in titles:
            titles.append(args.title)
        if args.name:
            data.setdefault('names', {})[args.uuid] = args.name
    else:
        titles[:] = [title for title in titles if title != args.title]
        if not titles:
            grants.pop(args.uuid, None)
            data.setdefault('names', {}).pop(args.uuid, None)
    data['grants'] = grants
    temporary = args.config.with_suffix('.json.tmp')
    temporary.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    temporary.replace(args.config)
    print(f'{args.action}: {args.uuid} / {args.title}; distribute {args.config}')


if __name__ == '__main__':
    main()
