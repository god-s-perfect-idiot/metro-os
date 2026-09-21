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
  | Battery | 1×1 | Vertical battery glyph (fill from bottom) + digits only (no `%`). Accent fill; no bottom title |
  | Notifier | 2×2 | Cycles tray notification peeks only (600ms flip, ~5s hold) — no static `notifier` front face. Peek = title/body + app-name footer + count badge. Idle copy when access denied / tray empty. Requires notification-listener access. Accent fill |
  | Storage Sense | 2×4 (wide) | Phone free / used storage in GB (and SD row when secondary volume exists). Accent fill |

- **Tile chrome:** Square 0dp corners; bottom-start title only when `showTitle` (Storage Sense). Content color from `MetroColors.tileContentColor`
- **Layout packing (v1 fixed):** Time at (0,0) 2×4; Battery at (0,2) 1×1; Notifier at (1,2) 2×2; Storage Sense at (0,4) 2×4
- **Interactions (v1):** Tiles are display-only except Notifier taps open notification-listener settings when access is denied. No pin-to-Start yet
- **Live updates:** Time ticks every minute; Battery from `ACTION_BATTERY_CHANGED`; Storage from `StatFs`; Notifier from `NotificationListenerService` (hold ~5s, flip 600ms — same as Start)

## Images

| Image | Page | Notes |
|-------|------|-------|
| `time_wide_dark_cobalt.png` | Time face | Wide digital clock reference (ignore weather / location / temp in capture) |
| `battery_1x1_dark_crimson.png` | Battery face | 1×1 vertical battery + digits |
| `catalog_dark_cyan.png` | Widget catalog | Full catalog capture — see `known-gaps.md` until captured |

## Research notes (WP8.1)

### Time

No first-party “Time” Start live tile shipped with WP8.1 (tray clock only). Third-party apps (TimeMe Tile, Clock Hub) pinned large digital clock tiles — typically **medium (2×2)** or **wide (2×4)**. Catalog uses **2×2** digital face as the default.

### Battery (Battery Saver)

Official **Battery Saver** app tile in WP8.1: pinable from the app list; live face shows **current charge as a percentage** with a **dynamic battery glyph**; when Battery Saver is engaged (typically ≤20%) a **shield** affordance appears. Real-time tile refresh required OS build **8.1.14203+**. Supports small and medium pins; catalog defaults to **2×2**.

### Storage Sense

Official **Storage Sense** settings app tile in WP8.1: pinable; live face shows **phone (and SD) space usage as numbers** (used / free). Catalog defaults to **2×4** so phone + optional SD rows fit Start-wide proportions.

### Notifier

Metro-os catalog widget (not a stock WP8.1 app). Mirrors Start live-tile **notification peeks**: when multiple notifications are active, the tile flips through each peek one by one (600ms turnstile, ~5s hold). Front face shows a naked count badge like Start; back face shows title/body with the source app name as footer.

## Out of scope (v1)

- Pinning widgets to the launcher Start grid / Android App Widget host
- Resize cycle, edit mode, reorder
- Additional Sense / third-party widget faces
- 6-column density
- Opening a specific notification from a Notifier peek tap
