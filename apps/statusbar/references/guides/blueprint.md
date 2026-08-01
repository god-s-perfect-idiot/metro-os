# Statusbar — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Pages

### Page 1 — Collapsed tray

- Layout: WP **32dp** content band across the top; **clock only** (right-aligned); battery and other indicators hidden
- Coverage: the overlay window is sized to the **full system status-bar inset height** (status bar / notch / hole-punch), so no part of the Android bar peeks through below the WP band. Content is vertically centered within that height; the WP band is never shorter than 32dp.
- Background: opaque theme color (or translucent/hidden per app request)
- Interactions: tap anywhere on tray expands the indicator row; **swipe down** opens Action Center

### Page 2 — Expanded tray

- Layout: same height; full indicator row left-aligned in WP order; battery + clock on the right
- Indicator order L→R: cellular + data label, Wi-Fi; battery + clock on the right
- Interactions: icons drop in one-by-one from above (**200ms**/icon, **90ms** stagger, right → left); hold **5000ms**; exit upward one-by-one (same R→L order)
- v1 may use static/stub Wi-Fi glyph; cellular/data and battery are real telemetry

### Page 3 — Progress tray state

- Layout: collapsed or expanded tray with accent indeterminate spinner left of clock row
- Interactions: shell or app requests progress via service intent; clears when operation completes

### Page 4b — Active call banner

- Layout: solid green strip (`#107C10`) directly under the system tray (~36dp) while Phone has an active call
- Content: display name (left) + `calling…` / elapsed timer (right), white text
- Interactions: tap restores Phone in-call UI; dismisses when the call ends
- Source: Phone posts an ongoing `CATEGORY_CALL` notification (tag `active_call`); not listed as a toast-style Action Center row

### Page 4 — Action Center

- Layout (top → bottom):
  1. System tray strip (indicators stay revealed; clock cluster shows **time** over **battery % + date**)
  2. Row of **four** quick-action tiles (equal width, square, 4dp gaps). Active = accent fill; inactive = dark gray (`#1F1F1F` dark / `#E6E6E6` light). Icon above ALL-CAPS label (SSID / device name when known)
  3. Link row: **clear all** (left, only when notifications exist) + **all settings** (right)
  4. Notification list grouped by app (app glyph + label header; title / body / timestamp rows). Empty state: centered “No notifications”
  5. Accent-colored bottom handle bar with a short white grip
- Default quick actions (v1): Wi-Fi, Bluetooth, Airplane mode, Internet sharing
- Interactions:
  - Swipe down from tray → shade slides open (~280ms)
  - Swipe up on shade / handle → closes (~240ms)
  - Tap quick action → toggle (or open system panel when Android blocks the radio)
  - Tap notification → launch content intent + dismiss
  - Clear all → dismiss eligible notifications
  - All settings → launch `com.metro.settings` (falls back to system Settings)
- Notifications: live via `NotificationListenerService`; shell FGS packages are excluded

## System behavior

| Signal | Behavior |
|--------|----------|
| Clock | Updates on minute boundary without layout jump |
| Theme | Observe `com.metro.system.THEME_CHANGED` |
| Visibility | Apps request opaque / translucent (0.5) / hidden modes via `metro-system-sdk` API |
| Overlay | `SYSTEM_ALERT_WINDOW` foreground service, hosted as a `TYPE_ACCESSIBILITY_OVERLAY` so it draws above the native status bar |
| Battery | Real `ACTION_BATTERY_CHANGED` telemetry; glyph fills proportionally and shows a plug while charging |
| Coverage | Window height = system status-bar inset when closed; MATCH_PARENT while Action Center is open/dragging |
| Action Center | Swipe down from tray; auto-collapse of tray indicators is suspended while open |

## Images

| Image | Page | Notes |
|-------|------|-------|
| `collapsed_dark.png` | Collapsed tray | Clock-only resting state |
| `expanded_dark.png` | Expanded tray / Action Center (DE) | Empty Action Center + quick actions |
| `action_center_dark_cyan.png` | Action Center | English capture with notifications + Clear All |
| `progress_dark.png` | Progress tray | Accent spinner visible (see known-gaps if missing) |

## Out of scope (v1)

- Customizable quick-action slots (Settings → notifications + actions)
- Two-stage short-swipe (quick actions only) vs full pull
- Per-app Action Center notification filters
- True carrier signal strength telemetry
