# Launcher — known gaps

Blueprint: [`guides/blueprint.md`](guides/blueprint.md)

## Closed

| Item | Fix |
|------|-----|
| 6-column grid | 4-column grid per blueprint |
| Wallpaper on Start | Black background per blueprint |
| Wrong tile sizes | 1×1, 2×2, 4×2 cycle |
| Centered edit overlay | In-grid edit: dim all tiles, focus active, corner buttons |
| App list over-engineered | Simple alphabetical list per blueprint |
| Navigation | Bottom-right → arrow opens app menu |
| Live tile flip (600ms) | Notification / peek back faces flip with 600ms turnstile |
| Notification → live tile | `TileNotificationListenerService` → badges + flip peeks |
| Tile drag reorder | Long-press then drag: floating tile under thumb, magnet reflow, persist on drop |

## Remaining

| Gap | Blueprint / reference |
|-----|----------------------|
| Real app icons in app list | `images/applist_dark_blue.png` |
| Pin from app list | — |

## Images vs blueprint

Files like `start_wallpaper_*.jpeg` show WP8.1 with wallpaper — **not** this project's Start page (black bg). Use them for tile icon/color inspiration only.
