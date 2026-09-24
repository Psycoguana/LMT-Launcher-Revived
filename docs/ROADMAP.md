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

### 1.10 Theme customization

Depends on 1.6 (Material 3 overhaul) being in place — needs an `AppCompatActivity` /
`DayNight` theme to attach a dynamic color scheme to. Not started; added to the
backlog 2026-09-23, requested by the user during 1.6 phase 1.

- **Light / dark / follow system**, the three standard `DayNight` modes, switched via
  `AppCompatDelegate.setDefaultNightMode`.
- **A small preset palette** to pick from (a handful of curated Material seed colors),
  plus a **custom RGB/hex picker** for anything else.
- **Material You dynamic color** (Android 12+, `DynamicColors` from the Material
  library) — derives the theme from the user's wallpaper. Needs a fallback (one of
  the presets above) for API < 31 or when the user opts out.
- Persist the choice through `SettingsSharedPrefsWrapper` like every other setting,
  and expose it from the rebuilt Settings screen (1.6 phase 2) rather than the old
  position-dispatch system.

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

### 3.7 🟡 Auto configuration computes a touchscreen factor of 0 (upstream, gestures/ISA mode only)

`SettingsViewHelper`'s Auto configuration row computes
`Math.round((maxFromGetevent / screenWidth) * 100.0f)` with two `int`s, so the
division truncates *before* the scaling. The common case is a touchscreen whose max
coordinate is `width − 1`, and that yields `0 / … * 100 = 0`. The `<= -1` guard
doesn't catch it, and a factor of 0 then divides `getIsaAction`'s coordinates to
infinity. The fix is a float division; the same applies to Y. Found while writing
`SettingsScreenUiTest`, not fixed yet: it needs root to exercise, and pie-only
users (the default) never reach it.

---

## Part 4 — Reconstruction cleanup still outstanding

- ~~Decompiler-generated names (`int i*`, `String str*`, …) and `JADX INFO`
  comments~~ — **done** in the readability pass (1.3); both counts are now 0.
- **minSdk 15 → 21**, deleting the now-dead `SDK_INT < 21` branches. This changes
  bytecode by design, so run the oracle in "explain every diff" mode for that
  commit rather than expecting `EQUIVALENT`.
