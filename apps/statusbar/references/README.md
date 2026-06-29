# Statusbar — reference materials

Visual and behavioral source material for implementing this app to WP8.1 fidelity.

Agents must read this folder **before** changing UI in `apps/statusbar/`.

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
| Collapsed tray | `images/collapsed_dark.png` | Clock right-aligned; dark WP8.1 page (Settings) resting tray |
| Expanded tray | `images/expanded_dark.png` | Full indicator row + battery glyph/clock/% (Action Center) |
| Progress tray | _missing — see [`known-gaps.md`](known-gaps.md)_ | Accent indeterminate progress in tray |

## Image catalog (attribution)

| File | Source | License | Page |
|------|--------|---------|------|
| `images/expanded_dark.png` | [Wikimedia Commons — Windows Phone 8.1 Benachrichtigungszentrale.png](https://commons.wikimedia.org/wiki/File:Windows_Phone_8.1_Benachrichtigungszentrale.png) (Armin2208) | Public domain | Expanded tray |
| `images/collapsed_dark.png` | [Wikimedia Commons — Windows Phone 8.1 Update 2 Einstellungen.png](https://commons.wikimedia.org/wiki/File:Windows_Phone_8.1_Update_2_Einstellungen.png) (Armin2208) | Public domain | Collapsed tray |

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
Reference: apps/statusbar/references/images/start_dark_blue.png
Guide: apps/statusbar/references/web-resources.md#start-screen
```

Golden screenshots for verify live in `screenshots/golden/` (captured from emulator, not WP8.1 source).

## Large binaries

Raw dumps > 5MB or video captures may live outside git. Document the storage location in this file if omitted.
