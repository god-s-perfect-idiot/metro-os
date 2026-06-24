# Files — reference materials

Visual and behavioral source material for implementing this app to WP8.1 fidelity.

Agents must read this folder **before** changing UI in `apps/files/`.

## Folder layout

```
references/
├── README.md           # This file — screen index and usage rules
├── web-resources.md    # Curated web guides, docs, and video links
├── images/             # WP8.1 screenshots for this app
│   └── <screen>_<theme>_<accent>.png
└── guides/             # Offline PDFs, saved articles, measurement notes
```

## Screens

| Screen | Image | Notes |
|--------|-------|-------|
| _(add rows — match AGENTS.md screen table)_ | `images/` | |

## Image naming

- Pattern: `<screen>_<theme>_<accent>.png`
- Examples: `start_dark_blue.png`, `applist_light_teal.png`
- Primary device profile: **768×1280** (Lumia 925 / xhdpi) — see `scope.md`
- Capture from WP8.1 GDR2+ device or use licensed marketing assets

## Web resources

Add links in [`web-resources.md`](web-resources.md). One section per screen or feature area.

Agents should open linked guides when implementing or reviewing a screen. Prefer official Microsoft / Windows Phone design documentation; add community captures only when official material is unavailable.

## Agent workflow

1. Identify the screen you are building (see `AGENTS.md` and app `README.md`).
2. Open the matching row in **Screens** above.
3. Read `web-resources.md` for behavior and interaction rules.
4. Compare implementation against `images/<screen>_<theme>_<accent>.png`.
5. Cite paths in commits/PRs:

```
Reference: apps/files/references/images/start_dark_blue.png
Guide: apps/files/references/web-resources.md#start-screen
```

Golden screenshots for verify live in `screenshots/golden/` (captured from emulator, not WP8.1 source).

## Large binaries

Raw dumps > 5MB or video captures may live outside git. Document the storage location in this file if omitted.
