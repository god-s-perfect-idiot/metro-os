# Widgets — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Pages

### Page 1 — Widget catalog (Start-style grid)

Homescreen widget picker surface. Opens immediately on launch — no pivots, no panorama.

- **Background:** black (`#000000`), same as Start
- **Title:** `MetroAppTitle` overline `widgets` (ALL CAPS via toolkit) — same app-title chrome as Settings / Files; flush-left 12dp start inset only. No large hub/page title under it (Start itself has no page title; this app keeps the suite overline only)
- **Grid:** **4 columns only** (never 6 / “show more columns”). Gap **8dp**, side gutters **12dp** — match launcher Start density (`TILE_GRID_GAP` / `TileChrome.Standard`)
- **Tile footprints** (grid units): **1×1**, **2×2**, or **2×4**
  - **2×4** means **2 rows × 4 columns** (Start wide / `4×2` in launcher storage naming)
- **Catalog tiles (v1):**

  | Widget | Default size | Face content |
  |--------|--------------|--------------|
  | Time | 2×4 (wide) | Centered 12-hour digital clock: small AM/PM left of hour, large Thin digits, square colon dots. No weather / location / temperature. Accent fill; no bottom title |
  | Battery | 1×1 | Vertical battery glyph (fill from bottom) + digits only (no `%`). Charge level only — not a saver toggle. Accent fill; no bottom title |
  | Notifier | 2×2 | Cycles tray notification peeks only (600ms flip, ~5s hold) — no static `notifier` front face. Peek = title/body + app-name footer + count badge. Idle copy when access denied / tray empty. Requires notification-listener access. Accent fill |
  | Analog clock | 1×1 | Flat analog dial: 12 thick hour bars + thin minute ticks, rectangular hour/minute hands (no numerals, no second hand). Accent fill; no bottom title |
  | Torch | 1×1 | Flashlight glyph (`ic_widget_torch`); tap toggles camera LED. Accent fill when off, white fill + accent glyph when on. Dimmed when flash unavailable. No bottom title |
  | Lock | 1×1 | Suite padlock glyph (`MetroAppGlyphs.Lockscreen`). Tap locks the device via Metro lockscreen accessibility (`GLOBAL_ACTION_LOCK_SCREEN`). Opens Accessibility settings when the service is not enabled. Accent fill; no bottom title |

- **Tile chrome:** Square 0dp corners; no bottom titles on current faces. Content color from `MetroColors.tileContentColor` (Torch on-state uses white fill + accent content)
- **Layout packing (v1 fixed):** Time at (0,0) 2×4; Battery at (0,2) 1×1; Notifier at (1,2) 2×2; Analog clock at (3,2) 1×1; Torch at (0,3) 1×1; Lock at (3,3) 1×1
- **Interactions:** Long-press any catalog tile pins it to Start (secondary tile at the catalog footprint). Tap: Notifier (opens the notifying app for the visible peek; opens notification-listener settings when access denied), Torch (toggles LED; requests `CAMERA` when needed), Lock (locks device or opens Accessibility settings). Display-only otherwise.
- **Live updates:** Time (digital + analog) ticks every minute; Battery from `ACTION_BATTERY_CHANGED`; Notifier from `NotificationListenerService` (hold ~5s, flip 600ms — same as Start); Torch from `CameraManager` torch callbacks

## Images

| Image | Page | Notes |
|-------|------|-------|
| `time_wide_dark_cobalt.png` | Time face | Wide digital clock reference (ignore weather / location / temp in capture) |
| `battery_1x1_dark_crimson.png` | Battery face | 1×1 vertical battery + digits |
| `analog_clock_1x1_dark_crimson.png` | Analog clock face | 1×1 flat dial with hour bars + minute ticks + hands |
| `torch_1x1_dark_crimson.png` | Torch face | 1×1 flashlight glyph — see `known-gaps.md` until captured |
| `lock_1x1_dark_crimson.png` | Lock face | 1×1 padlock glyph — see `known-gaps.md` until captured |
| `catalog_dark_cyan.png` | Widget catalog | Full catalog capture — see `known-gaps.md` until captured |

## Research notes (WP8.1)

### Time

No first-party “Time” Start live tile shipped with WP8.1 (tray clock only). Third-party apps (TimeMe Tile, Clock Hub) pinned large digital clock tiles — typically **medium (2×2)** or **wide (2×4)**. Catalog uses **2×4** digital face as the default.

### Battery

Charge-level live face (vertical glyph + digits). Inspired by the WP8.1 Battery Saver app’s percentage live tile (build **8.1.14203+**); this catalog face does not enable saver mode.

### Analog clock

Metro-os catalog widget (not a stock WP8.1 system tile). Flat minimal dial matching the suite accent-live-tile language: hour bars, minute ticks, two hands, no numerals.

### Torch

No first-party pinnable Torch Start tile on WP8.1 (Action Center had a flashlight quick action; third-party lamp apps pinned custom tiles). Catalog 1×1 toggles the rear LED via `CameraManager.setTorchMode`, matching the lamp-tile role of Flashlight XT / QuickLight-style apps.

### Notifier

Metro-os catalog widget (not a stock WP8.1 app). Mirrors Start live-tile **notification peeks**: when multiple notifications are active, the tile flips through each peek one by one (600ms turnstile, ~5s hold). Front face shows a naked count badge like Start; back face shows title/body with the source app name as footer.

### Lock

Metro-os catalog widget (not a stock WP8.1 system tile). Quick lock action using the suite padlock glyph; device lock is performed by the Metro lockscreen accessibility service (`MetroLockscreen.ACTION_LOCK_NOW`).

## Out of scope (v1)

- Catalog live faces on Start via App Widget host (prefer `MetroTileWidgetFace` + launcher render)
- Battery Saver catalog tile (deferred)
- Resize cycle, edit mode, reorder in the catalog
- Additional Sense / third-party widget faces
- 6-column density
- Opening a notification deep-link / content intent from a Notifier peek tap (v1 launches the app)
