# Recommendations

Things found during the reconstruction that are worth doing, listed with the reason
and the risk. None have been done — they are behaviour changes, and the project only
just reached verified equivalence.

## 1. Make `getVersion()` read `BuildConfig.VERSION_NAME`

`SettingsValues.getVersion()` returns a hardcoded string. Upstream kept it in sync
with the APK's `versionName` by running `sed` over the smali inside `build.sh`. This
project carries the same hazard in source form: bump `versionMinor` in
`app/build.gradle` and forget the string, and the Info tab reports the old version.
That already happened once during verification (package 3.11, Info tab v3.10).

```java
String getVersion() {
    return "v" + BuildConfig.VERSION_NAME;
}
```

Needs `buildFeatures { buildConfig true }`. Removes the whole class of drift, and
retires the `sed` step permanently. One method changes; record it in
`tools/accepted-deviations.json`.

## 2. Fix the latent out-of-bounds in `Action.toString()`

```java
if (mType > 0 && mType <= names.length) { typeName = names[mType]; }
```

`mType == names.length` indexes one past the end. Reachable only if a preference
holds a type id equal to the array length — a corrupted or hand-edited prefs file,
or a future id added to `names[]` without care. `<` is the correct bound. Preserved
faithfully so far because it is in the shipping app.

## 3. Restore `assert` at the 64 `$assertionsDisabled` sites

jadx renders the assert idiom as `static final boolean $assertionsDisabled = false`
plus `z = $assertionsDisabled`. It is faithful — the original dex already had the
constant folded — but it reads as nonsense. Replacing it with real `assert`
statements, or with plain `false`, is a readability win. Bytecode changes, so it
belongs in the readability pass with the oracle re-run per file.

## 4. Consider whether root is still the right default

The test phone shows the app degrading correctly without root: root-only actions log
`Failed to write remote command!` and continue. But ~20 of the 46 actions are
root-only, several of which now have public-API equivalents that did not exist when
LMT was written — `GestureDescription` for taps and swipes, `cmd statusbar` for the
notification shade. `AccessibilityHandler.performClick`/`dispatchGesture` already
exist, so the plumbing is there. Wiring accessibility fallbacks into
`Launcher.doXxxAction()` would make far more of the app work on an unrooted phone,
which is most phones now.

## 5. Known environment gotchas

- **Downgrades are blocked.** Lowering `versionCode` makes the APK uninstallable
  over an existing one (`INSTALL_FAILED_VERSION_DOWNGRADE`), even with `adb install -d`.
- **The upstream release key is not public**, so these builds cannot upgrade an
  installed upstream APK in place — an uninstall (and loss of settings) is required
  to switch. Worth an export/import feature if the app is to be distributed.
- **Settings hides overlays.** The pie does not appear over the Settings app. That
  is Android's tapjacking protection, not a bug to chase.
