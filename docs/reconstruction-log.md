# Reconstruction log

What was changed to turn the decompiled output into compiling Java, and why each
change is behaviour-preserving. Every item was checked against
`reference/apktool/smali/com/noname81/lmt`.

## Constants jadx invented

jadx replaces a plain integer with any library constant that happens to share its
value. All were verified against the smali before substitution:

| jadx emitted | real value | smali evidence |
|---|---|---|
| `MotionEventCompat.AXIS_*` (×18, in `Launcher.fireAction`'s switch) | the case labels `17`, `18`, `21`… | jadx's own `/* 17 */` comments |
| `PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED` | `1024 * 1024` (bytes→MB) | `const-wide/32 v4, 0x100000` |
| `ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION` (×6) | `200` | `const/16 v0, 0xc8` |
| `ActivityChooserView…MAX_ACTIVITY_COUNT_UNLIMITED` (×2) | `Integer.MAX_VALUE` | `const v0, 0x7fffffff` |
| `ViewCompat.MEASURED_STATE_MASK` | `0xFF000000` | `const/high16 v3, -0x1000000` |
| `GravityCompat.START` | `Gravity.START` | `const v8, 0x800003` |

## Decompiler artifacts that would not compile

| File | Problem | Fix |
|---|---|---|
| `PieMenu.init()` | two stray `break;` after an if/else in which every branch already breaks | removed |
| `InputDialog` | `int i;` placed before `super(context)` | declaration moved after the `super` call |
| `SettingsValues` | raw `TreeMap`, so the enhanced-for yielded `Object` | typed `TreeMap<Long, UsageStats>` |
| `SettingsViewHelper` | `iRound`/`iRound2`/`i2` hoisted to blank finals but assigned repeatedly inside a loop, while also being captured by a listener | mutable working locals, snapshotted into finals for capture |
| `RootContext` ×2 | `initRemote()`/`initRoot()` declare `throws Exception` and were called from a catch block | wrapped in the nested try-with-empty-catch the smali shows |
| `PiePointer.onShrink` | desugared lambda referencing its synthetic `f$0` field | plain anonymous `Runnable` calling `sendTap()` |
| `PieFragment` | a named local class rendered as an anonymous class taking a constructor argument | rewritten as an anonymous class over a captured local |

### One genuine decompiler bug

In `PieFragment.CommandSimpleAdapter.getView`, jadx emitted
`new View.OnClickListener(this.mOffset + i) { { this.mPosition = i; } }` — the
initializer assigns `i`, but the constructor argument is `mOffset + i`. The smali
constructor is unambiguous:

```smali
iput p2, p0, ...$1OnClickListenerWithPosition;->mPosition:I
```

`mPosition` takes the **argument**. Following jadx here would have made every pie
icon past the first ring edit the wrong slot.

## Deliberate changes

- `ContextCompat`/`ActivityCompat` → framework `checkSelfPermission` /
  `requestPermissions`. Both call sites are already `SDK_INT >= 23`-guarded, so the
  compat wrappers added nothing. Removing them removes the last real support-lib
  dependency besides ViewPager.
- The two D8 desugaring classes (`…$$ExternalSyntheticBackport4`,
  `…$$ExternalSyntheticApiModelOutline0`) inlined back to `Objects.equals` and plain
  casts across their 13 call sites, then deleted.
- `android.support.v13.app.FragmentStatePagerAdapter` reproduced as
  `FrameworkFragmentStatePagerAdapter` rather than adding `androidx.legacy` for one
  class.
- Access modifiers jadx widened were restored from its own
  `Access modifiers changed from:` annotations — truthful signatures, and it brings
  back the accessor pattern the reference dex uses.
- Whole tree formatted with `google-java-format --aosp`, which also unwrapped
  jadx's very long single-line statement chains.

## Resource filtering

The APK's `res/` is the merged app + support-library resource table (2,053 entries).
Only the app's own are kept (348), computed as the transitive closure of:

- every `R.<type>.<name>` in the 41 Java files
- every `@type/name` in the manifest and the 9 app layouts
- the `AppTheme` → `AppBaseTheme` chain
- **drawables resolved dynamically at runtime** via `getIdentifier()` — from
  `Action.names[]` (plus `_kitkat`/`_pixel` nav-button variants),
  `TouchServiceResult.names[]` (plus `_s`), and `pielp_s`/`isa_s`/`ok`

That last group is the one a naive filter drops: nothing references those icons
symbolically. Verified afterwards — all 89 dynamically-resolved drawables the
original shipped are present, and **zero** non-library drawables were lost.

All ~100 `values-<locale>/` directories were dropped: they are support-library
translations only, and the app has no translated strings of its own.
