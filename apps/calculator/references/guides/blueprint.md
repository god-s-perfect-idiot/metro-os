# Calculator — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

Target: **Windows Phone 8.1 GDR2+** Calculator on a portrait phone (768×1280 / xhdpi reference profile).

## Platform adaptation (v1)

WP8.1 switches between standard and scientific layouts via **device rotation** — this app reproduces that exactly:

- **Portrait → standard calculator** (the primary orientation).
- **Landscape (right-edge-up) → scientific calculator.**

There are **no on-screen mode tabs**; rotating the device is the mode switch, matching the WP8.1 calculator. The displayed value is preserved across rotation; only a half-entered operation chain is reset (the two modes evaluate differently).

Binary/programmer mode (landscape left-edge-up on WP8.1) is **out of v1 scope**.

> **Platform note:** Portrait remains the primary orientation per `scope.md` §portrait-only. Landscape is enabled solely to surface the scientific keypad, which is the authentic WP8.1 interaction — not a landscape-primary layout.

## App shell

- **Control model:** orientation-driven — no pivot, no tabs, no app bar.
- **Display:** large value (auto-sized 64→34sp Light, right-aligned) with a secondary **operation line** above it showing the pending operation / expression.
- **Theme:** Black background (`#000000`) on all surfaces.
- **Typography:** Noto Sans stand-in for Segoe WP.
- **No app bar:** WP8.1 Calculator is a full-bleed keypad utility with no bottom application bar.
- **No Material:** No FAB, chips, rounded cards, or elevation shadows.

## Evaluation rules (must match WP8.1)

| Mode | Rule | Example |
|------|------|---------|
| **Standard** | Left-to-right immediate execution (commercial calculator) | `3 + 5 × 8 =` → **64** |
| **Scientific** | Operator precedence (PEMDAS) | `3 + 5 × 8 =` → **43** |

Unary scientific functions (`sin`, `cos`, `tan`, `ln`, `log`, `√`, `x²`, `n!`, `10ˣ`) apply immediately to the current display value. Angle unit (`Deg` / `Rad` / `Grad`) affects trig functions.

## Pages

### Page 1 — Standard calculator (portrait, default)

- **Layout:**
  - Top ~34%: result display — operation line (grey) + large white number, right-aligned, flush to 24dp right margin. Shows `0` on launch.
  - Bottom ~66%: **6-row × 4-column** flat button grid with 6dp gaps on black.
  - Button rows (left → right):
    1. `C` | `MC` | `MR` | `M+`
    2. `⌫` | `±` | `%` | `÷`
    3. `7` | `8` | `9` | `×`
    4. `4` | `5` | `6` | `-`
    5. `1` | `2` | `3` | `+`
    6. `0` (double-width, spans cols 1–2) | `.` | `=` (accent red background `#E51400`)
  - Button style: flat dark-grey tiles, white label (26sp), no rounded corners. Press: accent fill flash with inverted text.
  - `=` tile uses red fill (`#E51400`) with white glyph at rest (not only on press).
- **Navigation:** Rotate device to landscape → Page 2 (scientific).
- **Interactions:**
  - `C` — clear all (display `0`, reset pending operation).
  - `⌫` — delete last digit; single digit becomes `0`.
  - `±` — toggle sign of display.
  - `%` — divide current display by 100.
  - `MC` / `MR` / `M+` — memory clear / recall / add display to memory.
  - Operators chain left-to-right (see table above).
- **Background:** Solid black.
- **Reference:** `images/standard_dark_blue.jpg`

### Page 2 — Scientific calculator (landscape)

- **Layout:**
  - Same display region as Page 1 (shorter, ~18% height).
  - **5-row × 8-column** grid reproducing the WP8.1 landscape scientific layout:
    1. `(` | `)` | `π` | `C` | `⌫` | `±` | `÷` | `%`
    2. `Deg` | `Rad` | `Grad` | `7` | `8` | `9` | `×` | `√`
    3. `sin` | `cos` | `tan` | `4` | `5` | `6` | `−` | `MC`
    4. `ln` | `log` | `10ˣ` | `1` | `2` | `3` | `+` | `MR`
    5. `n!` | `x²` | `xʸ` | `0` (double-width) | `.` | `=` (red) | `M+`
  - Angle mode buttons (`Deg` / `Rad` / `Grad`): selected mode uses lighter grey (`#3A3A3A`); others default dark-grey.
  - Function labels shrink to 17sp to fit (`sin`, `10ˣ`, `Grad`, etc.).
  - Keypad reserves 48dp bottom clearance for the Metro nav bar overlay (landscape system bar moves to the edge and supplies no bottom inset).
- **Navigation:** Rotate device to portrait → Page 1 (standard).
- **Interactions:**
  - Expression evaluation with operator precedence on `=`.
  - Parentheses supported for grouped expressions.
  - `π` inserts constant.
  - `xʸ` — power operator (display current, then `^` next operand).
  - Error state: display `Error`; next digit or `C` recovers.
- **Background:** Solid black.
- **Reference:** `images/scientific_dark_blue.jpg` (landscape capture; portrait grid above is the v1 adaptation)

## Images

| Image | Page | Notes |
|-------|------|-------|
| `standard_dark_blue.jpg` | Page 1 — standard | Portrait keypad, memory row, red equals |
| `scientific_dark_blue.jpg` | Page 2 — scientific | Landscape scientific layout (reference for button inventory) |

## Out of scope (v1)

- Binary / programmer / octal / hex modes
- Landscape orientation layouts
- Calculation history panel
- Cortana / voice input
