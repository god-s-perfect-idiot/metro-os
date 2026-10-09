# Statusbar — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Pages

### Page 1 — Collapsed tray

- Layout: WP **32dp** content band across the top; **rightmost layout icon** (usually clock) trailing-aligned, plus any **spacers after that icon** (with their group padding); every other group hidden
- Coverage: the overlay window is sized to the **full system status-bar inset height** (status bar / notch / hole-punch), so no part of the Android bar peeks through below the WP band. Content is vertically centered within that height; the WP band is never shorter than 32dp.
- Background: opaque theme color (or translucent/hidden per app request)
- Interactions: tap anywhere on tray, or going home / Start, expands the indicator row; **swipe down** opens the Android notification shade and hides the Metro tray while that shade is open

### Page 2 — Expanded tray

- Layout: same height; all enabled layout groups **freely justified** across the tray (`SpaceBetween`). Network cluster, Wi-Fi, mute, … stay as fixed groups
- Indicator order L→R (default): cellular + data label, Wi-Fi, mute (when ringer volume is 0), notification app (cycles), Wi-Fi hotspot, Bluetooth audio (headset/speaker), battery, clock
- Interactions: icons drop in one-by-one from above (**200ms**/icon, **90ms** stagger, **left → right**); hold **3s / 5s / 10s** or **Never** (setup **Hide icons after** ListPicker; WP default **5000ms**); timed options exit upward one-by-one (same L→R order) until only the rightmost icon remains
- Cellular bars, data label, Wi-Fi arcs, mute (ringer volume 0), hotspot, Bluetooth audio, notification apps, and battery use live device telemetry when their icons-tab toggle is on

### Page 3 — Progress tray state

- Layout: collapsed or expanded tray with accent indeterminate spinner left of clock row
- Interactions: shell or app requests progress via service intent; clears when operation completes

### Page 4 — Setup

- **Layout:** Fixed `STATUS BAR` app overline + pivot tabs `customise` | `icons` (do not scroll). **customise** scrollable body: master **Show status bar** toggle + **Statusbar background** / **Hide icons after** ListPickers + **configure statusbar** border button (subtitle: Add spacers, move icons around.) + **preview** tray + accent **permissions** section with body copy and overlay / accessibility / phone-state / notification-access / Bluetooth grants. **icons** scrollable body: toggles for Network, Wi-Fi, Silent, Notification icons, Wi-Fi hotspot, Bluetooth audio, Battery + bottom **preview** glyph strip (order: network bars, network type, Wi-Fi, silent, notification app, hotspot, Bluetooth audio, battery, clock). **configure** subpage: centered tray preview with accent drag thumbs sticking out under each enabled icon / spacer; drag thumbs to reorder; **add spacer** / **add tiny spacer** border buttons at the bottom (subtitle: You can use spacers to cover the notch and active mic areas.; spacers after the last icon stay after auto-hide; only blocked when another spacer would fold the tray); tap a spacer thumb to remove it. Multiple spacers allowed.
- **Navigation:** Launcher → Status Bar app.
- **Interactions:** Grant overlay + enable accessibility, then master toggle starts/stops the overlay FGS. Boot respects the same flag. Phone-state permission is optional (enables the mobile network label). Notification access is optional (cycles active notification app icons). Bluetooth connect is optional (headset vs speaker glyph).

## System behavior

| Signal | Behavior |
|--------|----------|
| Clock | Updates on minute boundary without layout jump |
| Theme | Observe `com.metro.system.THEME_CHANGED` |
| Visibility | Apps request opaque / translucent (0.5) / hidden modes via `metro-system-sdk` API; fullscreen surfaces use `MetroStatusBarFullscreenEffect` / `requestFullscreen` |
| Immersive | When Android status bars are hidden (API 30+), the Metro tray creeps out like `MODE_HIDDEN` |
| Overlay | `SYSTEM_ALERT_WINDOW` foreground service, hosted as a `TYPE_ACCESSIBILITY_OVERLAY` so it draws above the native status bar |
| Battery | Real `ACTION_BATTERY_CHANGED` telemetry; glyph fills proportionally (red ≤20%, foreground above). While charging: solid two-prong plug with a black edge stroke interrupts the casing — head at the top gap, cord ending at the bottom casing line |
| Cellular | Real `SignalStrength` level (`0..4`) mapped to four filled bars; data label from telephony display info |
| Wi-Fi | Real `WifiManager` RSSI mapped to three arcs (`0..3`); icon hidden when Wi-Fi is off/disconnected |
| Mute | Shown after Wi-Fi when `STREAM_RING` volume is 0 (speaker + X glyph); participates in expand/collapse stagger |
| Notification app | Cycles active notification package icons (notification listener); icons-tab toggle |
| Wi-Fi hotspot | Soft-AP / tethered hotspot glyph when active; icons-tab toggle |
| Bluetooth audio | Headset or speaker glyph when BT A2DP/SCO is connected; icons-tab toggle |
| Coverage | Window height = system status-bar inset (incl. cutout), so the Android bar is fully covered |
| Side insets | Physical left/right padding from cutout + waterfall + top rounded-corner chords; when Android privacy dots appear near the clock, the clock animates a small end nudge left (200ms, opaque tray — dots paint on top) |
| Notification shade | Swipe down on the tray opens the Android notification shade; the Metro overlay hides while the shade is open (accessibility overlay would otherwise paint on top of SystemUI) |

## Images

| Image | Page | Notes |
|-------|------|-------|
| `collapsed_dark.png` | Collapsed tray | Clock-only resting state |
| `expanded_dark.png` | Expanded tray | Full indicator row |
| `progress_dark.png` | Progress tray | Accent spinner visible (see known-gaps if missing) |

## Out of scope (v1)

- Privileged OEM carrier internals beyond app-level `SignalStrength` / Wi-Fi RSSI
- Action Center / notification shade / toast banners
- Quick settings metaphors
