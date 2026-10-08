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
- **App bar:** Standard — **search** (focuses library search); `…` → **more** / **downloads**
- **Background:** Theme background. No Material bottom navigation bar.

### Page 2 — More (full page)

- Mihon `MoreTab` chrome via toolkit: `MetroSettingsHeader` (`metron` / `more`),
  `MetroToggleSwitch` (downloaded-only / incognito), `MetroListItem` rows
- App bar back → library pane (owned by `MetronHomeScaffold`)

### Page 3 — Settings hub

- Category list via `MetroSettingsHeader` (`metron` / `settings`) + `MetroListItem`
- Bottom `MetroAppBar` (back + search); category screens use `PreferenceScaffold`
  (`MetroSettingsHeader` + preference rows with `MetroListItem` / `MetroToggleSwitch`)

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
