# LMT Launcher — maintainable source

A Gradle Android project reconstructed from the decompiled
[LMT Launcher](https://github.com/YMan84/lmt-launcher-android-patched) APK, so the
app can be maintained as ordinary Java instead of patched smali.

LMT is a pie-menu / edge-gesture launcher for Android, originally by **noname81**
([XDA thread](https://xdaforums.com/t/app-root-lmt-launcher-v3-2.1330150/), 2011–).
It was kept alive after abandonment by patching smali directly; this project turns
that back into source you can read, test and change.

## Status

| | |
|---|---|
| Builds | ✅ `./gradlew :app:assembleRelease` → 1.7 MB APK (upstream: 3.1 MB) |
| Bytecode-verified against upstream | ✅ `./tools/verify-equivalence.sh` → `EQUIVALENT` |
| Verified on device | ✅ rooted test phone, Android 15 |
| Tests | ❌ none yet — next milestone |
| Dependencies | `androidx.viewpager` and nothing else |

## Layout

```
app/src/main/java/com/noname81/lmt/   40 Java files, ~11k lines
app/src/main/res/                     126 files (app-owned only)
app/src/main/assets/                  InputContext*.jar, Roboto fonts   (verbatim)
app/src/main/jniLibs/                 libTouchServiceNative.so          (verbatim)
reference/                            upstream repo, read-only — the oracle's truth
tools/                                build + verification scripts
docs/                                 roadmap, verification records, baseline evidence
```

`reference/` and `keystore/` are untracked. Clone the upstream repo into
`reference/` to run the oracle; see `LMT-backups/` for the keystore.

## Build

```bash
. tools/env.sh          # JDK 21, Android SDK 36, build-tools 36.0.0
./gradlew :app:assembleRelease
```

`tools/build-baseline.sh` rebuilds the **upstream smali** into
`build-out/baseline.apk` for A/B comparison. Both are signed with the same local
key, so either installs over the other.

## Verifying a change

```bash
./tools/verify-equivalence.sh
```

This is the safety net. It builds the project, disassembles the dex, and compares
every `com.noname81.lmt` method and field against the upstream patched smali. It
must print `RESULT: EQUIVALENT`.

It does **not** compare raw text — the two sides come from different D8 versions,
so instruction order, branch polarity, register allocation and constant hoisting
all differ without meaning. Instead each method is reduced to the things that
determine behaviour (calls made, string literals, numeric constants with resource
ids resolved to `@type/name`) and compared as sets. That still catches what matters:
a constant whose value changed, a call that vanished, an edited string.

Intentional divergences live in `tools/accepted-deviations.json`, each with a
reason. Anything else that differs is reported as `UNEXPLAINED`. Full details in
[`docs/equivalence-oracle.md`](docs/equivalence-oracle.md).

> The oracle becomes meaningless once a file is rewritten in Kotlin — codegen
> changes wholesale. That is why tests come before the Kotlin migration.

## Two things to know before changing anything

**1. Do not build from the upstream repo's checked-in Java.** Its `jadx/` tree is
the *unpatched* v3.4 beta2; all seven fixes in its CHANGELOG exist only in
`apktool/smali/`. This project regenerates Java from the patched smali instead, so
the fixes are carried in by construction. Building from that Java would silently
revert them.

**2. The app never used AppCompat.** Its theme is `@android:style/Theme.Holo`
(`Theme.Material` on v21), activities extend `android.app.Activity`, fragments are
`android.app.Fragment`. Nearly every `android.support.*` import in the decompiler
output was jadx substituting an unrelated library constant for a plain integer —
`MotionEventCompat.AXIS_LTRIGGER` for `17`, and so on. Those were resolved against
the smali and replaced with literals, which is why the dependency list is one line.

## The root helper

`inputcontext/` builds the program that runs under `su` via `app_process` and does
what an app is not allowed to do — press Home, force-stop apps, reorder tasks.
`RootContext` copies it out of assets, starts it once, and feeds it commands on
stdin.

It used to ship as five prebuilt jars, one per SDK level, and carried a bug that
silently disabled every root key action on modern Android:
`InputManager.getInstance()` NPEs inside `app_process`. That had been reported on
XDA since September 2024 and never root-caused — the community workaround was to
turn root *off*, forcing the accessibility path.

Both are now resolved. The helper is built from source, and the five variants
collapsed into one: they only ever differed where hidden-API *signatures* changed,
which reflection defers to runtime. See
[`tools/inputcontext/README.md`](tools/inputcontext/README.md).

## Where things are going

[`docs/ROADMAP.md`](docs/ROADMAP.md) is the plan: tests → Kotlin rewrite → app-picker
search and system-apps toggle → import/export settings → Material 3 overhaul →
accessibility fallbacks. It also carries the bug list and the research behind each
item.

## Credits

Original app by **noname81**. Android 13–16 smali patches and the patched repo this
was reconstructed from by **motomotomotoG** / **YMan84**. The pie rendering, the
gesture engine and the action catalogue are all their work; this project only makes
it maintainable.
