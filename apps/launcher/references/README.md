# Launcher — reference materials

**Start with [`guides/blueprint.md`](guides/blueprint.md)** — authoritative page spec. Images are visual aids only.

## Pages (from blueprint)

| Page | Blueprint | Supplementary |
|------|-----------|---------------|
| Start (tile grid) | [blueprint.md § Tiles](guides/blueprint.md) | `images/start_dark_blue.png`, `guides/startmenu.md` |
| App menu | [blueprint.md § App Menu](guides/blueprint.md) | `images/applist_dark_blue.png`, `images/applist_search_dark_blue.png`, `images/jumplist_dark_blue.png` |

## Folder layout

```
references/
├── README.md
├── web-resources.md
├── guides/
│   ├── blueprint.md      # Authoritative — read first
│   └── startmenu.md      # Long-press edit detail
├── images/               # Visual reference only (do not override blueprint)
└── known-gaps.md
```

## Image catalog

Screenshots illustrate WP8.1 styling; layout and behavior come from `blueprint.md`.

| File | Illustrates |
|------|-------------|
| `start_dark_blue.png` | Tile colors and iconography on black Start |
| `applist_dark_blue.png` | App list row styling |
| `applist_search_dark_blue.png` | App list search: white accent-bordered field + match highlight |
| `jumplist_dark_blue.png` | Find-by-letter overlay: accent active / gray inactive grid |
| `start_tiles_*.jpeg` | Alternate tile compositions (not this project's grid spec) |
| `start_wallpaper_*.jpeg` | WP8.1 wallpaper examples — **not used** (blueprint: black bg) |
| `start_applist_transition.jpeg` | Swipe transition between Start and app list |

## Agent workflow

1. Read `guides/blueprint.md`
2. Use `images/` for visual polish only
3. Cite: `apps/launcher/references/guides/blueprint.md`

Golden emulator captures: `screenshots/golden/`.
