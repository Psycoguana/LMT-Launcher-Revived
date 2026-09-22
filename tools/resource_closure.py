#!/usr/bin/env python3
"""Compute the transitive set of resources the LMT app actually uses.

Seeds come from R.<type>.<name> in the 41 app Java files, plus @<type>/<name> in
AndroidManifest.xml and the app's own layout/xml files. The closure then follows
references inside every kept values entry and kept XML drawable, so styles pull in
their parents, drawables pull in their colors, and so on.

Everything not reached is a bundled support-library resource and gets dropped.
"""
import os, re, sys, xml.etree.ElementTree as ET
from collections import defaultdict

RES = sys.argv[1]
JAVA = sys.argv[2]

# --- app-authored files (everything else in layout/ is library-owned) ---
APP_LAYOUTS = {"color", "listheader", "listitem_description", "listitem_descriptionicon",
               "listitem_icondescription", "listitem_icondescriptionicon", "lmt",
               "notification", "toaster"}
APP_XML = {"service_config", "backupscheme"}

REF = re.compile(r'[@?](?:android:)?(\w+)/([\w.]+)')
ANDROID_REF = re.compile(r'[@?]android:')

def parse_values():
    """name -> {(type,name): (file, element_string)} for every values-* entry."""
    defs = defaultdict(list)          # (type,name) -> [(path, elem)]
    for d in sorted(os.listdir(RES)):
        if not d.startswith("values"):
            continue
        for fn in sorted(os.listdir(os.path.join(RES, d))):
            if not fn.endswith(".xml") or fn == "public.xml":
                continue
            p = os.path.join(RES, d, fn)
            try:
                root = ET.parse(p).getroot()
            except ET.ParseError:
                continue
            for el in root:
                if not isinstance(el.tag, str):
                    continue
                t = el.tag
                name = el.get("name")
                if name is None:
                    continue
                if t == "item":
                    t = el.get("type") or "item"
                if t == "declare-styleable":
                    t = "styleable"
                defs[(t, name)].append((p, el))
    return defs

def refs_in_element(el):
    out = set()
    for txt in list(el.itertext()) + [v for v in el.attrib.values()]:
        if not txt:
            continue
        for m in REF.finditer(txt):
            if ANDROID_REF.match(txt[max(0, m.start()-0):m.start()+9]):
                pass
            out.add((m.group(1), m.group(2)))
    # style parent="..."
    par = el.get("parent")
    if par:
        par = par.strip()
        if par.startswith("@") or par.startswith("?"):
            m = REF.search(par)
            if m:
                out.add((m.group(1), m.group(2)))
        elif par and not par.startswith("android:"):
            out.add(("style", par))
    return out

def refs_in_file(path):
    out = set()
    try:
        txt = open(path, encoding="utf-8", errors="replace").read()
    except Exception:
        return out
    for m in REF.finditer(txt):
        # skip @android:... framework refs
        start = m.start()
        if txt[start:start+9].startswith("@android:") or txt[start:start+9].startswith("?android:"):
            continue
        out.add((m.group(1), m.group(2)))
    return out

defs = parse_values()

# ---- seeds ----
seeds = set()
for fn in os.listdir(JAVA):
    if not fn.endswith(".java") or fn == "R.java":
        continue
    txt = open(os.path.join(JAVA, fn), encoding="utf-8", errors="replace").read()
    for m in re.finditer(r'\bR\.(\w+)\.(\w+)', txt):
        seeds.add((m.group(1), m.group(2)))

seeds |= refs_in_file(os.path.join(os.path.dirname(RES), "AndroidManifest.xml"))
for n in APP_LAYOUTS:
    seeds.add(("layout", n))
for n in APP_XML:
    seeds.add(("xml", n))
seeds.add(("style", "AppTheme"))
seeds.add(("style", "AppBaseTheme"))
seeds.add(("mipmap", "ic_launcher"))

# ---- closure ----
def file_candidates(t, name):
    """Non-values resource files for (type,name), across all qualifier dirs."""
    out = []
    for d in sorted(os.listdir(RES)):
        base = d.split("-")[0]
        if base != t:
            continue
        dp = os.path.join(RES, d)
        if not os.path.isdir(dp):
            continue
        for fn in os.listdir(dp):
            stem = fn.split(".")[0]
            if stem == name:
                out.append(os.path.join(dp, fn))
    return out

seen = set()
work = list(seeds)
kept_files = set()
while work:
    key = work.pop()
    if key in seen:
        continue
    seen.add(key)
    t, name = key
    # values-defined?
    for (p, el) in defs.get(key, []):
        for r in refs_in_element(el):
            if r not in seen:
                work.append(r)
    # file-based?
    for fp in file_candidates(t, name):
        kept_files.add(fp)
        if fp.endswith(".xml"):
            for r in refs_in_file(fp):
                if r not in seen:
                    work.append(r)

# ---- report ----
by_type = defaultdict(set)
for t, n in seen:
    by_type[t].add(n)

print("REACHED RESOURCE TYPES")
for t in sorted(by_type):
    print(f"  {t:12s} {len(by_type[t]):4d}")
print(f"\nkept non-values files: {len(kept_files)}")

import json
json.dump({"keys": sorted(f"{t}/{n}" for t, n in seen),
           "files": sorted(kept_files)},
          open("/tmp/res_closure.json", "w"), indent=1)
