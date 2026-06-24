# Store

**Package:** `com.metro.store`  
**Tier:** 2

## Status

Harness docs only — Android project not scaffolded yet. This README is the implementation guide for the v1 Metro store shell.

## App role

This app recreates the WP8.1 **Store** shell as an app-discovery surface for metro-os. In v1, it can be a stubbed catalog that showcases installed or curated Metro apps without implementing real commerce or update infrastructure.

The goal is the Metro browsing experience and information architecture, not a full app-store backend.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Static catalog or installed-app discovery strategy documented

## Screen inventory

### 1. Store panorama / discovery hub

- Category-led browsing experience
- Expected reference: `references/images/panorama_dark_blue.png`

### 2. Featured apps list

- Curated or discovered list of Metro apps
- Expected reference: `references/images/featured_dark_blue.png`

### 3. App detail surface

- Optional in v1, but recommended if the browsing model needs drill-down
- Expected reference: `references/images/detail_dark_blue.png`

## System functions and contracts

- May use static metadata in v1
- May link to installed Metro apps directly for “open” behavior
- Do not imply real purchases, account systems, or update pipelines unless they truly exist
- Keep the catalog source isolated so a future backend can replace static data cleanly

## UI and interaction guardrails

- Prefer panorama or hub presentation over dense store grids
- Keep promo surfaces flat and Metro-like
- No Material cards, banners, or bottom-sheet detail reveals
- App bar actions should stay minimal

## Data and state model

- `StoreCategory`, `StoreListing`, `StoreDetail`, `InstallState`
- Track whether a listing is installed and the action that should result: open, learn more, or unavailable

## Primary implementation order

1. Define static/discovered listing source
2. Build panorama/hub shell
3. Build featured/category lists
4. Add optional detail surface
5. Wire installed-app open behavior

## Test-critical user flows

1. Browse categories/featured content
2. Open installed Metro app from a listing
3. Handle unavailable/non-installed listings gracefully

## Reference and golden expectations

- `references/images/panorama_dark_blue.png`
- `references/images/featured_dark_blue.png`
- `references/images/detail_dark_blue.png`

## Commands

```bash
cd apps/store

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh store
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Real Windows Phone Store backend, purchases, and updates | Out of current v1 scope | Ship a stubbed Metro discovery shell with static or local app metadata only |

## Agent postmortem

_None._
