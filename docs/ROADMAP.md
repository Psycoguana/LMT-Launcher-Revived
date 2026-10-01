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
- ~~**UI safety net before the Material 3 rewrite (1.6)**~~ — **done.** Robolectric
  drives the real screens *by visible label*, so the expectations outlive the
  rewrite; only the driver (`UiTestSupport`) knows about today's ListView.
  - `SettingsScreenUiTest`: all 49 Settings rows (TSMode 1 shows every section).
    Each row is clicked on a fresh screen. The test answers whatever it opens
    (value, choice, colour, confirmation), toggles its switch, and diffs the
    stored settings. Frozen as a label → keys/started-intents table. The widget
    type is deliberately left out of the contract, so a choice dialog may become a
    switch in the new UI. Swapping two rows' `pos*` wiring fails with a message
    naming the crossed rows.
  - `SlotMappingUiTest`: every Pie/Gesture/ISA row edits the slot its label names.
    Expected slots come from how the *engine* reads them (`PieControl` rings,
    `TouchServiceResult.names`, `getIsaAction` zones), not from the fragments'
    header-skipping arithmetic. Pie icon buttons are covered too, since they use a
    separate offset table. Both mutations tried were caught.
  - `ActionPickerUiTest`: the row labelled `Action.names[t]` assigns action t,
    opens the app picker in the right mode (App 0, Activity 1, Shortcut 5), or
    asks for its payload. Every real action is offered; `Offset*` markers never
    are.
  - `MainTabsUiTest`: which tabs each TSMode shows.
  - `SettingsValues` caches slots in a static, so these tests must reset it after
    themselves. The first full run showed `SettingsSlotSchemaTest` inheriting
    action 45 from the picker test.
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

### 1.4 App search + "show system apps" toggle in the app picker  ✅ done
Search box in the action bar, plus an overflow toggle for system packages which is
remembered (`ShowSystemApps`, off by default — a stock device lists a couple of
hundred otherwise). Updated system apps such as Chrome count as user apps and stay
visible. 7 tests in `AppPickerFilterTest`.

**The trap here, twice.** `onItemClick` resolves a tap as
`mPackageInfos.get(position - 1)` — display index into the backing list — so
filtering the adapter alone would silently bind the wrong app. Every filter change
therefore rebuilds the model and the adapter together.

The *same* coupling then bit again somewhere I had not looked: the icon loader
reached into the activity's list by position from an `AsyncTask`, so a load queued
before a filter change indexed past the end of the shorter list and crashed with
`IndexOutOfBoundsException`. Caught on device, not by the oracle — which cannot see
it, since nothing it compares changed. `AppSimpleAdapter` now carries its own model
snapshot and never looks outside it.

### 1.5 Import / export settings  ✅ done
`SettingsBackup` exports every setting to `settings.json` plus any `pieN.png`
icons under the resource search path, zipped together; import validates the zip
has a `settings.json` entry, writes every key back, then
`restartServiceIfRequired()`. Reachable from Settings → Backup (bottom of the
tab). On API 19+ this opens the real Storage Access Framework picker
(`ACTION_CREATE_DOCUMENT` for export, `ACTION_OPEN_DOCUMENT` filtered to
`application/zip` for import) so the user picks the destination/source from
Downloads, Drive, or wherever — not a typed path. Below API 19 (minSdk is
still 15), or if there is no owning fragment to route the SAF result back to
(`SettingsViewActivity`'s dead-code path), it falls back to the original
`InputDialog` path-entry flow. 7 tests in `SettingsBackupTest`; verified
on-device both ways — pushing a hand-edited backup through the path-entry
fallback, and separately exporting/importing through the real Downloads
picker UI.

**`SettingsBackup` takes a stream, not a `File`.** SAF only ever hands back a
`content://` `Uri` — there is no filesystem path to open a `FileOutputStream`
on. `exportZip`/`importZip` take an `OutputStream`/`InputStream` instead, so
the same code serves both `ContentResolver.openOutputStream(uri)` (SAF) and
`new FileOutputStream(file)` (the path-entry fallback, and the tests).

**The SAF result has to come back through the fragment, not the activity.**
`LMT.onActivityResult` doesn't call `super`, and doesn't know about
`SettingsViewHelper` (each fragment builds its own instance) — so routing the
picker result through the `Activity` would need new plumbing there.
`android.app.Fragment.startActivityForResult` dispatches results through the
framework's own fragment-result path independently of the hosting
`Activity.onActivityResult` override, which is exactly how `PieFragment`'s
existing gallery-icon picker already works. `SettingsViewFragment` now owns
`onActivityResult` the same way, and delegates to two new
`SettingsViewHelper` methods.

**Goes through the existing wrapper, not raw SharedPreferences.** `SettingsBackup`
reads and writes every key via `SettingsSharedPrefsWrapper`'s loadInt/loadString/
saveInt/saveString — the same layer every setting already uses — so it handles
the `LMTExternalConfig` second backend (see 1.2's heads-up) automatically,
without ever checking which one is active.

**Scalar defaults are copied from the real getters, not reinvented.** An
untouched setting has no key in storage at all; its effective value comes only
from the fallback each `load*()` method passes. `SettingsBackup` keeps its own
`{key, default}` table copied verbatim from `SettingsValuesBase`, rather than
calling those getters, because two of them (`TouchscreenScreenFactorX/Y`)
convert to a derived float and back would risk a rounding-truncation bug.
Confirmed on the device that a stock install's `LMT.xml` holds only 3 of the
~240 keys the export produces — everything else is a getter-supplied default,
so getting these right is what makes a backup from an untouched install
non-destructive to restore.

**Wrong screen, caught before shipping.** The first pass added the Backup
section to `SettingsViewActivity` — which matches `SettingsViewHelper`'s own
javadoc ("host for settings sub-screens too large for the main tabs") but turns
out to be dead code: nothing in the app ever starts it by `Intent`. The live
screen is `SettingsViewFragment`, whose section list is built dynamically per
feature mode rather than with `SettingsViewActivity`'s fixed positions. Caught
by driving the actual screen with argent before calling this done, not by the
oracle or the unit tests — neither can see that an `Activity` is unreachable.

**I/O correctness bugs the oracle and the type system don't see:**
- The first `exportZip` closed the shared `ZipOutputStream` after copying the
  first icon file's `FileInputStream` in a `try`/`finally` that closed both
  streams, so a second icon or the trailing `closeEntry()` threw. Caught by a
  test, not by inspection.
- Zip-slip: an icon entry named e.g. `icons/../../evil.png` would write outside
  the resource search directory on import. Guarded by a canonical-path check
  before any entry is extracted; pinned by
  `importZipCannotEscapeTheIconDirectory`.

This also solves a real distribution problem: these builds are signed with a local
key, so they cannot upgrade an installed upstream APK in place — switching requires
an uninstall, which currently loses every setting. See 3.3.

### 1.6 Material 3 overhaul  ✅ done
The app was `@android:style/Theme.Holo` (`Theme.Material` on v21) with framework
`Activity`/`Fragment` and `ActionBar` tab navigation, deprecated since API 21.
Done in three phases, each verified on-device before the next:

- **Phase 1** (`84620a5`, `94a3df7`, `fbde96e`): `minSdk` 15→21, `LMT` becomes an
  `AppCompatActivity` with `MaterialToolbar` + `TabLayout` + `ViewPager2`
  (`FrameworkFragmentStatePagerAdapter` deleted), the 5 tab fragments move to
  AndroidX. Also extended the equivalence oracle so a `reworked` class skips
  content diffs too, not just member presence — per-method bytecode
  equivalence stops being useful signal once a class is being intentionally
  rewritten.
- **Phase 2** (`6a1edc2`): the Settings tab becomes a `PreferenceFragmentCompat`
  backed by a small `PreferenceDataStore`, replacing the ~1572-line
  `SettingsViewHelper` position-dispatch dispatcher and the dead
  `SettingsViewActivity`. Kept `ColorDialog`/`InputDialog` (plain
  `android.app.AlertDialog.Builder`) instead of stock `ListPreference`/
  `EditTextPreference` — their dialogs build on
  `androidx.appcompat.app.AlertDialog`, which Robolectric's `ShadowAlertDialog`
  doesn't track, and would have silently broken the UI safety-net tests.
- **Phase 3** (`f7f0ad7`..`3e6d8ec`): `PieFragment`/`GesturesFragment`/
  `IsasFragment`/`CommandSelectActivity`/`MultiSelectActivity` move from
  ListView + `SeparatedListAdapter` to RecyclerView, via a new shared
  `SectionedRows<T>` (reproduces the old header/row flattening, plus an
  `absoluteRowIndex` that replaces fragile hand-rolled position arithmetic).
  `AsyncDrawableTask` (`AsyncTask`) is replaced by a plain-`Executor`
  `IconLoader` and deleted. `CommandSelectActivity`/`MultiSelectActivity`
  become `AppCompatActivity` with their own `MaterialToolbar`. Dialogs stay on
  plain `AlertDialog.Builder`, same reasoning as phase 2 — confirmed
  `MaterialAlertDialogBuilder` has the identical `ShadowAlertDialog` problem.
- **Polish round** (`c8b1566`, `d87472e`, 2026-09-24): the phases above made the
  app M3-*themed*; this made it look M3. The dialog blocker above turned out to
  be wrong — `ShadowAlertDialog.getLatestDialog()` is the inherited
  `ShadowDialog` static, which tracks any `Dialog` — so `ColorDialog`/
  `InputDialog` and the inline confirms now use `MaterialAlertDialogBuilder`
  (M3 Sliders, outlined text fields); tests read dialog fields from the shown
  view tree instead of shadow accessors. The app picker's search is an inline
  pill field in the toolbar (no "LMT" title, no autofocus) and "show system
  apps" is a filter `Chip` instead of an overflow menu item. Settings switches
  are `MaterialSwitch`. Every list screen's section titles and rows now match
  the Settings tab's M3 category/row styling (sentence case, no all-caps), and
  the `layout-v17` copies that silently shadowed `layout/` are gone. Also fixed
  a real race it surfaced: `IconLoader` worker threads could build a second
  `SettingsValues` and lose the selected slot (was the flaky
  `SlotMappingUiTest` ISA test).

The pie overlay itself is custom `Canvas` drawing and was unaffected throughout.

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

### 1.10 Theme customization  ✅ done

Settings → General → **Appearance** opens its own page (`AppearanceActivity` +
`AppearanceFragment`), so the long Settings list only gains one row.

- **Theme:** follow system / light / dark, via `AppCompatDelegate.setDefaultNightMode`.
  Works on every API level.
- **Follow system color** (switch): Material You, the scheme Android derives from
  the wallpaper. Picking a color from the grid turns it off; turning it off goes
  back to the last grid pick.
- **Color:** a grid of color circles (`ColorSwatchDialog`, a plain RecyclerView in
  a Material dialog, no extra library): Default (the stock M3 baseline, unchanged
  look), 14 Material-palette presets and Custom. The circles use an oval-masked
  ripple (the stock borderless ripple drew a square on One UI). Custom opens
  `ThemeColorDialog`: a saturation/brightness square, a hue bar, a preview circle
  and a hex field, all hand-drawn Views (`SaturationValueView`, `HueBarView`)
  rather than a library; opaque only, since the color seeds a scheme. The Color
  row shows the current choice as a circle (`ColorDotPreference`).
  Everything but Default goes through Material's `DynamicColors`,
  which builds a full M3 tonal scheme from the wallpaper or a single seed
  (`DynamicColorsOptions.setContentBasedSource`), so no hand-written palettes.
  `DynamicColors` is Android 12+ only; below that both rows are disabled and say so.
- Stored as `ThemeMode`/`ThemeFollowSystemColor`/`ThemeColor`/`ThemeCustomColor` ints and included in the
  backup export/import (1.5).
- `ThemedActivity` is the base of every screen. Night mode must be set in
  `attachBaseContext`: set in `onCreate`, AppCompat patches the configuration in
  place when the app's mode differs from the system's, which silently discards a
  seeded colour scheme applied to the same resources (seen on-device: light mode +
  blue came up purple). The colour scheme is applied before `super.onCreate`.
  `LMT` compares a theme stamp in `onResume` and recreates when a colour changed
  elsewhere (night mode changes recreate activities by themselves).
- Also fixed: the status bar takes the surface colour, but icons stayed white in
  light mode. `values-v23` sets `windowLightStatusBar` from `isLightTheme`.
- Tests: `AppearanceUiTest` (every theme choice, the system-color switch and a
  grid pick turning it off, disabled before API 31), `ThemeColorDialogTest` (hex
  in/out) and `ThemeHelperTest`. Seeded schemes can't be
  exercised under Robolectric (its resource engine can't apply the
  `ResourcesLoader` colour override), so that path was verified on the test phone:
  blue, teal, pink, a custom color from the new picker, system color, light and
  dark, and back to Default. Radio buttons
  and switches were measured on-device too: 20dp radio icons and 52dp switch
  tracks, the M3 defaults.
- Follow-up polish (light theme and responsiveness):
  - The bundled command/gesture icons are white PNGs drawn for the pie's dark
    background and vanished on light lists. `IconLoader` now tints any grey-only,
    mostly white bitmap `colorOnSurfaceVariant` on a light theme. App icons and
    coloured icons are left alone. The command picker's checkmark is tinted
    `colorPrimary`.
  - Switching tabs flashed every icon: `ViewPager2` dropped off-screen pages and
    each tab's `onResume` rebinds its rows, resetting icons to the placeholder
    and fading them in. All tabs now stay alive (`setOffscreenPageLimit`), and
    `IconLoader.load` takes a row key: rebinding the same row keeps the current
    icon and swaps the reload in place. A load that lands after its view was
    rebound is dropped (this was an existing race).
  - The app picker scanned every installed package on the main thread before
    showing anything. It now opens at once with pulsing skeleton rows and fills
    in when a background scan finishes.
  - Single-choice dialogs drew the platform radio, which One UI leaves grey when
    checked. `ThemeOverlay.LMT.MaterialAlertDialog` swaps in
    `dialog_singlechoice_item` with a radio drawn from theme colours (primary
    when checked).
  - Export/import toasts say "Settings exported" / "Settings imported" with no
    URI. The "Show system apps" chip sits at the right.

### 1.11 Blurred pie background  ✅ done (Samsung live blur done; AOSP blur-behind still open)

**Samsung live blur, done (Samsung flagship):** `SamsungBlur` sets `SemBlurInfo` (`BLUR_MODE_WINDOW`) on the
`PieBlur` view inside LMT's own overlay window, by reflection, when `SemFloatingFeature` has
`SEC_FLOATING_FEATURE_GRAPHICS_SUPPORT_3D_SURFACE_TRANSITION_FLAG` (the Samsung flagship has it, the test phone
doesn't and keeps the screenshot). No screenshot, so the pie shows at once and the blur is
live. Needs no probe app or extra accessibility service. A window blur can't leave a hole, so
"Blur behind pie" becomes a disc (centre blurred too); the **Samsung Live Blur** switch
(`PieBlurLive`) falls back to the screenshot ring. It is on by default only where live blur
exists and greyed out elsewhere. Toggling it restarts the service: once set, the Samsung blur
stays on the reused `PieBlur` view, so the screenshot route would otherwise never come back.

Blur what's behind the pie while it is open, as the system does for the
notification shade and power menu, instead of drawing the pie over the plain app.

**Findings (2026-09-24), from the phones' own framework/services jars plus web research:**

- **AOSP cross-window blur is off on both test phones.** `adb shell wm disable-blur`
  prints `Blur supported on device: false` on the test phone and the Samsung flagship
  (`ro.surface_flinger.supports_background_blur` unset). So `FLAG_BLUR_BEHIND` /
  `setBlurBehindRadius` / `setBackgroundBlurRadius` (API 31) do nothing there. Samsung's
  developer forum has an unanswered "Why does OneUI not support CrossWindowBlur?"
  thread (S24 Ultra). This route only helps Pixels and similar phones.
- **Samsung has its own blur API: `android.view.SemBlurInfo` + `View.semSetBlurInfo`**
  (decompiled from `framework.jar` on both phones). Three modes:
  - `BLUR_MODE_WINDOW` (0): a live blur of whatever is behind the window, done by
    SurfaceFlinger, with presets (`BLUR_UI_*`/`BLUR_BG_*`), corner radius, clip rect and tint.
    Gated only by `CoreRune.FW_WINDOW_BLUR_SUPPORTED`, which comes from the floating
    feature `SEC_FLOATING_FEATURE_GRAPHICS_SUPPORT_3D_SURFACE_TRANSITION_FLAG`,
    *not* by the AOSP switch. **The test phone doesn't have that feature** (logs "This model
    doesn't support window blur!"). The Samsung flagship likely does (untested: disconnected).
  - `BLUR_MODE_WINDOW_CAPTURED` (1): blurs a bitmap you pass in. The test phone declares
    `SEC_FLOATING_FEATURE_GRAPHICS_SUPPORT_CAPTURED_BLUR`, so this is probably how One UI
    blurs its own shade on low-end phones: capture once, blur the still image.
  - `BLUR_MODE_CANVAS` (2): blurs the view's own content (useless here).
  - Not documented. It would be called through reflection, and it's unknown whether hidden-API
    rules let a third-party app call it (to test). There's also a
    `SEM_EXTENSION_FLAG_CHANGE_DIM_EFFECT_TO_BLUR` window extension flag (turns dim-behind
    into blur); not yet traced.
- **The screenshot route's cost is one capture per pie open, not a stream.**
  `AbstractAccessibilityServiceConnection.takeScreenshot` (test phone `services.jar`) calls
  `WindowManagerService.captureDisplay` → SurfaceFlinger on the GPU → a `HardwareBuffer`
  handed over without a copy (`Bitmap.wrapHardwareBuffer`), rate-limited to one call per
  333 ms (confirmed in code), needs the `canTakeScreenshot` capability (flag 128). Upper
  bound for the capture on the test phone: root `screencap` to /dev/null takes 280–410 ms, but that
  includes starting a process, copying to the CPU and writing ~10 MB. The accessibility
  path skips all three; its real latency is **unmeasured**. The blur itself
  (`RenderEffect`, API 31) runs on the RenderThread (GPU). The screen behind is frozen
  while the pie is open, which is fine (video behind the pie would freeze).
- **Libraries don't help:** BlurView (Dimezis), RealtimeBlurView and BlurBehind blur views
  *inside their own app*. None can see another app's pixels from an overlay.
- **Rejected:** MediaProjection (a consent dialog every session and a cast indicator), root
  `screencap` (too slow, see above).
- **Window geometry is not a blocker:** `PieLayout.expandLayout()` already makes the
  window full screen while the pie is open; `shrinkLayout()` puts back the edge strip.
  Set blur in the first, clear it in the second.

**Scope: whole screen or just behind the pie? Undecided, so build both** as one
setting (Off / Behind the pie / Whole screen) and pick the default after seeing them.
Every route below can do both:

| Route | Where it works | Behind the pie only |
|---|---|---|
| Samsung `BLUR_MODE_WINDOW` | Samsung phones with the 3D-surface feature flag (likely flagships) | Supports clip path and corner radius, so a disc fits naturally |
| AOSP blur (`setBackgroundBlurRadius`) | Android 12+ where `isCrossWindowBlurEnabled()` (Pixels and some other brands, never Samsung) | A disc-sized window with a corner radius of half its size; cheaper than full screen |
| Accessibility screenshot + `RenderEffect` blur | Any Android 11+ with the service on, any brand. The universal floor | Capture cost is the same (whole display); only the (already cheap) blur shrinks. A frozen snapshot is much less noticeable in a small disc |
| None | Android 5–10, or the service off | Today's look (maybe a dim). Rooted phones below 11 could use root `screencap`, but it's slow |

Disc details: the pie opens where you touch and grows outward, so the blur must be a
circle that follows it, not a square. Pick routes in the table's order, checked while
the app runs, and listen with `addCrossWindowBlurEnabledListener` (battery saver turns it off).

**Probe results (test phone, TEST_MODEL, Android 15, 2026-09-24).** `blurprobe/` is a separate
throwaway app driven by adb broadcasts (commands in `ProbeService`'s javadoc). Sideloaded,
it needs `cmd appops set com.noname81.blurprobe ACCESS_RESTRICTED_SETTINGS allow` before
its accessibility service can be enabled.

| Route | Result on the test phone |
|---|---|
| Accessibility screenshot + `RenderEffect`, **disc** (r=400 px) | ✅ Works, aligned. Capture **~21 ms**; to the first frame committed **~75–90 ms** warm (6 runs, avg 91 ms) |
| Same, **full screen** | ✅ Works. Capture ~20 ms; first frame 90–160 ms warm (avg 211 ms including one 672 ms cold first run) |
| Samsung `BLUR_MODE_WINDOW` (live) | ❌ "This model doesn't support window blur!" (as predicted from the feature flag) |
| Samsung `BLUR_MODE_WINDOW_CAPTURED` | ⚠️ Works but worse: needs an `ARGB_8888` CPU copy (54–208 ms), a HARDWARE bitmap crashes at draw time, first frame 230–630 ms, a weaker blur, and it stretches the *whole* bitmap into the view (a disc would need a CPU crop first) |
| `semAddExtensionFlags(CHANGE_DIM_EFFECT_TO_BLUR)` + `FLAG_DIM_BEHIND` | ❌ No blur, and the dim disappeared too |
| AOSP `FLAG_BLUR_BEHIND` | ❌ `isCrossWindowBlurEnabled=false`, nothing drawn. `ViewRootImpl.createBackgroundBlurDrawable` is not reachable, so an AOSP *disc* on an overlay would need another way (a disc-sized window with blur-behind blurs the whole screen) |

Reflection into `SemBlurInfo` / `View.semSetBlurInfo` is **not** blocked by the hidden-API
rules. The times above include creating a new window each time. The real pie reuses its
expanded window, so it only swaps a bitmap in and should be faster.

**Conclusion so far:** the accessibility screenshot + `RenderEffect` route is fast
enough (one ~20 ms GPU capture per pie open, blur on the RenderThread; nothing runs while
the pie is closed, so no battery cost at idle) and is the universal Android 11+ route. Samsung
live blur and the dim-to-blur flag still need testing on the Samsung flagship (flagship, likely has the
feature flag): `--es t sem --ei mode 0 --es scope disc` and `--es t dimblur`. Android 11
(API 30) has no `RenderEffect`; it needs a downscale + CPU blur on a small copy, or no blur.

**Build notes for the real thing:** capture on the pie's touch-down, before the pie
draws (otherwise the pie is in the screenshot), and fade the blur in. Keep one blur view in
the expanded window and only swap its bitmap. Close the `HardwareBuffer` on shrink. Opening
the pie twice within 333 ms gets `ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT` → reuse the
last frame. Needs `canTakeScreenshot` in LMT's service config (check whether that makes
existing users re-enable the service). Add an Off / Behind the pie / Whole screen setting,
backed up by 1.5's export.

**Built (2026-09-24):** `PieBlur` (a View added behind the pie) implements the accessibility
screenshot + `RenderEffect` route. Settings → Pie style → **Blur** (None / Blur background /
Blur behind pie, key `PieBlur`) and **Blur amount** (radius 1–150 px, default 50, key
`PieBlurRadius`); both are in the export. `PieMenu.onTouchEvent` calls `PieBlur.open` on
touch-down (the pie stays invisible until the frame arrives, 150 ms timeout) and `close`
on release/cancel. `canTakeScreenshot` was added to `xml-v22/service_config.xml`: on the test phone the
update kept the service enabled and granted the capability (`capabilities=160`). Samsung
live blur (Samsung flagship) and AOSP blur-behind are not wired yet; they'd be upgrades on top of this.

Behind-pie mode blurs a **ring**: the inner circle stays sharp (the blur sits on a `RenderNode`
and `onDraw` clips the hole out with `clipOutPath`, since outlines can only clip convex shapes).
Confirmed by hand on the Samsung flagship. Verified on the test phone with synthetic touches (`adb shell input motionevent DOWN/UP` on the pie
strip; the pie *does* accept them) and see-through pie colors (`#99000000,#cb000000,,#ffa70000,,,`).

### 1.12 Red theme color makes text look washed out  ✅ done

Cause, measured on the test phone: in dark mode M3 puts `colorPrimary` (and `colorSecondary`) at
**tone 80**, and at that lightness red can only reach chroma ~29 (`#ffb4a9`, salmon). The
scheme isn't the problem: Content, Fidelity and Vibrant all give the same tone-80 primary.
Also, the Settings tab's category titles read `android:colorAccent`, which M3 maps to the
*secondary* color (muted `#a23d32` in light mode), while the other tabs' headers use primary.

Fix (`ThemeHelper.applySeed`): do what `setContentBasedSource(seed)` does (SchemeContent →
`ColorResourcesOverride`), but in dark mode replace `material_personalized_color_primary`
with the same hue at **tone 70** (`DARK_PRIMARY_TONE`, red → `#ff8a7b`, chroma ~50). Contrast stays
8.1:1 on the surface, 6.3:1 on dialog containers and 5.7:1 under on-primary text. Tone 65
(`#ff7161`) would be redder at 6.9 / 5.3 / 4.9. `android:colorAccent` → `?attr/colorPrimary`,
so every header uses the same color. Light mode keeps M3's tone 40 (`#a50007`, already vivid).
Wallpaper (Material You) and Default aren't touched.

The bigger cause on the Samsung flagship: One UI reports `UiModeManager.getContrast() == -1` (*reduced*
contrast, `settings get secure contrast_level`), and Material builds the seeded scheme with it,
so onSurface text came out `#b89d99` instead of `#fadcd7`: every title looked greyed out
or disabled. `ThemeHelper.systemContrast` now clamps at 0 (high contrast still applies). Uses Material's `@RestrictTo` color API,
so re-check it on Material upgrades.

### 1.13 Optimization pass: less verbose code, performance, battery  ✅ done

A review pass over the whole app, looking for:

- **Verbosity:** code shapes left over from decompilation (redundant `this.` qualifiers,
  `-2`/`8388613`-style magic numbers instead of named constants, duplicated `PieLayout` add
  blocks in `PieContainer.attachToWindowManager`, long `if` ladders that could be tables),
  dead branches for SDKs below minSdk 21 (e.g. `SDK_INT < 16` in `PieContainer.rotate`;
  overlaps with Part 4), and helpers now shared by the M3 rewrite.
- **Performance:** work on the main thread (settings reads, package queries, icon
  decoding), repeated `SettingsValues` lookups in hot paths (pie touch/draw), allocations in
  `onDraw`/`onTouchEvent` in `PieMenu`/`PieControl`, an in-memory app-icon cache
  (the app picker re-fades its icons on every open).
- **Battery:** what runs while idle: the always-on overlay strips, the accessibility
  event types subscribed (`typeNotificationStateChanged` only?), wake locks, polling
  or timers in `PieStatusInfo` (clock/battery/notifications while the pie is closed),
  broadcast receivers kept registered, and the root `InputContext` process lifetime.
- Keep every change oracle-clean; measure before/after (`dumpsys gfxinfo`,
  `dumpsys batterystats`, Perfetto) rather than guessing.

**Findings so far** (Perfetto via argent's native profiler, `top -H`, test phone):

- **Battery bug, fixed:** with gestures on (`TSMode` < 2) and no working root, the native
  recogniser reads a `su getevent` pipe whose child already exited. `read()` returns EOF
  forever and `TouchServiceThread` burns **96% of a core, even with the screen idle** (89% of
  it in the kernel). The loop is inside `libTouchServiceNative.so`, so `TouchService` now
  has a watchdog. It samples the thread's CPU time from `/proc/self/task/<tid>/stat` 2 s
  after start, decides 10 s later, then checks every 60 s (on uptime, so it never wakes the
  phone). Over half a core → it calls `quit()` on the thread and the library and shows
  a toast. Measured after the fix: 0 CPU ticks over 10 s idle.
- **Pie:** ~300 ms CPU per open/sweep/close across all threads; main-thread time is the
  framework relayout from `expandLayout` plus the blur's first frame. LMT's own
  `PieMenu` code never shows up in the samples, so micro-optimizing it wouldn't be measurable.
- **Idle:** no leaked `Timer` threads, no wakeups with the pie closed.
- **Settings UI:** cold start ~520 ms / warm ~80 ms on the test phone; tab switching and scrolling
  miss the odd frame by 11-25 ms, in framework code that's still interpreted (`nterp`).
  **Baseline profile, done:** `app/src/main/baseline-prof.txt` marks all of LMT's code
  hot, and `profileinstaller` is pinned to 1.4.1. The 1.3.1 that AppCompat brought in
  logged `RESULT_UNSUPPORTED_ART_VERSION` on Android 15+, so no profile, not even
  AndroidX's own, was ever applied there. Cold start on the test phone (median of 6): verify
  598 ms, speed-profile 572 ms, full `speed` compile 547 ms (the ceiling). About 4%: startup is
  mostly framework work (theme, inflation), not LMT's code. The compile happens in the
  phone's idle maintenance after install.
- **Root that isn't there, fixed at the source:** `RootContext` treated "su started" as root,
  but Magisk can still deny it (the test phone does). It now sends `id` to its already-open root
  shell once, on a worker thread, and records granted (`uid=0`) or denied (EOF). One extra
  command per process start, no polling. With root denied or missing:
  `isRootAvailable()` returns false (root paths fall back to accessibility where they have one);
  the gesture thread isn't started at all (toast: gestures need root); "Set active features"
  greys out the two Gestures options and says why; the action picker greys out actions whose
  caption offers no way other than root on this Android version (`Action.needsRoot`) and
  explains with a toast on tap. The saved feature set is kept, so granting root later brings
  gestures back: after a "no", `recheckRoot()` asks su again when the service starts and when
  the Settings screen resumes with gestures selected (coming back from Magisk). It skips phones
  with no su on the PATH, and pie-only users are never asked again. Accessibility-capable actions stay selectable even while the service is off.
- `<profileable android:shell="true"/>` added to the manifest so Perfetto gets call stacks
  from release builds.
- **Gestures on the wrong device (upstream):** the default gesture input, `event4`, is a
  proximity sensor on the test phone (the touchscreen is `event3`), and nothing ran Auto configuration
  for you, so gestures silently did nothing. Before the reader starts, the service now checks
  the saved device reports touch positions (`getevent -p`) and otherwise switches to the
  touchscreen `TouchscreenDetector` finds, with a toast. Auto configuration shares the detector,
  which also fixes it reading only the last digit of the device number (`event10` → 0).
- **Leaked root readers (upstream):** stopping a reader closed its pipe but never killed the
  `cat /dev/input/eventN` running under su, so every service restart left one behind (6 on
  the test phone). The same su call that lists the devices first runs `pkill -f '^cat /dev/input/event'`.
  "Set gesture input" and Auto configuration no longer open the device themselves on top of
  restarting the service (that made two readers).
- **Settings dialogs (UX):** number rows are sliders (range, unit, Default button); rows whose
  number encoded modes are choice dialogs ("Custom..." leads to a size slider or the color
  editor). The user image folder is picked with the system file picker: `IconFolder` reads
  and writes a picked `content://` folder (persisted permission) or a plain path, for the pie,
  the Pie tab and backups. Upstream bug found on the way: `loadResourceSearchPath()` ignored
  the saved value, so the typed path never did anything. The Script action gets a
  **Browse...** button; the picked document is turned back into a path for the root shell
  (storage volumes and Downloads' `raw:` ids; cloud files have none).
  Every row's summary now ends in "Currently: ..." (slider rows: "500 ms (default)" or
  "300 ms, default 500 ms"), updated as soon as it changes. Both "Clear ... blacklist" rows ask
  first and show how many apps are excluded; with none they only say so.
  The pie color editor previews a real pie slice (`PieTileView`, pointing right like the app
  icon) instead of a color swatch: fill and outline with their gradients, shift stroke and a
  tinted icon, at the user's radii and sizes, over a checkerboard for transparency. It shows
  the slice selected while "selected color" is edited, and a typed color string updates it.
- **Code cleanup:**
  - Bytecode-neutral: the decompiler's `@Override // <class>` and `// from class:` comments,
    self-assignments (`x = x;`, 12 in `PieMenu.layoutPie`), a bare `hashCode()`, and magic
    numbers replaced by their constants (`Toast.LENGTH_*`, `Context.*_SERVICE`,
    `MotionEvent.ACTION_*`, `TypedValue.COMPLEX_UNIT_DIP`, `PendingIntent.FLAG_IMMUTABLE`,
    `Gravity.END`). The oracle output didn't change.
  - Bare `Double.isNaN(x);` statements removed (3 methods, accepted deviations).
  - Every `SDK_INT` check against 16-21 is gone with its dead branch: pre-Lollipop
    recent/running-task code in Launcher (and the `ActivityManager` lookups, task-list
    fields, `moveTaskToFront`/`startActivity`, the `Timer` kill paths),
    `initRemoteProcess20`, the reflection mobile-data toggle, and more. About 260 lines fewer.
  - Left alone: `this.` qualifiers (pure churn) and the long settings/action `if` ladders
    (the oracle compares them method by method; rewriting them as tables would hide real
    changes in the noise).

---

### 1.14 Shizuku integration — possible future feature

Gestures and ISAs need root only because `libTouchServiceNative.so` runs
`su -c "cat /dev/input/eventN"` (decoded from its `Logic::setInputDeviceGetevent`: the
command is built from immediates, the `Pure` direct-open mode is never called). A normal app
can't read `/dev/input`, but Android's **shell** user can: on the test phone, `adb shell` is uid 2000
with the `input` group, and `cat /dev/input/event4` works without root. Shizuku runs a
process as that user, so gestures could work root-free through it:

- Needs our own event reader in place of the library's `su` pipe (the library hardcodes `su`),
  which ties in with 1.9 (replace or delete the library).
- Shizuku could also stand in for other root-only actions that shell can do (`input keyevent`,
  `am force-stop`, `svc data`, ...) — see Part 2.4.
- Costs: the Shizuku dependency and its permission flow; Shizuku must be restarted after
  each reboot on unrooted phones (wireless debugging).
- Not an accessibility route: `AccessibilityServiceInfo.setMotionEventSources` (API 34)
  *consumes* the events it listens to, so the phone would stop getting touches.
- ISAs alone could go root-free without Shizuku, as invisible overlay strips like the pie's.

Other things Shizuku (the shell user) could do, more useful to a pie-only user:

- **Run the `InputContext` helper as shell instead of root.** Most root-only actions
  (next/previous app, kill app, long-press keys, Menu, arbitrary keys, immersive mode) go
  through that helper, and shell holds the permissions its calls need (`INJECT_EVENTS`,
  `REORDER_TASKS`, `FORCE_STOP_PACKAGES`). Most of the actions greyed out without root would
  then work on an unrooted phone. The biggest win.
- **Keep the accessibility service enabled.** Android drops LMT from
  `enabled_accessibility_services` when it is force-stopped (seen on the test phone) and in other
  cases (see 3.5). With `WRITE_SECURE_SETTINGS`, granted once through Shizuku or by hand
  (`adb shell pm grant com.noname81.lmt android.permission.WRITE_SECURE_SETTINGS`), LMT could put
  itself back. Small, and it doesn't even need Shizuku running.
- Grant usage access (LastApp, PieRecentApps) without the Settings round trip.

Priority: gestures low (the user only uses the pie); the helper and the accessibility
re-enable are the interesting ones.

### 1.15 Settings UX — possible next features

- **Status card at the top of Settings** when something LMT relies on is missing: accessibility
  off (Android drops it on force-stop, see 1.14), no overlay permission, no root while
  gestures are selected. Tapping it fixes or explains the problem. It would have caught the
  silent accessibility drops seen on the test phone.
- **Live previews:** show the activation strips while dragging their thickness, length and
  gravity, and a small pie preview in the radius, gap and outline sliders, so the result is
  visible before closing the dialog.

### 1.16 Feature requests from the XDA thread

Collected 2026-09-25 from the [LMT Launcher thread](https://xdaforums.com/t/app-root-lmt-launcher-v3-2.1330150/)
(XDA answers direct fetches with 403; found through web search limited to the thread).
None of these were built upstream.

- **Pie folders / sub-pies**: asked for several times ("long press opens a second pie", like
  the AOSP browser pie and the Android 4.2 camera settings pie; "folders in pie positions").
  The author answered he might add it. Done in 1.17 (mockup A: outer ring).
- **Brightness slider wrapped around the pie**. Done in 1.17.
- **Pie stays open after lifting the finger**, about 1.5 s or until the next touch outside
  the trigger areas, since Back often needs several presses in a row.
- **Icon pack support** for the slice icons.
- **Different icon sizes per ring**: smaller on the inner ring, bigger on the outer.
- **Corner pie**: a pie that opens from a screen corner.
- **App slices show the app's name or icon** instead of the generic "APP".
- **Bottom pie** with a short swipe up to open it (bottom positions exist now).
- **Keep the Back slice on screen for a short while** (same need as the delayed hide above).

### 1.17 Brightness dial and pie folders  ✅ done (verified on the test phone)

Both from 1.16. The dial came first because it fits the pie pointer's extension hook.

- **49 BrightnessDial** (`PieBrightness`, a `PieMenu.PieView`): hovering its slice arms it;
  sliding out past the pie's rim within the slice's angles takes the touch, and the angle
  sets `Settings.System.SCREEN_BRIGHTNESS` live (1..255 on a 2.2 gamma curve, adaptive
  switched off). Up is brighter for side pies, right for a bottom pie. Sliding back into the
  centre hands the touch back; lifting fires nothing. Releasing on the slice without dialing
  toggles adaptive brightness. Needs "Modify system settings"; picking the action or tapping
  the slice opens that screen. Drawn as a white arc on a dark track with a knob and a
  percentage, readable on any wallpaper.
- **Brightness dial setting** (`PieBrightnessDial`, Pie extensions): the same dial without
  spending a slice on it, like the status infos. When on, it is drawn faint just outside the
  pie every time it opens; resting the finger on it for 80% of the long press time grabs it
  (a haptic tick), then sliding along it sets the brightness. Passing over it on the way to an
  outer slice doesn't change anything. It may overlap the status infos' clock.
- **50 Folder** (mockup A, outer ring): a pie slot holds a folder of up to 8 actions
  (`PieFolder<slot>Item<i> Type/String/Icon`, name in the slot's own string, backed up with
  the rest). Picked from the action picker on pie slots only (not gestures, ISAs or inside
  another folder), which then opens `FolderActivity` to name it and pick its items.
  - The folder opens when its slice is hovered or after a long press, by the **Open pie
    folders** setting (`PieFolderOpen`). A folder set as a slice's long-press action always
    opens on the long press.
  - `PieMenu.openFolder` puts the items one ring beyond the outermost, centred on the folder
    slice (each at most the folder's sweep or 0.55 rad) and kept inside the half circle; they
    fade in. Hovering a slice on the folder's ring or an inner one closes it, slices on rings
    between are crossed on the way out and leave it open; closing the pie closes it too.
  - After the long-press timer fires, PieMenu sends every hover as 40 instead of 36, so
    both signals open and close folders.
  - **Folder icons**: the icon left of the folder name opens a searchable grid of Google's
    Material Icons (the font and its codepoints are bundled in `assets/`, Apache 2.0, about
    2,200 icons; common folder-style icons first while the search is empty). The choice is
    saved as `symbol:<name>` in the slot's Icon field, so backups carry it, and drawn white so
    the pie tints it like its own icons.
  - The blur behind the pie also covers an open folder's slices, and only those
    (`PieBlur.showFolder`): the screenshot blur is clipped to the pie's disc plus the slices'
    outlines; Samsung's live blur, which only does rounded rectangles, gets one small rounded
    blur behind each folder slice (not yet checked on the Samsung flagship). The Pie tab shows a folder slot as `Folder "Name", N actions`.

### 1.18 Info tab redesign  ✅ done (verified on the test phone)

Chosen from mockups (canvas "LMT Info Tab Proposals"): header of A, body of B, pie of C.

- Header card: the pie drawn in from the card's right edge (`InfoPieView`, theme colors), the
  app icon (`drawable-nodpi/lmt_icon.png`, redrawn as a vector from the 192 px legacy icon;
  source in `docs/art/lmt_icon.svg`), a version chip (five double-taps open the debug menu),
  "Original app by noname81" (his XDA profile) and "Revived by Psycoguana" (GitHub), and
  stacked GitHub repo (no URL yet), XDA thread and Donate to noname81 buttons.
- Status card: "LMT is ready" with the active features (pie, gestures, ISAs) on a neutral
  card with a green badge; otherwise red with the first problem and, where it can, a tap that
  fixes it (allow overlay, start the service, gestures without root, open accessibility).
  Cards and tiles that react to taps get a rounded ripple.
- Tiles for Accessibility, Draw over apps, Usage access, Root, Modify settings and Phone
  calls; an off tile opens its permission screen. Refreshed on every resume.

## Part 2 — Accessibility fallbacks for root-only actions

Researched, not implemented. API levels below are from the SDK's own
`platforms/android-36/data/api-versions.xml`, not from memory.

The plumbing already exists: `AccessibilityHandler.performAction(int)` wraps
`performGlobalAction`, `performClick` wraps `dispatchGesture`, and
`xml-v22/service_config.xml` already declares `canPerformGestures="true"`.
`performGlobalAction` needs no extra manifest flags.

### 2.1 Drop-in replacements — one `case` each in `Launcher.fireAction`  ✅ done (Screenshot confirmed on-device; Menu and drawers awaiting a finger test)

| LMT action | today | replacement | since |
|---|---|---|---|
| **34 Screenshot** | root `keyevent 1000 120` | `GLOBAL_ACTION_TAKE_SCREENSHOT` (9) | API 28 |
| **40/41/42 Nova/Apex/Holo drawer** | root launcher intents | `GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS` (14) | API 31 |
| **5 Menu** | root `keyevent 82` | `GLOBAL_ACTION_MENU` (21) | **API 36** |

`GLOBAL_ACTION_MENU` is API **36**, so it works on a Samsung flagship (SDK 36) but not on the
Test phone (SDK 35). The drawer replacement opens the *system* all-apps surface, not
Nova's own drawer — close, not identical.

Implemented as `Launcher.accessibilityFallback(actionType)` (the version gate,
unit-tested in `AccessibilityFallbackTest`) and `performAccessibilityFallback`,
which runs it only when the accessibility service is connected:

- **Screenshot** and **Menu** try accessibility first, then the unchanged root
  key event. Both paths send the same thing.
- **Nova/Apex/Holo** still prefer root, because root opens that launcher's own
  drawer. Without root they open the system all-apps screen. With neither, the
  "needs root" toast is unchanged.
- Picker captions say which Android version the accessibility path needs, e.g.
  `[Accessibility (Android 9+)|Root]`.

Not verifiable by argent: the pie, gestures and ISAs only fire on a real finger
(see 1.9), so the live check is a manual one.

### 2.2 New capabilities LMT has no action for

`LOCK_SCREEN` (8, API 28) · `DISMISS_NOTIFICATION_SHADE` (15, API 31) ·
`DPAD_UP/DOWN/LEFT/RIGHT/CENTER` (16–20, API 33) ·
`MEDIA_PLAY_PAUSE` (22, API 36).

### 2.3 De-rooting that needs no accessibility at all

- **32 TaskerTask — ✅ done: root-free, with Tasker's own task picker.**
  Confirmed by a live pie trigger with a real finger.

  `Launcher.doTaskerAction` now `sendBroadcast`s
  `net.dinglisch.android.tasker.ACTION_TASK` (via the new `TaskerIntents` helper)
  instead of shelling `am broadcast` through root; `PERMISSION_RUN_TASKS` is
  declared in the manifest. Caption retagged `[Root]` → `[None]`. Neither root nor
  the accessibility service is involved — confirmed by inspecting Tasker's real
  installed receiver rather than trusting the reference source alone.

  **A real bug caught by testing against the real receiver, not just the docs.**
  `dumpsys package net.dinglisch.android.taskerm` shows the actual registered
  filter:
  ```
  net.dinglisch.android.taskerm/.ReceiverStaticRunTasks
    Action: "net.dinglisch.android.tasker.ACTION_TASK"
    Scheme: "id"
  ```
  A bare `ACTION_TASK` broadcast with no data never matches this filter and is
  dropped before it reaches Tasker at all — the reference `TaskerIntent` source
  sets this via `setRandomData()` but nothing documents *why* it's required.
  `TaskerIntents.runTask()` now sets `data = Uri.parse("id:" + random long)` to
  match, pinned by `TaskerIntentsTest.runTaskSetsAnIdSchemeDataUri`. Found by
  installing free-trial Tasker (`tasker.joaoapps.com/releases/playstore/`, no
  Google account needed on the test device) and checking why nothing happened.

  **The second bug, and the real reason this ever needed root.** With the data
  URI fixed, a live pie trigger still did nothing. `dumpsys activity broadcasts`
  showed every one of LMT's broadcasts as
  `SKIPPED ... Background execution not allowed ... to .ReceiverStaticRunTasks`.
  Since API 26 an implicit broadcast to a manifest-declared receiver reaches a
  backgrounded app only if the Intent names that app's package. A broadcast
  issued through root isn't subject to this per-app policy at all, which is why
  upstream's `su am broadcast` worked. `runTask()` now resolves whichever Tasker
  package is installed (`net.dinglisch.android.tasker`, then the Play Store's
  `...taskerm`, the same order as `TaskerIntent.getInstalledTaskerPackage()`)
  and calls `setPackage()`. Verified in isolation: the same explicit broadcast
  from `adb shell` then fails with `Permission Denial ... requires
  PERMISSION_RUN_TASKS` instead. That is a different gate, specific to shell
  lacking the permission, which shows the background block is gone.

  **Two separate user-side gates, both required, both silent when closed:**
  Android's runtime grant of `PERMISSION_RUN_TASKS` to LMT (Settings → Apps →
  LMT → Permissions → Additional permissions), *and* LMT's entry in Tasker's
  own allowlist (Tasker → Preferences → Misc → Allow External Access → select
  LMT). Granting the first did not grant the second; the live trigger only
  worked once both were set.

  **Testing limits worth knowing.** `adb shell am broadcast` can't stand in
  for LMT, because `PERMISSION_RUN_TASKS` is `prot=dangerous` and shell never
  holds it. Synthetic swipes (argent/uiautomator) never summoned the pie at
  all, consistent with 1.9: the touch detection is a native layer, below the
  input stack those tools inject into. The live trigger needs a real finger.

  **Task picker — done.** Selecting TaskerTask opens Tasker's own "Task
  Selection" list (`ACTION_TASK_SELECT` via `startActivityForResult`). The
  result's `getDataString()` is the bare task name, which is stored as the
  action's payload. When Tasker isn't installed (`resolveActivity` finds
  nothing), it falls back to the old free-text dialog. This is the same intent
  and parsing that two maintained apps ship: HTTP Shortcuts
  (`TaskerTaskPickerContract.kt`, `intent?.dataString`) and TapTap. Both were
  found with Sourcegraph code search after web search came up empty.

  *A mistake worth recording:* the first attempt shelved the picker as broken.
  It opened, toasted "No tasks found with name", and closed at once. The cause
  was the test, not the API. The only task had just been created and still
  sat under Tasker's unapplied-changes check mark, and the picker lists only
  saved tasks. Once the task was saved, the same intent showed the list. The
  lesson is to check the fixture before blaming the API.

  There is an existing stopgap (the **Shortcut** action can target Tasker's
  "Tasker Task" shortcut — motomotomotoG, XDA, Mar 2026) but it is a workaround,
  not the destination.
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

### 2.5 Mislabelled in the picker  ✅ done (LastApp)

**14 LastApp is tagged `[Root]` but already has a working non-root path** — it
checks `isRootAvailable()` and falls back to UsageStats + `doAppAction()`. The
picker told users it needs root when it does not, and since 1.13 greys root-only
actions out, it even blocked it. Now tagged `[Root|UsageStats]`, and choosing it
without root asks for usage access, like PieRecentApps. Verified on the test phone with
Magisk denying root: Calendar → pie → last app switched back to Chrome.
NextApp/PrevApp stay root-only: cycling through tasks has no UsageStats equivalent.

More broadly: several actions have a rich pre-Lollipop non-root implementation
followed by `if (SDK_INT >= 21) { root; return; }`. That legacy code is dead on
every modern device. It was abandoned because `getRecentTasks` was locked down in
API 21 — but UsageStats, which LMT already uses for `PieRecentApps`, is the modern
replacement for exactly that.

`Action.captions[]` carries the `[Root]` / `[Accessibility]` tags shown in the
picker, so every de-rooting must update its caption or the UI will lie.

### 2.6 New actions  ✅ done (Flashlight, Keep screen on, Pop-up window)

Ids are saved in settings and backups, so new actions are appended (46, 47, 48) and
`CommandSelectActivity` adds each to its section by id instead of by range.

- **46 Flashlight** [None]: `CameraManager.setTorchMode` on the first back camera with a flash.
  A `TorchCallback`, registered when `Launcher` is created, follows the state, so a torch turned
  on from quick settings goes off on the next toggle. Verified on the test phone.
- **47 KeepScreenOn** [None]: a 1-pixel, untouchable `TYPE_APPLICATION_OVERLAY` window with
  `FLAG_KEEP_SCREEN_ON`. Toggling again, turning the screen off or stopping the service removes
  it. Verified on the test phone (`dumpsys window` shows `mHoldScreenWindow` on LMT's window, then
  `null` after screen off). Its toast is suppressed on the test phone because LMT's notifications are
  off there.
- **48 PopUpWindow** [Root|Shizuku]: relaunches the app on screen with
  `am start --windowingMode 5 -n <launcher activity>`, which moves its running task into a
  freeform window (Samsung pop-up view). The app comes from the accessibility service, which
  now also subscribes to `TYPE_WINDOW_STATE_CHANGED` at runtime and keeps the package of the
  last activity window (`AccessibilityHandler.foregroundPackage`).
  - `ActivityOptions.setLaunchWindowingMode` is blocked for apps
    (`blocked,test-api, reflection, denied`); the attempt stays for phones that allow it.
  - Root goes through `runCommandRoot` (the su shell). `runCommandRemote` (InputContext)
    starts the activity its own way and drops `--windowingMode`.
  - Without root: Shizuku (`dev.rikka.shizuku:api/provider:13.1.5`, `ShizukuShell`). The
    first use shows Shizuku's permission prompt; `newProcess` is private in 13.x and is called
    by reflection. Verified on the test phone with root denied (Chrome went freeform from uid 2000).
  - Apps with `singleInstancePerTask` (Chrome) open a second task in the pop-up; without
    bounds Samsung picks a near full-size pop-up.
  - Shizuku's libraries declare minSdk 23; `tools:overrideLibrary` keeps LMT's 21 since they
    are only reached through the pop-up action, behind `pingBinder`.

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

### 3.2 ✅ FIXED — Latent out-of-bounds in `Action.toString()` (bound changed to `<`; see accepted-deviations.json)

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

### 3.5 ✅ FIXED — Accessibility service "silently unbinds" (was upstream)

**Root cause: `onInterrupt()` treated as a disconnect.** Upstream's
`AccessibilityHandler.onInterrupt()` set `instance = null; mInitialized = false`.
But the system sends `onInterrupt()` to *every bound service* whenever anything
calls `AccessibilityManager.interrupt()`. That means "stop your current feedback",
not "you are disconnected", and any app can trigger it. After one interrupt, LMT
treated its service as gone. The service itself stayed bound, so Settings still
showed it enabled, and `onServiceConnected()` never ran again until a toggle off
and on. That matches the reported symptom *and* the reported workaround.

**Reproduced on the test phone.** A throwaway `app_process` probe called
`IAccessibilityManager.interrupt(0)` as the unprivileged shell user, with no root.
LMT's service received `onInterrupt` in its own process, and `dumpsys
accessibility` still listed it under Bound and Enabled services before and after.

**Fix:** `onInterrupt()` now only logs (`LMT::Accessibility: onInterrupt -
ignored`). A mirror-image bug surfaced while writing the test: nothing cleared
the state on a *real* disconnect, so LMT kept calling into a dead service. A new
`onUnbind()` does that now. 4 tests in `AccessibilityHandlerTest`. The interrupt
test and the unbind test both failed against the old code before the fix.

To see how often interrupts actually happen on a device: `adb logcat | grep
"LMT::Accessibility"`.

Original report, kept for context:

Distinct from 3.1 and reported repeatedly on XDA: Android drops LMT's accessibility
binding while still *showing* it as enabled. motomotomotoG, Mar 2026 — "Android
disables Accessibility for the app ... Android will show that the app has
accessibility enabled, but it's not actually", fixed by toggling it off and on.
v3.11 was released to address a suspected cause and the reporter has not seen it
since, but at least one user still reported it afterwards. This matters more now,
because 3.1 means accessibility is the *only* working path for Home/Back/Recents —
when it unbinds, those actions die with no root fallback.

### 3.6 ✅ FIXED — `compare_fingerprints.py`'s `issues` counter was dead, a second silent oracle blind spot

**Fix (before the UI rewrite):** the dead counter is gone. Unexcused one-sided
methods are now collected, reported as `one-sided methods (REAL)`, and fail
`RESULT`. The 1.4 gap is backfilled (`SettingsValuesBase.smali` in
`new_members`). Mutation-checked: a stray method added to `Toaster` now gives
`REVIEW REQUIRED`.

**Also prepared for the rewrite:** whole-class additions and removals are now
declared in `accepted-deviations.json` (`new_classes` / `removed_classes`, each
with a reason) instead of a hard-coded Python set. Removing a class used to fail
with no way to excuse it. Checked by deleting the dead `SettingsViewActivity`: the
oracle failed until the removal was declared, then passed.

Original finding:

Found while adding 1.5: `SettingsValuesBase.smali` has printed
`!! method on one side only: loadShowSystemApps()I` /
`saveShowSystemApps(I)V` since ROADMAP 1.4 added the `ShowSystemApps` toggle —
and the run still reported `RESULT: EQUIVALENT`. The `issues` variable that
print statement increments (`tools/compare_fingerprints.py`, search `issues +=`)
is never read; `RESULT` is computed from `unexplained`/`missing`/`added`/
`hard_field_diffs` alone. So **any method that lands on the "one side only,
not excused by NEW_MEMBERS/REWORKED/synthetic/anon-shape" path prints a
warning but does not fail the build.** `SettingsValuesBase.smali` was never
added to a `new_members` entry when 1.4 landed — a gap distinct from the
documented arithmetic blind spot in 1.3 (see there for the print-vs-fail
distinction that one describes). Not fixed here to keep this change scoped;
either wire `issues` into `ok` in `compare_fingerprints.py`, or backfill the
missing `new_members` entry for `SettingsValuesBase.smali` — check the two
outstanding warnings are still there before doing either.

---

### 3.7 ✅ FIXED — Auto configuration computes a touchscreen factor of 0 (upstream, gestures/ISA mode only)

`SettingsViewHelper`'s Auto configuration row computes
`Math.round((maxFromGetevent / screenWidth) * 100.0f)` with two `int`s, so the
division truncates *before* the scaling. The common case is a touchscreen whose max
coordinate is `width − 1`, and that yields `0 / … * 100 = 0`. The `<= -1` guard
doesn't catch it, and a factor of 0 then divides `getIsaAction`'s coordinates to
infinity. The fix is a float division; the same applies to Y. Found while writing
`SettingsScreenUiTest`. Fixed by casting the divisor to `float`
(`SettingsViewFragment`, both axes). No oracle entry needed: the code lives in a
row lambda, and the `SettingsViewFragment$ANON` bucket is already accepted as new.
Not exercised on-device (needs root plus gestures/ISA mode).

### 3.8 ✅ FIXED — Chrome "This site can't ask for your permission" while LMT runs (upstream)

With the pie on, allowing a site's camera, microphone or location in Chrome failed with "Close
any bubbles or overlays from other apps. Then try again", and worked with LMT stopped.

- The pie's trigger strips are `TYPE_APPLICATION_OVERLAY` windows. Any touch over an app
  while such a window is on screen reaches the app flagged
  `FLAG_WINDOW_IS_PARTIALLY_OBSCURED` (`InputDispatcher: partially obscured by ... com.no...`
  in logcat), and Chrome's prompt refuses those touches (tapjacking protection).
- Fix: when LMT's accessibility service is connected, the strips are added through the
  service as `TYPE_ACCESSIBILITY_OVERLAY`, which the input system treats as trusted. The pie
  and blur work the same. `TouchService.accessibilityChanged` re-adds them as the other type
  when accessibility connects or goes away, so the pie never disappears. Keep screen on's
  1-pixel window does the same.
- Without the accessibility service it can't be avoided; workaround: tap "Block", then
  allow the permission from the site's settings (lock icon).
- Verified on the test phone: location and microphone prompts accept taps with the pie running;
  `dumpsys window` shows `ty=ACCESSIBILITY_OVERLAY`, and `APPLICATION_OVERLAY` again with
  accessibility off.
- Side effect, fixed: accessibility overlays also show above the lock screen, so the pie
  opened there with "pie on lock screen" off. Now, in that case, a screen off/on and
  user-present receiver makes the strips `FLAG_NOT_TOUCHABLE` while the keyguard is up, and
  touchable again once unlocked (verified on the test phone: `fl` gains 0x10 when locked).

### 3.9 🟡 HARDENED — pie shows and hides at once until the service restarts (upstream)

Reported on the Samsung flagship (also in the original app), not reproducible on demand. Likely causes found
by reading the code, all fixed defensively:

- The pie's extensions were wrapped up (`PieMenu.onShrink`: the pointer's tap and reset) only
  from `onSizeChanged` after the shrink. A tap that ends before the window has grown changes no
  size, so that state stayed until a restart. `shrinkLayout` now calls `onShrink` itself.
- An active pie pointer stops the slices from drawing; it is now reset whenever the pie opens.
- `ACTION_CANCEL` (a gesture the system takes) hid the pie but left the window full screen; it
  now shrinks like `ACTION_UP`.
- A new instance of the accessibility service takes its overlays away; the strips are re-added
  whenever the instance changes (`PieContainer.addedThrough`).
- Failed window updates are logged (`LMT::PieContainer`), so `adb logcat` shows what happened
  the next time.

---

## Part 4 — Reconstruction cleanup still outstanding

- ~~Decompiler-generated names (`int i*`, `String str*`, …) and `JADX INFO`
  comments~~ — **done** in the readability pass (1.3); both counts are now 0.
- ~~**minSdk 15 → 21**, deleting the now-dead `SDK_INT < 21` branches~~ — **done**
  (minSdk raised in 1.6; the branches went in the 1.13 cleanup). Removed methods and
  fields are listed one by one under `removed_members` in `tools/accepted-deviations.json`,
  a list `compare_fingerprints.py` now reads, so the rest of each class is still compared.
