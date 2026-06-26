# Photos — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

Target: **Windows Phone 8.1** Photos app on a portrait phone (768×1280 / xhdpi reference profile).

## App shell

- **Navigation model:** Pivot collection (`all` | `albums` | `favorites`) with drill-down to album grid and full-screen viewer.
- **Theme:** Black background (`#000000`) on all surfaces.
- **Typography:** Noto Sans stand-in for Segoe WP. Pivot headers lowercase, large (PivotTab style).
- **No Material:** No FAB, rounded photo cards, snackbars, or bottom sheets.

## Pages

### Page 1 — Collection (landing / pivot hub)

- **Layout:**
  - Small caps label `PHOTOS` optional; primary navigation is pivot headers flush-left.
  - **Pivot tabs:** `all` | `albums` | `favorites` (max 3, lowercase).
  - **All tab:** 4-column square thumbnail grid with thin black gutters (~2dp). Photos grouped under month headers (`MMMM yyyy`, accent-colored, 20sp SemiBold).
  - **Albums tab:** Vertical list of square album tiles (~45% screen width). Each tile shows cover thumbnail or solid `#333333` placeholder; album name bottom-left in small white text. `Camera Roll` is the default bucket.
  - **Favorites tab:** Same 4-column grid as All, filtered to user-favorited items only.
  - Empty states per tab when no media / no favorites.
- **Navigation:** Tap thumbnail → Page 3 (viewer). Tap album tile → Page 2 (album detail). Horizontal flick switches pivot.
- **Background:** Solid black.
- **Reference:** `images/hub_dark_blue.jpg` (camera-roll grid), `images/pivot_albums_dark_blue.jpg` (WP8.1 albums pivot).

### Page 2 — Album detail

- **Layout:** Page header = album name (24sp). 4-column thumbnail grid of album contents, newest first.
- **Navigation:** Back → collection on same pivot (`albums`). Tap thumbnail → viewer scoped to album.
- **Background:** Black.

### Page 3 — Photo viewer

- **Layout:** Full-bleed image centered on black. No rounded corners. Minimal chrome overlay (optional fade on tap).
- **Interactions:** Horizontal flick moves to prev/next photo in current collection. Tap toggles chrome. System Back → collection preserving pivot and scroll context.
- **Background:** Black.
- **Reference:** No high-fidelity WP8.1 capture yet — see `known-gaps.md`. WP8.1 auto-enters full-screen mode (search key disabled).

### Page 4 — Permission gate

- **Layout:** Full-page explanation + `allow access` (requests media read) + `continue without photos` (empty collection).
- **Navigation:** Grant → reload MediaStore. Skip → empty pivots with explanatory copy.
- **Note:** Android-only screen; no WP8.1 source.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `hub_dark_blue.jpg` | Page 1 — all tab | Camera roll 4-column grid + bottom app bar |
| `pivot_albums_dark_blue.jpg` | Page 1 — albums tab | WP8.1 SDK leak — albums pivot, local + online tiles |
| `pivot_date_dark_blue.jpg` | Page 1 — date grouping reference | Month header + 4-column grid (WP8 era; date grouping applies to All tab) |
| `pivot_date_grouped_dark_blue.jpg` | Page 1 — month jump reference | Collapsed month list pattern |

## Data model (implementation hint)

- `PhotoItem(id, uri, displayName, bucketId, bucketName, dateTakenMs, mimeType)`
- `AlbumGroup(bucketId, name, coverUri, count)`
- `DateGroup(label, photos)`
- `PhotosRoute { Collection, AlbumDetail(bucketId), Viewer(collectionKey, index) }`
- `CollectionPivot { All, Albums, Favorites }`

## System integration

- `READ_MEDIA_IMAGES` (API 33+) / `READ_EXTERNAL_STORAGE` (API ≤32) for `MediaStore.Images`.
- Tile widget provider: recent-photo grid via `MetroTilePhotoGrid` (accent fallback cells when empty).
- Favorites persisted in app `SharedPreferences` (local IDs).

## Out of scope (v1)

- OneDrive / Facebook online albums (WP8.1 links out to apps)
- Multi-select delete / share
- Video playback
- Photos+camera settings page
- Cloud sync
