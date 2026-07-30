# Keyboard — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

Target: **Windows Phone 8.1** touch keyboard (SIP / Word Flow) on a portrait phone (768×1280 / xhdpi reference profile).

Engine: derived from [FlorisBoard](https://github.com/florisboard/florisboard) (Apache-2.0), restyled to WP8.1.

## Platform adaptation (v1)

- Android requires enabling and selecting an IME in system settings — Metro settings surfaces **Enable** / **Select** banners that deep-link to those system sheets (documented platform exception).
- Shape writing (Word Flow swipe) is accepted when the engine supports it; visual chrome must still match tap-typing screenshots.
- Portrait primary. Landscape SIP allowed as secondary (WP8.1 supported both).

## Surfaces

### Surface 1 — Alpha SIP (QWERTY)

- **Layout (portrait):**
  1. **Prediction bar** — full-width black strip; three centered word suggestions (e.g. Happy / Happened / Happens); white text ~18sp; no chips, pills, or icons.
  2. **Row 1:** `q w e r t y u i o p` (10 keys)
  3. **Row 2:** `a s d f g h j k l` (9 keys, horizontally inset vs row 1)
  4. **Row 3:** Shift | `z x c v b n m` | Backspace
  5. **Row 4:** `&123` | emoji | `,` | `space` | `.` | Enter
- **Key chrome (dark):**
  - Keyboard background `#000000`
  - Key face `#1A1A1A` (flat rectangles, **0dp** corner radius)
  - Thin black gutters between keys
  - Glyphs white, lowercase for letters; space key shows lowercase `space`
  - Press: accent fill flash (system / MetroPreferences accent)
  - Hold / long-press: see Surface 3b
- **Key chrome (light):** white / `#E5E5E5` keys, black glyphs (same geometry)
- **Pop-up:** character preview above finger on press (engine); rectangular, no Material elevation.
- **Reference:** `images/sip_alpha_dark_black.png`

### Surface 2 — Symbols / numbers (`&123`)

- Numbers and symbols grid matching WP8.1 secondary page (engine layout).
- Same key chrome rules as Surface 1.
- **Reference gap:** see `known-gaps.md`

### Surface 3 — Emoji

- Emoticon picker opened from smiley key.
- **Layout (portrait):**
  1. **Emoji grid** — 6-column fixed grid of flat rectangular cells; dark key face `#1A1A1A` on black; thin black gutters.
  2. **Bottom strip** — single row: `abc` | category tabs (♥ / :) / ☺ / 🎈 / 🍕 / ✈ / ☁ / !?) | Backspace.
  - Active category: **full cell accent fill** (not Material underline).
  - Pressed `abc` / Backspace: accent fill.
- Return to letters after insert when advanced option enabled (v1: engine default).
- **Reference:** `images/sip_emoji_dark_blue.png`

### Surface 3b — Hold / long-press popup

- Stem above the held key uses **accent fill** with white glyph.
- Extended alternatives: light-gray cells (`#D0D0D0`) + black glyphs; focused alternative = accent fill + white glyph.
- Rectangular, 0dp radius, no Material elevation.
- **Reference:** `images/sip_hold_popup_dark_blue.png`

### Surface 4 — Keyboard settings (Metro app)

Mirrors **Settings → keyboard** on WP8.1 for the front page, then opens the full FlorisBoard settings tree (Metroized) from **advanced**:

| Page | Contents |
|------|----------|
| **keyboard** | Setup banners (enable/select IME); writing languages list; **add keyboards**; **advanced** |
| **\<language\>** | **Suggest text** toggle (maps to prediction bar) |
| **advanced** | Opens the full FlorisBoard settings hub (languages, theme, keyboard, smartbar, typing, gestures, clipboard, media, extensions, other, about) — all screens use WP8.1 Metro chrome (`MetroSettingsHeader`, Metro preference rows/toggles/dialogs) |

- Use `MetroSettingsHeader`, `MetroListItem`, `MetroToggleSwitch` — no Material in Metro chrome.
- Page titles lowercase WP style (`keyboard`, `settings`, …).
- **Reference gap:** settings captures — see `known-gaps.md`

## Interactions

| Action | Behavior |
|--------|----------|
| Tap letter | Insert glyph; update predictions |
| Tap suggestion | Commit word + space |
| Shift | Capitalize next / caps lock on double-tap (engine) |
| `&123` | Symbols page |
| Emoji | Emoticon page |
| Backspace | Delete |
| Enter | Editor action / newline |
| Long-press key | Accented / alternate popup |

## Out of scope (v1)

- Full multi-language pack marketplace UI (stub **add keyboards**)
- Cortana / voice dictation
- Rewriting FlorisBoard IME engine internals beyond WP8.1 SIP chrome and Metro settings shell
- Niche JetPref pickers (color / time) still use JetPref dialogs inside Metro rows — full Metro replacements tracked as follow-up
- Legacy FABs on a few extension/dictionary screens → migrate to `MetroAppBar`
