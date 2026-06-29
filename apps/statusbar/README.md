# Status Bar

**Package:** `com.metro.statusbar`  
**Tier:** 0

## Status

**Implemented** — renders the WP8.1 system tray on top of the Android status bar: collapsed clock,
tap-to-expand indicator row, minute-boundary clock ticks, 8s auto-collapse, indeterminate progress
affordance, and per-app opaque/translucent/hidden modes. Battery is real device telemetry
(`ACTION_BATTERY_CHANGED`) with proportional fill + charging bolt; the remaining radio indicators are
static v1 glyphs. The tray auto-starts on boot once permissions are granted, and exposes its per-app
contract via `MetroStatusBar` in `metro-system-sdk`.

### Permissions required (both)

1. **Display over other apps** (`SYSTEM_ALERT_WINDOW`) — granted from `MainActivity`.
2. **Accessibility service** (`StatusBarAccessibilityService`) — enabled from `MainActivity` →
   Accessibility settings.

The accessibility service is **mandatory for visibility**: a plain `TYPE_APPLICATION_OVERLAY` window
is always layered *below* the system status bar (window layer ~111000 vs the system bar's ~151000),
so the tray would be painted behind it and stay invisible. The tray is therefore hosted as a
`TYPE_ACCESSIBILITY_OVERLAY` (layer ~311000) when the accessibility service is connected, which is
the only non-root way to draw the Metro tray *over* the Android status bar. This mirrors how the
navbar covers the system navigation bar. Without the accessibility service the overlay falls back to
the app-overlay layer and is hidden behind the system status bar (the historical "Start does
nothing" symptom).

### Inter-app contract (`metro-system-sdk`)

Other Metro apps drive the tray through `com.metro.system.MetroStatusBar` — no classpath dependency
on this app. Requests are broadcasts targeted at the tray's exported `StatusBarRequestReceiver`:

```kotlin
MetroStatusBar.requestProgress(context, visible = true)        // show the indeterminate affordance
MetroStatusBar.requestVisibility(context, MetroStatusBar.MODE_TRANSLUCENT) // 0.5 opacity
MetroStatusBar.requestVisibility(context, MetroStatusBar.MODE_HIDDEN)
MetroStatusBar.requestRefresh(context)                          // re-read theme/accent
```

## App role

This app recreates the WP8.1 **system tray** on Android: the compact tray line, tap-to-expand indicator reveal, clock surface, temporary progress indication, and per-app tray visibility behavior.

It is not a generic Android notification shade replacement. The goal is the narrow WP tray behavior described in `scope.md`: a 32dp strip, mostly-hidden indicators, and a short-lived expanded state.

## Build gate

- `metro-ui-android` verified
- `metro-system-sdk` verified
- Overlay/service implementation approach chosen and documented before coding
- This app is part of Tier 0 shell

## Surface inventory

### 1. Collapsed tray

- Default resting state
- Battery glyph + clock always visible on the right; base connection indicators (cellular, Wi-Fi) on the left
- Overlay window is sized to the full system status-bar inset (incl. notch/cutout) so the Android bar is fully covered
- Expected reference: `references/images/collapsed_dark.png`

### 2. Expanded tray

- Triggered by tap on tray area
- Reveals the full WP8.1 indicator row (per `references/images/image.png`): cellular, data connection (`4G`), call forwarding, roaming, Wi-Fi, Bluetooth, quiet hours, driving mode, ringer, location — with battery + clock remaining on the right
- Auto-collapses after 8 seconds
- Expected reference: `references/images/expanded_dark.png`

### 3. Progress tray state

- Used during longer-running shell-visible operations
- Shows indeterminate accent spinner/progress affordance in tray
- Should coexist with theme and indicator rules rather than replace the tray design entirely
- Expected reference: `references/images/progress_dark.png`

## System functions and contracts

### Overlay architecture

- Must be implemented as an always-available shell overlay/service, not embedded inside a consumer app
- Candidate Android strategies: overlay window with `SYSTEM_ALERT_WINDOW`, accessibility-driven shell overlay, or other documented shell-safe approach
- Pick one implementation path and record the reason in future architecture notes before deep implementation

### Time and indicator state

- Clock updates every minute with zero visible layout jump
- Indicator order (left group, per `references/images/image.png`): cellular, data connection, call forwarding, roaming, Wi-Fi, Bluetooth, quiet hours, driving mode, ringer, location; battery + clock are the right group
- v1 may use static or stubbed data for radio indicators if device telemetry is impractical; battery is real telemetry

### Theme and app integration

- Read foreground/background style from `MetroPreferences`
- Support opaque, translucent, or hidden tray request modes per app contract
- Observe `THEME_CHANGED` and redraw within one frame

### Progress and app requests

- Long operations may request a tray progress state
- Any API for per-app tray styling should live in `metro-system-sdk`, not in direct app imports

## UI and interaction guardrails

- Height: `32dp`
- Default visual priority: clock first, everything else tucked away
- Expand animation: `200ms`
- Collapse animation: `200ms`
- Auto-collapse timeout: `8000ms`
- No Material status bar styling, dropdown shade affordances, cards, or quick settings metaphors
- Avoid oversized icons; keep glyphs minimal and monochrome per theme
- Respect WP8.1 chrome opacity behavior when translucent mode is requested

## Data and state model

- Maintain a small in-memory tray state object: visibility mode, expanded/collapsed state, theme snapshot, indicator snapshot, progress state, last interaction time
- Auto-collapse timer should be shell-owned and cancelable on repeated interactions
- Keep indicator logic decoupled from rendering so static-v1 and dynamic-future sources can swap cleanly

## Primary implementation order

1. Choose overlay/service architecture
2. Render collapsed tray with clock and theme support
3. Implement tap-to-expand and auto-collapse timing
4. Add indicator ordering and placeholder indicator sources
5. Add tray progress state
6. Add per-app visibility/translucency requests via `metro-system-sdk`

## Test-critical user flows

1. Tray renders at boot/app launch in collapsed state
2. Tap expands indicators and auto-collapses after 8 seconds
3. Theme change updates tray colors without restart
4. Minute boundary updates clock correctly
5. Progress request shows and clears progress state predictably
6. Hidden/translucent tray modes honor app requests

## Reference and golden expectations

- `references/images/collapsed_dark.png`
- `references/images/expanded_dark.png`
- `references/images/progress_dark.png`
- `screenshots/golden/collapsed_dark_blue.png`
- `screenshots/golden/expanded_dark_blue.png`

If the underlying assets are absent in the repo, preserve these canonical filenames for future references and screenshot tests.

## Commands

```bash
cd apps/statusbar

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh statusbar
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Real carrier/radio signal behavior mirrors system internals | Android app-level access to all shell telemetry can be restricted or OEM-specific | Radio indicators (cellular, data, call forwarding, roaming, Wi-Fi, Bluetooth, quiet hours, driving, ringer, location) use static v1 glyphs; battery uses real `ACTION_BATTERY_CHANGED` telemetry. Tray layout and timing are exact. |
| Status bar is a true system-reserved region | An installed app can only overlay via `SYSTEM_ALERT_WINDOW`, which is layered below the system status bar | The tray is hosted as a `TYPE_ACCESSIBILITY_OVERLAY` (via `StatusBarAccessibilityService`) so it draws above the system status bar; requires enabling the accessibility service. Falls back to `TYPE_APPLICATION_OVERLAY` (hidden behind the system bar) when not enabled. |

## Agent postmortem

_None._
