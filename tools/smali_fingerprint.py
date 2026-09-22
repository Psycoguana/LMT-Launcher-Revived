#!/usr/bin/env python3
"""Behavioural fingerprint of a smali tree, for comparing two builds of LMT.

A raw textual diff of two smali trees is useless here: register allocation, line
numbers, synthetic class names and resource ids all shift legitimately between the
apktool-rebuilt reference and our Gradle build. This reduces each method to the
things that actually determine behaviour:

  * the opcode sequence (mnemonics only, registers dropped)
  * every method and field reference it invokes
  * every string literal
  * every numeric literal, with resource ids (0x7f......) normalised away

Two methods with the same fingerprint do the same thing. A difference is a real
behavioural difference and must be explained before it is accepted.
"""
import os, re, sys, json, hashlib
from collections import OrderedDict

# Package renames that are intentional and must not count as differences.
RENAMES = [
    (re.compile(r'Landroid/support/v4/view/ViewPager'), 'Landroidx/viewpager/widget/ViewPager'),
    (re.compile(r'Landroid/support/v13/app/FragmentStatePagerAdapter'),
     'Lcom/noname81/lmt/FrameworkFragmentStatePagerAdapter'),
    (re.compile(r'Landroid/support/v4/view/PagerAdapter'), 'Landroidx/viewpager/widget/PagerAdapter'),
]
RES_ID   = re.compile(r'0x7f[0-9a-f]{6}')
R_FIELD  = re.compile(r'Lcom/noname81/lmt/R\$(\w+);->([\w.]+):I')
ID_MAP   = {}
OPCODE   = re.compile(r'^\s{4}([a-z][a-z0-9/-]*)\b')
STRING   = re.compile(r'const-string(?:/jumbo)?\s+[vp]\d+,\s*"((?:[^"\\]|\\.)*)"')
REF      = re.compile(r'(L[\w/$]+;->[\w$<>]+[^\s]*)')
NUM      = re.compile(r'(RES:@[\w.]+/[\w.]+|-?0x[0-9a-fA-F]+|-?\d+)')

SKIP_OPS = {'.line', '.local', '.end', '.restart', '.param', '.prologue', '.source'}

def canon(text):
    for pat, rep in RENAMES:
        text = pat.sub(rep, text)
    # D8 emits explicit null checks as Object.getClass(); their presence varies
    # by compiler version and they cannot change behaviour.
    text = re.sub(r'Ljava/lang/Object;->getClass\(\)Ljava/lang/Class;', 'NULLCHECK', text)
    # jadx infers a concrete collection type where the original declared the
    # interface. Same call, different static type.
    text = re.sub(r'Ljava/util/(?:LinkedList|ArrayList|AbstractList);->', 'Ljava/util/List;->', text)
    text = re.sub(r'Ljava/util/(?:TreeMap|HashMap|LinkedHashMap|SortedMap|NavigableMap);->', 'Ljava/util/Map;->', text)
    text = re.sub(r'Ljava/util/(?:HashSet|TreeSet|LinkedHashSet|SortedSet);->', 'Ljava/util/Set;->', text)
    # Compiler-assigned names that carry no behaviour: synthetic accessor numbers,
    # anonymous/lambda class numbering, and the numbering of switch-map classes.
    text = re.sub(r'access\$\d+', 'access$N', text)
    # Normalise every compiler-named nested class inside type references, in all
    # its flavours: anonymous ($1), named local ($1LongpressRunnable - the digit
    # prefix is the compiler's), and desugared lambdas.
    def _anon_type(m):
        segs = m.group(1).split('$')
        segs = ['ANON' if (seg[:1].isdigit()
                           or seg.startswith('$ExternalSyntheticLambda')
                           or re.fullmatch(r'ExternalSyntheticLambda\d+', seg))
                else seg for seg in segs]
        return 'Lcom/noname81/lmt/' + '$'.join(segs) + ';'
    text = re.sub(r'Lcom/noname81/lmt/([\w$]+);', _anon_type, text)
    # An `sget` on R$type->name and an inlined 0x7f... constant are the same
    # resource reference; normalise both to "@type/name" so the two builds compare.
    text = R_FIELD.sub(lambda m: f'RES:@{m.group(1)}/{m.group(2)}', text)
    def sub_id(m):
        return 'RES:' + ID_MAP.get(str(int(m.group(0), 16)), '@unknown/' + m.group(0))
    return RES_ID.sub(sub_id, text)

def fingerprint_method(body):
    ops, refs, strs, nums = [], [], [], []
    for line in body:
        s = line.strip()
        if not s or s.startswith('#'):
            continue
        if s.startswith('.'):
            continue          # directives carry no behaviour
        if s.startswith(':'):
            continue          # labels: positions are implied by the opcode order
        m = OPCODE.match(line)
        if m:
            ops.append(m.group(1))
        sm = STRING.search(line)
        if sm:
            strs.append(sm.group(1))
        for r in REF.findall(line):
            refs.append(r)
        for rr in re.findall(r'RES:@[\w.]+/[\w.]+', line):
            nums.append(rr)
        # Numeric literals only. Take the operand after the last comma, so the
        # opcode width ("const/4") and register numbers ("v2") are not mistaken
        # for literals - they are allocation details, not behaviour.
        head = s.split(' ')[0]
        if head.startswith('const'):
            operand = s.rsplit(',', 1)[-1].strip() if ',' in s else ''
            if operand and not operand.startswith('"') and not operand.startswith('RES:'):
                nums.append(operand.split('#')[0].strip())
        elif head in ('.packed-switch', '.sparse-switch'):
            nums.append(s.split(' ', 1)[-1].strip())
        elif head.startswith('fill-array-data') or head == '.array-data':
            pass
    return {'ops': ops, 'refs': refs, 'strings': strs, 'nums': nums}

def parse_class(path):
    text = canon(open(path, encoding='utf-8', errors='replace').read())
    lines = text.split('\n')
    methods, fields, cur, name = OrderedDict(), [], None, None
    for line in lines:
        s = line.strip()
        if s.startswith('.field'):
            fields.append(re.sub(r'\s+', ' ', s))
        elif s.startswith('.method'):
            name = re.sub(r'^\.method\s+', '', s)
            # access flags can legitimately differ in ordering; sort them
            parts = name.split(' ')
            sig = parts[-1]
            flags = sorted(parts[:-1])
            name = ' '.join(flags + [sig])
            cur = []
        elif s.startswith('.end method'):
            if cur is not None and name:
                methods[name] = fingerprint_method(cur)
            cur, name = None, None
        elif cur is not None:
            cur.append(line)
    return {'fields': sorted(fields), 'methods': methods}

def scan(root):
    out = {}
    for dirpath, _dirs, files in os.walk(root):
        for fn in files:
            if not fn.endswith('.smali'):
                continue
            p = os.path.join(dirpath, fn)
            rel = os.path.relpath(p, root)
            out[rel] = parse_class(p)
    return out

if __name__ == '__main__':
    if len(sys.argv) > 3:
        ID_MAP.update(json.load(open(sys.argv[3])))
    json.dump(scan(sys.argv[1]), open(sys.argv[2], 'w'), indent=0, sort_keys=True)
    print(f"fingerprinted {len(scan(sys.argv[1]))} classes -> {sys.argv[2]}")
