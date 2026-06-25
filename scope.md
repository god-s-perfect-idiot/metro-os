# Project scope

This is an Android app suite that aims to revitalize the Metro UI design language. The goal is a **1:1 recreation of the Windows Phone 8.1 (WP8.1) user experience on Android** — not a reinterpretation, not Material Design with Metro colors, not "inspired by." Every screen, transition, control, and system surface must match WP8.1 behavior and appearance as closely as Android platform constraints allow.

The suite rewrites all necessary system and first-party apps in WP8.1 UX: launcher, status bar, navigation bar, browser, notes, music player, and more.

---

## Goals

| Goal | Definition of done |
|------|-------------------|
| Visual fidelity | Side-by-side screenshots against WP8.1 reference devices pass a pixel-diff threshold (≤ 2% per screen region) |
| Interaction fidelity | Gestures, transitions, and navigation match WP8.1 timing curves and affordances |
| System cohesion | All apps share accent color, theme, typography, and animation language via shared toolkit |
| Autonomous development | An AI agent can scaffold, implement, test, fix, and produce deployables per app with minimal human input |

---

## App suite

Each app is an independent Android project. Package name: `com.metro.<app>`.

### Tier 0 — System shell (required before any consumer app ships)

| App | Package | Role |
|-----|---------|------|
| **Launcher** | `com.metro.launcher` | Start screen, live tiles (fed by app tile widgets), app list, wallpaper, tile pinning |
| **Status bar** | `com.metro.statusbar` | System tray overlay: clock, signal, battery, expandable indicators |
| **Navigation bar** | `com.metro.navbar` | Soft keys: Back, Start, Search; theme-colored bar; swipe-to-hide on supported layouts |

These three apps form the **Metro Shell**. Consumer apps assume the shell is installed and expose WP8.1-standard intents/contracts.

### Tier 1 — Core apps (first wave)

| App | Package | Role |
|-----|---------|------|
| **Browser** | `com.metro.browser` | IE Mobile–style browser: address bar, tabs, favorites, reading view |
| **Notes** | `com.metro.notes` | OneNote-style hub: notebooks, sections, pages; pivot navigation |
| **Music** | `com.metro.music` | Xbox Music–style: artists/albums/songs pivots, now playing, playlists |

### Tier 2 — Extended suite (subsequent waves)

| App | Package | Role |
|-----|---------|------|
| **Photos** | `com.metro.photos` | Hub + date/ album pivots |
| **Calendar** | `com.metro.calendar` | Agenda / day / month pivots |
| **Mail** | `com.metro.mail` | Linked inboxes, conversation view |
| **Messaging** | `com.metro.messaging` | SMS/MMS thread list |
| **People** | `com.metro.people` | Contact hub, social integration surface |
| **Phone** | `com.metro.dialer` | Call history, speed dial, dial pad, in-call UI |
| **Store** | `com.metro.store` | App discovery shell (stub acceptable in v1) |
| **Settings** | `com.metro.settings` | System settings mirroring WP8.1 settings hierarchy |
| **Calculator** | `com.metro.calculator` | Portrait scientific calculator |
| **Clock** | `com.metro.clock` | Alarms, world clock, timer, stopwatch pivots |
| **Files** | `com.metro.files` | File explorer with pivot filters |

New apps are added only after they have an entry in this table and a folder scaffold under `apps/`.

---

## Repository layout

```
metro-os/
├── scope.md                 # This document — source of truth
├── AGENTS.md                # AI agent operating rules (generated from scope)
├── toolkits/                # Shared WP8.1 scaffolding (see Support tools)
│   ├── metro-ui-android/    # Compose/XML components, themes, motion
│   ├── metro-system-sdk/    # Inter-app contracts, preference keys, intents
│   └── metro-test-harness/  # Screenshot diff, gesture replay, accessibility checks
├── apps/
│   ├── launcher/
│   │   ├── app/             # Android application module
│   │   ├── test/            # Unit + instrumented tests
│   │   ├── screenshots/     # Golden reference images (WP8.1 + approved baselines)
│   │   ├── deploy/          # Release APK/AAB output (gitignored)
│   │   ├── AGENTS.md        # App-specific agent instructions
│   │   └── README.md        # Build, run, verify commands
│   ├── statusbar/
│   ├── navbar/
│   ├── browser/
│   ├── notes/
│   ├── music/
│   └── …
├── references/              # Global WP8.1 assets (design PDFs, device profiles)
│   └── (per-app refs live in apps/<name>/references/)
└── scripts/
    ├── verify-app.sh        # Per-app verification entrypoint
    ├── verify-all.sh        # Full suite verification
    └── install-shell.sh     # Install Tier 0 on device/emulator
```

### Per-app folder rules

1. **Self-contained**: An app folder must build, test, and produce a deployable without touching sibling app source (shared code lives only in `toolkits/`).
2. **Standard commands** (every `apps/<name>/README.md` must document these):

   ```bash
   ./gradlew :app:assembleDebug          # Build debug APK
   ./gradlew :app:installDebug           # Install on connected device/emulator
   ./gradlew :app:test                   # Unit tests
   ./gradlew :app:connectedDebugAndroidTest  # Instrumented tests
   ../../scripts/verify-app.sh <name>    # Full agent verification gate
   ```

3. **No cross-app imports**: Apps communicate only via `metro-system-sdk` intents, content providers, and shared preferences — never via direct classpath dependencies on another app module.
4. **Deploy output**: Release artifacts go to `apps/<name>/deploy/` and are gitignored. Only version tags and checksums are committed if needed.

---

## WP8.1 design system — 1:1 rules

These rules are **non-negotiable** unless a documented Android platform limitation forces a deviation (deviation must be logged in the app's `README.md` under "Platform exceptions").

### 1. Typography

> **Font stand-in (v1):** Use **Noto Sans** in place of WP8.1 Segoe WP. Keep the same size and weight roles below so a future Segoe swap is drop-in.

| Rule | Value |
|------|-------|
| Primary typeface | **Noto Sans** (bundle Light, Regular, Medium, SemiBold, Bold, Black). SIL Open Font License — ship in `metro-ui-android`. Never Roboto in Metro chrome |
| Minimum body size | **15sp** — no smaller text in interactive UI |
| Page title | 64sp Noto Sans Light, flush left |
| Section header | 20sp Noto Sans SemiBold |
| List item title | 24sp Noto Sans Regular |
| List item subtitle | 16sp, 60% foreground opacity |
| Text alignment | Flush left for all Latin scripts; respect RTL mirroring |
| Font variety | Max **2 weights** per screen (e.g. SemiBold headers + Regular body) |
| Decorative type | No shadows, outlines, or gradients on text |

### 2. Color and theme

WP8.1 themes are **background (dark/light) + single accent**. No per-control arbitrary colors.

#### Background colors

| Theme | Background | Secondary surface |
|-------|------------|-------------------|
| Dark | `#000000` | `#1F1F1F` |
| Light | `#FFFFFF` | `#F2F2F2` |

#### Accent palette (exact hex — user picks one system-wide)

| Name | Hex |
|------|-----|
| Blue (default) | `#1BA1E2` |
| Red | `#E51400` |
| Green | `#339933` |
| Orange | `#F09609` |
| Purple | `#A200FF` |
| Teal | `#00ABA9` |
| Lime | `#8CBF26` |
| Brown | `#996600` |
| Pink | `#FF0097` |
| Magenta | `#FF0097` |

#### Foreground colors

| Theme | Primary text | Secondary text |
|-------|--------------|----------------|
| Dark | `#FFFFFF` | `#999999` |
| Light | `#000000` | `#666666` |

#### Rules

- Accent color applies to: pivot headers (selected), progress bars, toggles (on), links, active app bar icons, tile count badges, keyboard highlight.
- **Never** use pure black or pure white as a tile background (tiles disappear against theme).
- Contrast ratio ≥ **4.5:1** for body text; ≥ **7:1** preferred for small labels.
- App bar and system chrome use theme background at **80% opacity** over content (WP8.1 `ApplicationBar` default).
- Validate every screen in **both** dark and light theme before merge.

### 3. Layout grid

| Rule | Value |
|------|-------|
| Base unit | **12px** grid |
| Screen margins | 24dp left/right for list content |
| Tile grid | 6 columns on standard phone width; tile size Small = 99×99dp, Medium = 198×99dp (half-height), Wide = 198×198dp |
| Tile gap | 4dp |
| List item height | 76dp (single line), 90dp (two line) |
| Touch target minimum | 44×44dp |
| Page header height | 98dp including title area |
| Panorama header | Full-bleed background image, 432dp tall header region |
| Pivot header strip | 48dp height, selected item underlined with 3dp accent bar |

### 4. System chrome

#### Status bar (System Tray)

- Height: **32dp**
- Default: clock visible (right-aligned); other indicators hidden until user taps tray
- On tap: all indicators slide in for **8 seconds**, then slide out
- Indicator order (left → right): cellular signal, Wi-Fi, Bluetooth, alarm, location, battery
- Supports: opaque, translucent (`backgroundOpacity` 0.5), or hidden per-app
- Progress: indeterminate accent spinner in tray during long operations
- **Must not** use Material status bar styling

#### Navigation bar (soft keys)

- Three keys: **Back** (chevron), **Start** (Windows logo), **Search** ( magnifier )
- Bar height: **48dp** (72dp on devices with extra chin — match reference device)
- Background: theme color or user-selected nav bar color (matches accent option in WP8.1 Update)
- Icons: white on dark bar, black on light bar
- Back behavior: navigate page stack within app → exit app → resume previous app (never acts as backspace in text fields)
- Start: return to launcher Start screen
- Search: invoke system search / Cortana surface (stub OK in v1)
- Swipe up from bottom edge: hide bar; swipe up again: reveal (where shell supports it)

#### App bar

- Position: **bottom** of screen, always
- Default mode: **Standard** (icon buttons visible)
- Panorama pages: **Minimized** mode (ellipsis only) — mandatory
- Max icon buttons: **4**
- Max menu items: **5** (text-only, flyout above bar)
- Icon size: 48×48dp touch, 26×26dp glyph (white circle on accent when pressed)
- Opacity: theme background at 80%
- **Never** place app bar at top
- **Never** use FAB — WP8.1 has no floating action button

### 5. Navigation patterns

#### Pivot

- Use for **filtering and categorizing** content within a section (tabs)
- Max **7** pivot items visible; prefer 3–5
- Selected pivot: accent underline + accent title color; unselected: secondary foreground
- Switch via horizontal flick or tap on header
- Lazy-load off-screen pivot content (render neighbor tabs only)
- Do **not** use pivot for top-level app hub — use Panorama or Hub

#### Panorama

- Use for **app hub / overview** — horizontal canvas wider than screen
- Show peek of next panel on right edge (~40dp)
- All panorama items rendered at load (unlike pivot)
- Background: full-bleed image, parallax scroll at 0.5× content speed
- App bar on panorama: **Minimized** only
- Do **not** put dense lists on panorama panels — summaries and entry points only

#### Page navigation

- Forward navigation: slide left (new page enters from right)
- Back navigation: slide right (current page exits to right)
- Duration: **300ms** with `ease-out` cubic bezier equivalent to WP `NavigationThemeTransition`
- No vertical page transitions except modal dialogs
- Hardware/gesture back must mirror WP8.1 stack behavior

#### Hub (Windows Phone 8.1 hub control)

- Large hero section at top, then grouped content sections
- Use for content-heavy apps (Music, Photos, Office apps)

### 6. Controls (1:1 mapping)

| WP8.1 control | Android implementation | Notes |
|---------------|------------------------|-------|
| `ToggleSwitch` | Custom switch | Pill shape, accent fill when on, 38×20dp thumb travel |
| `Slider` | Custom slider | Accent track, 24dp thumb |
| `ProgressBar` | Indeterminate: accent bar sweep; Determinate: accent fill | |
| `ListBox` / `LongListSelector` | `MetroListView` | Tilt-on-press animation (3° Z-rotate, 150ms) |
| `TextBox` | Underline style when focused | Accent underline, no Material TextInputLayout |
| `ApplicationBar` | `MetroAppBar` | See App bar section |
| `MessageDialog` | Centered modal | Title 24sp, body 16sp, accent buttons |
| `CustomDatePicker` / `TimePicker` | Looping selectors | WP8.1 scroll-wheel style |
| `LiveTile` | App tile widget → launcher | See Tiles section; apps export data, launcher renders |

**Prohibited controls**: Material 3 buttons, chips, cards with elevation, bottom sheets, snackbars, navigation rail, drawer navigation.

### 7. Tiles (launcher)

| Size | Dimensions | Notes |
|------|------------|-------|
| Small | 99×99dp | Icon + optional counter |
| Medium | 198×99dp | Most common default |
| Wide | 198×198dp | Shell / OEM only in WP8.1 — gate behind launcher flag |

- Tile images: **173×173px** design asset (scaled by launcher)
- Counter: Noto Sans, accent color, top-right
- Flip/title animations: match WP8.1 `TileAnimation` timing
- Deep link: tile tap launches app's default panorama/hub page
- Secondary tile: long-press → pin to start (launcher handles)

#### Live tiles — app tile widgets

Live tiles are **not** drawn inside the launcher from hard-coded per-app logic. Each app that can appear on the Start screen must **export tile widget data** via `metro-system-sdk` so the launcher can subscribe, render, and animate tiles as WP8.1 live tiles.

| Responsibility | Owner |
|----------------|-------|
| Tile layout, flip/cycle animations, grid placement | Launcher |
| Tile content (title, images, counter, flip faces, deep link URI) | Source app via tile widget provider |
| Pinning / unpinning / resize | Launcher |

**App requirements**

- Register a **tile widget provider** (content provider + update contract in `metro-system-sdk`) for every pin-capable surface (primary app tile and secondary tiles where supported).
- Expose at minimum: display title, background/accent color, icon or image URI, optional **counter**, and optional **back face** content for flip animations on medium/wide tiles.
- Push updates when underlying data changes (broadcast `com.metro.system.TILE_UPDATE`) and on a periodic refresh interval matching WP8.1 live-tile cadence (launcher may coalesce requests).
- Tile widget payloads must be renderable **without launching the app** — no in-process UI from the source app on the Start screen.

**Launcher requirements**

- Discover registered tile widgets for installed Metro apps and bind them to pinned tiles on the Start screen.
- Re-render on `TILE_UPDATE` and scheduled refresh; run flip/cycle animations when the provider supplies multiple faces.
- Fall back to static icon + title if an app has no tile widget registered (acceptable for stub/Tier 2 apps until implemented).

**Examples** (reference behavior, not optional polish): Music shows now-playing on flip; Mail shows unread count; Calendar shows next appointment; Weather shows current conditions (when Weather ships).

### 8. Icons and imagery

- **Simple, geometric, monochromatic** — no skeuomorphism
- System icons: 26×26dp, single color (white or black per theme)
- No drop shadows on icons
- Photography: full-bleed when used; never rounded-corner "Material cards"
- Splash screen: full-screen branded image, **no text requiring reading**, visible ≤ 3 seconds

### 9. Motion and animation

| Animation | Duration | Easing |
|-----------|----------|--------|
| Page transition | 300ms | Ease-out cubic |
| Pivot header switch | 250ms | Ease-in-out |
| Panorama scroll | Physics-based deceleration | Friction coefficient matching WP scroll viewer |
| List item tilt (press) | 150ms | Ease-out, 3° rotation |
| App bar show/hide | 200ms | Slide from bottom |
| Status tray expand | 200ms in, 200ms out | After 8000ms auto-collapse |
| Live tile flip | 600ms | WP8.1 turnstile animation |

- Perceived instant threshold: **< 500ms**
- Always show progress feedback for operations > 500ms
- **No** Material shared-element transitions or elevation animations

### 10. Audio and haptics

- Key press: subtle tick (optional, match WP keyboard)
- Long list scroll: no overscroll glow (use WP rubber-band edge fade)
- Error: brief vibration, 50ms

---

## Support tools (`toolkits/`)

Reusable scaffolding so animations and UX stay consistent across apps.

### `metro-ui-android`

- Compose (preferred) or View-based components implementing every control in §6
- Theme provider: `MetroTheme(dark: Boolean, accent: Color)`
- Motion library: `MetroTransitions` with timing constants from §9
- Typography: `MetroTextStyle` enum matching §1
- **Rule**: Apps must not reimplement controls locally — import from toolkit

### `metro-system-sdk`

- `MetroPreferences`: read/write `accent_color`, `theme_mode`, `font_scale`, `nav_bar_color`
- Intent contracts: `MetroIntents.LAUNCH_APP`, `MetroIntents.SEARCH`, `MetroIntents.SHARE`
- `ContentProvider` URI scheme: `content://com.metro.system/...`
- Broadcast: `com.metro.system.THEME_CHANGED`
- **Tile widgets**: `MetroTileContract` — manifest metadata + content-provider paths for apps to export live-tile data; `MetroTileData` model (title, images, counter, flip faces, deep link); broadcast `com.metro.system.TILE_UPDATE` when tile content changes

### `metro-test-harness`

- Screenshot comparison against `apps/<name>/references/images/` and per-app `screenshots/golden/`
- Gesture replay scripts (JSON): flick, tap, long-press sequences
- Accessibility: touch target audit, contrast checker
- Transition timing profiler: fail if duration deviates > 50ms from spec

---

## Inter-app communication

| Setting | Storage | Owner |
|---------|---------|-------|
| Theme (dark/light) | `MetroPreferences` | Settings app writes; all apps observe |
| Accent color | `MetroPreferences` | Settings app writes |
| Font scaling | `MetroPreferences` | Settings / system |
| Nav bar color | `MetroPreferences` | Settings app writes |
| Default launcher | Android `ROLE_HOME` | Launcher app |
| Live tile content | App tile widget provider (`MetroTileContract`) | Each pin-capable app writes; launcher reads |
| Status bar overlay | `SYSTEM_ALERT_WINDOW` | Status bar app |
| Navigation bar injection | Accessibility service or `TYPE_NAVIGATION_BAR` overlay | Navbar app |

All apps register a `ThemeChangeReceiver` and re-render within **1 frame** of preference change.

---

## Development process

### Technology stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose (primary) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | Latest stable |
| Build | Gradle Kotlin DSL |
| DI | Hilt (optional per app) |

### Branch and quality gates

1. Agent scaffolds app from template in `toolkits/`
2. Implements features against `apps/<name>/references/` (images + web guides)
3. Runs `./scripts/verify-app.sh <name>` — **must pass before human review**
4. Golden screenshots updated only with explicit approval (diff reviewed)

### Definition of done (per app)

- [ ] All screens in dark + light theme
- [ ] All screens with each accent color (spot-check: blue, green, red minimum)
- [ ] Unit test coverage ≥ 60% on business logic
- [ ] Instrumented tests for primary user flows
- [ ] Screenshot tests pass (≤ 2% pixel diff)
- [ ] No Material components in UI layer (lint rule enforced)
- [ ] `README.md` documents build, install, verify
- [ ] APK builds signed debug; release signing documented
- [ ] Platform exceptions documented (if any)
- [ ] Tile widget provider registered and verified (apps pin-capable on Start screen; launcher exempt)

---

## AI agent harness

This project is an experiment in **full AI-driven app suite creation** with minimal human interference. Agents must be **self-correcting**: implement → verify → fix → re-verify until green.

### Agent operating loop

```
┌─────────────┐
│ Read scope  │
│ + AGENTS.md │
└──────┬──────┘
       ▼
┌─────────────┐     fail    ┌──────────────┐
│ Implement   │◄───────────│ Fix issues   │
│ feature     │             │ (max 5 iter) │
└──────┬──────┘             └──────▲───────┘
       ▼                           │
┌─────────────┐     fail           │
│ verify-app  │────────────────────┘
│ .sh         │
└──────┬──────┘
       │ pass
       ▼
┌─────────────┐
│ Update      │
│ golden only │
│ if approved │
└─────────────┘
```

### Agent rules

1. **Read before write**: Always read `scope.md`, root `AGENTS.md`, and `apps/<name>/AGENTS.md` before editing.
2. **Toolkit first**: Check `toolkits/` for existing components before creating new ones. Add to toolkit if reusable across ≥ 2 apps.
3. **No scope drift**: Do not add features, dependencies, or UI patterns not in this document.
4. **No Material**: Lint failure on `com.google.android.material` imports in app UI modules.
5. **Test every change**: Run unit tests after logic changes; run full `verify-app.sh` before marking task complete.
6. **Self-correct**: On verification failure, diagnose from logs/screenshots, fix, and re-run (up to 5 attempts). Escalate to human only after 5 failures with a written postmortem in the app README.
7. **Reference-driven**: Implement `apps/<name>/references/guides/blueprint.md` first; use `images/` for visual polish only.
8. **Shell dependency**: Tier 1+ apps must not ship until Tier 0 apps pass verification on the same device profile.
9. **Idempotent scripts**: All verify scripts exit 0 on pass, non-zero with actionable stderr on fail.
10. **Commit discipline**: One logical change per commit; message format: `<app>: <imperative summary>` (e.g. `launcher: add medium tile flip animation`).

### Verification script contract (`scripts/verify-app.sh`)

Each run must execute, in order:

1. `./gradlew :app:assembleDebug` — build succeeds
2. `./gradlew :app:test` — unit tests pass
3. `./gradlew :app:connectedDebugAndroidTest` — instrumented tests pass (skip gracefully if no device, but warn)
4. `metro-test-harness screenshot-diff` — compare against golden
5. `metro-test-harness lint-metro` — no banned imports, contrast violations, or undersized touch targets
6. `metro-test-harness motion-profile` — transition timing within tolerance

Output: JSON report at `apps/<name>/deploy/verify-report.json`.

### Device profiles for testing

| Profile | Resolution | Density | Use |
|---------|------------|---------|-----|
| `lumia-520` | 480×800 | hdpi | Minimum supported layout |
| `lumia-925` | 768×1280 | xhdpi | Primary reference |
| `lumia-1520` | 1080×1920 | xxhdpi | Large phone |

All screenshot tests run against `lumia-925` profile unless app README specifies otherwise.

---

## Build order

```
Phase 1: toolkits (metro-ui-android, metro-system-sdk, metro-test-harness)
    ↓
Phase 2: Tier 0 shell (launcher → statusbar → navbar)
    ↓
Phase 3: Tier 1 apps (browser, notes, music) — parallel allowed
    ↓
Phase 4: Tier 2 apps — parallel allowed per app isolation rules
```

---

## Out of scope (v1)

- Cortana / voice assistant backend
- Microsoft account sync
- True cellular indicator data (stub icons acceptable)
- Windows Phone Store backend
- Tablet/landscape-first layouts (portrait phone only for v1)
- Custom ROM / system partition installation (apps install as user APKs)

---

## Reference materials

**Per app** — maintain in `apps/<name>/references/`:

- `guides/blueprint.md` — **authoritative** pages, layout, and interactions
- `images/` — visual reference only (does not override blueprint)
- `web-resources.md` — supplementary external links

**Global** — maintain in repo-root `references/`:

- WP8.1 official UI Design and Interaction Guide (PDF)
- Device profile screenshot sets (optional)
- Noto Sans font reference copies (OFL; also bundled in toolkit)
- Control timing measurements captured from reference device

---

*Last updated: 24 June 2026*
