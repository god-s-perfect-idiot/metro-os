# Music — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

Target: **Windows Phone 8.1 Xbox Music** (store app titled **Music**) on a portrait phone (768×1280 / xhdpi). Streaming catalog in metro-os maps Xbox Music Pass / OneDrive streaming → **YouTube Music**.

## App shell

- **Control model:** Wide **panorama** hub with left-to-right sections. Collection drill-ins use **pivot** (artists | albums | songs | playlists | genres).
- **Brand title:** Hub uses panoramic lowercase **`metro music`** via `MetroPanoramaBrandTitle` (metro-os brand in place of WP8.1's `xbox music`; ExtraLight 96sp). It bleeds off the right edge; the panorama parallax is derived from the measured overflow so the tail of the word is fully revealed by the last pane — **no** `MetroAppTitle` / `MUSIC` overline on the landing panorama. Ink defaults to **white**; when colourful album art is loaded, paint a horizontal gradient sampled left→right from that cover. Drill-in pages (collection pivots, settings) may still use `MetroAppTitle` where appropriate.
- **Theme:** Black background (`#000000`). Accent from `MetroPreferences` (captures often teal/green). While a track is loaded, the **hub** background is a **clear artist photo** (full-bleed, dimmed with a dark scrim) — WP8.1 Xbox Music used the artist image behind the panorama, not a blur of the playing cover (Reference: `images/hub_nowplaying_compare_dark_unknown.jpg`). Prefer YouTube Music channel art or a music-filtered Wikipedia/Wikimedia hero so the now-playing album square does not repeat the same image; never use the per-track thumbnail as the wash. Return to `#000000` when playback is empty or no artist photo resolves. Drill-in pages stay flat black. Brand-title colour may still sample the **album** cover.
- **App bar:** Minimized `…` on hub; expand for search/settings/sync. Transport on now-playing is **in-page** (circular prev / play / next), not Material mini-player.
- **No Material:** No FAB, snackbars, bottom sheets, cards, rounded album frames.

## WP 8.1 Music information architecture

| Panorama section (hub) | Role |
|------------------------|------|
| **collection** | artists / albums / songs / genres / playlists / radio links into pivots |
| **get music** | Discovery / YouTube Music connect + search (maps Store / Explore) |
| **now playing** | Large art, scrubber, up next, shuffle/repeat/queue glyphs, circular transport |
| **local** | On-device library actions: settings + sync now (same accent tile chrome as get music) |

Collection list pages show **`showing <filter>`** (`MetroShowingLabel`) with filters: **all music** | **on this device** | **youtube music** (maps WP: All Music / On my phone / Streaming).

## Pages

### Page 1 — Hub landing (panorama)

- **Layout:**
  - Brand **`metro music`** — huge Light type, flush left, clips / bleeds right; scrolls with panorama offset until the tail is exposed (Reference: `images/hub_fullpage.png`). **Do not** put `MetroAppTitle("MUSIC")` on this surface.
  - Section titles via `MetroPanorama`: `collection` | `get music` | `now playing` | `local` (next title peeks).
  - **collection:** vertical lowercase links — artists, albums, songs, genres, playlists, radio.
  - **get music:** accent tiles laid out like Start squares — glyph centered above a bottom-left label — **top songs** (first) / **search** / **connect|account** / **recent**, plus sync status when connected. Top songs opens a pivot (**top songs** | **billboard 100**): flush-left title rows with a streaming radio glyph beside the artist (no rank index), YouTube Music Top Songs chart and Billboard Hot 100 (resolved to YT streams). 24dp under the pane title before the squares, and 24dp under the tile row, so the blocks clear the `get music` descenders (`images/hub_fullpage.png`).
  - **now playing:** track + art + scrubber + up next + circular transport (Reference: `images/hub_nowplaying_dark_green.jpg`). Track title, artist (`by …`), and **Up next** stay on one line: they overrun the end margin and clip mid-glyph at the screen edge — never wrap.
  - **local:** same Start-style accent tile chrome as get music, but **full half-width** tiles (larger hit targets). Three tiles — **settings**, **sync now**, **music directories** (folder glyph). Status line under the row reports on-device song count, scan progress, or sync message. Settings opens Page 6; sync now refreshes MediaStore + YouTube Music library; music directories opens the folder multi-select.
  - **Backdrop:** all hub panes share one background; a clear, dimmed artist photo replaces black when a track loads (see App shell § Theme).
    - Transport (previous / play-pause / next) is **flush left** at the page margin, circles spaced one diameter apart — never centred or spread across the pane. Every tap plays a full press: the circle nudges a few dp down-left, then eases back to rest (the motion always finishes, even on a short tap).
    - Scrubber is the **circle seek** (`MediaCircleSeekBar`), directly under the art and only as wide as the art: elapsed time flush left, `-remaining` flush right, 2dp hairline track at 20% foreground between them, white played segment, and a 14dp hollow white ring (3dp stroke, empty centre) as the thumb. Ring travel is inset by its radius, so the played segment stops at the ring's opening (References: `images/hub_nowplaying_dark_green.jpg`, `images/hub_nowplaying_compare_dark_unknown.jpg` right pane).
- **Navigation:** Swipe between hub panes. Collection links → pivot. Play → jump to now playing pane.
- **Reference:** `images/hub_fullpage.png`, `images/hub_nowplaying_dark_green.jpg`

### Page 2 — Collection hub pane

- **Layout:** Panorama title `collection`. Summary cards or short lists linking into pivots (recent plays, pin entry to artists/albums/songs). Keep typography-first; do not build a dense Material dashboard.
- **Navigation:** Tap artists/albums/songs/playlists/genres → Page 3. Recent item → now playing / album.
- **Reference:** `images/hub_nowplaying_compare_dark_unknown.jpg` (content density), `images/artists_showing_dark_teal.jpg` (showing pattern)

### Page 3 — Library pivot (artists | albums | songs | playlists | genres)

- **Layout:**
  - Pivot headers lowercase Metro style; active white, inactive grey with next-header peek.
  - `showing all music` / `showing on this device` / `showing youtube music` via `MetroShowingLabel` under headers.
  - Artists, albums, and songs all group under sticky accent letter markers (`#` section first, then `a`–`z`); tapping a marker opens the find-by-letter grid (`MetroJumpList`).
  - Rows are dense — closer together than the default 76/90dp list metrics — so more of the library fits per screen.
  - Albums: art thumbnail (square) + title + artist subtitle.
  - Songs: title + artist subtitle; optional trailing affordance reserved (WP download → unused for local).
  - Empty states: grey Metro copy when library empty or permission denied.
- **Navigation:** Tap artist → Page 4. Tap album → Page 5. Tap song → play + jump to now playing. Showing → filter menu (Page 7 pattern). Letter marker → jump grid → picked letter scrolls that section to the top.
- **Reference:** `images/artists_showing_dark_teal.jpg`, `images/showing_menu_dark_teal.jpg`, `images/song_row_download_dark_teal.jpg`

### Page 4 — Artist detail

- **Layout:** Artist name caps overline; pivot `songs` | `albums` | `about`.
  - **songs / albums:** accent section headers `in collection` (library tracks/albums) then `discover` (YouTube Music search results not already owned). Discover needs YT Music connected; otherwise show a connect hint.
  - **about:** YouTube Music artist description first; else Wikidata music entity (+ English Wikipedia when present). Reject same-name non-music wiki pages. Facts from Wikidata when available (`formed`, `origin`, `genres`).
- **Artists list subtitle:** `{n} songs discovered` (collection count for that artist).
- **Navigation:** Back → library pivot. Tap collection/discover song → play. Tap album → Page 5 (YT discover albums load remote tracks).
- **Reference:** `images/album_detail_dark_teal.jpg` (detail chrome). Discover/about sections: see `known-gaps.md`.

### Page 5 — Album detail

- **Layout:** Artist caps overline; album title; accent section headers `in collection` (library tracks for this album) then `discover` (YouTube Music album browse or search results not already owned).
- **Navigation:** Track tap → play that section’s queue from index. Back → previous. Discover albums with a YT browse id load full album tracks under discover when nothing is in collection.
- **Reference:** `images/album_detail_dark_teal.jpg`. Discover section: see `known-gaps.md`.

### Page 6 — Settings

- **Layout:** `SETTINGS` overline + `music` page title. Toggle **Connect to YouTube Music** (maps WP “Connect to streaming music”). Link **YouTube Music account** (opens connect WebView). Optional Sync now for library refresh.
- **Reference:** `images/settings_dark_teal.jpg`, `images/settings_sync_dark_unknown.jpg`

### Page 6b — Music directories

- **Layout:** `MetroMultiSelectList` (`FOLDERS` overline) — checkbox + folder glyph + path title (with song count). Lists every MediaStore folder that currently contributes local tracks. Checked = include in library; unchecked folders are excluded on save. New folders appear checked by default.
- **Navigation:** From hub **local** → music directories tile. Confirm (check) saves and reloads the local library; cancel / Back discards.
- **Reference:** Settings connected-apps picker pattern (`MetroMultiSelectList`); no WP8.1 capture — see `known-gaps.md`.

### Page 7 — Showing filter menu

- **Layout:** Full-page menu on the theme background: `FILTER BY:` section header, then a tight stack of choices (all music, on this device, youtube music) with the active filter in accent and the rest in primary text, then a bordered `cancel` button. Rows turnstile in (rotate about the left edge, 45ms stagger) when the menu opens.
- **Reference:** `images/showing_menu_dark_teal.jpg`, `images/showing_toggle_dark_teal.jpg`

### Page 8 — YouTube Music connect

- **Layout:** Full-screen WebView for Google / YouTube Music sign-in; on success persist session and return to Settings with toggle On.
- **Platform:** No official public YT Music SDK — see README platform exceptions.

### Page 9 — Explore / search (hub pane)

- **Layout:** `MetroTextBox` search field (WP8.1 light-fill square TextBox, accent border when focused) + result list (songs/artists) from YouTube Music when connected; otherwise prompt to connect.
- **Navigation:** Result → play streaming item in same now-playing chrome.

### Page 10 — Permission / empty library

- **Layout:** Metro empty / permission copy + border button to grant `READ_MEDIA_AUDIO` / storage.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `hub_fullpage.png` | 1 | Hub panorama: collection / get music / now playing / local + panoramic brand (capture shows three panes; local is metro-os addition using get-music tile chrome) |
| `nowplaying_dark_green.jpg` | 1 | Same capture alias |
| `hub_dark_green.jpg` | 1–2 | Hub landing alias |
| `hub_nowplaying_compare_dark_unknown.jpg` | 1 | Old vs new now-playing UI (Windows Club) |
| `artists_showing_dark_teal.jpg` | 3 | Artists + showing label |
| `pivot_artists_dark_teal.jpg` | 3 | Alias |
| `showing_toggle_dark_teal.jpg` | 3, 7 | Showing control callout |
| `showing_menu_dark_teal.jpg` | 7 | Filter choices |
| `album_detail_dark_teal.jpg` | 4–5 | Album/song detail chrome |
| `song_row_download_dark_teal.jpg` | 3, 5 | Song row + download affordance |
| `settings_dark_teal.jpg` | 6 | Streaming connect toggle |
| `settings_sync_dark_unknown.jpg` | 6 | Sync now |
| `recent_remove_dark_unknown.jpg` | 2 | Recent plays context menu |
| `start_music_tile_dark_blue.jpg` | — | Launcher Music live tile (context) |
| `marketing_lumia_outdoor.jpg` | — | Device era context only |
| `hub_collection_refresh_photo.jpg` | 1–2 | Photo of collection refresh UI |
| `collection_artists_dark_teal.jpg` | — | Marketing / hero duplicate — low fidelity for layout |

## Out of scope (v1)

- Xbox Music Pass purchase / Store checkout
- OneDrive music folder sync (replaced by YT Music library when signed in)
- Kid’s Corner, Cortana playlist voice
- Video / Music+Videos hub merge
- Perfect pixel Live Tile flip content (launcher owns tiles)
