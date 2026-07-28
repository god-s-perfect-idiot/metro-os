# Files — reference materials

Visual and behavioral source material for implementing this app to WP8.1 fidelity.

Agents must read this folder **before** changing UI in `apps/files/`.

## Folder layout

```
references/
├── README.md           # This file — screen index and usage rules
├── web-resources.md    # Curated web guides, docs, and video links
├── known-gaps.md       # Missing captures + workarounds
├── images/             # WP8.1 screenshots for this app (see known-gaps)
│   └── <screen>_<theme>_<accent>.png
└── guides/             # Offline PDFs, saved articles, measurement notes
    └── blueprint.md
```

## Screens

| Screen | Image | Notes |
|--------|-------|-------|
| Filter pivots + browse | `images/pivots_dark_blue.png` | Gap — blueprint Page 1 + Ars / Microsoft posts |
| Folder / file list detail | `images/list_dark_blue.png` | Gap — path + date/size rows |
| Permission gate | `images/permission_dark_blue.png` | Gap — blueprint Page 2 |

## Image naming

- Pattern: `<screen>_<theme>_<accent>.png`
- Examples: `pivots_dark_blue.png`, `list_dark_blue.png`
- Primary device profile: **768×1280** (Lumia 925 / xhdpi) — see `scope.md`
- Capture from WP8.1 GDR2+ device or use licensed marketing assets

## Web resources

Add links in [`web-resources.md`](web-resources.md). One section per screen or feature area.

Prefer official Microsoft / Windows Phone documentation; community captures (Ars Technica) when official UI detail is thin.

## Agent workflow

1. Identify the screen you are building (see `AGENTS.md` and app `README.md`).
2. Open the matching row in **Screens** above.
3. Read `web-resources.md` and `guides/blueprint.md`.
4. If the image is missing, follow `known-gaps.md`.
5. Cite paths in commits/PRs:

```
Reference: apps/files/references/guides/blueprint.md (Page 1)
Guide: apps/files/references/web-resources.md#filter-pivots--browse-list
```

Golden screenshots for verify live in `screenshots/golden/` (captured from emulator, not WP8.1 source).

## Large binaries

Raw dumps > 5MB or video captures may live outside git. Document the storage location in this file if omitted.
