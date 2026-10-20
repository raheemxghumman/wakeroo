"""Builds app/src/main/assets/i18n/<lang>.json from translator files in tools/i18n_src.

Each tools/i18n_src/<lang>.txt line is "<index>|<translation>", where index points into
tools/i18n_src/keys.json (the English keys, sorted). "\\n" stands for a line break.
Placeholders ({0}, %d, %02d) and leading/trailing spaces must match the English key.

keys.json is append-only, so existing indexes never move: --sync adds strings that are new in
the code to the end and lists them, ready to translate on new lines in every language file.

  python tools/i18n_build.py            build every language
  python tools/i18n_build.py es fr      build some
  python tools/i18n_build.py --sync     append new code strings to keys.json
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'tools', 'i18n_src')
OUT = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'i18n')
LANGS = ['hi', 'ur', 'es', 'pt', 'id', 'ar', 'tr', 'fr']
TOKEN = re.compile(r'\{\d+\}|%0?\d*[ds]|%\.\d+f')


def build(lang, keys):
    path = os.path.join(SRC, lang + '.txt')
    table, problems = {}, []
    for n, line in enumerate(open(path, encoding='utf-8'), 1):
        line = line.rstrip('\n')
        if not line.strip() or line.startswith('#'):
            continue
        idx, _, text = line.partition('|')
        i = int(idx)
        key = keys[i]
        text = text.replace('\\n', '\n')
        if sorted(TOKEN.findall(key)) != sorted(TOKEN.findall(text)):
            problems.append(f'line {n} (#{i}): placeholders differ: {key!r} -> {text!r}')
        # Keep the spacing that glues fragments together (" · rotating").
        lead = key[:len(key) - len(key.lstrip())]
        trail = key[len(key.rstrip()):]
        text = lead + text.strip() + trail
        if i in table:
            problems.append(f'line {n}: #{i} translated twice')
        table[i] = text
    missing = [i for i in range(len(keys)) if i not in table]
    os.makedirs(OUT, exist_ok=True)
    json.dump({keys[i]: v for i, v in sorted(table.items())}, open(os.path.join(OUT, lang + '.json'), 'w', encoding='utf-8'),
              ensure_ascii=False, indent=0)
    print(f'{lang}: {len(table)}/{len(keys)}', 'OK' if not missing and not problems else '')
    for p in problems: print('  ', p)
    if missing: print('   missing:', missing[:40])
    return not missing and not problems


def sync(keys):
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import i18n
    new = sorted(set(i18n.keys()) - set(keys))
    for k in new:
        print(f'{len(keys)}|{k!r}')
        keys.append(k)
    json.dump(keys, open(os.path.join(SRC, 'keys.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
    print(f'{len(new)} new keys')


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    keys = json.load(open(os.path.join(SRC, 'keys.json'), encoding='utf-8'))
    if sys.argv[1:] == ['--sync']:
        sync(keys)
        sys.exit(0)
    langs = sys.argv[1:] or LANGS
    ok = all([build(l, keys) for l in langs])
    sys.exit(0 if ok else 1)
