# Settings — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Pages

### Page 1 — Settings root (system list)

- **Layout:** Black/white theme background. Small ALL CAPS app overline `SETTINGS` via `MetroAppTitle`, then large lowercase page title `system` (64sp Light). Scrollable list of setting rows (single-line title 24sp; optional subtitle 16sp secondary for current value). 12dp horizontal margins. No separators — whitespace only. List item height 76dp.
- **Navigation:** From app list / Action Center “all settings”. Back exits the app.
- **Interactions:** Tap a row → push detail page with 300ms horizontal slide. v1 interactive rows: `start+theme`, `ease of access`. Other WP8.1 system rows may appear as disabled/gray placeholders or be omitted — do not invent Material preference categories.
- **Background:** Theme background (dark `#000000` / light `#FFFFFF`).

### Page 2 — start+theme

- **Layout:** `SETTINGS` overline + title `start+theme`. Vertical stack:
  1. Intro body: “Change your phone's background and **accent colour** to suit your mood today, this week or all month.” — the words `accent colour` use the current accent colour; the rest is primary text.
  2. **Accent colour** — secondary label, then a full-width bordered combo (1dp primary stroke, 0dp corners) containing a small square swatch (~24dp) + lowercase colour name. Tap opens accent picker (Page 3).
  3. **Background** dark/light ListPicker — **omitted in v1** (theme stays dark; see known-gaps / platform exceptions).
  4. **Show more Tiles** — toggle (optional v1; may be deferred if launcher already owns wide-tile config via build flag).
  5. **Choose photo** / Start background — out of scope for v1.
- **Navigation:** Back → Settings root.
- **Interactions:** Changing accent writes `MetroPreferences` and broadcasts `THEME_CHANGED` immediately (suite-wide).
- **Background:** Theme background.
- **Reference:** `images/start_theme_dark_cobalt.png` (Background row present in capture — do not implement that row).

### Page 3 — Accents picker

- **Layout:** Full-screen accents page titled `ACCENTS` (uppercase section header; no `SETTINGS` overline). Grid of **official WP8.1 accents** (20 colors, **4×5** square tiles, 0dp corners, 8dp gaps — same column count and gutter language as the find-by-letter grid). Selected accent shows a white checkmark + primary-text border ring.
- **Entrance:** Each tile flips in with `MetroDiagonalFlip` — `rotationX` 90° → 0°, 300ms ease-out, diagonal stagger (`row + col`) at 40ms (identical to letter-grid / `MetroJumpList`).
- **Outro:** Tapping a colour applies it immediately, then plays the reverse diagonal flip wave (0° → 90°) before popping back to start+theme. Back without a selection returns immediately (no outro).
- **Navigation:** Selecting a palette color applies immediately, runs outro, then returns to start+theme. Back without change discards nothing (already applied only on tap).
- **Interactions:** Palette hex values only for `accent_color`. Broadcast `THEME_CHANGED` on apply.
- **Background:** Theme background.
- **Reference:** `images/accents_picker_dark.png`

### Page 4 — ease of access

- **Layout:** `SETTINGS` overline + title `ease of access`. First control block:
  - Label **Text size**
  - Bordered **Sample** preview box showing the word `Sample` at the active scale
  - Discrete **7-step** Metro slider (accent fill left of thumb; secondary track)
  - Helper text: changes text size across Metro apps (People, Phone, messaging, etc.)
  - Other WP8.1 controls (High contrast, Narrator, Screen magnifier, TTY/TDD) are **out of scope for v1** — omit or show disabled with secondary caption.
- **Navigation:** Back → Settings root.
- **Interactions:** Slider writes `font_scale` (seven fixed steps) and broadcasts `THEME_CHANGED` (include `font_scale` extra). All Metro apps observe and recompose.
- **Background:** Theme background.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `start_theme_dark_cobalt.png` | Page 2 | WP start+theme — intro + Background + Accent colour combo; implement Accent only |
| `ease_of_access_dark_cyan.png` | Page 4 | Official WP ease of access — Text size slider (7 steps), Sample preview, toggles |
| `accents_picker_dark.png` | Page 3 | Eight Forums WP8 accent grid — `ACCENTS` title, 4×5 tiles |
| `accent_palette_wp8_dark.png` | Page 3 | Generated strip of the 20 official WP8 accent hex values (palette aid) |

Missing device capture for root → see [`known-gaps.md`](../known-gaps.md).

## Out of scope (v1)

- Full WP8.1 system settings inventory (Wi‑Fi, cellular, backup, kid’s corner, etc.)
- Navigation bar colour settings
- Start background photo / parallax wallpaper picker
- Custom / Color Changer RGB accent picker
- High contrast, Narrator, Screen magnifier, browser captions
- Sync my settings / Microsoft account
- Application-specific settings hubs (IE, photos+camera, …)
