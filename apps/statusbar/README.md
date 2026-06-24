# Status Bar

**Package:** `com.metro.statusbar`  
**Tier:** 0

## Status

Harness docs only — Android project not scaffolded yet. This README is the detailed implementation brief for the future shell overlay.

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
- Clock visible on the right
- Other indicators hidden unless tray is expanded or a reference state requires visibility
- Expected reference: `references/images/collapsed_dark.png`

### 2. Expanded tray

- Triggered by tap on tray area
- Reveals cellular, Wi-Fi, Bluetooth, alarm, location, and battery indicators
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
- Indicator order must remain: cellular, Wi-Fi, Bluetooth, alarm, location, battery
- v1 may use static or stubbed data for radio indicators if device telemetry is impractical

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
| Real carrier/radio/battery signal behavior mirrors system internals | Android app-level access to all shell telemetry can be restricted or OEM-specific | Allow static/stub indicators in v1 while preserving exact tray layout and timing |

## Agent postmortem

_None._
