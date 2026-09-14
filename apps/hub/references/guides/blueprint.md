# Hub — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

**Note:** Hub is a **metro-os original** (not a WP8.1 inbox app). Layout language follows Xbox Music–style panorama (`MetroPanorama` + panoramic brand), not Material.

## Pages

### Page 1 — Hub panorama / home

- **Control model:** `MetroPanorama` with **3 panes** (`home` → `apps` → `featured`).
- **Brand:** Giant panoramic title `hub` (same ExtraLight treatment as Music’s `metro music` brand) — not a small app overline.
- **Pane title:** lowercase HubTitle `home` (next title peeks ~40dp).
- **Content:** HubLink-style list (`MetroListItem` + `MetroTextStyle.HubLink`):
  1. `metro os apps` → opens **first-party** only (`first-party` Firestore; GitHub latest-release fallback)
  2. `related apps` → opens the second-party Firestore catalog (`second-party`)
  3. `unofficial metro apps` → opens the third-party Firestore catalog (`third-party`)
  4. `get started with os` → **disabled / greyed** (v1)
  5. `metro os github` → opens the public GitHub repo
  6. `extras+info` → opens **Page 6 — extras+info**
- **App bar:** Minimized (ellipsis only) on panorama; expand reveals **search** (opens Page 7).
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

### Page 3 — Featured apps (panorama pane)

- **Pane title:** lowercase HubTitle peeks from page 2 (brand-only chrome; in-pane section header).
- **In-pane header:** Small section title `featured apps` (`MetroTextStyle.SectionHeader`).
- **Content:** Store-style list of **4 random apps** drawn from the combined Firestore catalog (`first-party` + `second-party` + `third-party`; GitHub latest-release fallback when Firestore is empty). Picks are stable for the session until a cold re-pick (empty featured cache after full catalog load).
- **Row:** Same Store-style rows as Page 4 (icon + title + description + By).
- **Tap:** Opens **Page 5 — App detail** (back returns to the panorama hub).
- **App bar:** Minimized.
- **Empty / loading:** Body copy only — no Material snackbars; silent while a soft catalog refresh runs over existing picks.

### Page 4 — Suite apps list (drill-in, not a panorama pane)

- **Pattern:** Full page with `MetroAppTitle` + page title (e.g. `metro os apps` or category name).
- **Data:** Fetch `GET https://api.github.com/repos/god-s-perfect-idiot/metro-os/releases/latest`, map `.apk` assets to list rows (Firestore catalog preferred when present).
- **Row:** Store-style icon + **title**, **description**, and **By:** publisher only (no version / size on the list).
- **Tap:** Opens **Page 5 — App detail**.
- **App bar:** Refresh.
- **Loading / error:** full-page `MetroLoadingScreen` only while the catalog is empty and a blocking load is in flight (`CatalogLoadMode.Loading`). Refresh / navigation / foreground soft-sync over an already-populated list must stay silent — never overlay dots or a loader on existing rows. Body error copy — no Material snackbars.
- **Back:** Returns to panorama hub.

### Page 5 — App detail (drill-in)

- **Pattern:** Full page with `MetroAppTitle` + page title = app display name.
- **Content:** Icon + title + publisher; full description; version; download size; category.
- **App bar:** Store-style centered text buttons `download` + `share` (`MetroAppBarTextButton`). Download installs the APK (`REQUEST_INSTALL_PACKAGES` + `FileProvider`); share sends the app GitHub URL (Firestore `githubRepo`, else the suite repo). Download disabled while a download is in progress.
- **Loading:** Inline `MetroLoadingDots` + “Downloading…” on the detail body while the APK downloads.
- **Back:** Returns to the previous surface (suite list, search, or panorama hub).

### Page 6 — extras+info (drill-in)

- **Pattern:** Full page (not panorama). Large page title `extras+info` (no app overline). Layout follows Lumia **extras+info** (`references/images/extras_info_dark_cyan.png`).
- **Software release:** Section header `Software release`; large accent name **`Metro Ruby`** in **ruby / crimson** (`MetroColors.AccentCrimson`) with a circular `i` glyph — alphas for metro-os are branded Metro Ruby.
- **Link:** Underlined `source on github` → GitHub repo.
- **Body:** Short project blurb (metro-os / WP8.1 on Android / Metro Ruby channel).
- **List:** First action is a bordered **`buy me a coffee`** button → `https://buymeacoffee.com/godsperfectidiot`. Following lines are plain “Name value” rows: latest GitHub release tag, channel, suite app count, publisher, platform.
- **Footer:** Bordered **`more info`** button → latest release tag URL (or repo if tag unknown).
- **App bar:** None (content + buttons only).
- **Back:** Returns to panorama hub.

### Page 7 — Search (drill-in)

- **Pattern:** Full page like Music **explore** — `MetroAppTitle` + HubTitle `search` + focused `MetroTextBox` (placeholder `search`).
- **Entry:** Panorama app-bar search icon.
- **Data:** Filters the full suite catalog (Firestore preferred / GitHub fallback) by display name, description, publisher, package, or APK asset name. Blank query shows hint copy (no rows); non-matching query shows empty-state copy.
- **Row:** Same Store-style rows as Page 4; tap opens **Page 5 — App detail** (back returns to search).
- **Loading:** Full-page `MetroLoadingScreen` only while the catalog is empty and a blocking load is in flight.
- **App bar:** None on the search page (textbox is the chrome).
- **Back:** Returns to panorama hub.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `panorama_dark_teal.png` | Hub panorama | Layout reference copied from Music hub (panorama + brand + pane peek). Hub is metro-os original — see `known-gaps.md`. |
| `extras_info_dark_cyan.png` | extras+info | Authentic Lumia extras+info capture (cyan accent stand-in; Hub uses Metro Ruby crimson). |

## Out of scope (v1)

- Enabling get started
- Empty 3rd-party catalog until curated docs exist
- In-app update of Hub itself mid-session UX polish beyond install intent
- Curated Firestore `explore` docs driving featured (v1 uses random picks from combined catalogs)
