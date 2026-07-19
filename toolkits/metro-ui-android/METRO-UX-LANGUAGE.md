# Metro UX language — Windows Phone 8.1

**Authoritative visual and interaction spec for metro-os agents.**

This document translates authentic WP8.1 Metro design (Segoe WP era, Update 8.1) into implementable rules for `metro-ui-android` and every app. It supplements [`scope.md`](../../scope.md) with per-control UX decisions. When they conflict, **`scope.md` wins**; when this doc is more specific about a control's look-and-feel, **this doc wins**.

| Priority | Source |
|----------|--------|
| 1 | `scope.md` — numeric tokens, motion timings, banned patterns |
| 2 | **This file** — shape language, control anatomy, state machines |
| 3 | `apps/<name>/references/guides/blueprint.md` — page layout |
| 4 | `apps/<name>/references/images/` — visual polish only |

**Research basis:** Microsoft *UI Design and Interaction Guide for Windows Phone 7* (v2.0, foundation for WP8.1), *Essential graphics, visual indicators, and notifications for Windows Phone*, WP8 design guidance (pivot/panorama/app bar), and `scope.md` measurements validated against Lumia 925 WP8.1 GDR2 captures.

---

## 1. Design philosophy

Metro on Windows Phone 8.1 is **content-first, chrome-minimal, typography-driven**.

| Principle | What it means in practice |
|-----------|---------------------------|
| **Clean, light, open, fast** | Generous whitespace, no ornamental chrome, no gradients or skeuomorphism |
| **Typography is the UI** | Size and weight carry hierarchy; decoration is rare |
| **Motion explains place** | Transitions show where you came from and where you are going |
| **Squares and lines** | Layout is grid-aligned rectangles; accent bars and tile edges are **90°** |
| **One accent** | User picks a single system accent; it tints active states, not arbitrary widgets |
| **Hardware back** | No in-app back button in the app bar; Back is always the system nav key |

**Banned outright:** Material Design (FAB, chips, elevated cards, snackbars, bottom sheets, navigation drawer/rail, Roboto in Metro chrome, rounded-corner photo cards, drop shadows for depth).

---

## 2. Shape language — squares vs rounds

WP8.1 uses **two shape families**. Mixing them incorrectly is the most common agent mistake.

### 2.1 Square family (default)

Use **sharp 0dp corner radius** unless a control is listed in §2.2.

| Element | Shape rule |
|---------|------------|
| **Live tiles** | Perfect rectangles on a 12dp grid; **no** corner radius |
| **Border text buttons** | Rectangle with 1:1 aspect unconstrained; **square corners**, visible border stroke |
| **List rows** | Full-width horizontal bands; edge-to-edge within 24dp margins |
| **Pivot accent underline** | 3dp-thick **rectangle** under selected header |
| **Checkboxes** | 20×20dp **square** outline; filled accent when checked |
| **Progress bar** | Rectangular track and fill |
| **Message dialog** | Centered rectangle panel; square corners |
| **Panorama section headers** | Flush-left text blocks over imagery |
| **Photography** | Full-bleed rectangles; **never** rounded "cards" |

### 2.2 Round family (exceptions only)

Rounded geometry is **reserved** for these controls — nowhere else.

| Element | Shape rule |
|---------|------------|
| **App bar icon press affordance** | **Circle** (not rounded-rect) drawn behind icon on press — system-provided in WP, replicated in `MetroAppBar` |
| **App bar icon glyph area** | 26×26dp icon centered in 48×48dp touch target |
| **ToggleSwitch** | **Pill** capsule; 38×20dp track, circular thumb |
| **Slider thumb** | **Circle**, 24dp diameter |
| **Radio button** | **Circle** outline with inner dot when selected |
| **Indeterminate progress** | Accent bar with soft ends (WP system control) |

**Agent rule:** If you are about to add `RoundedCornerShape` to a button, tile, list item, or dialog — **stop**. You almost certainly want either a **border button** (square) or an **app bar icon button** (circular press).

---

## 3. Spatial system

### 3.1 Grid

| Token | Value | Usage |
|-------|-------|-------|
| Base unit | **12dp** | All spacing snaps to multiples of 12 |
| Screen side margins | **24dp** | List and page content |
| Tile gap | **4dp** | Start screen grid |
| Tile columns | **6** | Standard phone width |

### 3.2 Touch targets

| Token | Value |
|-------|-------|
| Minimum touch target | **44×44dp** |
| App bar icon button | **48×48dp** |
| List row height (1 line) | **76dp** |
| List row height (2 line) | **90dp** |
| Pivot header strip | **48dp** tall |

---

## 4. Typography

**Typeface:** Noto Sans (Segoe WP stand-in). Never Roboto in Metro chrome.

| Role | Size | Weight | Opacity / color |
|------|------|--------|-----------------|
| Page title | 64sp | Light | Primary foreground; flush left |
| Section header | 20sp | SemiBold | Primary foreground |
| List item title | 24sp | Regular | Primary foreground |
| List item subtitle | 16sp | Regular | **60%** foreground |
| Body / interactive minimum | 15sp | Regular | Primary foreground |
| Dialog title | 24sp | Regular | Primary foreground |
| Dialog body | 16sp | Regular | Primary foreground |
| App bar menu item | 15sp | Regular | Primary foreground; **lowercase** |
| App bar icon text hint | 15sp | Regular | Shown when menu expanded; **lowercase** |

**Rules:**

- Max **2 font weights** on one screen.
- No text shadows, outlines, or gradients.
- Push button label: max **2 words**, usually a verb.
- Application titles and pivot headers: **lowercase** in WP8.1 system apps (match in metro-os).
- Title caps for hub/panorama hero titles; sentence caps for settings descriptions.

**Toolkit:** `MetroText` + `MetroTextStyle`.

---

## 5. Color and theme

### 5.1 Theme model

WP8.1 = **dark or light background** + **one user accent**. No per-control color picking.

| Theme | Background | Secondary surface | Primary text | Secondary text |
|-------|------------|-------------------|--------------|----------------|
| Dark | `#000000` | `#1F1F1F` | `#FFFFFF` | `#999999` |
| Light | `#FFFFFF` | `#F2F2F2` | `#000000` | `#666666` |

### 5.2 Accent usage

Accent applies **only** to:

- Selected pivot header text and 3dp underline
- Toggle on-state fill
- Progress bar fill
- Hyperlinks
- Active / pressed app bar icon tint
- Tile counter numerals
- Checkbox checked fill
- Keyboard caret highlight

Accent does **not** apply to: tile backgrounds (user/app chosen), body text, borders on border buttons, or decorative fills.

### 5.3 Chrome opacity

App bar, status tray, and system overlays use theme background at **80% opacity** over scrolling content.

**Toolkit:** `MetroColors`, `MetroTheme`.

---

## 6. Control specifications

Each subsection defines **anatomy → states → do / don't → toolkit mapping**.

---

### 6.1 Live tile (`MetroTile`)

**Purpose:** Start screen shortcut; may show title, counter, flip faces.

| Property | Spec |
|----------|------|
| Corner radius | **0dp** — sharp square |
| Small | 99×99dp |
| Medium | 198×99dp |
| Wide | 198×198dp |
| Design asset | 173×173px source image |
| Title position | Bottom-left, 8dp inset, max 2 lines |
| Counter | Top-right, accent color, system font |
| Background | App-provided; **never** pure `#000000` or `#FFFFFF` |
| Press | No tilt; optional subtle scale only if matching launcher ref |
| Long press | Pin / resize / unpin (launcher) |

**Don't:** Rounded corners, drop shadows, Material card elevation, gradient overlays.

---

### 6.2 App bar (`MetroAppBar`)

**Purpose:** Up to four primary actions as **icon buttons**; overflow in **text menu**.

| Property | Spec |
|----------|------|
| Position | **Bottom** of screen, full width — never top |
| Height | 72dp portrait (icon row + padding) |
| Background | Theme @ 80% opacity |
| Standard mode | Up to **4** icon buttons visible |
| Minimized mode | **Ellipsis only** (`…`) — **mandatory on panorama** |
| Icon buttons | **Icon only** in the bar — **no text-only bar buttons** |
| Icon asset | 48×48dp canvas; **26×26dp** white glyph centered; transparent PNG |
| Icon color | White on dark theme, black on light — monochrome only |
| Press state | `MetroAppBar` fills a **white/black circle** behind the icon and inverts the glyph — **do not bake the press circle into the asset** |
| Disabled icon | 40% opacity |
| Collapsed reveal | Icon row only (no labels); `…` ellipsis pinned top-right |
| Expanded reveal | Tapping `…` fades in a **text label beneath every icon** and slides a **text-only menu list in below the icon row** — the bar grows upward from the bottom edge |
| Menu | Up to **5** text-only items, listed beneath the icon row when expanded |
| Menu item text | lowercase, 14–20 char recommended |
| Menu item height | 48dp minimum touch row |
| Expand gesture | Tap `…` (or any icon's dots) to toggle |
| Dismiss | Tap outside (scrim), system Back, or select a menu item |

**Don't:** FAB, top toolbar, text buttons in icon row, colored multi-hue icons, more than 4 icons "because slots exist".

**Toolkit:** `MetroAppBar` (uncontrolled or `expanded`/`onExpandedChange` controlled), `MetroAppBarIcon` (icon-type or custom-glyph), `MetroAppBarMenuItem`, `MetroAppBarDefaults`. Place it last in a bottom-aligned `Box` so the expanded panel overlays page content.

---

### 6.3 Border text button (`MetroBorderButton`)

**Purpose:** In-page actions, dialog commits, settings confirmations. This is the classic WP **rectangular outlined button** — what users describe as "white square border text buttons."

| Property | Dark theme | Light theme |
|----------|------------|-------------|
| Corner radius | **0dp** | **0dp** |
| Border | 3dp stroke, `#FFFFFF` | 3dp stroke, `#000000` |
| Background (rest) | Transparent | Transparent |
| Text | 15sp SemiBold, `#FFFFFF` | 15sp SemiBold, `#000000` |
| Background (pressed) | `#33FFFFFF` (20% white) | `#33000000` (20% black) |
| Background (disabled) | Transparent; border + text @ 40% | Same |
| Min height | 44dp | 44dp |
| Horizontal padding | 16dp | 16dp |
| Label | Max 2 words; verb-first ("save", "delete", "connect") |

**Dialog button order:** Affirmative (**ok**, **yes**, **save**) on the **left**; dismissive (**cancel**, **no**) on the **right**.

**Don't:** Rounded corners, filled accent background (that's a link or pivot selection), icons inside border buttons unless using a separate icon-button pattern.

**Toolkit:** `MetroBorderButton` (planned — until then, match this spec exactly in app code).

---

### 6.4 Icon button with circular press (`MetroIconButton`)

**Purpose:** Same press language as app bar icons, used **in content** (e.g. media transport, map zoom). Distinct from border text buttons.

| Property | Spec |
|----------|------|
| Touch target | 48×48dp minimum |
| Glyph | 26×26dp, monochrome |
| Press | **Circle** 48dp diameter behind glyph @ 20% foreground |
| Rest | Transparent — no visible border |
| Shape | **Circular press**, not rounded rectangle |

**Don't:** Square border, pill shape, or Material `IconButton` ripple.

---

### 6.5 Hyperlink (`MetroHyperlink`)

| Property | Spec |
|----------|------|
| Appearance | Accent-colored text, **no** underline at rest |
| Press | Accent @ 80% opacity |
| Usage | Navigation only — not for firing modals |
| Disabled | Hidden entirely if permanently unavailable |

---

### 6.6 List item (`MetroListItem`)

| Property | Spec |
|----------|------|
| Shape | Full-width **rectangle** row |
| Height | 76dp (1-line) / 90dp (2-line) |
| Title | 24sp Regular |
| Subtitle | 16sp @ 60% opacity |
| Chevron | Optional 12dp accent or secondary glyph for drill-in |
| Press | **Tilt** 3° Z-rotation, 150ms ease-out |
| Separator | None between items — whitespace bands only |
| Margins | 24dp horizontal |

**Don't:** Card elevation, rounded corners, swipe actions (WP8.1 uses app bar for row actions).

---

### 6.6.1 Empty / support text (`MetroEmptyState`)

Shown when a list or page has no content yet (e.g. "No recent calls.", "No conversations yet.").

| Property | Spec |
|----------|------|
| Alignment | **Top-left** (`Alignment.TopStart`) — never centered |
| Type style | **`ListItemTitle`** (24sp) — large, not body-sized |
| Color | Secondary foreground (60% opacity) |
| Margins | 24dp horizontal + 24dp top |
| Background | Theme background |

**Standard across all apps.** Empty-state text must be big, left- and top-aligned (matches the WP8.1 Phone app's "no recent calls" treatment). Use `MetroEmptyState` from the toolkit; do not center, shrink, or restyle empty text per-app.

**Don't:** Centered text, `Body` (15sp) size, illustrations, or Material empty-state graphics.

---

### 6.7 Pivot (`MetroPivot`)

| Property | Spec |
|----------|------|
| Use case | Filter / categorize **similar** content |
| Max items | **7** (prefer 3–5) |
| Header height | 48dp |
| Selected | Accent text + **3dp accent underline** (square ends) |
| Unselected | Secondary foreground, no underline |
| Header labels | 1–2 words, lowercase |
| Switch animation | 250ms ease-in-out |
| Nesting | **Never** pivot inside pivot; **never** pivot inside panorama |

---

### 6.8 Panorama (`MetroPanorama`)

| Property | Spec |
|----------|------|
| Use case | Hub / overview only — not dense data |
| Header region | 432dp tall, full-bleed background image |
| Peek | ~40dp of next panel visible on right |
| Parallax | Background scrolls at **0.5×** content speed |
| App bar | **Minimized only** |
| Content | Summaries and entry points — not long lists |

---

### 6.9 Toggle switch (`MetroToggleSwitch`)

| Property | Spec |
|----------|------|
| Shape | **Pill** track |
| Track | 38×20dp |
| Thumb | Circular, travels full track width |
| On | Accent fill |
| Off | 20% foreground stroke on transparent |
| Label | Sentence caps, 15sp, left of control |

---

### 6.10 Checkbox (`MetroCheckBox`)

| Property | Spec |
|----------|------|
| Box | **20×20dp square**, 2dp border |
| Checked | Accent fill + white checkmark |
| Label | Sentence caps; tap label or box |
| Indeterminate | Avoid unless mirroring WP multi-select edge case |

---

### 6.11 Radio button (`MetroRadioButton`)

| Property | Spec |
|----------|------|
| Outer | **Circle** 20dp, 2dp border |
| Selected | Inner accent dot 10dp |
| Group | Mutually exclusive |
| Label | 1–2 lines max |

---

### 6.12 Text box (`MetroTextBox`)

| Property | Spec |
|----------|------|
| Rest | No box border; bottom line only optional |
| Focus | **Accent underline** 2dp; no Material TextInputLayout |
| Text | 15sp Regular |
| Placeholder | 60% foreground |

---

### 6.13 Slider (`MetroSlider`)

| Property | Spec |
|----------|------|
| Track | 4dp height, secondary foreground |
| Fill | Accent |
| Thumb | **24dp circle** |
| Value bubble | Optional accent label above thumb while dragging |

---

### 6.14 Progress bar (`MetroProgressBar`)

| Property | Spec |
|----------|------|
| Indeterminate | Accent bar sweeps horizontally |
| Determinate | Rectangular accent fill |
| Label | Sentence caps; ellipsis allowed for ongoing ("downloading…") |

---

### 6.15 Message dialog (`MetroMessageDialog`)

| Property | Spec |
|----------|------|
| Panel | Centered rectangle, **0dp** corners, secondary surface background |
| Title | 24sp |
| Body | 16sp |
| Buttons | `MetroBorderButton` row — affirmative left, cancel right |
| Scrim | `#80000000` over page |
| Entry | Fade + slight scale — no Material bottom sheet |

---

### 6.16 Hub (`MetroHub`)

| Property | Spec |
|----------|------|
| Hero | Large 64sp title over imagery or accent band |
| Sections | Grouped lists with section headers 20sp SemiBold |
| Use | Content-heavy apps (Music, Photos) |

---

### 6.17 Page header (`MetroPageHeader`)

| Property | Spec |
|----------|------|
| Title | 64sp Light, flush left |
| Region height | 98dp including title block |
| Subtitle | 16sp @ 60% below title if needed |

---

### 6.18 Find by letter / jump list (`MetroJumpList`)

WP8.1 LongListSelector alphabet jump. Used whenever a list groups rows under letter markers (app list, People `all`, etc.).

| Property | Spec |
|----------|------|
| Trigger | Tap a letter section marker in the list |
| Layout | Full-screen overlay; **4 columns** × 7 rows |
| Cells | `#`, `a`–`z`, then locale **globe** tile (28 total) |
| Active tile | System **accent** fill, white lowercase glyph, tappable |
| Inactive tile | `#2B2B2B` fill (`MetroColors.JumpListInactive`), not actionable |
| Scrim | Translucent black so the list remains faintly visible in gaps |
| Select | Tap active letter → scroll list to that section and dismiss |
| Dismiss | Hardware Back, or tap scrim outside tiles |
| Locale tile | Globe glyph; inactive unless the app supplies locale jump support |
| Helpers | `MetroJumpListLogic.sortKey` / `activeLetters` / `showSectionMarkers` / `diagonalIndex`; section anchors use `MetroLetterTile` |
| Entrance | Each tile flips in around its **horizontal center** (`rotationX` 90° → 0°, 300ms ease-out). Stagger by diagonal (`row + col`) from top-left at 40ms steps |
| Search mode | While an inline list search field is active, **omit** letter section markers (`showSectionMarkers(false)`). Jump list is unavailable until search is dismissed. App-list search uses a **white fill + accent border** square field (not the underline TextBox); matching characters in result labels use accent. |

**Agent rule:** Do not reimplement jump grids in apps. Import `MetroJumpList` from `metro-ui-android`.

---

## 7. System chrome (shell apps)

### 7.1 Status bar

- Height **32dp**; clock right-aligned by default
- Tap tray → all indicators slide in **8s** then hide
- No Material status styling

### 7.2 Navigation bar (soft keys)

- **Back** (chevron), **Start** (Windows logo), **Search** (magnifier)
- Bar **48dp**; icons monochrome per theme
- Back navigates page stack — never backspace in text fields

---

## 8. Motion

| Animation | Duration | Easing |
|-----------|----------|--------|
| Page forward / back | 300ms | Ease-out |
| Pivot switch | 250ms | Ease-in-out |
| List tilt press | 150ms | Ease-out, 3° |
| App bar show/hide | 200ms | Slide from bottom |
| Live tile flip | 600ms | Turnstile |
| Status tray | 200ms | Auto-hide after 8000ms |

Show progress for operations **> 500ms**. No Material shared-element transitions.

**Toolkit:** `MetroTransitions`, `Modifier.metroTiltOnPress()`.

---

## 9. Icons and imagery

| Rule | Spec |
|------|------|
| Style | Simple geometry, monochromatic, real-world metaphors |
| Detail | Minimal fine lines |
| System icons | 26×26dp effective, single color |
| Shadows | **None** on icons |
| Photography | Full-bleed; square crop; no rounded frames |
| Splash | Full-screen image, no readable text, ≤ 3s |

---

## 10. Copy and voice

| Pattern | Rule |
|---------|------|
| Voice | Human, friendly, concise — not "Error code 4B696C626F" |
| Buttons | Verbs, ≤ 2 words |
| Questions | Avoid in labels; OK in dialog titles |
| Ellipsis | Progress only ("syncing…"), never in button labels |
| Periods | Not on checkbox/radio labels |

---

## 11. Decision tree for agents

```
Need an action control?
├─ Primary, frequent, fits icon? → App bar icon button (circular press)
├─ Secondary or hard to iconize? → App bar menu text item
├─ In-page commit / dialog? → Border text button (square corners)
├─ In-page icon-only? → Icon button with circular press
├─ Navigation to another page? → Hyperlink or list item
└─ On/off setting? → ToggleSwitch (pill) — not checkbox

Need a container shape?
├─ Start screen cell? → Tile (sharp rectangle)
├─ Settings row? → List item (sharp rectangle)
├─ Modal? → Message dialog (sharp rectangle)
└─ Photo? → Full-bleed rectangle (never rounded card)
```

---

## 12. Anti-patterns (lint failures)

| Wrong | Right |
|-------|-------|
| `FloatingActionButton` | App bar icon |
| `Card` with `elevation` | Flat list row or hub section |
| `RoundedCornerShape(12.dp)` on buttons | `MetroBorderButton` @ 0dp **or** circular icon press |
| `NavigationBar` / drawer | Pivot, panorama, or hub |
| `Snackbar` | Message dialog or system toast stub |
| `TopAppBar` | Bottom `MetroAppBar` |
| Roboto in chrome | Noto Sans via `MetroText` |
| Text-only button in app bar icon row | Move to menu or use border button in content |
| Pure black/white tile | Tinted tile color from palette |

---

## 13. Toolkit component index

| UX spec section | Composable | Status |
|-----------------|------------|--------|
| §6.1 Tile | `MetroTile` | Implemented |
| §6.2 App bar | `MetroAppBar` | Implemented |
| §6.3 Border button | `MetroBorderButton` | Planned |
| §6.4 Icon button | `MetroIconButton` | Planned |
| §6.5 Hyperlink | `MetroHyperlink` | Planned |
| §6.6 List item | `MetroListItem` | Planned |
| §6.7 Pivot | `MetroPivot` | Planned |
| §6.8 Panorama | `MetroPanorama` | Planned |
| §6.9 Toggle | `MetroToggleSwitch` | Planned |
| §6.10 Checkbox | `MetroCheckBox` | Planned |
| §6.11 Radio | `MetroRadioButton` | Planned |
| §6.12 Text box | `MetroTextBox` | Planned |
| §6.13 Slider | `MetroSlider` | Planned |
| §6.14 Progress | `MetroProgressBar` | Planned |
| §6.15 Dialog | `MetroMessageDialog` | Planned |
| §6.16 Hub | `MetroHub` | Planned |
| §6.17 Page header | `MetroPageHeader` | Implemented |
| §6.18 Jump list | `MetroJumpList`, `MetroLetterTile`, `MetroJumpListLogic` | Implemented |
| Theme / color | `MetroTheme`, `MetroColors` | Implemented |
| Typography | `MetroText`, `MetroTextStyle` | Implemented |
| Motion | `MetroTransitions` | Implemented |

**Rule:** Apps must not reimplement these primitives locally. Import from `metro-ui-android`.

---

## 14. Verification

Agents must confirm UI work against this document **and** [`docs/DESIGN-CHECKLIST.md`](../../docs/DESIGN-CHECKLIST.md) before marking tasks complete.

```bash
./scripts/verify-toolkit.sh metro-ui-android   # toolkit changes
./scripts/verify-app.sh <name>                 # app UI changes
```

Harness gate: `metro-test-harness/scripts/check-ux-language.sh` verifies this file exists and is linked from agent docs.
