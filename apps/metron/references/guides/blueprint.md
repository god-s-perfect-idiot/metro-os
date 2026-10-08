# Metron — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Metron is a **full Mihon fork** (`https://github.com/mihonapp/mihon`) with Metro home chrome.
There is no WP8.1 manga product — home layout follows Xbox Music / People **panorama hubs**.
All library / source / reader data is real Mihon (SQLDelight, extensions, downloads) — no demo seed.

## Pages

### Page 1 — Panorama home

- **Control model:** `MetroPanorama` with **4 panes**: `library` → `updates` → `history` → `browse`
- **Brand:** Giant panoramic title `metron` (Music ExtraLight treatment)
- **Pane bodies:** Real Mihon `LibraryTab` / `UpdatesTab` / `HistoryTab` / `BrowseTab` content
  (upstream presentation until those screens are Metroized)
- **Library motion:** Drill-ins (category, search, manga) use whole-page pivot enter/exit via
  `DefaultNavigatorScreenTransition` — tiles themselves do not stagger-animate.
- **App bar search (per pane):**
  - **library** → dedicated library search page (not an in-panorama text box)
  - **history** → animated in-pane search bar; hides when unfocused; tap search again to reopen
  - **browse** → dedicated Metro global search page (tag buttons for pinned / all / has results)
- **App bar options (sliding chrome card, not Material sheets):**
  - Filter / sort / display / categories each slide up a full-width card (same `#1F1F1F`
    bg as the app bar) from the bottom edge **over** the bar; dismiss slides it back down
    (scrim tap, Back, or toggle the same action).
  - **library** — filter icon; `…` → **sort** / **display** / **update library** / **more** /
    **downloads**. Display is badge toggles only (no compact grid modes — fixed cover tiles).
  - **updates** — filter icon; `…` → **categories** / **upcoming** / **more** / **downloads**.
  - **history / browse** — unchanged icons + overflow
- **App bar:** `…` → pane-specific text options (above) plus **more** / **downloads**
- **Background:** Theme background. No Material bottom navigation bar.

### Page 2 — More (full page)

- Mihon `MoreTab` chrome via toolkit: `MetroSettingsHeader` (`metron` / `more`),
  `MetroToggleSwitch` (downloaded-only / incognito), `MetroListItem` rows
- App bar back → library pane (owned by `MetronHomeScaffold`)

### Page 3 — Settings hub

- Category list via `MetroSettingsHeader` (`metron` / `settings`) + `MetroListItem`
- Bottom `MetroAppBar` (search); category screens use `PreferenceScaffold`
  (`MetroSettingsHeader` + preference rows with `MetroListItem` / `MetroToggleSwitch`)
- Settings search: `MetroAppTitle` / hub title / `MetroTextBox` / `MetroListItem` results
  (same language as library search) — not Material `TopAppBar`

### Page 3b — Upcoming updates

- Drill-in from updates `…` → **upcoming**
- `MetroSettingsHeader` (`metron` / `upcoming`) + square month grid (calendar cell language)
  + date section headers + cover `MetroListItem` rows
- Bottom `MetroAppBar` (filter icon; `…` → guide); filter uses `MetronOptionsCard`
  (not Material sheets / badges)

### Pages 4+ — Upstream Mihon flows

- Manga detail, chapter list, reader, browse source, global search, extensions, trackers,
  download queue, categories, onboarding — voyager stack from MainActivity as in Mihon
- Metro overhaul of these screens is incremental; behavior must remain Mihon-compatible

## Images

| Image | Page | Notes |
|-------|------|-------|
| `panorama_music_dark_green.jpg` | 1 | Music panorama brand + panes language |
| `panorama_people_dark_blue.jpg` | 1 | People hub panorama peeks / titles |
| `hub_photos_dark_blue.jpg` | 1 library | Cover / media grid density reference |

Manga-specific captures: see [`known-gaps.md`](../known-gaps.md).

## Out of scope (v1 Metro chrome)

- Metroizing every Mihon drill-in (manga detail, reader chrome, list-picker dialogs, …)
- Replacing TachiyomiTheme globally with MetroTheme on non-home / non-settings screens

Upstream Mihon features (extensions, trackers, downloads, backups, local source) remain **in scope** via the vendored tree.
