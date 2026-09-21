# Hub — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

**Note:** Hub is a **metro-os original** (not a WP8.1 inbox app). Layout language follows Xbox Music–style panorama (`MetroPanorama` + panoramic brand), not Material.

## Pages

### Page 1 — Hub panorama / home

- **Control model:** `MetroPanorama` with **4 panes** (`home` → `apps` → `featured` → `local`).
- **Brand:** Giant panoramic title `hub` (same ExtraLight treatment as Music’s `metro music` brand) — not a small app overline.
- **Pane title:** lowercase HubTitle `home` (next title peeks ~40dp).
- **Content:** HubLink-style list (`MetroListItem` + `MetroTextStyle.HubLink`):
  1. `metro os apps` → opens **first-party** only (`first-party` Firestore; GitHub latest-release fallback)
  2. `related apps` → opens the second-party Firestore catalog (`second-party`)
  3. `unofficial metro apps` → opens the third-party Firestore catalog (`third-party`)
  4. `metro os github` → opens the public GitHub repo
  5. `extras+info` → opens **Page 6 — extras+info**
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
- **Section header:** Suite release tag (e.g. `alpha-8`, accent `SectionHeader`) on **first-party** lists only (`metro os apps`, core, shell). Related / unofficial (second/third party) lists omit it.
- **Data:** Fetch `GET https://api.github.com/repos/god-s-perfect-idiot/metro-os/releases/latest`, map `.apk` assets to list rows (Firestore catalog preferred when present).
- **Row:** Store-style icon + **title**, **description**, and **By:** publisher only (no version / size on the list).
- **Tap:** Opens **Page 5 — App detail**.
- **App bar:** Refresh.
- **Loading / error:** full-page `MetroLoadingScreen` whenever a blocking catalog load is in flight (`CatalogLoadMode.Loading`), including app-bar refresh of any hub group (first / second / third party). Foreground soft-sync may stay silent. Body error copy — no Material snackbars.
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
- **Support:** Section header `support` + Start-menu–style **2×2** tile grid (square tiles, 0dp corners, title bottom-start). Enter animation: slide in from the right in order (1,1) → (1,2) → (2,1) → (2,2) with a short stagger.
  1. `Entropy` — GitHub avatar bg, green face, **black** title → `https://buymeacoffee.com/godsperfectidiot`
  2. `Alexthew1` — People user icon, cobalt (midnight blue) face → `https://buymeacoffee.com/alexthew`
  3. `Cherryhoax` — GitHub avatar bg, purple face → `https://buymeacoffee.com/cherryhoax`
  4. `Cyanexani` — People user icon, red face → `https://buymeacoffee.com/anikethpani`
- **List:** Plain “Name value” rows after the tiles: latest GitHub release tag, channel, suite app count, publisher, platform.
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

### Page 8 — Local (panorama pane)

- **Pane:** Fourth panorama page after featured.
- **In-pane header:** Small section title `local` (`MetroTextStyle.SectionHeader`).
- **Tiles:** Music get-music–style accent tiles (0dp corners, accent fill, **centered icon**, label bottom-start), two-up row:
  1. `updater` → **Page 9 — Updater**
  2. `device` → **Page 10 — Device**
- **Suite list:** Below the tiles, section header `metro os apps` + installed **first-party** suite packages (Core/Shell catalog / `com.metro.*`). Same row language as Device (square icon, name, version/update description, circular download when an update is available).
- **App bar:** Minimized.

### Page 9 — Updater (drill-in)

- **Pattern:** Full page mirroring WP8.1 **Settings → phone update** (`references/images/phone_update_dark_red.png`): `MetroAppTitle` overline `SETTINGS` + large page title `updater`.
- **Section:** `Update status` (`SectionHeader`) + body copy naming the latest metro-os release tag and whether every first-party suite APK is installed at that release’s versions (missing or older count).
- **Link:** Underlined `Learn more.` → GitHub release page for that tag (or `/releases/latest` if unknown).
- **Action:** Bordered `update all` (`MetroBorderButton`) — enabled when any suite app is missing or outdated; **greyed** when all are installed and current. Downloads and installs every missing/outdated APK (one package-installer confirmation at a time).
- **Omit:** Notification checkboxes and preferred install time (out of scope).
- **App bar:** None.
- **Back:** Returns to panorama hub.

### Page 10 — Device (drill-in)

- **Pattern:** Full page with `MetroAppTitle` + page title `device`.
- **Content:** Only **installed** packages that appear in Hub first-party / second-party / third-party catalogs **and** have a catalog `versionName` or `versionCode`. Store-style square icon, name + version/update description, circular download when a catalog APK URL exists.
- **Tap update:** Downloads and installs that APK (same install pipeline as app detail).
- **App bar:** Refresh (re-scan installed packages against the catalog).
- **Back:** Returns to panorama hub.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `panorama_dark_teal.png` | Hub panorama | Layout reference copied from Music hub (panorama + brand + pane peek). Hub is metro-os original — see `known-gaps.md`. |
| `extras_info_dark_cyan.png` | extras+info | Authentic Lumia extras+info capture (cyan accent stand-in; Hub uses Metro Ruby crimson). |
| `phone_update_dark_red.png` | updater | Authentic WP8.1 Settings → phone update capture (omit toggles / install time in Hub). |

## Out of scope (v1)

- Empty 3rd-party catalog until curated docs exist
- In-app update of Hub itself mid-session UX polish beyond install intent
- Curated Firestore `explore` docs driving featured (v1 uses random picks from combined catalogs)
- Batch silent install of every outdated APK in one tap (platform requires per-APK confirmation)
