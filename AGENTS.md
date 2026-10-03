# AGENTS.md

Guide for coding agents (and people) working in this repository. Claude Code reads it too (2.1.277+).

## What this is

LMT Launcher, a pie menu and edge-gesture launcher for Android, originally by noname81. This
repository is a revived edition: the last patched APK was turned back into Java, then fixed,
modernized (Material 3) and extended. The user-facing summary is in `README.md`; the full
history, research and findings are in `docs/ROADMAP.md`.

- Package `com.noname81.lmt`, Java only (no Kotlin), minSdk 21, targetSdk 34, compileSdk 36.
- One app module (`app/`) plus `inputcontext/`, the root helper built from source.
- `libTouchServiceNative.so` (in `app/src/main/jniLibs/`) is the original closed-source gesture
  recognizer. It is shipped as is; don't try to modify it.

## Rules

- **Run the bytecode check before every commit**: `./tools/verify-equivalence.sh` must print
  `RESULT: EQUIVALENT`. Intentional changes to upstream classes are recorded in
  `tools/accepted-deviations.json` (see "The bytecode check" below).
- **Action ids are persisted** in settings and backups: never renumber them, only append new
  ones at the end of `Action` (next free id: 51).
- **Never lower `versionCode`.** Version is semver in `app/build.gradle`:
  `versionCode = major * 10000 + minor * 100 + patch`. It must stay above 4100 (upstream 3.11).
- **Every setting must be backed up**: add new keys to `SettingsBackup` (`INT_DEFAULTS` /
  `STRING_DEFAULTS`, or the slot loops).
- **Format Java with google-java-format `--aosp`** (needs JDK 21).
- Commit only when asked, one commit per feature, with a message that explains why.
- US English in code and UI ("color", not "colour").
- `reference/` (upstream repo) and `keystore/` are local only and git-ignored.

## Environment setup

1. **JDK 21** and the **Android SDK** with platform 36, build-tools 36.0.0 and platform-tools
   (`adb`). Android Studio works too: open the repository root.
2. **`tools/env.sh`** exports `JAVA_HOME`, `ANDROID_HOME` and `PATH` for the command line. It
   holds macOS Homebrew paths (`/usr/local/opt/openjdk@21`,
   `/usr/local/share/android-commandlinetools`); edit them for your machine. Source it before
   building: `. tools/env.sh`.
3. **Python 3**, for the bytecode check's scripts.
4. **The upstream reference**, for the bytecode check only:
   `git clone https://github.com/YMan84/lmt-launcher-android-patched reference`.
5. **Signing (optional)**: release signing reads `keystore/key.properties` (`storeFile`,
   `storePassword`, `keyAlias`, `keyPassword`; `storeFile` is relative to `keystore/`).
   Without it, debug builds use the standard debug key and release builds are unsigned.
6. **google-java-format**: `brew install google-java-format`. Run it with JDK 21:
   `J=$(ls -d /usr/local/Cellar/openjdk@21/*/bin | head -1); JAVA_HOME=${J%/bin} PATH=$J:$PATH google-java-format --aosp -i <files>`.

## Build, install, run

```bash
. tools/env.sh
./gradlew :app:assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease        # app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell monkey -p com.noname81.lmt -c android.intent.category.LAUNCHER 1
```

- Installing an update **stops the pie service**. Start it again from the Info tab's status
  card or Settings → Activate LMT.
- On a phone, LMT needs **Draw over other apps** and its **accessibility service**; root only
  for gestures, ISAs and `[Root]` actions. On a test device:
  `adb shell settings put secure enabled_accessibility_services com.noname81.lmt/com.noname81.lmt.AccessibilityHandler`
  and `adb shell settings put secure accessibility_enabled 1`. That replaces the whole list of
  enabled services, so don't do it on a phone with other accessibility services in use.
- A debug build installs over a release build only if both use the same key.

## Tests

```bash
./gradlew :app:testReleaseUnitTest                                   # all (~130)
./gradlew :app:testReleaseUnitTest --tests com.noname81.lmt.InfoTabTest
```

- Plain JUnit plus **Robolectric** (`app/src/test/`, SDK 33). UI tests host fragments in a
  themed activity (`UiTestSupport.hostActivity()`), reset singletons and prefs with
  `UiTestSupport.resetSingletons`, and find rows by label, not position.
- Contract tests to keep in step: `SettingsScreenUiTest` (every Settings row ↔ its pref key),
  `ActionPickerUiTest` (every picker row assigns its action), `SettingsSlotSchemaTest`
  (slot counts), `NewActionsTest` (appended ids and their picker sections).
- Results are in `app/build/test-results/testReleaseUnitTest/*.xml`.
- Unit tests run in one JVM with a 1 GB heap (`app/build.gradle`). If a run hangs with an idle
  test worker, `./gradlew --stop` and run again.
- Things Robolectric can't do (seeded color schemes, blur, the pie overlay, real touches) are
  checked on a device; `docs/ROADMAP.md` says how each feature was verified.

## The bytecode check

```bash
./tools/verify-equivalence.sh     # builds, disassembles, compares with reference/
```

It reduces every method of `com.noname81.lmt` to what decides its behavior (calls, strings,
constants, resource names) and compares it with the original patched APK's smali. Anything
that differs and isn't explained is reported as `UNEXPLAINED` or `REVIEW REQUIRED`. How to
explain a change in `tools/accepted-deviations.json`:

- `deviations`: one entry per changed upstream method (`class`, `method` as the script prints
  it, `reason`). When you change a method that already has an entry, append to its reason.
- `new_members`: a class gaining fields or methods. `"reworked": true` means the class was
  deliberately restructured: removals and content changes are accepted too.
- `new_classes`, `removed_classes`, `removed_members`.

Write the reason as what changed and why, with the ROADMAP section. Details:
`docs/equivalence-oracle.md`.

## Debugging

- **Logs**: every class logs under `LMT::<Class>`: `adb logcat | grep LMT::`. Most detailed
  logs are behind `DEBUG` flags.
- **Debug menu**: five double-taps on the version chip in the Info tab (`DebugHelper`). It
  toggles launcher, touch service and pie logging, and can force the no-root path.
- **Is the pie up?** `adb shell dumpsys window windows | grep -A3 "u0 com.noname81.lmt}:"`
  shows the edge strips, their size and type (`ACCESSIBILITY_OVERLAY` when the accessibility
  service is on, else `APPLICATION_OVERLAY`).
- **Open the pie from adb**: synthetic touches work. Press at the right edge, then hold:
  `adb shell input motionevent DOWN 1075 1200`, then `MOVE 1060 1200`, `screencap`,
  `MOVE`/`UP` back at the start so no slice fires. Coordinates depend on the screen and the
  activation area settings.
- **Settings** live in `/data/data/com.noname81.lmt/shared_prefs/LMT.xml` (root to read or edit
  on a device). Force-stop LMT before editing it.
- **Profiling**: the app is `profileable`, so Perfetto and `simpleperf` work on release builds.
- **Gotchas**:
  - `am force-stop com.noname81.lmt` removes LMT from the enabled accessibility services.
  - `uiautomator dump` (and tools built on it) unbinds LMT's accessibility service; use
    screenshots and `adb shell input` instead.
  - LMT has its own light/dark setting (`ThemeMode`: 0 system, 1 light, 2 dark); switching
    the phone's dark mode doesn't change LMT if it's set to 1 or 2.
  - Without root, `su` may exist but be denied (Magisk); `RootContext` checks for real
    (`id` → `uid=0`).

## Where things are

All app code is in `app/src/main/java/com/noname81/lmt/`.

### Screens

| What | Where |
|---|---|
| Main activity and its five tabs | `LMT` |
| Settings tab: every row, and helpers (`addSwitch`, `addSliderDialog`, `addChoiceDialog`, `addColorChoice`, `withCurrent`) | `SettingsViewFragment` |
| Switch rows ↔ prefs | `SettingsPreferenceDataStore` |
| Pie / Gestures / ISAs tabs (slot lists) | `PieFragment`, `GesturesFragment`, `IsasFragment` |
| Info tab | `InfoFragment`, `InfoPieView`, `res/layout/info.xml` |
| Appearance (theme, colors) | `AppearanceActivity`, `AppearanceFragment`, `ThemeHelper`, `ThemedActivity` |
| Action picker | `CommandSelectActivity` |
| App / activity / shortcut picker, blacklists | `MultiSelectActivity` |
| Pie folder editor | `FolderActivity`, `IconPickerDialog`, `MaterialIcons` |
| Dialogs | `InputDialog` (text and single choice), `ColorDialog` (color strings), `ThemeColorDialog`, `ColorSwatchDialog` |
| Color editor previews | `ColorPreview`, `PieTileView`, `PiePointerPreviewView`, `PieStatusInfoPreviewView` |

### Settings and actions

| What | Where |
|---|---|
| Every setting's load/save, slot vectors, folders, edit target | `SettingsValuesBase` |
| Singleton, screen size, recent apps (UsageStats) | `SettingsValues` |
| SharedPreferences access | `SettingsSharedPrefsWrapper` |
| Backup and restore (zip) | `SettingsBackup` |
| Action catalogue: ids, `names[]`, `captions[]` (with `[Root|Accessibility|None]` tags), icons | `Action` |
| What each action does | `Launcher.fireAction` |
| New actions' code | `Torch`, `KeepScreenOn`, `PopUpWindow`, `ShizukuShell`, `TaskerIntents`, `PieBrightness.toggleAdaptive` |
| Action icons | `res/drawable-xxhdpi/<action name in lower case>.png` (white, 144 px) |

### The pie and the service

| What | Where |
|---|---|
| Foreground service: starts the pie and gestures, CPU watchdog, re-adds the pie when accessibility changes | `TouchService` |
| Edge strip windows: expand on touch, shrink on lift, lock-screen handling, window type | `PieContainer` |
| Slots → pie items, click/long-press/hover dispatch, folders | `PieControl`, `PieControlBase`, `PieItem` |
| Pie layout, drawing, touch, folder ring | `PieMenu` |
| Extensions drawn with the pie | `PiePointer`, `PieStatusInfo`, `PieBrightness` (`PieMenu.PieView`) |
| Blur | `PieBlur` (screenshot + RenderEffect), `SamsungBlur` (live) |
| Notifications shown on the pie | `NotificationDataHelper` (fed by `AccessibilityHandler`) |

### System

| What | Where |
|---|---|
| Accessibility service: global actions, gestures, screenshots, foreground app | `AccessibilityHandler` |
| Root: `su` shell and the root helper | `RootContext`; helper source in `inputcontext/` |
| Permission checks and requests | `PermissionChecker` |
| Gestures: native recognizer, gesture names, touchscreen detection | `TouchServiceNative`, `TouchServiceResult`, `TouchscreenDetector` |
| Gesture feedback (vibration, image) | `Toaster` |
| Boot autostart, quick settings tile | `StartupReceiver`, `QuickSettingsTileService` |
| Icons: loading, user image folder, Material Icons | `IconUtils`, `IconLoader`, `IconFolder`, `MaterialIcons` |
| Sectioned lists | `SectionedRows` (`SeparatedListAdapter` is the older one) |

## Recipes

**New action**
1. Append the id constant, its `names[]` entry and its `captions[]` entry in `Action`. Tag the
   caption with what it needs: `[None]`, `[Accessibility]`, `[Root]`, combinations like
   `[Accessibility|Root]`.
2. Add a `case` in `Launcher.fireAction`.
3. Add the id to its section in `CommandSelectActivity.Adapter`.
4. Add `res/drawable-xxhdpi/<name in lower case>.png`.
5. Extend `NewActionsTest`; add the oracle entries (`Launcher.fireAction`, new classes).

**New setting**
1. `load…`/`save…` in `SettingsValuesBase`.
2. The row in `SettingsViewFragment` (a switch also needs `SettingsPreferenceDataStore`).
3. The key in `SettingsBackup`.
4. The row → key pair in `SettingsScreenUiTest`.
5. Settings the pie reads at startup need `restartServiceIfRequired()` after saving.

**Change the pie**
`PieMenu` handles drawing and touch, `PieControl` maps slots to items, and `PieContainer`
manages the windows. Check it on a device: open the pie with `adb shell input motionevent`
and take screenshots.

## Documentation

- `docs/ROADMAP.md`: everything done, found and planned, with the research behind it. Part 1
  features, Part 2 root → accessibility work, Part 3 bugs. Add a section when you finish
  something.
- `docs/equivalence-oracle.md`: how the bytecode check works.
- `docs/reconstruction-log.md`: how the Java was recovered from the APK.
- `tools/inputcontext/README.md`: the root helper.
- `docs/art/lmt_icon.svg`: the app icon as a vector.
