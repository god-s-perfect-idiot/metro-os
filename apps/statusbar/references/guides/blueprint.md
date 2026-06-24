# Statusbar — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Pages

### Page 1 — Collapsed tray

- Layout: **32dp** strip across top; clock right-aligned; indicators hidden
- Background: opaque theme color (or translucent/hidden per app request)
- Interactions: tap anywhere on tray expands indicators

### Page 2 — Expanded tray

- Layout: same 32dp height; indicator row left-aligned in WP order; clock remains right
- Indicator order L→R: cellular, Wi-Fi, Bluetooth, alarm, location, battery
- Interactions: expand animation **200ms**; auto-collapse after **8000ms**; collapse animation **200ms**
- v1 may use static/stub indicator glyphs

### Page 3 — Progress tray state

- Layout: collapsed or expanded tray with accent indeterminate spinner left of clock row
- Interactions: shell or app requests progress via service intent; clears when operation completes

## System behavior

| Signal | Behavior |
|--------|----------|
| Clock | Updates on minute boundary without layout jump |
| Theme | Observe `com.metro.system.THEME_CHANGED` |
| Visibility | Apps request opaque / translucent (0.5) / hidden modes via future `metro-system-sdk` API |
| Overlay | `SYSTEM_ALERT_WINDOW` foreground service |

## Images

| Image | Page | Notes |
|-------|------|-------|
| `collapsed_dark.png` | Collapsed tray | Clock-only resting state |
| `expanded_dark.png` | Expanded tray | Full indicator row |
| `progress_dark.png` | Progress tray | Accent spinner visible |

## Out of scope (v1)

- True carrier signal strength telemetry
- Quick settings / notification shade metaphors
