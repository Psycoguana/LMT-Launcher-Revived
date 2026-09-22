# LMT Launcher — buildable Java source

A Gradle Android project reconstructed from the decompiled
[LMT Launcher](https://github.com/YMan84/lmt-launcher-android-patched) APK, so the
app can be maintained as ordinary Java instead of patched smali.

```
app/src/main/java/com/noname81/lmt/   40 Java files, ~8.4k lines
app/src/main/res/                     124 files (app-owned only)
app/src/main/assets/                  InputContext*.jar, Roboto fonts  (verbatim)
app/src/main/jniLibs/                 libTouchServiceNative.so         (verbatim)
reference/                            upstream repo, read-only - the oracle
tools/                                build + verification scripts
docs/                                 baseline evidence, verification records
```

## Build

```bash
. tools/env.sh          # JDK 21, Android SDK 36, build-tools 36.0.0
./gradlew :app:assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk` (~1.7 MB, versionCode 4100).

`tools/build-baseline.sh` rebuilds the upstream smali tree to
`build-out/baseline.apk` (~3.1 MB) for A/B comparison. Both are signed with the
same local key (`keystore/`, gitignored), so either installs over the other.

## Verifying a change

```bash
./tools/verify-equivalence.sh
```

This is the safety net. It builds the project, disassembles the dex, and compares
every `com.noname81.lmt` method against the upstream patched smali. It must print
`RESULT: EQUIVALENT`. Anything it cannot classify as behaviour-free is reported as
`UNEXPLAINED` and should be understood before the change is kept.

See `docs/equivalence-oracle.md` for what it normalises and why.

## What this project is not

The upstream repo's checked-in `jadx/` Java is the **unpatched** v3.4 beta2 — every
fix in its CHANGELOG lives only in `apktool/smali/`. This project therefore does not
use that Java. It regenerates Java from the patched smali, so all seven fixes are
carried in by construction rather than by hand.

## Dependencies

`androidx.viewpager:viewpager` — and nothing else.

The app never used AppCompat: its theme is `@android:style/Theme.Holo`
(`Theme.Material` on v21), its activities extend `android.app.Activity`, and its
fragments are `android.app.Fragment`. Almost every `android.support.*` import in the
decompiler output was jadx substituting an unrelated library constant for a plain
integer — `MotionEventCompat.AXIS_LTRIGGER` for `17`, and so on. Those were resolved
against the smali and replaced with literals. The support-v13 pager adapter is
reproduced as `FrameworkFragmentStatePagerAdapter` rather than pulling in the
deprecated `androidx.legacy` artifact.
