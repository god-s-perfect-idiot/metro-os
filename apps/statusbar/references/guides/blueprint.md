# Statusbar — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Pages

### Page 1 — Collapsed tray

- Layout: WP **32dp** content band across the top; **right group always visible** = battery glyph + clock; **left group** shows the base connection indicators (cellular, Wi-Fi)
- Coverage: the overlay window is sized to the **full system status-bar inset height** (status bar / notch / hole-punch), so no part of the Android bar peeks through below the WP band. Content is vertically centered within that height; the WP band is never shorter than 32dp.
- Background: opaque theme color (or translucent/hidden per app request)
- Interactions: tap anywhere on tray expands the full indicator row

### Page 2 — Expanded tray

- Layout: same height; full indicator row left-aligned in WP order; battery + clock remain right
- Indicator order L→R (per `images/image.png`): cellular, data connection (`4G`), call forwarding, roaming, Wi-Fi, Bluetooth, quiet hours, driving mode, ringer, location — then **battery + clock on the right**
- Interactions: expand/collapse crossfade **200ms**; auto-collapse after **8000ms**
- v1 may use static/stub indicator glyphs (radio state is not yet wired); battery is real telemetry

### Page 3 — Progress tray state

- Layout: collapsed or expanded tray with accent indeterminate spinner left of clock row
- Interactions: shell or app requests progress via service intent; clears when operation completes

## System behavior

| Signal | Behavior |
|--------|----------|
| Clock | Updates on minute boundary without layout jump |
| Theme | Observe `com.metro.system.THEME_CHANGED` |
| Visibility | Apps request opaque / translucent (0.5) / hidden modes via future `metro-system-sdk` API |
| Overlay | `SYSTEM_ALERT_WINDOW` foreground service, hosted as a `TYPE_ACCESSIBILITY_OVERLAY` so it draws above the native status bar |
| Battery | Real `ACTION_BATTERY_CHANGED` telemetry; glyph fills proportionally and shows a plug while charging |
| Coverage | Window height = system status-bar inset (incl. cutout), so the Android bar is fully covered |

## Images

| Image | Page | Notes |
|-------|------|-------|
| `collapsed_dark.png` | Collapsed tray | Clock-only resting state |
| `expanded_dark.png` | Expanded tray | Full indicator row |
| `progress_dark.png` | Progress tray | Accent spinner visible |

## Out of scope (v1)

- True carrier signal strength telemetry
- Quick settings / notification shade metaphors
