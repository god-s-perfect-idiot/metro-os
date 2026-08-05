# Agent instructions — Music (`com.metro.music`)

**Tier 1** | Package: `com.metro.music`

Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

**Xbox Music** — panorama hub (now playing / collection / radio / explore), artists/albums/songs pivots, now playing, playlists. Streaming = **YouTube Music**.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| Toolkits verified | Yes |
| Tier 0 shell passes verify | **Yes** |

## Screens

| Screen | Pattern | Reference |
|--------|---------|-----------|
| Hub / Now playing | `MetroPanorama` + panoramic `metro music` brand (no app overline) | `references/images/hub_fullpage.png` |
| Artists / Albums / Songs | `MetroPivot` + `MetroShowingLabel` | `references/images/artists_showing_dark_teal.jpg` |
| Album / artist detail | Full page | `references/images/album_detail_dark_teal.jpg` |
| Settings / YT connect | Full page | `references/images/settings_dark_teal.jpg` |

## WP8.1 rules

- Hub panorama; collection uses pivots (≤ 5 primary: artists, albums, songs, playlists, genres)
- Now playing: large square art, `MediaCircleSeekBar` scrubber (art-width, elapsed/remaining times on either side, hollow ring thumb — not `MetroSlider`), circular prev/play/next, up next line
- Swipe art up = next, down = previous
- `showing …` filter via `MetroShowingLabel`
- Local via MediaStore; streaming via YouTube Music connect
- List items use `MetroListItem` tilt

## Primary flows

1. Scan local library; populate pivots
2. Tap song → now playing
3. Play/pause/seek + background notification
4. Settings → connect YouTube Music → search/play in explore or Showing=youtube music

## Golden screenshots

```
screenshots/golden/hub_dark_blue.png
screenshots/golden/nowplaying_dark_blue.png
```

## Verify

```bash
../../scripts/verify-app.sh music
```

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Xbox Music cloud | Defunct | YouTube Music |
