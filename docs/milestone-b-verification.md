# Milestone B — regenerated Java verified against patched smali

Source: `apktool b reference/apktool` → `build-out/patched-source.apk` →
`jadx --show-bad-code --no-res`. Output in `generated/jadx/sources/com/noname81/lmt/`.

## Patches confirmed present (they are absent from the repo's own jadx tree)

| Patch | Evidence in regenerated Java |
|---|---|
| Android 14/16 root fix | `RootContext.java:96` — `if (Build.VERSION.SDK_INT >= 60)`, so `InputContext34.jar` is permanently dead and SDK ≥ 29 uses `InputContext29.jar` |
| Ext-storage toast fix | `PermissionChecker` — `if (Build.VERSION.SDK_INT >= 33 \|\| hasExternalStorageReadPermission(...))` |
| Third pie ring | `PieFragment` — `R.string.pie_level_3_item_*`; `PieControl` — a third `makeItem(..., 3)` loop |
| Tab reorder | `LMT.createTabs()` — `InfoFragment` added after `PieFragment` |

## Decompile quality

jadx 1.5.6 with `--show-bad-code` produced **no hard failures** — both methods that
failed in the repo's checked-in tree came back as real Java:

- `Action.toString()` — verified line-by-line against
  `Action.smali`. Identical semantics; the "Code duplicated" warning was only jadx
  emitting the `"none"` assignment in two branches instead of sharing a label.
- `PieMenu.init(Context)` — 236 lines recovered. jadx warned
  "Can't fix incorrect switch cases order", so the desugared string-switch was
  decoded from smali by hand and checked:
  - `pswitch_data_0` is a packed-switch from `0x30` (`'0'`), so string `"N"` → index `N`.
  - `pswitch_data_1` maps index `N` → `pswitch_(15-N)`.
  - Endpoints spot-checked: index 0 → `pswitch_f` (`SDK_INT` vs `0x1f`, the
    `system_accent*` resource ids) and index 7 → `pswitch_8`
    (`argb(0x7f,0,0,0)`, `argb(0x40,0xff,0,0)`, `argb(0xc8,0xff,0,0)`).
  - Both match the generated Java exactly. **The reconstruction is correct.**

The other 9 warnings are all "Can't rename method to resolve collision" on
anonymous-class bridge methods — cosmetic, no logic involved.

## Latent bug noted (preserved, not fixed)

`Action.toString()` bounds-checks with `mType <= names.length`, so `mType ==
names.length` would throw `ArrayIndexOutOfBoundsException`. Faithfully preserved
for now; candidate fix once equivalence is established.
