# Launcher — known gaps

Blueprint: [`guides/blueprint.md`](guides/blueprint.md)

## Closed

| Item | Fix |
|------|-----|
| 6-column grid | Optional via Settings `show_more_columns` (default remains 4-col) |
| Wallpaper on Start | Black background per blueprint |
| Wrong tile sizes | 1×1, 2×2, 4×2 cycle |
| Centered edit overlay | In-grid edit: dim all tiles, focus active, corner buttons |
| App list over-engineered | Simple alphabetical list per blueprint |
| Navigation | Bottom-right → arrow opens app menu |
| Live tile flip (600ms) | Notification / peek back faces flip with 600ms turnstile |
| Notification → live tile | `TileNotificationListenerService` → badges + flip peeks |
| Music now-playing live tile | `MusicNowPlayingStore` via MediaSession → album art + transport by size |
| Notification progress → tile | `TileNotificationStore` maps progress extras + remaining-time copy onto a front-face bar |
| Tile drag reorder | Long-press then drag: floating tile under thumb, magnet reflow, persist on drop |

## Remaining

| Gap | Blueprint / reference |
|-----|----------------------|
| Real app icons in app list | `images/applist_dark_blue.png` |
| Pin from app list | — |

## Images vs blueprint

Files like `start_wallpaper_*.jpeg` show WP8.1 with wallpaper — useful for transparent-tile / window framing. Gaps between tiles stay black; only accent tiles reveal the photo.
