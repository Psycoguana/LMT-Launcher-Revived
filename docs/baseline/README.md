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
- Accessibility service — was already enabled.
