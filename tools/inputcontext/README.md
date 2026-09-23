# InputContext — the root helper

`app/src/main/assets/InputContext{20,21,24,28,29}.jar` are prebuilt dex jars that
`RootContext` copies out and runs under `su` via `app_process`. They are the last
binary blob in the project besides `libTouchServiceNative.so`, and they contain the
bug that breaks every root key action (ROADMAP 3.1).

## What is here

- `decompiled/InputContext<sdk>.java` — all five variants, decompiled with
  `jadx --show-bad-code`. **All five decompile fully; no failures.** ~515 lines each,
  a single class.
- `probe/` — a standalone experiment that answers "which injection route works on
  this Android version?" without touching the app. `probe/build-and-run.sh` builds
  and runs it on the test device.

## Why there are five variants

They are not five different programs. They differ *only* where a hidden framework
API changed **signature**, which cannot be handled by a runtime `SDK_INT` branch
inside one compilation unit — hence five separate compilations:

| Boundary | What changed |
|---|---|
| 20 → 21 | `startActivity` takes `ProfilerInfo` instead of `IThumbnailReceiver`/`ParcelFileDescriptor`; `RecentTaskInfo.origActivity` → `.baseActivity` |
| 21 → 24 | `getRecentTasks(...)` returns a sliced list, needs `.getList()` |
| 24 → 28 | `ActivityManagerNative.getDefault()` → `ActivityManager.getService()` |
| 28 → 29 | `moveTaskToFront` gained two leading parameters |

**This matters**: if those calls go through *reflection* instead, signature
resolution moves to runtime and **all five collapse into one source file** with
ordinary `SDK_INT` branches. That also removes the need for a framework stub jar,
because reflection compiles against the public SDK.

## The bug, and the verified fix

`injectKeyEvent` and `injectPointerEvent` are one line each:

```java
InputManager.getInstance().injectInputEvent(event, 2);
```

`InputManager.getInstance()` NPEs inside `app_process` on modern Android, so every
`input keyevent` / `tap` / `text` command is a silent no-op.

Measured on the test phone (Android 15 / SDK 35), root granted — and confirmed by the
system volume actually moving, not merely by the absence of an exception:

| Route | Result |
|---|---|
| `InputManager.getInstance()` (current) | **FAIL** — `NullPointerException` |
| `InputManagerGlobal.getInstance()` (API 34+) | **OK** |
| `IInputManager` via `ServiceManager.getService("input")` | **OK** |

A fix should try `InputManagerGlobal` first and fall back to `InputManager` on
older releases, both reflectively. The `ServiceManager` route also works and is
older API, so it is a reasonable second fallback.

## Note on coverage

The equivalence oracle compares the APK's `classes.dex` only. These jars are
**assets** — the oracle does not see them at all. Anything done here needs its own
verification; the probe is the pattern to follow.
