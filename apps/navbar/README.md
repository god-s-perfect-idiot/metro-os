# Navigation Bar

**Package:** `com.metro.navbar`  
**Tier:** 0

## Status

Harness docs only — Android project not scaffolded yet. This README is the implementation guide for the WP8.1 soft-key shell surface.

## App role

This app recreates the WP8.1 **navigation bar** on Android: Back, Start, and Search keys in a Metro-styled bottom bar with theme-driven color, hide/show gestures, and system-shell semantics.

The navbar must feel like Windows Phone navigation chrome, not Android gesture navigation with a themed skin. The app’s main responsibility is behavioral consistency across Metro apps, especially for Back and Start semantics.

## Build gate

- `metro-ui-android` verified
- `metro-system-sdk` verified
- Status bar and launcher shell assumptions understood
- Chosen implementation path documented before coding

## Surface inventory

### 1. Default navigation bar

- Three keys: Back, Start, Search
- Theme-colored or preference-driven background
- White icons on dark backgrounds, black icons on light backgrounds
- Expected reference: `references/images/bar_dark_blue.png`

### 2. Hidden bar state

- Swipe-up reveal / hide behavior where platform support allows
- Hidden state must be reversible and predictable
- Expected reference: `references/images/hidden_dark.png`

## System functions and contracts

### Back behavior

- Back navigates within current app page stack first
- Then exits app if stack is exhausted
- Must never turn into a text-field backspace affordance
- Cross-app dispatch logic should live in shared contracts, not per-app hacks

### Start behavior

- Start always returns to `com.metro.launcher`
- This should work as a shell action rather than merely launching a random activity

### Search behavior

- Search dispatches `MetroIntents.SEARCH`
- Cortana/global search behavior may be a stub in v1, but the key contract must exist

### Theme behavior

- Source bar color from `MetroPreferences.nav_bar_color` or theme fallback
- Observe `THEME_CHANGED` and redraw quickly

### Android implementation seam

- Candidate implementation paths called out by repo docs are overlay / accessibility-service style approaches
- Before implementation, explicitly choose and document:
  - how events are captured
  - how bar visibility is controlled
  - what OEM/device limitations apply

## UI and interaction guardrails

- Height: `48dp` by default unless a reference device profile justifies a taller chin
- Icons must be Metro glyphs, not Material icons
- No translucent gesture pill, no bottom sheet affordances, no nav rail behavior
- Pressed state should feel like WP chrome, not heavy Android ripple
- Hide/show gesture should be swipe-up based only where technically supported
- Keep the bar visually flat; no elevation cards or shadows

## Data and state model

- Persist user-selected nav bar color if supported by Settings
- Maintain current visibility mode, theme snapshot, and latest foreground app state if needed
- Do not let app-local code own global navbar state

## Primary implementation order

1. Choose overlay/accessibility architecture
2. Render static bar with correct theme and icons
3. Wire Start launch behavior
4. Wire Back dispatch semantics
5. Add Search contract stub
6. Add swipe hide/reveal behavior
7. Verify behavior across launcher and at least one Tier 1 app

## Test-critical user flows

1. Tap Start returns to launcher
2. Tap Back navigates correctly within an app and exits when stack ends
3. Tap Search dispatches the shared search contract
4. Theme/nav color changes update the bar immediately
5. Swipe hide/reveal works or gracefully no-ops on unsupported profiles with documented exception

## Reference and golden expectations

- `references/images/bar_dark_blue.png`
- `references/images/hidden_dark.png`
- `screenshots/golden/bar_dark_blue.png`
- `screenshots/golden/bar_light_blue.png`

If reference files are not yet checked in, keep these filenames stable and document any temporary manual validation source.

## Commands

```bash
cd apps/navbar

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh navbar
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| System-level navbar injection behaves like OS chrome everywhere | Android implementations vary by OEM, permissions, and overlay capability | Ship the closest consistent shell overlay/accessibility implementation and document unsupported device classes |

## Agent postmortem

_None._
