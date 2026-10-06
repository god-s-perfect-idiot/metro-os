# News

**Package:** `com.metro.news`  
**Tier:** 2

## Status

Android project implemented — Bing News–style panorama hub, RSS-backed categories, article summary drill-in, local topics toggles.

## App role

Recreates the WP8 **Bing News** experience: a panoramic hub with a full-bleed top story, swipeable headline/category panes, and a simple article surface. Content is loaded from public RSS feeds (greenfield Metro UI — not a forked Material news app).

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Network permission for RSS; offline sample fallback required

## Screen inventory

### 1. Top story hero

- Full-bleed featured story under panoramic `news` brand
- Expected reference: `references/images/top_stories_dark.png`

### 2. Headlines / category panes

- Thumbnail + title lists for headlines, world, technology, business
- Expected reference: `references/images/headlines_dark.png`

### 3. Article summary

- Hero, headline, byline, RSS summary; open/share via app bar
- Expected reference: `references/images/article_dark.png`

### 4. Topics / sources

- Enable/disable category feeds locally
- Expected reference: `references/images/topics_dark.png`

## System functions and contracts

- Fetch RSS over HTTPS (OkHttp)
- Cache last-good feed XML in app files for offline
- Open full article with `Intent.ACTION_VIEW` (browser / default handler)
- Theme/accent via `MetroPreferences` / `MetroSystemTheme`
- No cross-app imports; no Material components

## UI and interaction guardrails

- `MetroPanorama` + `MetroPanoramaBrandEnter` / `MetroPanoramaBodyEnter`
- Drill-ins through `MetroSubpageHost`
- Square imagery (no rounded cards); bottom `MetroAppBar` only
- `Modifier.metroNavBarPadding()` on shell root

## Data and state model

- `NewsStory`, `NewsCategory`, `NewsFeedSnapshot`
- Route: `Hub` | `Article(id)` | `Topics` | `Sources`

## Primary implementation order

1. RSS parser + repository (with sample fallback)
2. Panorama hub (hero + category lists)
3. Article summary subpage
4. Topics/sources toggles
5. Refresh + open/share app-bar actions

## Test-critical user flows

1. Launch → load (or sample) stories into hub
2. Swipe categories; tap story → article
3. Open article in external viewer; Back returns to hub
4. Toggle a topic off → category pane empties / hides feed

## Reference and golden expectations

- `references/images/top_stories_dark.png`
- `references/images/headlines_dark.png`
- `references/images/article_dark.png`
- `references/images/topics_dark.png`

## Commands

```bash
cd apps/news

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh news
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Bing News cloud + partner full-text articles | No Bing News API / licensing | Public RSS summaries + `ACTION_VIEW` for full story |
| Live tile flipping top story photo | Suite tile pipeline not wired for news yet | Static Start glyph (`metro_app_news`) |
| Microsoft account topic sync | Out of v1 scope | Local SharedPreferences toggles only |

## Agent postmortem

_None._
