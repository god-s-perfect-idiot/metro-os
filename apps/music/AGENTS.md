# Agent instructions — Music (`com.metro.music`)

**Tier 1** | Package: `com.metro.music`

Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

**Xbox Music** — artists / albums / songs pivots, now playing, playlists hub.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| Toolkits verified | Yes |
| Tier 0 shell passes verify | **Yes** |

## Screens

| Screen | Pattern | Reference |
|--------|---------|-----------|
| Collection hub | `MetroHub` | `references/images/hub_dark_blue.png` |
| Artists / Albums / Songs | Pivot | `references/images/pivot_dark_blue.png` |
| Now playing | Full page | `references/images/nowplaying_dark_blue.png` |

## WP8.1 rules

- Hub with hero album art at top
- Pivot for artists, albums, songs (max 5 items)
- Now playing: large art, progress `MetroSlider`, play/pause in app bar
- Scan local audio via `MediaStore` only v1
- List items use `MetroListItem` tilt

## Primary flows

1. Scan library; populate pivots
2. Tap song → now playing
3. Play/pause/seek
4. Background playback with system media notification (minimal WP-styled)

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
| Xbox Music cloud | Out of scope | Local files only |
