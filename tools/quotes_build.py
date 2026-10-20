"""Builds app/src/main/assets/quotes/<lang>.json from tools/i18n_src/quotes_<lang>.txt.

Each line is "<id>|<translated quote>". Ids, authors and themes come from quotes/en.json, so a
translator only touches the words. Missing ids fall back to the English quote.

  python tools/quotes_build.py            build every language
  python tools/quotes_build.py es fr      build some
"""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'tools', 'i18n_src')
OUT = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'quotes')
LANGS = ['hi', 'ur', 'es', 'pt', 'id', 'ar', 'tr', 'fr']


def build(lang, english):
    table = {}
    for line in open(os.path.join(SRC, f'quotes_{lang}.txt'), encoding='utf-8'):
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        qid, _, text = line.partition('|')
        table[qid.strip()] = text.strip()
    missing = [q['id'] for q in english if q['id'] not in table]
    unknown = [i for i in table if i not in {q['id'] for q in english}]
    out = [dict(q, t=table.get(q['id'], q['t'])) for q in english]
    with open(os.path.join(OUT, lang + '.json'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('[\n' + ',\n'.join(json.dumps(q, ensure_ascii=False, separators=(',', ':')) for q in out) + '\n]\n')
    print(f'{lang}: {len(english) - len(missing)}/{len(english)}', 'OK' if not missing and not unknown else '')
    if missing: print('   missing:', missing)
    if unknown: print('   unknown ids:', unknown)
    return not missing and not unknown


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    english = json.load(open(os.path.join(OUT, 'en.json'), encoding='utf-8'))
    ok = all([build(l, english) for l in sys.argv[1:] or LANGS])
    sys.exit(0 if ok else 1)
