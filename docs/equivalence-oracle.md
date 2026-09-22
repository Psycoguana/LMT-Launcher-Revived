# The equivalence oracle

`tools/verify-equivalence.sh` proves that the reconstructed source still compiles to
the same behaviour as the shipping app. It builds the APK, disassembles it, and
compares every `com.noname81.lmt` method against
`reference/apktool/smali/com/noname81/lmt`.

## Why it is not a textual diff

The reference smali came out of the D8 that built the original APK; ours comes from
today's D8. They legitimately differ in instruction scheduling, branch polarity
(`if-eqz` vs `if-nez` + `goto`), register allocation, whether a repeated literal is
hoisted into one register, and how anonymous classes are numbered. A textual or even
order-sensitive comparison is pure noise against that.

So each method is reduced to the things that actually determine behaviour and
compared as **sets**:

- every method and field it references
- every string literal
- every numeric literal, with resource ids resolved to `@type/name` on both sides

A set still catches what matters — a constant whose value changed, a call that
disappeared, an edited string — which is exactly how a silently reverted patch would
show up. Opcode sequences are excluded entirely.

## Normalisations applied

| Normalised | Why |
|---|---|
| `0x7f......` → `@type/name` | ids are renumbered per build; the reference uses `sget` on `R$type` fields while an app module inlines them |
| `$1`, `$1LongpressRunnable`, `$$ExternalSyntheticLambda0` → `$ANON` | compiler-assigned nested-class names, in all three flavours |
| `access$123` → `access$N` | synthetic accessor numbering |
| `Object.getClass()` → `NULLCHECK` | D8 emits null checks this way; presence varies by version |
| `LinkedList`/`ArrayList` → `List`, `TreeMap`/`SortedMap` → `Map` | jadx infers a concrete type where the original declared the interface |
| `R`/`R$*` classes skipped | generated from the resource table; ids already compared symbolically |
| anonymous classes grouped per outer class | `$1` on one side is not necessarily `$1` on the other |

## Differences accepted as behaviour-free

The run currently reports 22, in six categories:

- **same method, different receiver type** — the compiler resolving a call to an
  anonymous subclass rather than its base, or our lambda-to-anonymous-class rewrite
- **support-lib compat wrapper replaced by guarded framework call** — deliberate:
  `ContextCompat.checkSelfPermission` → `Context.checkSelfPermission`, already
  `SDK_INT >= 23`-guarded, which D8 then outlines
- **synthetic-accessor bridge vs direct call** — the reference reaches private
  members from inner classes through `access$N`
- **captured-local renamed** — a `val$` synthetic field name, which cannot affect
  behaviour
- **named local class rewritten as anonymous** — its instance field becomes a capture
- **lambda body outlined vs inlined**

Methods carrying the `synthetic` flag that exist on only one side are skipped:
javac never puts that flag on human-written code, so this cannot hide a real method
going missing.

## A useful side effect

The reference dex preserves the original author's local variable names in `val$`
capture fields. `docs/original-names.json` lists all 14 recovered; they have been
adopted where they beat the decompiler's (`position` over `i`, `path`/`fileName`,
`inputDeviceFinal`/`factorXFinal`/`factorYFinal`).
