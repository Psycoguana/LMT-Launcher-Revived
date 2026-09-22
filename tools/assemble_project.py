#!/usr/bin/env python3
"""Assemble app/src/main from the reference apktool tree + regenerated Java.

Resources are filtered to the transitive closure of what the app actually
references (see tools/resource_closure.py for the reasoning), plus the drawables
the app resolves dynamically by name through getIdentifier(). Everything else in
reference/apktool/res is a bundled support-library resource and is dropped.
"""
import os, re, sys, json, shutil
import xml.etree.ElementTree as ET
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES  = os.path.join(ROOT, "reference/apktool/res")
APK  = os.path.join(ROOT, "reference/apktool")
JAVA = os.path.join(ROOT, "generated/jadx/sources/com/noname81/lmt")
OUT  = os.path.join(ROOT, "app/src/main")

APP_LAYOUTS = {"color","listheader","listitem_description","listitem_descriptionicon",
               "listitem_icondescription","listitem_icondescriptionicon","lmt",
               "notification","toaster"}
APP_XML = {"service_config","backupscheme"}
REF = re.compile(r'[@?](?:\+)?(?:(\w+):)?(\w+)/([\w.]+)')

def str_array(fn, var):
    txt = open(os.path.join(JAVA, fn), encoding="utf-8", errors="replace").read()
    m = re.search(r'String\[\]\s+' + var + r'\s*=\s*\{(.*?)\};', txt, re.S)
    return re.findall(r'"([^"]*)"', m.group(1))

# ---------- collect definitions from values-* ----------
defs = defaultdict(list)
for d in sorted(os.listdir(RES)):
    if not d.startswith("values"): continue
    for fn in sorted(os.listdir(os.path.join(RES, d))):
        if not fn.endswith(".xml") or fn == "public.xml": continue
        p = os.path.join(RES, d, fn)
        try: root = ET.parse(p).getroot()
        except ET.ParseError: continue
        for el in root:
            if not isinstance(el.tag, str): continue
            name = el.get("name")
            if name is None: continue
            t = el.tag
            if t == "item": t = el.get("type") or "item"
            if t == "declare-styleable": t = "styleable"
            defs[(t, name)].append((p, el))

def refs_in_text(txt):
    out = set()
    for m in REF.finditer(txt or ""):
        ns, t, n = m.group(1), m.group(2), m.group(3)
        if ns == "android": continue
        out.add((t, n))
    return out

def refs_in_element(el):
    out = set()
    for txt in list(el.itertext()): out |= refs_in_text(txt)
    for v in el.attrib.values():   out |= refs_in_text(v)
    par = (el.get("parent") or "").strip()
    if par and not par.startswith("android:"):
        if par.startswith("@") or par.startswith("?"): out |= refs_in_text(par)
        else: out.add(("style", par))
    return out

def files_for(t, name):
    out = []
    for d in sorted(os.listdir(RES)):
        if d.split("-")[0] != t: continue
        dp = os.path.join(RES, d)
        if not os.path.isdir(dp): continue
        for fn in os.listdir(dp):
            if fn.split(".")[0] == name: out.append(os.path.join(dp, fn))
    return out

# ---------- seeds ----------
seeds = set()
for fn in os.listdir(JAVA):
    if not fn.endswith(".java") or fn == "R.java": continue
    txt = open(os.path.join(JAVA, fn), encoding="utf-8", errors="replace").read()
    for m in re.finditer(r'\bR\.(\w+)\.(\w+)', txt): seeds.add((m.group(1), m.group(2)))
seeds |= refs_in_text(open(os.path.join(APK, "AndroidManifest.xml"), encoding="utf-8").read())
seeds |= {("layout", n) for n in APP_LAYOUTS}
seeds |= {("xml", n) for n in APP_XML}
seeds |= {("style","AppTheme"), ("style","AppBaseTheme"), ("mipmap","ic_launcher")}

# dynamic drawables resolved via getIdentifier()
existing_drawables = set()
for d in os.listdir(RES):
    if d.split("-")[0] != "drawable": continue
    for fn in os.listdir(os.path.join(RES, d)): existing_drawables.add(fn.split(".")[0])
dyn = set()
for a in str_array("Action.java", "names"):
    for suf in ("", "_kitkat", "_pixel", "_s"): dyn.add(a.lower() + suf)
for g in str_array("TouchServiceResult.java", "names"):
    for suf in ("", "_s"): dyn.add(g.lower() + suf)
dyn |= {"pielp_s", "isa_s", "ok"}
seeds |= {("drawable", n) for n in (dyn & existing_drawables)}

# ---------- closure ----------
seen, work, kept_files = set(), list(seeds), set()
while work:
    key = work.pop()
    if key in seen: continue
    seen.add(key)
    t, name = key
    for (_p, el) in defs.get(key, []):
        for r in refs_in_element(el):
            if r not in seen: work.append(r)
    for fp in files_for(t, name):
        kept_files.add(fp)
        if fp.endswith(".xml"):
            for r in refs_in_text(open(fp, encoding="utf-8", errors="replace").read()):
                if r not in seen: work.append(r)

# ---------- emit ----------
if os.path.isdir(OUT): shutil.rmtree(OUT)
os.makedirs(OUT)

# 1. non-values resource files
for fp in sorted(kept_files):
    rel = os.path.relpath(fp, RES)
    dst = os.path.join(OUT, "res", rel)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copy2(fp, dst)

# 2. values-* entries, keeping only reached keys, grouped by source dir
emitted = defaultdict(list)
for key in sorted(seen):
    for (p, el) in defs.get(key, []):
        qual = os.path.basename(os.path.dirname(p))
        emitted[qual].append((key, el))
for qual, items in sorted(emitted.items()):
    d = os.path.join(OUT, "res", qual)
    os.makedirs(d, exist_ok=True)
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for (t, n), el in items:
        s = ET.tostring(el, encoding="unicode").strip()
        s = re.sub(r'\s+xmlns:\w+="[^"]*"', "", s)
        lines.append("    " + s)
    lines.append("</resources>")
    open(os.path.join(d, "values.xml"), "w", encoding="utf-8").write("\n".join(lines) + "\n")

# 3. java, assets, jniLibs
dstj = os.path.join(OUT, "java/com/noname81/lmt"); os.makedirs(dstj)
for fn in sorted(os.listdir(JAVA)):
    if fn.endswith(".java") and fn != "R.java":
        shutil.copy2(os.path.join(JAVA, fn), os.path.join(dstj, fn))
shutil.copytree(os.path.join(APK, "assets"), os.path.join(OUT, "assets"))
shutil.copytree(os.path.join(APK, "lib"),    os.path.join(OUT, "jniLibs"))

# ---------- report ----------
by_type = defaultdict(set)
for t, n in seen: by_type[t].add(n)
print("resource closure:")
for t in sorted(by_type): print(f"  {t:12s} {len(by_type[t]):4d}")
print(f"\nresource files copied : {len(kept_files)}")
print(f"values qualifier dirs : {len(emitted)}  ({', '.join(sorted(emitted))})")
print(f"java files copied     : {len(os.listdir(dstj))}")
