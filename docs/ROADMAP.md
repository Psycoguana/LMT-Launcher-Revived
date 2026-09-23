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

### 1.2 Write tests  🟡 started
**Done:** JUnit + Robolectric wired up (`app/src/test/`), 39 tests green.
`ActionTest` pins the catalogue invariants — `names[]`/`captions[]` parity, every id
constant matching its slot, the `Offset*` sentinels staying ordered — plus a
regression test for the `toString()` off-by-one (verified it fails against the old
bound, so it is not a tautology). `SettingsSharedPrefsWrapperTest` pins the
persistence contract import/export will depend on, including the batched-editor
path where a write is invisible until `apply()`.

Test dependencies do not reach the release APK; the oracle stays `EQUIVALENT`.

**Remaining targets, in order:**

- ~~**`SettingsValuesBase` / `SettingsSharedPrefsWrapper`**~~ — **done**, 14 tests
  across `SettingsSharedPrefsWrapperTest` and `SettingsSlotSchemaTest`. The schema
  test is the important one: slots are addressed by bare integer index and
  `PieControl` walks hardcoded ranges (ring 1 = 0..9, ring 2 = 10..23, ring
  3 = 24..37), with nothing relating those ranges to the total — so an off-by-one
  would bind the wrong action to the wrong slice without failing to compile.
  Mutation-verified: reverting the pie count 38 → 24, i.e. silently losing the
  third-ring patch, fails two tests.
- ~~**`Action`**~~ — **done**, 8 tests. `TouchServiceResult` covered too (4 tests):
  its names are preference-key prefixes and drawable identifiers, not just labels.
- ~~**`PieMenu.findItem()` geometry**~~ — **done**, 13 tests. `getPolar` and
  `findItem` are private methods on a `FrameLayout` subclass, so they are driven by
  reflection against a real `PieMenu` and the scenario is installed by writing the
  private geometry fields. That exercises the shipped code and needs no production
  change, which matters while the oracle is still the safety net. Covers the polar
  branches (including the left-edge mirroring when `mCenter.x < mSlop`), ring
  bracketing, `mTouchOffset` widening, exclusive sweep bounds, and the
  `expandTriggerArea` branch where the outermost ring goes unbounded — which is
  exactly what a third ring re-points. Mutation-verified: flipping
  `getLevel() == mLevels` to `!=` and `<` to `<=` each fail the intended tests.
- **Instrumented smoke test** — service starts, pie opens, three rings render.
  This is currently done by hand via argent; `argent-qa-flows` could record it.
- **`inputcontext/` has no automated coverage at all** (865 lines) and, being an
  asset rather than part of `classes.dex`, no oracle coverage either. Its reflective
  routes can only really be validated against a real Android runtime — the probe in
  `tools/inputcontext/probe/` is the pattern; making it an on-device harness that
  asserts rather than prints is the next step.

> **Heads-up for 1.5 (import/export): there is a second, hidden storage backend.**
> `SettingsSharedPrefsWrapper`'s constructor checks
> `Settings.System.getInt(resolver, "LMTExternalConfig", 0)`. If that is **non-zero**
> the wrapper never opens SharedPreferences at all and instead reads and writes
> every setting through `Settings.System`. An exporter that only reads `LMT.xml`
> will silently produce an empty backup for anyone who has that flag set, and an
> importer that only writes `LMT.xml` will appear to succeed and change nothing.
> Handle both paths, or refuse clearly when the flag is set.

Do this **before** the Kotlin rewrite, so the rewrite has a safety net that
survives the bytecode oracle becoming meaningless.

### 1.3 Readability pass  ✅ done

**660 decompiler markers → 0**, across all 40 files, with the oracle `EQUIVALENT`
at every step and 39 tests green. Verified on device afterwards: service starts,
pie renders, no crashes.

Beyond renaming, two structural wins that stayed bytecode-identical:
- `Launcher.fireAction`'s switch dispatched on bare integers; all 38 cases now use
  the `Action.*` constants that already existed, generated from `Action.java`'s own
  declarations so the mapping cannot drift.
- 62 expanded post-increments collapsed in the settings screens: the original was
  `field = pos++`, which jadx had expanded into chains of `int iN = iM + 1`.

**Lesson worth keeping.** The oracle compares calls, strings and constants as
*sets*, so it cannot see an off-by-one in a runtime counter — nothing it looks at
changes. The post-increment collapse was therefore verified separately, by
reconstructing the assignment order from both the old and new source and comparing
(37 and 30 assignments, identical). Where a change touches arithmetic rather than
names, the oracle is not sufficient on its own.

Class-level javadoc added to all 40 files, drawn from `reference/plan.md` plus what
reading each file surfaced. They record the *why* the code cannot: why the trigger
strips are kept tiny, why `PermissionChecker` takes a `log` flag, why `PieMenu`
needed no change to gain a third ring, which storage contracts the tests guard, and
where the two root channels differ.

> **Rule for this pass: rename and comment only — do not add fields.** Extracting a
> magic number into a `private static final` emits a new field, which the oracle
> correctly reports as a real structural difference. Accumulating accepted
> deviations for cosmetics would erode exactly the signal that makes doing this in
> Java worthwhile. Named constants can come later as a separate, reviewed step.

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

### 1.7 ~~Rewrite in Kotlin~~ — deferred, probably cancelled

**Decision (2026-09-23): not doing this, at least not as a migration.**

It costs the oracle — Kotlin changes codegen wholesale, so per-method comparison
against the reference smali becomes meaningless. That trades verification of all 40
classes for a test suite covering 5, and buys nothing functional.

It also does not solve the readability problem: converting `int i2` yields
`var i2: Int`. Renaming does, and keeps the oracle (see 1.3).

And every genuine modernisation need here is language-independent — `AsyncTask`
(deprecated API 30, 6 files) → `Executor`; `ActionBar.Tab` (deprecated API 21,
`LMT.java`) → `TabLayout` + `ViewPager2`; `android.app.Fragment` (deprecated API 28,
7 files) → AndroidX fragments. All are part of the Material 3 overhaul (1.6) and all
doable in Java.

**What we are doing instead:** new code (import/export, app-picker search) may be
written in Kotlin from the start — the benefits are free there and there is no
migration risk. Revisit a wider migration only if it still appeals once features
land and coverage is real.

### 1.8 Accessibility fallbacks
Deferred by request. Full analysis in Part 2.

### 1.9 `libTouchServiceNative.so` — research, then probably delete

The last binary blob. 387 KB of C++ per ABI (four ABIs), stripped of `.symtab` and
debug info — but as a shared library it still exports **608 dynamic symbols, 565 of
them mangled C++**, which leak the full class and method structure without
disassembling anything.

**What it is for.** Drawing a shape anywhere on screen, inside any app, to fire an
action — square, diamond, "delete" scribble, question mark, and two-finger swipe
combinations. Fourteen gestures (`TouchServiceResult.names`), 11 of them two-finger,
because a one-finger square would collide with ordinary app interaction.

**Why it needs root and native code.** Android delivers touches only to the window
under your finger, so an app cannot see you drawing a square while you scroll
Chrome. This reads the touchscreen directly from `/dev/input`, below the framework,
so it observes the raw finger stream *without consuming it* — the app underneath
still works normally. That is the whole trick, and it is why this cannot be Java
against public APIs.

**Its structure is already known**, from the mangled names alone:

- `gestures::Recognizer` — `resample`, `rotateBy`, `scaleToSquare`,
  `translateToOrigin`, `distanceAtBestAngle`, `pathDistance`, `pathLength`,
  `centroid`, `boundingBox`. This is the **$1 Unistroke Recognizer** (Wobbrock,
  Wilson & Li, UIST 2007), function for function, in the paper's own naming — a
  published algorithm of ~200 lines with a free reference implementation.
- `gestures::Samples::getGesture*()` — the 14 templates as code.
- `Logic` — evdev plumbing, with two input paths: direct `/dev/input` read, and
  shelling out to `getevent` (hence the `popen2`/`pclose2` symbols).
- `Fingers` — multitouch state tracking. `geometry::Point2D` / `Rectangle2D` — trivial.

**Effort, if we ever rebuild it.** To readable pseudo-C: about a day in Ghidra, and
the 565 named symbols mean you are never lost. To *compilable, correct* C++: weeks,
and the result would be unmaintainable — decompilers emit pseudo-C, not source. To
**reimplement**: days, and it is the only sensible option — published algorithm,
14 template point arrays (extractable from `.rodata`, or simply re-recorded), and a
fresh evdev reader. The recogniser is arguably a candidate for pure Java/Kotlin,
which would delete the `.so` outright and make gestures work without root.

**But it is probably dead weight.** `TSMode` defaults to `2` and
`TouchService.onCreate` only starts the native thread when `TSMode < 2`, so the
default install never loads it. It serves only the legacy Gestures and ISA systems,
which additionally need root and raw `/dev/input`. It is from the 2011 era of
hardware buttons and no gesture navigation.

**Untested and probably rotten.** We have never exercised it. Given the
`InputContext` jar silently rotted for two years, and this pokes at `/dev/input` and
SELinux in ways Android has steadily tightened, do not assume it works on Android 15.

**Decision (2026-09):** leave alone for now. Research whether anyone still relies on
it — the XDA thread is the place to ask — then very probably delete it along with
the Gestures/ISA feature set, which would also remove `TSMode` 0 and 1.

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

- **32 TaskerTask** — the goal is a **first-class, root-free Tasker action**: the
  user picks a task from a proper picker and it runs. Not a workaround.

  Today the action is tagged `[Root]` and shells out to `am broadcast` purely as a
  way to send a broadcast, which needs no root at all. The work is:
  1. **Picking** — replace the free-text task-name field with Tasker's own task
     picker (`net.dinglisch.android.tasker.ACTION_OPEN_PREFS` / the
     `TaskerIntent` task-select intent) launched via `startActivityForResult`, so
     the user chooses from a list instead of typing a name that must match exactly.
  2. **Running** — `sendBroadcast` of `net.dinglisch.android.tasker.ACTION_TASK`
     with the task name, declaring
     `net.dinglisch.android.tasker.PERMISSION_RUN_TASKS` in the manifest. Tasker's
     "Allow External Access" must be on; detect it and say so plainly rather than
     failing silently.
  3. Retag the caption from `[Root]` to `[None]`.

  Note the mechanism is Tasker's own intents, not the accessibility service —
  accessibility is not involved in this one. There is an existing stopgap (the
  **Shortcut** action can target Tasker's "Tasker Task" shortcut — motomotomotoG,
  XDA, Mar 2026) but it is a workaround, not the destination.
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

### 3.1 ✅ FIXED — root key injection was broken on Android 15  (was pre-existing, upstream)

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

**Independently reported on XDA since September 2024**, and never fixed. See
"Corroboration" below — this is a long-standing, widely-hit bug, not an edge case.

**Version boundary.** Verified here on Android 15 (SDK 35). A user posted the
identical trace in Sept 2024 with `InputManager.java:277` where ours reads `:405`,
so it was already broken at least one Android release earlier. The exact first
broken release is still untested.

**This is not a regression from the reconstruction.** Proven by invoking the
verbatim shipped `InputContext29.jar` standalone under `su`, entirely outside LMT:
it fails identically. The jar ships byte-identical and `RootContext`/`Launcher` are
bytecode-verified equivalent.

**Blast radius** — every root key action is a no-op: `Home` (root
path), `Menu`, `HomeLongpress`, `BackLongpress`, `MenuLongpress`, `Key`,
`Screenshot`, `NowOnTap`, `Assistant`, `Search` (root path), plus `input tap` and
`input text`, which means `PiePointer`'s tap-warp too.

**What still works:** the `am` family (`last-app`, `next-app`, `prev-app`,
`kill-app`, `kill-all-apps`, `force-stop`, `move-to-front-or-start`) runs without
error — a different code path through `IActivityManager` reflection.

**FIXED** (see `inputcontext/` and `tools/inputcontext/README.md`). The helper is
now built from source by the `:inputcontext` Gradle module instead of shipping
prebuilt, and `InputInjector` resolves a working route at runtime —
`InputManagerGlobal` first, then `IInputManager` via `ServiceManager`, then the
original `InputManager.getInstance()` so older devices are unaffected.

All five per-SDK jars collapsed into one: they only ever differed where hidden-API
*signatures* changed, and the rebuilt helper reaches those APIs reflectively, which
defers signature resolution to runtime. `RootContext.copyRemote()` lost its SDK
ladder accordingly (recorded in `tools/accepted-deviations.json`).

Verified end to end on the test phone through the real app — pie slot bound to `Key 24`,
`Fire action type: Key` followed by `injectInputEvent via InputManagerGlobal`, and
the system volume moved. The last binary blob besides `libTouchServiceNative.so`
is gone.

**What this costs, action by action.** LMT tries accessibility first for its most
common actions, so the damage is uneven:

- *Dead, no alternative* (root-only, key-injection based): `HomeLongpress` (2003),
  `MenuLongpress` (2082), `BackLongpress` (2004), `Menu` (82), `Key` (arbitrary),
  `Screenshot`, `NowOnTap` (219), `Assistant` (219).
- *Accessibility-only now* — fine with the service enabled, dead without it:
  `Home`, `Back`, `RecentApps`, `Search`, `OpenPowerMenu`, and `PiePointer`'s tap
  (it calls `performClick` first, `input tap` only as the root fallback).
- *Unaffected* — these use `runCommandRoot`, a plain `su` shell, not InputContext:
  `Script`, `Wifi`, `Data`, `ImmersiveMode`, `UnpinApp`, `TaskerTask`, `Activity`.
- *Unaffected* — the `am` family through InputContext uses `IActivityManager`
  reflection, a different code path that runs without error: `NextApp`, `PrevApp`,
  `LastApp`, `KillApp`, `KillAllApps`, `move-to-front`.

**Corroboration from the XDA thread** (`xdaforums.com/t/...1330150`, pages 736-745).
Users hit this for two years without it being root-caused:

- **Kyonex**, Oct 2024, Samsung Flip 6 + KernelSU — "in root mode many features in
  LMT stop working (pie pointer draws fine but **won't simulate touch**, actions
  like **back**, **key 26**, **longpress home** won't work at all), **so I disable**
  [root]". Turning root *off* is the community workaround, because that forces the
  accessibility path.
- **NullCode**, Oct 2024 — posted the **identical stack trace**: same NPE, same
  `InputManager.getInstance`, same `InputContext.injectKeyEvent:367` /
  `sendKeyEvent:350` / `runInput:156` line numbers. Added: "I checked google docs
  and apparently the API this app uses is deprecated". Then: "I know absolutely
  nothing about decompiling apks and SMALI so it's pretty much impossible to try
  fix it for me." Nobody picked it up.
- **hugozo**, Dec 2025, OnePlus 12 rooted stock — "pie pointer works fine **without
  root**; **with root**, the 'pointed app' dont start, nothing append". Exactly the
  root-preferred-and-broken path.
- **93fuelslut**, Sept 2025, Pixel 10 Pro XL / Android 16 — "recent, home and back
  doesn't work ... the soft nav keys don't."
- **noname81** (the original author), Dec 2025 — "I think I fixed the first issue
  with **using accessibility instead root for pointer by default**." That is the
  accessibility-first fix, but applied only to `PiePointer`. The rest of the
  key-injection actions were left on the broken path, which matches what we measured.
- Still open per hugozo: a gesture bound to keycode **1026** (long-press power) is
  "broken since android 15 **with or without root**" — that one is not explained by
  this bug and needs separate investigation.

So the fix direction is already endorsed by the original author; it just was never
finished beyond the pointer.

This also makes 2.1's `GLOBAL_ACTION_TAKE_SCREENSHOT` more valuable than it first
appeared: it is not a convenience, it is the only way Screenshot works at all on Android 15.

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

### 3.5 🟡 Accessibility service silently unbinds (separate, still live)

Distinct from 3.1 and reported repeatedly on XDA: Android drops LMT's accessibility
binding while still *showing* it as enabled. motomotomotoG, Mar 2026 — "Android
disables Accessibility for the app ... Android will show that the app has
accessibility enabled, but it's not actually", fixed by toggling it off and on.
v3.11 was released to address a suspected cause and the reporter has not seen it
since, but at least one user still reported it afterwards. This matters more now,
because 3.1 means accessibility is the *only* working path for Home/Back/Recents —
when it unbinds, those actions die with no root fallback.

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
