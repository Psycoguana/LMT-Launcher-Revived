# LMT — roadmap and findings

Planned work, plus everything discovered during the reconstruction that is worth
acting on later. Nothing here is done unless it says so.

Current state: the project builds from Java, is bytecode-verified equivalent to the
upstream patched APK (`tools/verify-equivalence.sh` → `EQUIVALENT`), and is
confirmed working on a rooted test phone (Android 15 / SDK 35).

---

## Part 1 — Planned features

In roughly the order that keeps each step verifiable.

### 1.1 Reproduce the original faithfully  ✅ done
Equivalence established and recorded. `tools/accepted-deviations.json` holds every
intentional divergence with a reason; anything else that differs is reported as
`UNEXPLAINED` by the oracle.

### 1.2 Write tests
No test suite exists today; the oracle proves *equivalence*, not *correctness*.
Highest-value targets, in order:

- **`SettingsValuesBase` / `SettingsSharedPrefsWrapper`** — pure pref
  serialisation, trivially unit-testable with Robolectric, and the layer that
  import/export (1.4) will depend on.
- **`Action`** — the id ↔ name ↔ caption tables, `toString()`, and the
  `Offset*` sentinel grouping that `CommandSelectActivity` relies on. Pure data.
- **`PieMenu.findItem()` geometry** — hit-testing by `innerRadius`/`outerRadius`/
  `start`/`sweep` is pure maths and is where a third-ring regression would surface.
- **Instrumented smoke test** — service starts, pie opens, three rings render.
  This is currently done by hand via argent; `argent-qa-flows` could record it.

Do this **before** the Kotlin rewrite, so the rewrite has a safety net that
survives the bytecode oracle becoming meaningless.

### 1.3 Rewrite in Kotlin
Note the ordering hazard: the equivalence oracle compares our dex against the
reference smali, and Kotlin changes codegen wholesale (null checks, `$i$f$...`
inlining, synthetic accessors). **The oracle stops being useful the moment a file
becomes Kotlin.** So per file: tests first (1.2), convert, then rely on tests.
Suggested order — leaves first, `Launcher`/`PieMenu` last:
`FileUtils` → `Toaster` → `IconUtils` → `Action` → `SettingsValues*` → fragments →
`PieMenu`/`Launcher`.

### 1.4 App search + "show system apps" toggle in the app picker
`MultiSelectActivity` currently builds one flat list from `getInstalledPackages`.
Needs: a `SearchView` filtering the adapter, and a menu toggle for
`ApplicationInfo.FLAG_SYSTEM`. Note the upstream CHANGELOG already has a fix here
("over-aggressive filter drops apps without `android:appCategory`") — read that
before touching the filter, and keep its behaviour.

### 1.5 Import / export settings
All state is SharedPreferences (`LMT.xml`) plus the PNG icons under
`ResourceSearchPath`. Export = serialise prefs to JSON + zip the icons; import =
validate, write, then `restartServiceIfRequired()`.

This also solves a real distribution problem: these builds are signed with a local
key, so they cannot upgrade an installed upstream APK in place — switching requires
an uninstall, which currently loses every setting. See 3.3.

### 1.6 Material 3 overhaul
The app is `@android:style/Theme.Holo` (`Theme.Material` on v21) with framework
`Activity`/`Fragment` and `ActionBar` tab navigation, which has been deprecated
since API 21. A full overhaul means AppCompat/Material3 themes, AndroidX fragments,
`TabLayout` + `ViewPager2`, and replacing the hand-rolled
`FrameworkFragmentStatePagerAdapter`. The pie overlay itself is custom `Canvas`
drawing and is unaffected.

Do this **after** Kotlin and tests — it touches every screen at once.

### 1.7 Accessibility fallbacks
Deferred by request. Full analysis in Part 2.

---

## Part 2 — Accessibility fallbacks for root-only actions

Researched, not implemented. API levels below are from the SDK's own
`platforms/android-36/data/api-versions.xml`, not from memory.

The plumbing already exists: `AccessibilityHandler.performAction(int)` wraps
`performGlobalAction`, `performClick` wraps `dispatchGesture`, and
`xml-v22/service_config.xml` already declares `canPerformGestures="true"`.
`performGlobalAction` needs no extra manifest flags.

### 2.1 Drop-in replacements — one `case` each in `Launcher.fireAction`

| LMT action | today | replacement | since |
|---|---|---|---|
| **34 Screenshot** | root `keyevent 1000 120` | `GLOBAL_ACTION_TAKE_SCREENSHOT` (9) | API 28 |
| **40/41/42 Nova/Apex/Holo drawer** | root launcher intents | `GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS` (14) | API 31 |
| **5 Menu** | root `keyevent 82` | `GLOBAL_ACTION_MENU` (21) | **API 36** |

`GLOBAL_ACTION_MENU` is API **36**, so it works on a Samsung flagship (SDK 36) but not on the
Test phone (SDK 35). The drawer replacement opens the *system* all-apps surface, not
Nova's own drawer — close, not identical.

### 2.2 New capabilities LMT has no action for

`LOCK_SCREEN` (8, API 28) · `DISMISS_NOTIFICATION_SHADE` (15, API 31) ·
`DPAD_UP/DOWN/LEFT/RIGHT/CENTER` (16–20, API 33) ·
`MEDIA_PLAY_PAUSE` (22, API 36).

### 2.3 De-rooting that needs no accessibility at all

- **32 TaskerTask** is tagged `[Root]` but shells out to `am broadcast` purely as a
  way to send a broadcast. A plain `sendBroadcast` does the same. Caveat: modern
  Tasker gates this behind "Allow External Access" and the
  `net.dinglisch.android.tasker.PERMISSION_RUN_TASKS` permission, which the
  manifest would need to declare.
- **30 KillApp** — `killBackgroundProcesses` is already called in the pre-API-21
  branch and `KILL_BACKGROUND_PROCESSES` is already in the manifest. What broke on
  Lollipop was `getRunningTasks` for identifying the foreground app;
  `SettingsValues.getPackageNamesOfRecentApps()` already solves that via UsageStats.
  Partial fidelity — kills background processes, not a true force-stop.
- **4 HomeLongpress / 36 NowOnTap / 37 Assistant** all just summon the assistant.
  `Intent(Intent.ACTION_ASSIST)` does that without root.
- **26 Key**, for media keycodes only → `AudioManager.dispatchMediaKeyEvent`, which
  works on every API level (better than waiting for API 36's `MEDIA_PLAY_PAUSE`).

### 2.4 Genuinely root-only — no public API

`BackLongpress` / `MenuLongpress` (synthetic 2004/2082 keycodes) · `NextApp` /
`PrevApp` · `Data` (needs carrier privileges) · `ImmersiveMode` (needs
`WRITE_SECURE_SETTINGS`) · `UnpinApp` · `KillAllApps` · `Script` · arbitrary
non-media `Key`.

### 2.5 Mislabelled in the picker

**14 LastApp is tagged `[Root]` but already has a working non-root path** — it
checks `isRootAvailable()` and falls back to UsageStats + `doAppAction()`. The
picker tells users it needs root when it does not.

More broadly: several actions have a rich pre-Lollipop non-root implementation
followed by `if (SDK_INT >= 21) { root; return; }`. That legacy code is dead on
every modern device. It was abandoned because `getRecentTasks` was locked down in
API 21 — but UsageStats, which LMT already uses for `PieRecentApps`, is the modern
replacement for exactly that.

`Action.captions[]` carries the `[Root]` / `[Accessibility]` tags shown in the
picker, so every de-rooting must update its caption or the UI will lie.

---

## Part 3 — Bugs and hazards found

### 3.1 🔴 Root key injection is broken on Android 14+  (pre-existing, upstream)

**Verified on the test phone (Android 15) with root fully granted.** Root itself works:
`InputContext.jar` is copied, `su` is spawned, root-owned `app_process` helpers run,
and commands are written to the helper without error. But every key-injection
command silently fails:

```
E/LMT::InputContext: Failed to process command: input keyevent 24
E/LMT::InputContext: java.lang.NullPointerException
	at java.util.Objects.requireNonNull(Objects.java:207)
	at android.hardware.input.InputManager.getInstance(InputManager.java:405)
	at com.noname81.lmt.InputContext.injectKeyEvent(InputContext.java:367)
```

`InputManager.getInstance()` — a hidden API — now NPEs inside a bare `app_process`.

**This is not a regression from the reconstruction.** Proven by invoking the
verbatim shipped `InputContext29.jar` standalone under `su`, entirely outside LMT:
it fails identically. The jar ships byte-identical and `RootContext`/`Launcher` are
bytecode-verified equivalent.

**Blast radius** — every root key action is a no-op on Android 14+: `Home` (root
path), `Menu`, `HomeLongpress`, `BackLongpress`, `MenuLongpress`, `Key`,
`Screenshot`, `NowOnTap`, `Assistant`, `Search` (root path), plus `input tap` and
`input text`, which means `PiePointer`'s tap-warp too.

**What still works:** the `am` family (`last-app`, `next-app`, `prev-app`,
`kill-app`, `kill-all-apps`, `force-stop`, `move-to-front-or-start`) runs without
error — a different code path through `IActivityManager` reflection.

**Fixing it** is now tractable: jadx decompiles `InputContext29.jar` cleanly, so the
helper can be rebuilt from source. The fix is to stop using
`InputManager.getInstance()` and obtain the service the modern way. Doing so means
adding the helper jar to the build (it is currently a prebuilt asset), which is a
worthwhile change in its own right — that jar is the last binary blob besides
`libTouchServiceNative.so`.

This also makes 2.1's `GLOBAL_ACTION_TAKE_SCREENSHOT` more valuable than it first
appeared: it is not a convenience, it is the only way Screenshot works at all on a
modern device.

### 3.2 🟡 Latent out-of-bounds in `Action.toString()`

```java
if (mType > 0 && mType <= names.length) { typeName = names[mType]; }
```

`mType == names.length` indexes one past the end. Reachable from a corrupted or
hand-edited prefs file, or from adding an id to `names[]` carelessly. `<` is correct.

### 3.3 🟡 Signing key and downgrade hazards

- The upstream release key is not public, so these builds cannot upgrade an
  installed upstream APK in place — switching needs an uninstall, losing settings.
  This is the strongest argument for doing import/export (1.5) early.
- Lowering `versionCode` makes the APK uninstallable over an existing one
  (`INSTALL_FAILED_VERSION_DOWNGRADE`), even with `adb install -d`.

### 3.4 🟢 Not bugs — don't chase these

- **The pie does not render over the Settings app.** Android hides
  `SYSTEM_ALERT_WINDOW` overlays over Settings as tapjacking protection.
- **`dumpsys audio | grep streamVolume` reads the wrong stream.** Use
  `cmd media_session volume --stream 3 --get` when measuring volume changes; the
  first number in `dumpsys audio` is a different stream and does not move.
- **Reinstalling clears the accessibility service binding.** Re-enable it after
  every install, or accessibility-backed actions silently do nothing.

---

## Part 4 — Reconstruction cleanup still outstanding

- **347 `int i*`, 123 `String str*`, 69 `boolean z*`, 64 `float f*`** and similar
  decompiler-generated names. `docs/original-names.json` holds the 14 names
  recovered from the reference dex's `val$` capture fields — the reference
  preserves the original author's local names wherever a lambda or anonymous class
  captured one, so check there before inventing a name.
- **39 `JADX INFO` comments** still in the sources.
- **minSdk 15 → 21**, deleting the now-dead `SDK_INT < 21` branches. This changes
  bytecode by design, so run the oracle in "explain every diff" mode for that
  commit rather than expecting `EQUIVALENT`.
