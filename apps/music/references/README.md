# Music — reference materials

Visual and behavioral source material for implementing Xbox Music (WP8.1) fidelity.

Agents must read this folder **before** changing UI in `apps/music/`.

## Folder layout

```
references/
├── README.md           # This file — screen index and usage rules
├── web-resources.md    # Curated web guides and image attribution
├── known-gaps.md       # Missing/low-fidelity captures
├── images/             # WP8.1 screenshots for this app
│   └── <screen>_<theme>_<accent>.jpg
└── guides/             # blueprint.md (authoritative)
```

## Reading order

1. [`guides/blueprint.md`](guides/blueprint.md) — pages and interactions
2. This README screen table
3. [`web-resources.md`](web-resources.md) — behavior sources
4. Matching file under `images/`
5. [`known-gaps.md`](known-gaps.md) when an image is weak

## Screens

| Screen | Image | Notes |
|--------|-------|-------|
| Hub / Now playing | `images/hub_nowplaying_dark_green.jpg` | Primary Lumia capture (AAWP) |
| Now playing (alias) | `images/nowplaying_dark_green.jpg` | Same as hub now playing |
| Hub alias | `images/hub_dark_green.jpg` | Same landing |
| Now playing old vs new | `images/hub_nowplaying_compare_dark_unknown.jpg` | Up next + header density |
| Artists + showing | `images/artists_showing_dark_teal.jpg` | Pivot + Showing label |
| Showing menu | `images/showing_menu_dark_teal.jpg` | All / device / streaming |
| Album detail | `images/album_detail_dark_teal.jpg` | Art + download text action |
| Settings | `images/settings_dark_teal.jpg` | Connect to streaming toggle |
| Sync | `images/settings_sync_dark_unknown.jpg` | Sync Now |
| Start Music tile | `images/start_music_tile_dark_blue.jpg` | Launcher context only |

## Image naming

- Pattern: `<screen>_<theme>_<accent>.jpg`
- Primary device profile: **768×1280** (Lumia 925 / xhdpi)
- Prefer real WP8.1 captures; marketing photos are labeled and not used for layout measurement

## Agent workflow

1. Identify the screen (`AGENTS.md` / blueprint).
2. Open the matching row above.
3. Read `web-resources.md` for behavior.
4. Compare implementation to `images/…`.
5. Cite paths in commits/PRs.

Golden screenshots for verify live in `screenshots/golden/` (emulator), not WP8.1 source.
