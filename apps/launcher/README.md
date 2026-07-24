# Launcher

**Package:** `com.metro.launcher`  
**Tier:** 0

## Status

**Core flows implemented** — persisted tile grid, app launch, swipe-right app list with search, long-press tile edit (resize/unpin), live tile contract + `TILE_UPDATE` receiver, theme/accent propagation. Wallpaper/parallax polish deferred per implementation order step 6.

## App role

This app is the WP8.1 **Start experience** on Android. It owns the Start tile grid, wallpaper-backed shell surface, alphabetical app list, tile pinning/unpinning, tile resize/edit affordances, and the deep-link entry path into all Metro apps.

The launcher is not a generic Android home screen. It must behave like WP8.1 first and Android second. Every navigation, animation, spacing rule, and tile behavior should be judged against the WP8.1 Start screen rather than common Android launcher patterns.

## Build gate

- `metro-ui-android` verified
- `metro-system-sdk` verified
- `MetroTile` and tile motion primitives available in toolkit
- This app is part of Tier 0 shell, so it can be built before consumer apps

## Current implementation snapshot

- `MainActivity` hosts `LauncherShell` with horizontal Start ↔ app list navigation
- Pinned tiles persisted in `PinnedTileStore`; default seed for shipped metro apps only (uninstalled packages are omitted)
- Tile taps launch via `PackageManager` / deep link from `MetroTileContract`
- App list: alphabetical `com.metro.*` discovery with inline search filter and find-by-letter jump list
- Long-press tile edit overlay: resize (cycles small/medium/wide when `BuildConfig.WIDE_TILES`), unpin, and drag-to-reorder (tile follows thumb; magnet reflow; order persisted on drop)
- `TILE_UPDATE` and `THEME_CHANGED` broadcasts refresh tile content and shell theme
- Live tile payloads read via `MetroTileContract`; static fallback when no provider registered
- System notifications (via `NotificationListenerService`) drive tile badges and WP8.1 flip/peek faces for pinned apps
- Custom Start faces for select third-party apps (Chrome: three brand wedges + blue center disc)
- Wallpaper/parallax not yet implemented

## Screen inventory

### 1. Start screen

- Full-screen vertical Start surface with 6-column tile grid
- Primary launch surface and home destination
- Supports theme background, accent-driven tile details, and wallpaper treatment where reference requires it
- Long-press enters tile action mode rather than Android-style launcher drag-only behavior
- Expected reference: `references/images/start_dark_blue.png`

### 2. App list

- Reached by swiping right from Start
- Alphabetical installed app list with search/filter support
- No navigation drawer, no bottom tabs, no floating search treatment
- Must preserve WP8.1 app list rhythm and typography
- Expected reference: `references/images/applist_dark_blue.png`

### 3. Tile edit / context surface

- Long-press tile reveals resize and unpin actions
- Edit mode must feel like WP8.1 tile management, not Android home-screen widgets
- Wide tile availability must stay behind feature flag unless explicitly enabled
- Expected reference: `references/guides/startmenu.md`

## System functions and contracts

### Home role

- Launcher must advertise Android home capability and serve as the default home app
- Any onboarding or setup flow should be minimal and should not break the WP8.1 illusion

### Installed app discovery

- Discover installed Metro apps from package metadata / queries
- Prefer `com.metro.*` targets and app contracts exposed through `metro-system-sdk`
- App list ordering must be deterministic and alphabetized

### Tile contract

- Launcher renders tiles; source apps provide tile data
- Read tile widget payloads through `MetroTileContract`
- Respond to `com.metro.system.TILE_UPDATE`
- Fall back to static icon and title when an app has no live tile provider yet
- With notification-listener access granted, merge active Android notifications into badges and flip/peek faces for any pinned package (shell FGS packages excluded)
- Gmail (`com.google.android.gm`) peeks map notification extras to three live-tile lines: user name (sender), title (subject), content (body preview)

### Theme and accent propagation

- Read theme and accent from `MetroPreferences`
- Re-render within one frame after preference changes
- Never hardcode tile accent variants outside the official palette
- Metro suite and Android system apps use the system accent for Start/app-list tile fills unless `MetroAppRegistry.strongBrandHex` is set; third-party apps keep icon-derived brands
- Selected third-party packages use `CustomTileBranding` (white Metro glyph + accent or brand fill) or composed faces in `CustomTileFaces` (e.g. Chrome). Includes Google Search (accent), YouTube Music (red), WhatsApp (green), and Camera (accent).

### Navigation contracts

- Tile tap launches the app’s primary experience
- Secondary tiles should deep-link when a provider supplies a URI
- Back behavior should mirror shell expectations rather than random activity stack behavior

## Detailed UI and interaction guardrails

- Tile sizes: Small `99x99dp`, Medium `198x99dp`, Wide `198x198dp`
- Tile grid: 6 columns with `4dp` gap
- Tile art source asset target: `173x173px`
- Counter badge: content-colored bold naked numeral; center-right on 1×1/2×2, bottom-right on 4×2; never circle/pill/Material badge styling; cap at `99+`; wide peek shows app icon left of count
- Never use pure black or pure white tile backgrounds
- Live tile flip timing: `600ms` turnstile-style motion
- Start to app list navigation is a horizontal relationship, not a drawer reveal
- No FAB, no cards, no Material ripple-heavy launcher behaviors
- Maintain left-aligned Metro typography; avoid centering page chrome unless reference explicitly shows it

## Data and state model

- Persist pinned tile order and size locally
- Persist whether wide tiles are enabled
- Cache tile content enough to render Start quickly before async refresh
- Separate display state from provider payloads so animation and persistence remain launcher-owned

## Primary implementation order

1. Replace hardcoded sample tiles with persisted tile model
2. Implement real app launch contracts
3. Build app list with search and swipe-right navigation
4. Add long-press tile edit mode with resize and unpin
5. Bind live tile provider updates
6. Add wallpaper/parallax polish only after core flows pass verify

## Test-critical user flows

1. Set launcher as default home and return to Start successfully
2. Render pinned tiles using persisted state
3. Tap tile and launch the correct `com.metro.*` target
4. Receive tile update broadcast and refresh visible tile content
5. Swipe to app list and filter alphabetically
6. Long-press tile, resize/unpin, and persist result
7. Theme/accent change updates visible shell immediately

## Reference and golden expectations

Per-app references: [`references/`](references/)

- `references/images/start_dark_blue.png`
- `references/images/applist_dark_blue.png`
- `references/guides/startmenu.md`
- `references/web-resources.md` — web guides for each screen
- `references/known-gaps.md` — current UI debt vs WP8.1

Golden (emulator verify):
- `screenshots/golden/start_dark_blue.png`
- `screenshots/golden/start_light_blue.png`
- `screenshots/golden/applist_dark_blue.png`

If these files are missing in the checkout, keep the paths stable in code/tests/docs and note the absence instead of inventing new filenames.

## Commands

```bash
cd apps/launcher

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh launcher
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Wide tiles were limited in WP8.1 device/OEM contexts | Device support varies and may not be worth shipping in v1 | Keep wide tiles behind `BuildConfig.WIDE_TILES`, default off |

## Agent postmortem

_None._
