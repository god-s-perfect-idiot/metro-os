# Browser

**Package:** `com.metro.browser`  
**Tier:** 1

## Status

Harness docs only — Android project not scaffolded yet. This README is the detailed build brief for the WP8.1 browser app.

## App role

This app recreates **IE Mobile** from WP8.1: single-page browsing with Metro chrome, tabs, favorites, and a reading-view-oriented browsing experience. It should feel light, flat, and shell-consistent rather than like a skinned modern Chromium browser.

The browser is a Tier 1 app and should not be developed ahead of a verified Tier 0 shell.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify on the target device profile
- `WebView` approach chosen and documented

## Screen inventory

### 1. Browser page

- Main browsing surface for a single active tab
- Address bar in Metro chrome, not a Material omnibox
- Bottom app bar for core actions
- Expected reference: `references/images/browse_dark_blue.png`

### 2. Tabs surface

- Represents open tabs with WP-style navigation
- May be implemented as a pivot-like surface or dedicated tabs page depending on reference fidelity
- Must keep tab switching lightweight and horizontally coherent
- Expected reference: `references/images/tabs_dark_blue.png`

### 3. Favorites

- Flat list of saved favorite entries
- No card-heavy bookmark grid unless a reference explicitly supports it
- Expected reference: `references/images/favorites_dark_blue.png`

## System functions and contracts

### Rendering engine

- Use Android `WebView`
- Do not replace with Chrome Custom Tabs
- Encapsulate browser engine concerns so Metro chrome and navigation stay app-owned

### Navigation semantics

- Entering a URL loads in the active tab
- Back key should first use `WebView` history, then app navigation, then exit
- Forward should be available through app bar when history exists

### Tab model

- Manage multiple tabs with persisted or restorable state
- Define active-tab selection, tab creation, and tab disposal explicitly
- Keep tab model separate from UI so future restore/session support is possible

### Favorites

- Add/remove favorite from current page
- Persist locally in v1
- Use a simple deterministic storage shape before adding sync or cloud semantics

### Reading view

- Reading view is part of the app role in `scope.md`
- If full extraction is too complex initially, build the seam so a simplified v1 mode can be added without rewriting browser navigation
- Do not silently scope it out without documenting that choice

## UI and interaction guardrails

- Address bar belongs at the top of the content area
- `MetroAppBar` sits at the bottom with core actions: back, forward, favorite, tabs
- Horizontal page transitions inside browser chrome should remain around `300ms`
- No Material pull-to-refresh, tabs strip chrome, or omnibox treatment
- Avoid dense icon clutter; Metro chrome should stay minimal
- Respect system theme and accent immediately

## Data and state model

- `BrowserTab`: id, current URL, title, favicon URI if available, loading state, history capability flags
- `FavoriteEntry`: title, URL, created timestamp, optional site icon reference
- Keep active tab state and favorites persistence independent from the composable tree

## Primary implementation order

1. Scaffold app shell and `WebView` host
2. Implement browser page with URL entry and load state
3. Add back/forward browser semantics
4. Add multi-tab model and tabs surface
5. Add favorites persistence and management
6. Add reading-view seam and any v1 simplified behavior

## Test-critical user flows

1. Launch browser and load a typed URL
2. Navigate back and forward within `WebView` history
3. Open a new tab and switch tabs
4. Add and remove a favorite
5. Restore theme/accent without visual regressions
6. Exit predictably when browser history and app stack are exhausted

## Reference and golden expectations

- `references/images/browse_dark_blue.png`
- `references/images/tabs_dark_blue.png`
- `references/images/favorites_dark_blue.png`
- `screenshots/golden/browse_dark_blue.png`
- `screenshots/golden/tabs_dark_blue.png`

If these assets are not yet present, preserve the path contract and avoid renaming reference targets.

## Commands

```bash
cd apps/browser

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh browser
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| IE Mobile rendering and reading-view behavior | Android uses `WebView`, not Trident/IE engine | Match WP8.1 chrome and navigation exactly while using `WebView` as the rendering substrate |

## Agent postmortem

_None._
