# Calculator — reference materials

Visual and behavioral source material for implementing this app to WP8.1 fidelity.

Agents must read this folder **before** changing UI in `apps/calculator/`.

## Folder layout

```
references/
├── README.md           # This file — screen index and usage rules
├── web-resources.md    # Curated web guides, docs, and video links
├── images/             # WP8.1 screenshots for this app
│   └── <screen>_<theme>_<accent>.<ext>
└── guides/
    └── blueprint.md    # Authoritative page and interaction spec
```

## Screens

| Screen | Image | Notes |
|--------|-------|-------|
| Standard calculator | `images/standard_dark_blue.jpg` | Portrait 6×4 keypad, memory row, red equals |
| Scientific calculator | `images/scientific_dark_blue.jpg` | Landscape 5×8 scientific (shown when device is rotated) |

## Image naming

- Pattern: `<screen>_<theme>_<accent>.<ext>`
- Primary device profile: **768×1280** (Lumia 925 / xhdpi) — see `scope.md`
- Captures from Windows Central forum (Lumia WP8.1, August 2013)

## Agent workflow

1. Read `guides/blueprint.md` for layout and evaluation rules.
2. Compare implementation against `images/standard_dark_blue.jpg` and `images/scientific_dark_blue.jpg`.
3. Cite paths in commits/PRs:

```
Reference: apps/calculator/references/images/standard_dark_blue.jpg
Guide: apps/calculator/references/guides/blueprint.md
```

Golden screenshots for verify live in `screenshots/golden/` (captured from emulator, not WP8.1 source).
