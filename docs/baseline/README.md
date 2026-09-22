# Baseline behaviour — upstream patched smali build (v3.10)

Captured 2026-09-22 on the rooted rooted test phone (Android 15 / SDK 35),
from `build-out/baseline.apk` (`tools/build-baseline.sh`, our own signing key).

Every reconstruction step is compared against this.

## Confirmed working

- App launches; tab row reads `SETTINGS | PIE | INFO` — the tab-reorder patch is live.
  (`docs/baseline/01-settings-tabs.png`)
- `ACTIVATE LMT` starts the service; "LMT started" toast appears.
- Pie opens on an edge swipe-in and renders **three concentric rings** — the
  third-ring patch is live. (`docs/baseline/02-pie-three-rings.png`)
- No "grant external storage read permission" toast on launch — that patch is live.
- `RootContext` reaches `su`: Magisk showed a superuser request for
  `com.noname81.lmt`, which is the fix from CHANGELOG §"Root prompt never
  appearing". Before that patch the app threw `FileNotFoundException` in
  `copyRemote()` and never spawned `su` at all.

## Known gap

Root was **denied** during capture (the Magisk dialog's grant button is disabled
briefly for apps that draw overlays, and the countdown auto-denied). Magisk has
persisted `policy=1` for LMT's uid, so it no longer re-prompts. Re-granting has to
go through the Magisk app's Superuser tab before root actions can be exercised in
Milestone D. This does not affect Milestones A–C.

## Permissions granted on the device

- "Appear on top" (SYSTEM_ALERT_WINDOW) — enabled manually; required for the pie.
- Accessibility service — **correction:** an earlier draft recorded this as already
  enabled, but that reading came from the Samsung flagship, not the test phone. On the test phone accessibility
  was off (`accessibility_enabled=0`, `enabled_accessibility_services=null`) and was
  enabled later, during Milestone D verification.

---

# Milestone D — on-device verification of the reconstructed build

Run 2026-09-22 on the test phone, installing `app-release.apk` over the baseline.

| Check | Result |
|---|---|
| Installs over the baseline with `-r`, no uninstall | versionCode 4000 → 4100, `Success` |
| App launches, tab row | `SETTINGS \| PIE \| INFO` — tab-reorder patch intact |
| Settings survive the upgrade | autostart still on |
| `TouchService` starts | "LMT started"; `ServiceRecord` present |
| Pie renders from an edge swipe | three concentric rings, geometry and colours identical to baseline — **validates the hand-reconstructed `PieMenu.init()`** |
| Pie dismisses on release | yes |
| Pie config UI | LEVEL 1 (5 items), LEVEL 2, **LEVEL 3 (7 items + longpress)** — third-ring patch intact in the UI, all `pie_level_3_*` strings resolve |
| Action picker (`CommandSelectActivity`) | full catalogue renders **with icons** — confirms the dynamically-resolved drawables survived resource filtering |
| Writing a setting | Pie item 1 → BACK; persisted to disk as `PieItem0 Type = 7` |
| Accessibility dispatch | `Fire action type: Back` with no `RootContext` error afterwards, i.e. it took `GLOBAL_ACTION_BACK`, not root |
| Crashes | none (`AndroidRuntime`/`FATAL` absent for the package) |

## Not verified

- **Root-backed actions.** Magisk still has `policy=1` (deny) for LMT's uid from the
  baseline session, so `RootContext` cannot initialise. Triggering a root-only action
  (`KillAllApps`) logs `Failed to write remote command!` and a `System.err` stack
  trace, then continues — which is the correct no-root behaviour, and exercises the
  patched nested try/catch at `RootContext.java:236`, but it is not a test of the
  root path working. Clearing the deny in the Magisk app's Superuser tab is needed
  first.
- **The pie over the Settings app.** It does not appear there. This is Android's
  tapjacking protection hiding `SYSTEM_ALERT_WINDOW` overlays over Settings, not an
  LMT regression — the same build renders the pie normally over the launcher.
- **SDK 36.** The Samsung flagship was only intermittently reachable over wireless adb and was
  never used for the A/B.
