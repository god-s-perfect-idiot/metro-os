# News — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

Target: **Windows Phone 8 Bing/MSN News** hub on a portrait phone (768×1280 / xhdpi). WP8.1 kept the same panorama/hub language; this app follows that Bing News surface, not a Material news reader.

## Platform adaptation (v1)

| WP8.1 / Bing News | metro-os v1 |
|-------------------|-------------|
| Bing News cloud + partner article bodies | Public RSS feeds (BBC World / Tech / Business + NPR top) |
| Microsoft account sync of topics/sources | Local topic enable list only |
| Live tile flipping top story | Out of v1 (static Start glyph) |
| In-app partner article reader | Summary page + open full article via `ACTION_VIEW` |

## App shell

- **Control model:** `MetroPanorama` hub with giant lowercase brand `news` (Music/Hub language).
- **Theme:** System dark/light + accent via `MetroSystemTheme` / `MetroPreferences`.
- **App bar:** Minimized `…` on the panorama; **refresh** + menu **topics** / **sources**.
- **Drill-ins:** `MetroSubpageHost` for article + topics/sources.
- **No Material:** No cards, chips, FAB, bottom sheets, or snackbars.

## Pages

### Page 1 — Top story hero (panorama pane 0)

- **Layout:**
  - Full-bleed hero image filling the pane (square corners).
  - Bottom overlay: large white headline (wrap OK on hero only), then SOURCE · RELATIVE TIME in smaller caps.
  - No pane title (blank panorama title so the image reads edge-to-edge under the brand).
- **Navigation:** Tap hero → Page 4 (article). Swipe → Page 2.
- **Interactions:** Refresh reloads RSS; offline falls back to last-good / sample bundle.
- **Reference:** `images/top_stories_dark.png`, collage `images/bing_news_screens_dark.png`

### Page 2 — Headlines (panorama pane 1)

- **Layout:**
  - Panorama title `headlines` only (no secondary section band like `TOP STORIES >`).
  - Vertical list of stories: optional square thumbnail (left) + title + `source · time` subtitle.
- **Navigation:** Tap row → Page 4. Swipe to world / technology / business panes.
- **Reference:** `images/headlines_dark.png`

### Page 3 — Category panes (panorama panes 2–4)

- **Layout:** Same list language as headlines; panorama titles only (`world`, `technology`, `business`) — no secondary `WORLD >` band.
- **Data:** One RSS feed per pane.
- **Reference:** `images/headlines_dark.png` (same list chrome; category titles differ)

### Page 4 — Article summary

- **Layout:**
  - Optional hero image (full width, square).
  - Large headline, then author/source/date secondary line.
  - Body = RSS description / summary (plain text).
  - Bottom app bar: **open** (browser / default viewer) + **share**.
- **Navigation:** Back exits via `MetroSubpageHost` pivot-out to the hub.
- **Reference:** `images/article_dark.png`

### Page 5 — Topics / sources

- **Layout:**
  - Page title `topics` (or `sources` when opened from that menu entry).
  - List of feed categories with on/off via `MetroToggleSwitch` (local preference).
  - Source rows show feed display name + short subtitle (no commerce / account).
- **Navigation:** App-bar menu from hub; Back returns to hub.
- **Reference:** `images/topics_dark.png`

## Images

| Image | Page | Notes |
|-------|------|-------|
| `top_stories_dark.png` | 1 | Full-bleed hero + headline overlay |
| `headlines_dark.png` | 2–3 | Category headline list with thumbnails |
| `article_dark.png` | 4 | Article reader summary |
| `topics_dark.png` | 5 | Sources / topics lists |
| `bing_news_screens_dark.png` | 1–5 | Windows Central multi-screen collage |

## Out of scope (v1)

- Bing account sign-in / cross-device topic sync
- Breaking-news push alerts
- Live tile photo flip
- Video stories
- Full offline article archive
