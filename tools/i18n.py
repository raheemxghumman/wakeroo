"""Wakeroo translation tooling.

  python tools/i18n.py wrap  <file.kt>...   wrap user-facing string literals in tr("...") (one-off migration)
  python tools/i18n.py keys                 print every catalogue key found in the code
  python tools/i18n.py check                list keys missing from each language in app/src/main/assets/i18n
  python tools/i18n.py stub <lang>          add missing keys to <lang>.json with an empty value

Keys are the English source text. Templates become numbered placeholders:
  "Rings in $until"  ->  tr("Rings in {0}", until)
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'app', 'src', 'main', 'java')
CATALOGUE = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'i18n')
LANGS = ['hi', 'ur', 'es', 'pt', 'id', 'ar', 'tr', 'fr']

# Calls whose string arguments are code, not copy.
SKIP_PREFIX = re.compile(
    r'(Log\.[a-z]\(|@ColumnInfo|@Suppress\(|@\w+\(|valueOf\(|ofPattern\(|setRequestProperty\(|Regex\(|getSharedPreferences\(|PreferencesKey\(|'
    r'error\(|setOf\(|label = $|const val |Uri\.parse\(|startsWith\(|endsWith\(|contains\(|getString\(|split\(|substringBefore|'
    r'\bn\($|\btr\($|Exception\(|require\(|check\(|TODO\(|putExtra\(|getStringExtra\(|setAction\(|replace\()'
)
DATE_PATTERN = re.compile(r"^[EMdyHhmsaLGuk ,.'/-]+$")


def lex_string(src, i):
    """Parse a regular Kotlin string starting at src[i] == '"'. Returns (end_index, parts) where parts is a
    list of ('text', s) and ('expr', code)."""
    assert src[i] == '"'
    j = i + 1
    parts, buf = [], ''
    while j < len(src):
        c = src[j]
        if c == '\\':
            buf += src[j:j + 2]; j += 2; continue
        if c == '"':
            if buf: parts.append(('text', buf))
            return j + 1, parts
        if c == '$' and j + 1 < len(src) and src[j + 1] == '{':
            if buf: parts.append(('text', buf)); buf = ''
            k, depth = j + 2, 1
            while k < len(src) and depth:
                ch = src[k]
                if ch == '"':
                    if src.startswith('"""', k):
                        k = src.index('"""', k + 3) + 3; continue
                    k, _ = lex_string(src, k); continue
                if ch == '{': depth += 1
                elif ch == '}': depth -= 1
                k += 1
            parts.append(('expr', src[j + 2:k - 1])); j = k; continue
        if c == '$' and j + 1 < len(src) and (src[j + 1].isalpha() or src[j + 1] == '_'):
            if buf: parts.append(('text', buf)); buf = ''
            m = re.match(r'[A-Za-z_][A-Za-z0-9_]*', src[j + 1:])
            parts.append(('expr', m.group(0))); j += 1 + len(m.group(0)); continue
        buf += c; j += 1
    raise ValueError('unterminated string')


def literals(src):
    """Yield (start, end, parts) for every regular string literal outside comments and raw strings."""
    i = 0
    while i < len(src):
        if src.startswith('//', i):
            i = src.find('\n', i); i = len(src) if i < 0 else i; continue
        if src.startswith('/*', i):
            i = src.index('*/', i) + 2; continue
        if src.startswith('"""', i):
            i = src.index('"""', i + 3) + 3; continue
        if src[i] == "'":
            m = re.match(r"'(\\.|[^\\'])'", src[i:])
            i += len(m.group(0)) if m else 1; continue
        if src[i] == '"':
            end, parts = lex_string(src, i)
            yield i, end, parts
            i = end; continue
        i += 1


def is_copy(parts):
    text = ''.join(p for kind, p in parts if kind == 'text')
    if not re.search(r'[A-Za-z]{2,}', text): return False
    if DATE_PATTERN.match(text): return False
    if re.match(r'^[a-z0-9_.:/-]+$', text): return False  # ids, keys, paths, urls
    if text.startswith(('http', 'android.', 'com.', 'package:', 'content:')): return False
    if re.search(r'[&=]', text) or text in ('SIM', 'UTF-8'): return False  # query strings, ids, charsets
    # A template with real words around its values ("$count objects") is copy too.
    if any(kind == 'expr' for kind, _ in parts) and re.search(r'[A-Za-z]{3,}', text): return True
    return text[:1].isupper() or ' ' in text.strip() or text.strip()[:1] in '"“#'


def as_key(parts):
    key, args = '', []
    for kind, p in parts:
        if kind == 'text': key += p
        else: key += '{%d}' % len(args); args.append(p)
    return key, args


def wrap_file(path):
    src = open(path, encoding='utf-8').read()
    out, last, count = [], 0, 0
    for start, end, parts in literals(src):
        line_start = src.rfind('\n', 0, start) + 1
        prefix = src[line_start:start]
        if SKIP_PREFIX.search(prefix) or prefix.rstrip().endswith(('tr(', 'n(')) or not is_copy(parts):
            continue
        key, args = as_key(parts)
        call = 'tr("%s"%s)' % (key, ''.join(', ' + a for a in args))
        out.append(src[last:start]); out.append(call); last = end; count += 1
    if not count: return 0
    out.append(src[last:])
    text = ''.join(out)
    if 'import com.wakeroo.app.ui.i18n.tr\n' not in text and not path.endswith('I18n.kt'):
        idx = text.index('\nimport ') + 1
        text = text[:idx] + 'import com.wakeroo.app.ui.i18n.tr\n' + text[idx:]
    open(path, 'w', encoding='utf-8').write(text)
    return count


def keys():
    found = {}
    for dirpath, _, files in os.walk(SRC):
        for f in files:
            if not f.endswith('.kt'): continue
            p = os.path.join(dirpath, f)
            src = open(p, encoding='utf-8').read()
            for m in re.finditer(r'\b(tr|n)\("', src):
                _, parts = lex_string(src, m.end() - 1)
                if all(kind == 'text' for kind, _ in parts):
                    key = ''.join(p for _, p in parts)
                    key = key.encode('utf-8').decode('unicode_escape') if '\\' in key else key
                    found.setdefault(key, os.path.relpath(p, SRC))
    return found


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    cmd = sys.argv[1]
    if cmd == 'wrap':
        total = sum(wrap_file(p) for p in sys.argv[2:])
        print('wrapped', total)
    elif cmd == 'keys':
        for k in sorted(keys()): print(k)
    elif cmd in ('check', 'stub'):
        all_keys = keys()
        langs = [sys.argv[2]] if cmd == 'stub' else LANGS
        bad = 0
        for lang in langs:
            path = os.path.join(CATALOGUE, lang + '.json')
            table = json.load(open(path, encoding='utf-8')) if os.path.exists(path) else {}
            missing = [k for k in all_keys if not table.get(k)]
            unused = [k for k in table if k not in all_keys]
            print(f'{lang}: {len(all_keys) - len(missing)}/{len(all_keys)} translated, {len(unused)} unused')
            for k in missing[:15]: print('   missing:', k)
            bad += len(missing)
            if cmd == 'stub':
                for k in missing: table.setdefault(k, '')
                json.dump(dict(sorted(table.items())), open(path, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
        sys.exit(1 if bad and cmd == 'check' else 0)


if __name__ == '__main__':
    main()
