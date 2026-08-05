# Music

**Package:** `com.metro.music`  
**Tier:** 1

## Status

Android project implements WP 8.1 **Xbox Music**–style player: local MediaStore library + YouTube Music connect/playback, per `references/guides/blueprint.md`.

## App role

Recreates the WP8.1 **Music** (Xbox Music) experience: panorama hub (now playing → collection → radio → explore), artists/albums/songs/playlists/genres pivots, now playing with Metro transport, and a **Showing** filter for local vs streaming.

**Streaming stand-in:** Xbox Music Pass / OneDrive streaming is mapped to **YouTube Music** (connect account, browse/search, play). Local files remain first-class via `MediaStore`.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Media scanning and Media3 playback before UI polish

## Screen inventory

Authoritative spec: [`references/guides/blueprint.md`](references/guides/blueprint.md)

| Screen | Reference |
|--------|-----------|
| Hub / Now playing | `references/images/hub_nowplaying_dark_green.jpg` |
| Artists + showing | `references/images/artists_showing_dark_teal.jpg` |
| Album detail | `references/images/album_detail_dark_teal.jpg` |
| Settings / connect streaming | `references/images/settings_dark_teal.jpg` |

## System functions and contracts

### Library source

- Local: `MediaStore.Audio` (`READ_MEDIA_AUDIO` / storage)
- Streaming: YouTube Music session after connect (Innertube-style client; no official YT Music SDK)
- `Showing` filter: all | on this device | youtube music

### Playback stack

- Media3 `MediaSessionService` + ExoPlayer
- Unified queue for local URIs and YT Music stream URLs
- System media notification (minimal Metro-compatible)

### Navigation

- Hub panorama → collection pivots → detail → now playing
- Background playback when leaving UI

## UI and interaction guardrails

- `MetroPanorama`, `MetroPivot`, `MetroListItem`, `MetroShowingLabel`, `MetroAppBar`, `MetroToggleSwitch`
- Collection pivots group under `MetroLetterTile` markers and open `MetroJumpList`; rows use the dense metrics in `MusicListRow`
- Large art + circular transport on now playing; no Material mini-player
- Now-playing scrubber is the app-local `MediaCircleSeekBar`: hollow ring thumb on a 2dp hairline
  track, elapsed and remaining times flanking it, row sized to the album art width. The rectangular
  `MetroSlider` thumb is wrong on this surface — keep it for settings and volume, and promote the
  circle seek into `metro-ui-android` only once a second app needs a media scrubber.
- Swipe up/down on art for next/prev track
- Hub backdrop follows the loaded track's **album** cover (not the per-track thumbnail): `AlbumTintLogic`
  takes the art's dominant hue and crushes its value to a near-black wash, crossfaded in over
  `MetroTransitions.PageTransitionMs`. Black background returns when nothing is loaded, and the wash
  is skipped in light theme where white type would invert. Drill-in pages stay flat black.

## Data and state model

- `Song`, `Album`, `Artist`, `Playlist`, `PlaybackQueueItem`, `LibrarySource` (Local | YouTubeMusic)
- Persist last queue position when reasonable
- YT Music cookies/session in app-private prefs

## Primary implementation order

1. MediaStore query layer + models
2. Hub panorama + collection pivots
3. Playback service + now playing
4. YouTube Music connect + search/play
5. Settings Showing / connect toggles
6. Background notification

## Test-critical user flows

1. Grant audio permission; scan local library into pivots
2. Play local song; scrub; next/prev
3. Background playback continues
4. Connect YouTube Music; search; play streaming track in same now-playing UI
5. Showing filter switches local vs YT vs all

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
| Xbox Music Pass / OneDrive streaming catalog | Service shut down; no Xbox API | YouTube Music as streaming backend |
| Official Xbox Music client APIs | None on Android | WebView Google sign-in + Innertube client for library/search/player |
| SD Card as distinct Showing filter | Scoped storage | Fold into “on this device” |
| System media volume HUD | Owned by `com.metro.volume` | Do not draw a second volume chrome |
| Perfect Live Tile flips | Launcher owns tiles | Optional now-playing metadata broadcast later |
| Direct stream URLs for a streamed track | Innertube refuses YouTube Music art tracks to a player client with no visitor identity, and `googlevideo` rate limits unbounded reads and 403s range-less requests | Player walks ANDROID_VR → IOS → WEB_REMIX carrying a cached `visitorData`, and `ChunkedDataSource` reads in bounded 512 KiB ranges. See `references/known-gaps.md` for the measured client matrix |

## Agent postmortem

_None._
