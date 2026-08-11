# Statusbar — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Pages

### Page 1 — Collapsed tray

- Layout: WP **32dp** content band across the top; **clock only** (right-aligned); battery and other indicators hidden
- Coverage: the overlay window is sized to the **full system status-bar inset height** (status bar / notch / hole-punch), so no part of the Android bar peeks through below the WP band. Content is vertically centered within that height; the WP band is never shorter than 32dp.
- Background: opaque theme color (or translucent/hidden per app request)
- Interactions: tap anywhere on tray, or going home / Start, expands the indicator row

### Page 2 — Expanded tray

- Layout: same height; full indicator row left-aligned in WP order; battery + clock on the right
- Indicator order L→R: cellular + data label, Wi-Fi; battery + clock on the right
- Interactions: icons drop in one-by-one from above (**200ms**/icon, **90ms** stagger, right → left); hold **3s / 5s / 10s** (setup **Hide icons after** ListPicker; WP default **5000ms**); exit upward one-by-one (same R→L order)
- v1 may use static/stub Wi-Fi glyph; cellular/data and battery are real telemetry

### Page 3 — Progress tray state

- Layout: collapsed or expanded tray with accent indeterminate spinner left of clock row
- Interactions: shell or app requests progress via service intent; clears when operation completes

## System behavior

| Signal | Behavior |
|--------|----------|
| Clock | Updates on minute boundary without layout jump |
| Theme | Observe `com.metro.system.THEME_CHANGED` |
| Visibility | Apps request opaque / translucent (0.5) / hidden modes via `metro-system-sdk` API |
| Overlay | `SYSTEM_ALERT_WINDOW` foreground service, hosted as a `TYPE_ACCESSIBILITY_OVERLAY` so it draws above the native status bar |
| Battery | Real `ACTION_BATTERY_CHANGED` telemetry; glyph fills proportionally and shows a plug while charging |
| Coverage | Window height = system status-bar inset (incl. cutout), so the Android bar is fully covered |

## Images

| Image | Page | Notes |
|-------|------|-------|
| `collapsed_dark.png` | Collapsed tray | Clock-only resting state |
| `expanded_dark.png` | Expanded tray | Full indicator row |
| `progress_dark.png` | Progress tray | Accent spinner visible (see known-gaps if missing) |

## Out of scope (v1)

- True carrier signal strength telemetry
- Action Center / notification shade / toast banners
- Quick settings metaphors
