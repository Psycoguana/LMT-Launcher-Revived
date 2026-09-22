#!/usr/bin/env python3
"""Build id -> "@type/name" maps so both builds' resource references are comparable.

Resource ids are renumbered between builds, and an application module inlines R
constants while the apktool reference keeps `sget` on R$type fields. Mapping both
sides back to symbolic names lets the oracle actually verify resource wiring
instead of ignoring it.
"""
import re, sys, json, os

def from_public_xml(path):
    out = {}
    for m in re.finditer(r'<public type="(\w+)" name="([\w.]+)" id="(0x[0-9a-fA-F]+)"', open(path, encoding='utf-8').read()):
        out[int(m.group(3), 16)] = f"@{m.group(1)}/{m.group(2)}"
    return out

def from_r_txt(path):
    out = {}
    for line in open(path, encoding='utf-8'):
        parts = line.split()
        if len(parts) >= 4 and parts[0] == 'int':
            try: out[int(parts[3], 16)] = f"@{parts[1]}/{parts[2]}"
            except ValueError: pass
    return out

if __name__ == '__main__':
    src, dst = sys.argv[1], sys.argv[2]
    m = from_public_xml(src) if src.endswith('.xml') else from_r_txt(src)
    json.dump({str(k): v for k, v in m.items()}, open(dst, 'w'))
    print(f"{len(m)} resource ids -> {dst}")
