# Camera — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

Target: **Windows Phone 8.1 Microsoft Camera** on a portrait phone (768×1280 / xhdpi reference profile). UI also supports landscape viewfinder chrome (controls stay on screen edges).

Engine: Open Camera `CameraController` + `Preview` (GPL-3.0-or-later fork). Metro owns all chrome.

## App shell

- **Navigation model:** Single full-bleed viewfinder. More… opens an edge settings panel (not a Material sheet). Photo settings / video settings are list pages. Burst review is an in-app drill-in after a burst.
- **Theme:** Always dark chrome over the live preview (`#000000` letterbox). Icons and labels are white outline Metro style. Accent color applies to selected mode ring and active toggles only.
- **Typography:** Noto Sans stand-in for Segoe WP. Transient status like `flash off` is page-title scale (64sp light), lowercase. Always planted just after the ISO/lens controls row (layout top-start); glyphs rotate in place on tilt — position does not jump.
- **No Material:** No FAB, snackbars, bottom sheets, rounded elevated cards, or Material camera controls.

## Pages

### Page 1 — Viewfinder (landing)

- **Layout:**
  - Full-bleed camera preview (letterboxed black if aspect ≠ screen).
  - **Left column** (up to 5 customizable quick settings, circular white outline icons, ~44dp touch): default set = Flash, Camera switch (front/rear), Lenses, ISO, White balance. Top-left may show last-capture thumbnail (Camera Roll shortcut).
  - **Right column:** vertically stacked circular mode buttons — Photo (camera), Burst (overlapping frames), Video (camcorder). **Selected mode** uses a larger ring / filled treatment. Ellipsis `…` at top-right opens More.
  - Just after the ISO/lens row: brief toast-like status text when a setting changes (`flash off`, `iso 400`, etc.) — no Material Snackbar. Fixed layout anchor under that row; glyphs rotate in place on tilt.
- **Interactions:**
  - Tap shutter (selected mode primary action on the right, or dedicated shutter when photo mode is selected) → capture.
  - Tap burst mode → switch mode; shutter then fires rapid multi-shot.
  - Tap video mode → switch mode; shutter starts/stops recording (recording indicator on accent).
  - Tap flash / ISO / WB → cycle or open a compact option strip; do not open Material menus.
  - Tap camera switch → flip front/rear.
  - Tap Lenses → list stub (“No lenses installed”) — WP8.1 Lenses marketplace is out of scope for v1.
  - Tap last thumbnail → open `com.metro.photos` (or system viewer) for the URI.
  - Tap-to-focus on preview; pinch may zoom when hardware supports it.
- **Background:** Live preview; black elsewhere.
- **Reference:** `images/viewfinder_shortcuts_dark.png`, `images/viewfinder_engadget_dark.jpg`

### Page 2 — More / viewfinder customization

- **Layout:** Black panel covering the trailing ~55–60% of the screen; leading edge still shows a sliver of live preview with the left quick-settings column. Header ellipsis. Primary links: `photo settings…`, `video settings…`. Section label `Show these settings in the viewfinder` (secondary/gray). Row of up to 5 tappable slots that assign which shortcuts appear on Page 1.
- **Navigation:** Back / tap preview sliver dismisses panel → Page 1.
- **Reference:** `images/photo_settings_dark.jpg`

### Page 3 — Photo settings

- **Layout:** Standard Metro list page on black: ISO, exposure value, white balance, aspect ratio / resolution, scene, focus assist light, burst retention (“Delete unsaved bursts after N days”).
- **Navigation:** Back → Page 2 or 1.
- **Reference:** `images/photo_settings_dark.jpg` (entry), blueprint for list density.

### Page 4 — Video settings

- **Layout:** Metro list: video resolution/quality, continuous focus, recording light / torch, white balance.
- **Navigation:** Back → Page 2 or 1.

### Page 5 — Burst review (post-capture)

- **Layout:** Black chrome. Large selected frame; horizontal thumbnail strip of burst frames; caption `Deleting unsaved photos in 7 days` (or user preference). App bar: save (disk), delete / more `…`.
- **Navigation:** Save keeps selected frames to Camera Roll; Back discards unsaved per retention policy.
- **Reference:** `images/burst_mode_dark.png`

## Images

| Image | Page | Notes |
|-------|------|-------|
| `viewfinder_shortcuts_dark.png` | 1 | Official Windows Experience Blog callouts — left shortcuts + right photo/burst/video |
| `viewfinder_engadget_dark.jpg` | 1 | Landscape viewfinder, flash-off status text |
| `photo_settings_dark.jpg` | 2–3 | More panel + “Show these settings in the viewfinder” |
| `burst_mode_dark.png` | 5 | Burst review strip + retention caption |

## Out of scope (v1)

- Third-party WP8.1 Lenses marketplace / Nokia Cinemagraph / Bing Vision
- Cortana camera integration
- RAW / Pro Cam exclusive Lumia PureView UI
- Open Camera histogram, zebra stripes, focus peaking overlays
EOF