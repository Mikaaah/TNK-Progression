"""Apply TNK's tooltip palette using Pufferfish's native vanilla text components."""
import json
import re
from pathlib import Path

PALETTE = {
    'title': '#E8C989',
    'brand': '#B9A5CA',
    'branch': '#C6A0F6',
    'muted': '#B8ADBF',
    'bonus': '#A8D5A2',
    'condition': '#C6BCB0',
    'mana': '#C6B0F2',
}


def plain(value):
    if isinstance(value, str):
        return value
    if isinstance(value, list):
        return ''.join(map(plain, value))
    return value.get('text', '') + ''.join(map(plain, value.get('extra', [])))


def component(text, role, bold=False):
    return {'text': text, 'color': PALETTE[role], 'bold': bold, 'italic': False}


def description(text, extra=False):
    parts = []
    role = 'muted' if extra else 'bonus'
    for index, line in enumerate(text.splitlines(keepends=True)):
        bold = False
        if not extra and index < 3:
            role = ('brand', 'branch', 'muted')[index]
            bold = index == 1
        elif line.startswith(('Title:', 'Unlocks Title:')):
            role = 'title'
            bold = True
        elif line.startswith(('Mana source:', 'Shared Mana')):
            role = 'mana'
        elif line.startswith(('Requires ', 'Refund:', 'Adapted effect:', 'Named mod')):
            role = 'muted'
        elif line.startswith('• '):
            role = 'bonus'
        # Preserve wrapping and all wording; carry a condition's color across lines.
        marker = re.search(r' — |\bwhile\b|\bwhen\b|\bwith equipped\b|\bagainst\b|\brequires\b|\bTotal cap:|\bCooldown:', line)
        if marker and index >= 3 and not extra:
            parts.append(component(line[:marker.start()], role, bold))
            role = 'condition'
            parts.append(component(line[marker.start():], role))
        else:
            parts.append(component(line, role, bold))
    return {'text': '', 'extra': parts}


def style_definitions(definitions):
    for definition in definitions.values():
        definition['title'] = component(plain(definition['title']), 'title', True)
        definition['description'] = description(plain(definition['description']))
        definition['extra_description'] = description(plain(definition['extra_description']), extra=True)


if __name__ == '__main__':
    path = Path(__file__).resolve().parents[1] / 'skilltree/rebuilt588/config/puffish_skills/categories/character/definitions.json'
    definitions = json.loads(path.read_text())
    style_definitions(definitions)
    path.write_text(json.dumps(definitions, ensure_ascii=False, indent=2) + '\n')
    print(f'TNK_TOOLTIP_COLORS_OK: styled {len(definitions)} definitions.')
