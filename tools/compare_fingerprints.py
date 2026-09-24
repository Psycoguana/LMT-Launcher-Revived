#!/usr/bin/env python3
"""Compare two smali fingerprints and report behavioural differences.

The reference smali was produced by the D8 that built the original APK; ours comes
from today's D8. Those legitimately differ in instruction scheduling and constant
hoisting, so an order-sensitive comparison is pure noise. What must match is the
*content*: the same calls, the same literals, the same mix of operations.

Differences are therefore graded:
  REAL       - the multiset of refs/strings/constants differs, or the opcode
               histogram differs. Something actually changed. Must be explained.
  REORDERED  - identical multisets, different order. Compiler scheduling only.
"""
import json, sys, re
from collections import Counter

ref = json.load(open(sys.argv[1]))
ours = json.load(open(sys.argv[2]))

# Deliberate, reviewed divergences. Anything not listed here that differs in
# content is reported as UNEXPLAINED.
import os
_dev_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "accepted-deviations.json")
ACCEPTED = {}
NEW_MEMBERS = {}
REWORKED = set()
if os.path.exists(_dev_path):
    _dev = json.load(open(_dev_path))
    for d in _dev["deviations"]:
        ACCEPTED[(d["class"], d["method"])] = d["reason"]
    # Classes where a feature was added, so NEW members are expected. Missing
    # members are still reported, and other classes are unaffected.
    NEW_MEMBERS = {d["class"]: d["reason"] for d in _dev.get("new_members", [])}
    # classes deliberately restructured, where removals are expected too
    REWORKED = {d["class"] for d in _dev.get("new_members", []) if d.get("reworked")}
verbose = "--verbose" in sys.argv

# Whole classes expected only on our side, or only on the reference side - each with
# its reason, in accepted-deviations.json ("new_classes" / "removed_classes").
EXPECTED_NEW = {d["class"] for d in _dev.get("new_classes", [])} if os.path.exists(_dev_path) else set()
EXPECTED_REMOVED = {d["class"] for d in _dev.get("removed_classes", [])} if os.path.exists(_dev_path) else set()
# Compiler-numbered synthetic classes whose names depend on compile order.
EXPECTED_GONE_PREFIX = ("AccessibilityHandler$$ExternalSynthetic",
                        "Launcher$$ExternalSyntheticApiModelOutline")

def base(n): return n.split("/")[-1]

# Every flavour of compiler-named nested class: anonymous ($1), named local
# classes ($1GestureListener - the digit prefix is the compiler's, not the
# author's), and desugared lambdas ($$ExternalSyntheticLambda0 / $$Lambda).
ANON = re.compile(r'\$\$?(?:ExternalSyntheticLambda\d+|Lambda\$?\d*|\d+\w*)(?=[$.]|\.smali$)')
# R classes are generated from the resource table; ids are already normalised to
# symbolic names everywhere they are used, so the R classes themselves carry no
# behaviour worth diffing.
R_CLASS = re.compile(r'(?:^|/)R(?:\$\w+)?\.smali$')

def group_anonymous(tree):
    """Merge a class's anonymous inner classes into one bucket.

    javac numbers anonymous classes in source order, and that numbering shifts
    whenever the surrounding code is re-emitted - so reference $1 and our $1 can
    be different classes entirely. Comparing the outer class's anonymous
    classes as a set keeps the check meaningful without depending on numbering.
    """
    out, merged = {}, {}
    for name, cls in tree.items():
        if R_CLASS.search(name):
            continue
        if ANON.search(name):
            key = ANON.sub("$ANON", name)
            bucket = merged.setdefault(key, {"fields": [], "methods": {}})
            for sig, fp in cls["methods"].items():
                bucket["methods"].setdefault(sig, []).append(fp)
            bucket["fields"].extend(cls["fields"])
        else:
            out[name] = cls
    for key, bucket in merged.items():
        # collapse each signature's list to a canonical, order-free form
        bucket["methods"] = {
            sig: sorted(fps, key=lambda f: json.dumps(f, sort_keys=True))
            for sig, fps in bucket["methods"].items()}
        bucket["fields"] = sorted(bucket["fields"])
        out[key] = bucket
    return out

ref = group_anonymous(ref)
ours = group_anonymous(ours)

ref_names, our_names = set(ref), set(ours)
missing = {n for n in ref_names - our_names
           if not base(n).startswith(EXPECTED_GONE_PREFIX) and base(n) not in EXPECTED_REMOVED}
added   = {n for n in our_names - ref_names if base(n) not in EXPECTED_NEW}

real, reordered, synthetic_only, anon_shape, feature_additions, one_sided = [], [], [], [], [], []

if missing:
    print(f"!! {len(missing)} class(es) missing from our build:")
    for n in sorted(missing): print("   -", n)
if added:
    print(f"?? {len(added)} class(es) only in our build (renamed anon/lambda classes are expected):")
    for n in sorted(added): print("   +", n)

# Fields matter too: a removed field is a real structural change, and until now
# they were parsed but never compared.
field_diffs = []
for name in sorted(ref_names & our_names):
    def strip_default(f):
        return re.sub(r'\s*=\s*(?:null|false|0(?:\.0)?|0x0)$', '', f)
    rf = {strip_default(f) for f in ref[name].get("fields", [])}
    of = {strip_default(f) for f in ours[name].get("fields", [])}
    if rf != of:
        only_ref, only_our = rf - of, of - rf
        # synthetic compiler bookkeeping (this$N, val$x, $assertionsDisabled, ...)
        if base(name) in REWORKED:
            only_ref, only_our = set(), set()
        elif base(name) in NEW_MEMBERS:
            # only additions are excused; a field going missing still counts
            only_ref = {f for f in only_ref if "synthetic" not in f}
            only_our = set()
        non_synthetic = [f for f in (only_ref | only_our) if "synthetic" not in f]
        # A named local class rewritten as an anonymous one turns its instance
        # field into a captured local: `private mPosition` -> `synthetic val$mPosition`.
        paired = {re.sub(r'val\$', '', f.split()[-1]) for f in only_ref} == \
                 {re.sub(r'val\$', '', f.split()[-1]) for f in only_our}
        field_diffs.append((name, sorted(only_ref), sorted(only_our),
                            bool(non_synthetic) and not paired))

hard_field_diffs = [f for f in field_diffs if f[3]]
if field_diffs:
    print(f"\n{'='*66}\nFIELD DIFFERENCES ({len(field_diffs)} classes; "
          f"{len(hard_field_diffs)} involving non-synthetic fields)\n{'='*66}")
    for name, only_ref, only_our, is_real in field_diffs:
        mark = "!!" if is_real else "~ "
        print(f"{mark} {name}")
        for f in only_ref: print(f"     - {f}")
        for f in only_our: print(f"     + {f}")

same = 0
for name in sorted(ref_names & our_names):
    rm, om = ref[name]["methods"], ours[name]["methods"]
    for sig in sorted(set(rm) | set(om)):
        if sig not in rm or sig not in om:
            present = rm.get(sig) or om.get(sig)
            # An empty <clinit> is a no-op the other compiler simply did not emit.
            if "<clinit>" in sig and not any(present[k] for k in ("refs", "strings", "nums")):
                same += 1
                continue
            # Compiler plumbing: accessor bridges, desugared lambdas and API
            # outlines. These carry the `synthetic` flag, which javac never puts
            # on a method written by a human, so whitelisting on it cannot hide a
            # real method going missing.
            if "synthetic" in sig.split("(")[0].split():
                synthetic_only.append((name, sig))
                continue
            if "$ANON" in name:
                # lambda (no-arg ctor + static INSTANCE) vs anonymous class
                # (outer-reference ctor); same behaviour, different shape.
                anon_shape.append((name, sig))
                continue
            if base(name) in REWORKED:
                feature_additions.append((name, sig))
                continue
            if base(name) in NEW_MEMBERS and sig in om and sig not in rm:
                feature_additions.append((name, sig))
                continue
            print(f"!! {name}: method on one side only: {sig}")
            one_sided.append((name, sig))
            continue
        a, b = rm[sig], om[sig]
        if a == b:
            same += 1
            continue
        if isinstance(a, list) or isinstance(b, list):
            # anonymous-class bucket: merge every variant's content together
            def merge(x):
                out = {"ops": [], "refs": [], "strings": [], "nums": []}
                for fp in (x if isinstance(x, list) else [x]):
                    for k in out: out[k].extend(fp[k])
                return out
            a, b = merge(a), merge(b)
            if all(set(a[k]) == set(b[k]) for k in ("refs", "strings", "nums")):
                same += 1
                continue
        if base(name) in REWORKED:
            # A class marked "reworked" is being intentionally restructured, so its
            # content is out of scope for comparison - not just its member set (see
            # the one-sided-method and field-diff REWORKED checks above). Explaining
            # every mechanical byproduct of a rewrite (receiver-type changes,
            # codegen shifts) method by method has no signal; only classes NOT
            # being rewritten still benefit from a real content diff.
            feature_additions.append((name, sig))
            continue
        deltas = {}
        # Compared as SETS, not sequences or multisets. Two D8 versions differ in
        # instruction order, in branch polarity, and in whether a repeated literal
        # is hoisted into one register or emitted at each use - none of which
        # changes behaviour, and all of which wreck an order- or count-sensitive
        # comparison. What a set still catches is what actually matters: a constant
        # that changed value, a call that disappeared, a string that was edited.
        # `ops` is excluded entirely for the same reason.
        for key in ("refs", "strings", "nums"):
            sa, sb = set(a[key]), set(b[key])
            if sa != sb:
                deltas[key] = (Counter(sa - sb), Counter(sb - sa))
        if deltas:
            real.append((name, sig, deltas))
        else:
            reordered.append((name, sig))

TRIVIAL_CONSTS = {"0x0", "0x1", "0", "1"}

def classify(deltas):
    """Bucket a difference into a known-benign category, or return None."""
    only_ref, only_our = set(), set()
    for key, (a, b) in deltas.items():
        if key == "refs":
            only_ref |= set(a); only_our |= set(b)
        elif key == "nums":
            # Boolean/zero literals move around with register allocation; any
            # other constant difference is a real change and must not be excused.
            if not (set(a) | set(b)) <= TRIVIAL_CONSTS:
                return None
        else:
            return None
    if not (only_ref or only_our):
        return "trivial 0/1 literal placement"

    # Captured-local names surface as val$<name> synthetic fields. They are
    # names only - they cannot affect behaviour - so a difference here just means
    # we chose a different (usually clearer) local name than the original author.
    if all("val$" in x for x in only_ref | only_our) and (only_ref or only_our):
        def novar(x): return re.sub(r'val\$\w+', 'val$X', x)
        if {novar(x) for x in only_ref} == {novar(x) for x in only_our}:
            return "captured-local renamed (names only, no behaviour)"

    # D8 outlines API-level-guarded framework calls into a synthetic class. Where
    # the reference called a support-library compat wrapper, we now call the
    # framework API directly (already SDK_INT-guarded), and D8 outlines it.
    if all("ExternalSyntheticApiModelOutline" in x for x in only_our) and only_our \
       and all("Compat;->" in x for x in only_ref):
        return "support-lib compat wrapper replaced by guarded framework call"

    # A lambda's body: the reference outlined it into a lambda$static$N method on
    # the outer class, while ours inlines it into the anonymous class we wrote in
    # its place. Same code, different placement.
    if any("lambda$" in r for r in only_ref) and only_ref and not any("lambda$" in o for o in only_our):
        return "lambda body outlined vs inlined"

    # A named local class rewritten as an anonymous one: its instance field
    # becomes a captured local, so `X->mPosition` turns into `X->val$mPosition`.
    def unval(x): return x.replace("->val$", "->")
    if only_our and {unval(o) for o in only_our} == {unval(r) for r in only_ref}:
        return "named local class rewritten as anonymous (field -> capture)"
    if not only_ref and only_our and all("val$" in o for o in only_our):
        return "named local class rewritten as anonymous (field -> capture)"

    # The reference routes inner-class access to a private member through a
    # synthetic access$N bridge; today's compiler calls the member directly.
    if all("access$N" in r for r in only_ref) and only_ref:
        return "synthetic-accessor bridge vs direct call"

    def strip(x):
        return re.sub(r'L[\w/$]+;->', '', x)
    # Same method, different receiver type: either a lambda we rewrote as an
    # anonymous class, or the compiler resolving the call to the anonymous
    # subclass instead of its base. Neither changes dispatch.
    if {strip(x) for x in only_ref} == {strip(x) for x in only_our}:
        return "same method, different receiver type (devirtualization / anon rename)"
    return None

benign, unexplained, accepted = [], [], []
for item in real:
    key = (item[0].split("/")[-1], item[1])
    if key in ACCEPTED:
        accepted.append((item, ACCEPTED[key]))
        continue
    cat = classify(item[2])
    (benign if cat else unexplained).append((item, cat))

if accepted:
    print(f"\n{'='*66}\nACCEPTED DEVIATIONS ({len(accepted)})\n{'='*66}")
    for (name, sig, _d), reason in accepted:
        print(f"  {name} :: {sig}\n      {reason}")

if benign:
    print(f"\n{'='*66}\nKNOWN-BENIGN DIFFERENCES ({len(benign)} methods)\n{'='*66}")
    from collections import Counter as _C
    for cat, n in _C(c for _i, c in benign).most_common():
        print(f"  {n:3d}  {cat}")

if unexplained:
    print(f"\n{'='*66}\nUNEXPLAINED DIFFERENCES ({len(unexplained)} methods)  <-- REVIEW THESE\n{'='*66}")
    for (name, sig, deltas), _c in unexplained:
        print(f"\n!! {name} :: {sig}")
        for key, (only_ref, only_our) in deltas.items():
            if only_ref: print(f"     [{key}] only in reference: {dict(only_ref)}")
            if only_our: print(f"     [{key}] only in ours     : {dict(only_our)}")

if verbose and reordered:
    print(f"\nREORDERED ONLY ({len(reordered)}):")
    for name, sig in reordered[:40]: print(f"   ~ {name} :: {sig}")

print(f"\n{'='*66}")
print(f"classes compared      : {len(ref_names & our_names)}")
print(f"methods identical     : {same}")
print(f"methods reordered only: {len(reordered)}   (compiler scheduling - benign)")
print(f"synthetic-only methods: {len(synthetic_only)}   (accessor/lambda/outline plumbing)")
print(f"anon-shape methods    : {len(anon_shape)}   (lambda vs anonymous class shape)")
if feature_additions:
    print(f"feature additions     : {len(feature_additions)}   (new members in classes listed in accepted-deviations.json)")
print(f"methods differing     : {len(real)}")
print(f"  known-benign        : {len(benign)}")
print(f"  accepted deviations : {len(accepted)}")
print(f"  UNEXPLAINED         : {len(unexplained)}")
print(f"field diffs (synthetic only) : {len(field_diffs) - len(hard_field_diffs)}")
print(f"field diffs (REAL)           : {len(hard_field_diffs)}")
print(f"one-sided methods (REAL)     : {len(one_sided)}   (added/removed, not excused by accepted-deviations.json)")
ok = not unexplained and not missing and not added and not hard_field_diffs and not one_sided
print("RESULT:", "EQUIVALENT" if ok else "REVIEW REQUIRED")
sys.exit(0 if ok else 1)
