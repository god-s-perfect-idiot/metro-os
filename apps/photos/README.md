# Photos

**Package:** `com.metro.photos`  
**Tier:** 2

## Status

Harness docs only — Android project not scaffolded yet. This README is the implementation brief for the WP8.1 photos hub.

## App role

This app recreates the WP8.1 **Photos** experience: a hub-like landing page, album/date browsing pivots, and a full-screen viewer rooted in local device media.

The desired feel is large imagery, strong horizontal organization, and minimal chrome. It should not become a generic Material gallery app.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Tier 1 shell-adjacent interaction patterns understood before Tier 2 polish begins

## Screen inventory

### 1. Photos hub

- Landing surface with key entry points and featured recent imagery
- Expected reference: `references/images/hub_dark_blue.png`

### 2. Albums / date pivots

- Main content browsing surfaces
- Pivot should separate organizational views cleanly
- Expected reference: `references/images/pivot_dark_blue.png`

### 3. Photo viewer

- Focused image viewing surface with minimal Metro chrome
- Expected reference: `references/images/viewer_dark_blue.png`

## System functions and contracts

- Query local images using `MediaStore`
- Organize data into date-oriented and album-oriented sections
- Keep viewer state independent from collection state so deep links and back behavior remain predictable
- If sharing or set-as-wallpaper actions are added later, route them through app bar rather than Material sheets

## UI and interaction guardrails

- Use `MetroHub` / `MetroPivot` patterns instead of tabs or drawers
- Imagery should be full-bleed where the reference allows it
- No rounded Material cards or shadow-heavy albums
- Viewer chrome should be sparse and theme-aware

## Data and state model

- `PhotoItem`, `AlbumGroup`, `DateGroup`
- Track current viewer index, current grouping mode, and recently viewed state if useful

## Primary implementation order

1. Build `MediaStore` image repository
2. Build photos hub
3. Build album/date pivots
4. Build viewer with correct back behavior
5. Add optional lightweight app bar actions

## Test-critical user flows

1. Load local media collection
2. Switch between grouping pivots
3. Open image viewer from each grouping
4. Navigate back without losing pivot context

## Reference and golden expectations

- `references/images/hub_dark_blue.png`
- `references/images/pivot_dark_blue.png`
- `references/images/viewer_dark_blue.png`

## Commands

```bash
cd apps/photos

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh photos
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Phone-integrated cloud photo ecosystems | Out of current v1 scope | Focus on local device media while preserving WP8.1 layout and motion patterns |

## Agent postmortem

_None._
