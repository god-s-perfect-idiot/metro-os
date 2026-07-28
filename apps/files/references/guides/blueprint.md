# Files — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Pages

### Page 1 — Filter pivots + browse list

WP8.1 Files is a folder browser over phone / SD storage. Metro-os adds **content-type pivots** (per `README.md` / `scope.md`) as filters over the same browse tree.

- **Layout**
  - Black/white theme background (system theme)
  - `MetroAppTitle` “files” (ALL CAPS via toolkit)
  - `MetroPivot` headers (max 7): **all**, **documents**, **music**, **pictures**, **videos**
  - Under the pivot strip: current path as plain secondary text (e.g. `phone > Pictures`), flush left, 12dp horizontal margin
  - Vertical `MetroListItem` list: folders first (A–Z), then files (A–Z)
  - Leading 48dp square tile: volumes = accent fill with phone / SD card glyph; folders = accent fill + child-count badge; files = gray tile with document/media glyph + type badge (W/X/P/N/PDF)
  - Folder row title = name; subtitle = child count (e.g. `12 items`) when known
  - File row title = name; subtitle = `date · size` (e.g. `5/30/2014 · 1.2 MB`)
  - Empty: `MetroEmptyState` (“no files”, “folder is empty”, or permission message)
- **Navigation**
  - At storage root (`path == null`): list available volumes (`phone`, `sd card` when present)
  - Tap folder → push path; Back → pop path (or exit app at root)
  - Switching pivots keeps the current folder; only file visibility changes
  - Type pivots always show folders; files must match the active filter
- **Interactions**
  - Tap file → open with system `ACTION_VIEW` (respective app / chooser)
  - No Material drawers, cards, or bottom sheets
  - App bar (bottom): optional overflow only in later versions; v1 may omit actions other than open
- **Background:** solid theme background (no wallpaper)

### Page 2 — Storage permission gate

Shown when the app cannot list shared storage.

- **Layout:** `MetroAppTitle` + hub-style title (“files”) + body explaining all-files / storage access + `MetroBorderButton` to grant
- **Navigation:** stays until access is granted (or user leaves)
- **Interactions:** button launches system all-files / storage permission UI

### Page 3 — Open failure (inline, not a separate route)

When no app can handle a file mime type, show secondary text under the list or replace empty area briefly — never a Material snackbar. Prefer a simple message dialog only if already using toolkit dialogs; otherwise keep an in-list / empty-state message.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `pivots_dark_blue.png` | Page 1 | Pivot headers + list (gap — see `known-gaps.md`) |
| `list_dark_blue.png` | Page 1 | Folder path + file rows with date/size (gap) |
| `permission_dark_blue.png` | Page 2 | Access gate (gap) |

## Out of scope (v1)

- Copy / move / rename / delete / new folder
- Multi-select and share
- Search
- Cloud / OneDrive upload targets
- Hidden files
- Edit in place
