# Hub — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

**Note:** Hub is a **metro-os original** (not a WP8.1 inbox app). Layout language follows Xbox Music–style panorama (`MetroPanorama` + panoramic brand), not Material.

## Pages

### Page 1 — Hub panorama / home

- **Control model:** `MetroPanorama` with **2 panes** only (no third pane).
- **Brand:** Giant panoramic title `hub` (same ExtraLight treatment as Music’s `metro music` brand) — not a small app overline.
- **Pane title:** lowercase HubTitle `home` (next title peeks ~40dp).
- **Content:** HubLink-style list (`MetroListItem` + `MetroTextStyle.HubLink`):
  1. `all metro apps` → opens the suite APK list (latest GitHub release assets)
  2. `related projects` → stub until wired
  3. `get started` → **disabled / greyed** (v1)
  4. `github` → stub / external URL placeholder until wired
  5. `about project` → stub until wired
  6. `buy me a coffee` → stub until wired
- **App bar:** Minimized (ellipsis only) on panorama.
- **Background:** Theme background (dark/light). No Material cards.

### Page 2 — Quick links

- **Pane title:** lowercase HubTitle `apps` (peeks from page 1).
- **In-pane header:** Small section title `quick links` (`MetroTextStyle.SectionHeader`).
- **Content:** Accent square tiles (Music get-music tile language: 0dp corners, accent fill, label bottom-start), two-up row:
  - `core apps`
  - `shell apps`
  - `utility apps`
  - `productivity apps`
  - `3rd party apps`
  - `second party apps`
- Tapping a category opens a filtered APK list from the same latest-release catalog (empty categories show an empty state).
- **App bar:** Minimized.

### Page 3 — Suite apps list (drill-in, not a panorama pane)

- **Pattern:** Full page with `MetroAppTitle` + page title (e.g. `all metro apps` or category name).
- **Data:** Fetch `GET https://api.github.com/repos/god-s-perfect-idiot/metro-os/releases/latest`, map `.apk` assets to list rows.
- **Row:** `MetroListItem` — display name + optional subtitle (release tag / size / installed).
- **Tap:** Download APK to app cache → prompt install via package installer (`REQUEST_INSTALL_PACKAGES` + `FileProvider`).
- **Loading / error:** `MetroLoadingDots` / body error copy — no Material snackbars.
- **Back:** Returns to panorama hub.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `panorama_dark_teal.png` | Hub panorama | Layout reference copied from Music hub (panorama + brand + pane peek). Hub is metro-os original — see `known-gaps.md`. |

## Out of scope (v1)

- Page 3 panorama pane
- Wiring destinations for related projects / github / about / buy me a coffee (stubs OK)
- Enabling get started
- Real 3rd/second-party catalogs (tiles present; lists may be empty)
- In-app update of Hub itself mid-session UX polish beyond install intent
