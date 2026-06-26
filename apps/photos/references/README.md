# Photos — reference materials

Visual and behavioral source material for implementing this app to WP8.1 fidelity.

Agents must read this folder **before** changing UI in `apps/photos/`.

## Folder layout

```
references/
├── README.md           # This file — screen index and usage rules
├── web-resources.md    # Curated web guides, docs, and video links
├── known-gaps.md       # Missing reference captures + workarounds
├── images/             # WP8.1 screenshots for this app
│   └── <screen>_<theme>_<accent>.<ext>
└── guides/
    └── blueprint.md    # Authoritative page spec
```

## Screens

| Screen | Image | Notes |
|--------|-------|-------|
| Collection — all pictures | `images/hub_dark_blue.jpg` | 4-column thumbnail grid |
| Collection — albums pivot | `images/pivot_albums_dark_blue.jpg` | WP8.1 albums + online tiles |
| Collection — date grouping | `images/pivot_date_dark_blue.jpg` | Month header + grid |
| Collection — month jump | `images/pivot_date_grouped_dark_blue.jpg` | Grouped month list |
| Photo viewer | _(gap)_ | See `known-gaps.md` |

## Image catalog

| File | Source | License / attribution |
|------|--------|----------------------|
| `hub_dark_blue.jpg` | [AAWP camera roll](http://allaboutwindowsphone.com/features/item/19292_How_to_Navigate_your_Camera_Ro.php) | Community reference capture |
| `pivot_albums_dark_blue.jpg` | [AAWP WP8.1 SDK leak](https://allaboutwindowsphone.com/news/item/19291_Windows_Phone_81_details_emerg.php) | Community reference capture |
| `pivot_date_dark_blue.jpg` | AAWP camera roll article | Community reference capture |
| `pivot_date_grouped_dark_blue.jpg` | AAWP camera roll article | Community reference capture |

## Agent workflow

1. Identify the screen (see `AGENTS.md` and app `README.md`).
2. Read `guides/blueprint.md` for the authoritative spec.
3. Open matching row in **Screens** above.
4. Compare implementation against `images/`.
5. Cite paths in commits/PRs:

```
Reference: apps/photos/references/images/pivot_albums_dark_blue.jpg
Guide: apps/photos/references/web-resources.md#collection-pivot-all--albums--favorites
```

Golden screenshots for verify live in `screenshots/golden/` (captured from emulator, not WP8.1 source).
