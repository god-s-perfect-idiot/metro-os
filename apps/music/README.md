# Music

**Package:** `com.metro.music`  
**Tier:** 1

## Status

Harness docs only — Android project not scaffolded yet. This README is the implementation guide for the Xbox Music-style app.

## App role

This app recreates the WP8.1 **Music** experience: a hub-like collection surface, artists/albums/songs pivots, now playing, and a local-library-first playback flow.

The app is not a generic Android media player. The visual and navigational target is Xbox Music on Windows Phone, with Metro typography, hero imagery, horizontal content organization, and restrained chrome.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Media scanning and playback architecture chosen before UI polish

## Screen inventory

### 1. Collection hub

- Entry surface with hero artwork and content summaries
- Should function as a WP-style hub, not a dense recycler-first dashboard
- Expected reference: `references/images/hub_dark_blue.png`

### 2. Artists / Albums / Songs pivot

- Primary library browsing surface
- Pivot headers must remain concise and Metro-styled
- Expected reference: `references/images/pivot_dark_blue.png`

### 3. Now playing

- Focused playback screen with large artwork, progress control, and transport actions
- Expected reference: `references/images/nowplaying_dark_blue.png`

## System functions and contracts

### Library source

- Use `MediaStore` to scan local audio in v1
- Do not assume cloud/Xbox catalog support
- Keep scanning logic testable and separate from UI

### Playback stack

- Repository for media catalog
- Playback service / media session layer
- UI state model for active queue, selected item, playback position, and buffering state

### Navigation semantics

- Hub -> pivoted library surfaces -> now playing
- Entering now playing from a song should preserve queue context
- Background playback must remain available when leaving the foreground UI

### Notification / background contract

- Android requires background media notification behavior
- Keep notification minimal and as visually close to Metro as Android permits

## UI and interaction guardrails

- Use `MetroHub`, `MetroListItem`, `MetroSlider`, and shared toolkit primitives where available
- Large art and strong typography should dominate now playing
- Do not introduce Material mini-player bars or card-based album layouts unless the reference requires similar grouping
- Pivot count should stay restrained
- Playback controls belong in Metro app chrome, not floating controls

## Data and state model

- `Artist`, `Album`, `Song`, `Playlist`, `PlaybackQueueItem`
- Persist last queue, last active item, and last playback position when reasonable
- Decouple scanned library entities from UI list sections for sorting and grouping flexibility

## Primary implementation order

1. Define `MediaStore` query layer and data models
2. Build collection hub and pivot browsing
3. Build playback service and now-playing state
4. Wire play/pause/seek and queue transitions
5. Add background playback notification and resume behavior
6. Add playlist surface only after library and now playing are stable

## Test-critical user flows

1. Scan local audio and populate pivots
2. Open song and start playback
3. Play/pause/seek reliably
4. Leave app and continue background playback
5. Return to now playing with correct queue state

## Reference and golden expectations

- `references/images/hub_dark_blue.png`
- `references/images/pivot_dark_blue.png`
- `references/images/nowplaying_dark_blue.png`
- `screenshots/golden/hub_dark_blue.png`
- `screenshots/golden/nowplaying_dark_blue.png`

## Commands

```bash
cd apps/music

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh music
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Xbox cloud music ecosystem | Out of current v1 scope | Support local files only and preserve the Metro information architecture |

## Agent postmortem

_None._
