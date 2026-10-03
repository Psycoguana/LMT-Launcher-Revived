# 🥧 LMT Launcher Revived

LMT is a pie menu and edge-gesture launcher for Android, originally by **noname81**
([XDA thread](https://xdaforums.com/t/app-root-lmt-launcher-v3-2.1330150/), 2011 onwards).
Swipe in from the screen edge and a pie of actions opens under your thumb: apps, shortcuts,
Back/Home/Recents, toggles, Tasker tasks and more.

This is a revived edition. The app was rebuilt from the last patched APK (thanks to [motomotomotoG](https://github.com/YMan84/lmt-launcher-android-patched) for the patches) into a normal,
testable Java project, then fixed, modernized and extended. It runs on current Android
(tested on Android 15 and 16) and most of it no longer needs root.

## ✨ What's new

### 🎨 Redesign

- **Material 3 throughout**: Material toolbar and tabs, preference-style Settings, Material
  dialogs, lists and pickers.
- **Appearance settings**: light, dark or follow system; Material You colors from the
  wallpaper (Android 12+), 14 preset colors or any custom color.
- **Settings that show their value**: every row says what it is currently set to; numbers
  are sliders with units and a Default button; coded options are choice dialogs.
- **Color editors with real previews**: the pie colors preview an actual pie slice, the
  pointer colors preview the pointer, the status info colors preview the clock and info
  lines. The status info font picker shows each font in itself.
- **Info tab**: credits and links, a status card that says whether LMT can work and fixes
  the first problem with a tap (start the service, allow the overlay, turn on
  accessibility), and a tile per permission.
- **App picker**: search, and a "show system apps" toggle.
- **File pickers** instead of typed paths for the pie image folder, scripts and backups.

### 🚀 New features

- 📁 **Pie folders**: a slice can hold up to 8 actions that open in a ring around it, on hover
  or after a long press. Folders have a name and any of ~2,200 Material icons.
- ☀️ **Brightness dial**: a dial around the pie sets the screen brightness. Use it as a slice
  action, or turn on the "Brightness dial" setting to have it around every pie without
  spending a slice.
- 🌫️ **Blur behind the pie**: behind the pie or the whole screen. On Samsung phones that
  support it, a live blur (Samsung Live Blur) instead of static.
- ⚡ **New actions**: Flashlight, Keep screen on, Pop-up window (opens the current app in a
  floating window; root or Shizuku).
- 💾 **Backup and restore**: export every setting and pie icon to a zip, import it anywhere.
- 🤖 **Tasker task picker**: choose the task from Tasker's own list instead of typing its name.

### 🔓 No longer needs root

| Action | Before | Now |
|---|---|---|
| Screenshot | root | accessibility service (Android 9+) |
| Menu key | root | accessibility service (Android 16+) |
| Nova / Apex / Holo app drawer | root | opens the system app drawer through accessibility (Android 12+); root still opens the launcher's own drawer |
| Tasker task | root | no permission beyond Tasker's own (runs through Tasker's broadcast) |
| Last app | labelled root-only | usage access |

New Pop-up window works with root or with [Shizuku](https://shizuku.rikka.app/).

Without root, LMT now says so instead of failing silently: root-only actions are greyed out
in the picker with an explanation, and gestures and ISAs (which read the touchscreen
directly) are disabled until root is granted. The pie never needs root.

### 🐛 Bugs fixed

From the original app:

- **Root key actions did nothing on Android 15**.
- **Pie can now be shown in settings**.
- **The accessibility service stopped working while still shown as enabled**: any app's
  accessibility "interrupt" made LMT think it was disconnected until you toggled it off and
  on.
- **Chrome couldn't ask for camera, microphone or location** ("close bubbles or overlays")
  while LMT was running. The edge strips are now accessibility overlays, which Android
  trusts (Android 12+, with LMT's accessibility service on).
- **Gestures now find the correct input device automatically**.
- **Battery drain**: without working root, the gesture thread spun at ~96% of a CPU core with
  the screen idle.
- **Root helpers leaked**: every service restart left another root `cat` process behind.
- **Auto configuration** computed a touchscreen factor of 0 (integer division).
- **The user image folder setting was ignored**: the saved path was never read.


## 📋 Requirements

- Android 5.0 or newer; the features above note their own minimum versions.
- **Draw over other apps** for the pie.
- **LMT's accessibility service** for Back, Home, Recents, screenshots, blur, notifications on
  the pie and the Chrome fix.
- Root only for gestures, ISAs and the actions marked `[Root]` in the picker.

## ⬆️ Upgrading from the original LMT

This build is signed with a different key, so it can't be installed over the original app.
Uninstall the original first. Settings don't carry over from the original, which has no
export; from this version on, Settings → Backup exports and imports everything.

## 🛠️ Building

```bash
. tools/env.sh          # JDK 21, Android SDK 36
./gradlew :app:assembleRelease
./gradlew :app:testReleaseUnitTest
```

Release signing reads `keystore/key.properties`, which is not in the repository.

### 🔍 Verifying a change against the original

```bash
./tools/verify-equivalence.sh
```

Every change was checked against the original app's bytecode. The script builds the project,
disassembles it and compares each method of `com.noname81.lmt` with the original patched
APK, reduced to what determines behavior (calls made, strings, constants with resource ids
resolved). It must print `RESULT: EQUIVALENT`. Intentional changes are listed, each with a
reason, in `tools/accepted-deviations.json`. It needs the original repository cloned into
`reference/`; details in [`docs/equivalence-oracle.md`](docs/equivalence-oracle.md).

### 🗂️ Layout

```
app/src/main/java/com/noname81/lmt/   the app
app/src/test/                         Robolectric and unit tests
inputcontext/                         the root helper, built from source
tools/                                build and verification scripts
docs/ROADMAP.md                       everything done, found and planned, with the research
```

The root helper (`inputcontext/`) is the small program LMT runs under `su` to inject keys
and manage tasks. It used to ship as five prebuilt jars, one per Android version, and is now
one jar built from source.

## 🙏 Credits

- **noname81**: the original LMT Launcher: the pie, the gesture engine and the action
  catalogue. [XDA profile](https://xdaforums.com/m/noname81.1864347/)
- **motomotomotoG / YMan84**: the Android 13 to 16 smali patches and the
  [patched repository](https://github.com/YMan84/lmt-launcher-android-patched) this edition
  was rebuilt from.
- **[Psycoguana](https://github.com/Psycoguana)**: this revived edition.

Third-party assets: [Material Icons](https://github.com/google/material-design-icons)
(Apache 2.0), brand icons from [Simple Icons](https://simpleicons.org/) (CC0),
[Shizuku API](https://github.com/RikkaApps/Shizuku-API) (Apache 2.0).
