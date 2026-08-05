# Music — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

Target: **Windows Phone 8.1 Xbox Music** (store app titled **Music**) on a portrait phone (768×1280 / xhdpi). Streaming catalog in metro-os maps Xbox Music Pass / OneDrive streaming → **YouTube Music**.

## App shell

- **Control model:** Wide **panorama** hub with left-to-right sections. Collection drill-ins use **pivot** (artists | albums | songs | playlists | genres).
- **Brand title:** Hub uses panoramic lowercase **`metro music`** (metro-os brand in place of WP8.1's `xbox music`; large Light / page-title scale). It is set at a fixed 96sp and bleeds off the right edge; the panorama parallax is derived from the measured overflow so the tail of the word is fully revealed by the last pane — **no** `MetroAppTitle` / `MUSIC` overline on the landing panorama. Drill-in pages (collection pivots, settings) may still use `MetroAppTitle` where appropriate.
- **Theme:** Black background (`#000000`). Accent from `MetroPreferences` (captures often teal/green). While a track is loaded, the **hub** background takes a darkened wash of the current **album** cover — WP8.1 faded the artist/album image behind the panorama (Reference: `images/hub_nowplaying_compare_dark_unknown.jpg`). Derive it from the album art, never the per-track thumbnail, keep it near-black so white type holds contrast, and return to `#000000` when playback is empty. Drill-in pages stay flat black.
- **App bar:** Minimized `…` on hub; expand for search/settings/sync. Transport on now-playing is **in-page** (circular prev / play / next), not Material mini-player.
- **No Material:** No FAB, snackbars, bottom sheets, cards, rounded album frames.

## WP 8.1 Music information architecture

| Panorama section (hub) | Role |
|------------------------|------|
| **collection** | artists / albums / songs / genres / playlists / radio links into pivots |
| **get music** | Discovery / YouTube Music connect + search (maps Store / Explore) |
| **now playing** | Large art, scrubber, up next, shuffle/repeat/queue glyphs, circular transport |

Collection list pages show **`showing <filter>`** (`MetroShowingLabel`) with filters: **all music** | **on this device** | **youtube music** (maps WP: All Music / On my phone / Streaming).

## Pages

### Page 1 — Hub landing (panorama)

- **Layout:**
  - Brand **`metro music`** — huge Light type, flush left, clips / bleeds right; scrolls with panorama offset until the tail is exposed (Reference: `images/hub_fullpage.png`). **Do not** put `MetroAppTitle("MUSIC")` on this surface.
  - Section titles via `MetroPanorama`: `collection` | `get music` | `now playing` (next title peeks).
  - **collection:** vertical lowercase links — artists, albums, songs, genres, playlists, radio.
  - **get music:** accent tiles (search / connect) + sync status + YT song rows when connected.
  - **now playing:** track + art + scrubber + up next + circular transport (Reference: `images/hub_nowplaying_dark_green.jpg`).
  - **Backdrop:** all three panes share one background; it washes from black to the darkened album colour when a track loads (see App shell § Theme).
    - Transport (previous / play-pause / next) is **flush left** at the page margin, circles spaced one diameter apart — never centred or spread across the pane.
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

- **Layout:** Artist name caps overline; pivot `songs` | `albums` (optional `bio` stub). Hero art optional. Song rows + play-on-art control.
- **Navigation:** Back → library pivot. Play → queue artist songs.
- **Reference:** `images/album_detail_dark_teal.jpg` (detail chrome / download link pattern)

### Page 5 — Album detail

- **Layout:** Square art with play overlay; album title; artist; track list; text action `download` only when source is streaming and offline cache is supported (v1: hide for local; YT Music may show unavailable stub).
- **Navigation:** Track tap → play album queue from index. Back → previous.
- **Reference:** `images/album_detail_dark_teal.jpg`

### Page 6 — Settings

- **Layout:** `SETTINGS` overline + `music` page title. Toggle **Connect to YouTube Music** (maps WP “Connect to streaming music”). Link **YouTube Music account** (opens connect WebView). Optional Sync now for library refresh.
- **Reference:** `images/settings_dark_teal.jpg`, `images/settings_sync_dark_unknown.jpg`

### Page 7 — Showing filter menu

- **Layout:** Full-page menu on the theme background: `FILTER BY:` section header, then a tight stack of choices (all music, on this device, youtube music) with the active filter in accent and the rest in primary text, then a bordered `cancel` button. Rows turnstile in (rotate about the left edge, 45ms stagger) when the menu opens.
- **Reference:** `images/showing_menu_dark_teal.jpg`, `images/showing_toggle_dark_teal.jpg`

### Page 8 — YouTube Music connect

- **Layout:** Full-screen WebView for Google / YouTube Music sign-in; on success persist session and return to Settings with toggle On.
- **Platform:** No official public YT Music SDK — see README platform exceptions.

### Page 9 — Explore / search (hub pane)

- **Layout:** Search field + result list (songs/artists) from YouTube Music when connected; otherwise prompt to connect.
- **Navigation:** Result → play streaming item in same now-playing chrome.

### Page 10 — Permission / empty library

- **Layout:** Metro empty / permission copy + border button to grant `READ_MEDIA_AUDIO` / storage.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `hub_fullpage.png` | 1 | Three-pane hub: collection / get music / now playing + panoramic xbox music brand |
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
