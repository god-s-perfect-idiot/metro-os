# Settings — reference materials

Visual and behavioral source material for implementing this app to WP8.1 fidelity.

Agents must read this folder **before** changing UI in `apps/settings/`.

## Folder layout

```
references/
├── README.md           # This file — screen index and usage rules
├── web-resources.md    # Curated web guides, docs, and video links
├── known-gaps.md       # Missing captures + workarounds
├── images/             # WP8.1 screenshots for this app
│   └── <screen>_<theme>_<accent>.png
└── guides/             # Offline PDFs, saved articles, measurement notes
    └── blueprint.md
```

## Screens

| Screen | Image | Notes |
|--------|-------|-------|
| Settings root | _gap_ — [`known-gaps.md`](known-gaps.md) | System list |
| start+theme | `images/start_theme_dark_cobalt.png` | Intro + Accent colour combo (omit Background row) |
| Accents picker | `images/accents_picker_dark.png` | `ACCENTS` title, 4×5 grid (Eight Forums WP8 capture) |
| Accent palette aid | `images/accent_palette_wp8_dark.png` | Generated 20-colour HEX strip |
| ease of access | `images/ease_of_access_dark_cyan.png` | Text size Sample + 7-step slider |

## Image naming

- Pattern: `<screen>_<theme>_<accent>.png`
- Examples: `ease_of_access_dark_cyan.png`
- Primary device profile: **768×1280** (Lumia 925 / xhdpi) — see `scope.md`

## Web resources

See [`web-resources.md`](web-resources.md). Authoritative page/interaction spec: [`guides/blueprint.md`](guides/blueprint.md).

## Agent workflow

1. Identify the screen in `guides/blueprint.md`.
2. Open the matching row above (or `known-gaps.md`).
3. Read `web-resources.md` for behavior.
4. Compare implementation against available images.
5. Cite paths in commits/PRs:

```
Reference: apps/settings/references/images/ease_of_access_dark_cyan.png
Guide: apps/settings/references/web-resources.md#ease-of-access
```

Golden screenshots for verify live in `screenshots/golden/` (captured from emulator, not WP8.1 source).
