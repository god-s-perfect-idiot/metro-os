# Calendar — reference materials

Visual and behavioral source material for implementing this app to WP8.1 fidelity.

Agents must read this folder **before** changing UI in `apps/calendar/`.

## Folder layout

```
references/
├── README.md           # This file — screen index and usage rules
├── web-resources.md    # Curated web guides, docs, and video links
├── known-gaps.md       # Missing/low-fidelity captures and workarounds
├── images/             # WP8.1 screenshots for this app
│   └── <screen>_<theme>_<accent>.<ext>
└── guides/             # Offline PDFs, saved articles, measurement notes
    └── blueprint.md    # Authoritative page spec
```

## Screens

| Screen | Image | Notes |
|--------|-------|-------|
| Agenda pivot | `images/agenda_dark_blue.png` | Chronological event list grouped by date (WP8.0 layout ref; restored in 8.1 Update 2) |
| Day pivot | `images/day_dark_blue.png` | Hourly schedule grid with date header |
| Month pivot | `images/month_dark_blue.jpg` | Month grid with event color bars (official Microsoft) |
| Live tile 2×2 | `images/live_tile_medium_dark_blue.png` | Agenda tile — title, time, today's date badge |
| Live tile 4×2 | `images/live_tile_wide_dark_blue.png` | Agenda tile — title, location, time, `Calendar` footer, date badge |
| Week view (ref only) | `images/week_dark_blue.jpg` | Out of v1 scope |
| Week expanded (ref only) | `images/week_expanded_dark_blue.png` | Out of v1 scope |

## Image catalog (attribution)

| File | Illustrates | Source |
|------|-------------|--------|
| `agenda_dark_blue.png` | Agenda list — time, title, duration, accent bar | Ian Griffiths — [Agenda View blog](http://www.interact-sw.co.uk/iangblog/2014/07/09/agenda-view) (WP8.0) |
| `day_dark_blue.png` | Day view hourly grid | Ian Griffiths — [Agenda View blog](http://www.interact-sw.co.uk/iangblog/2014/07/09/agenda-view) |
| `month_dark_blue.jpg` | Month grid + year header | Microsoft Devices Blog — [WP8.1 Calendar](https://blogs.windows.com/devices/2014/04/17/windows-phone-8-1-calendar/) (2014) |
| `live_tile_medium_dark_blue.png` | 2×2 Calendar live tile (title, all-day line, date badge) | WP8.1 device capture (user-provided) |
| `live_tile_wide_dark_blue.png` | 4×2 Calendar live tile (title, location, time, `Calendar` footer, date badge) | WP8.1 device capture (user-provided) |
| `week_dark_blue.jpg` | Week view with weather | Microsoft Devices Blog — [WP8.1 Calendar](https://blogs.windows.com/devices/2014/04/17/windows-phone-8-1-calendar/) (2014) |
| `week_expanded_dark_blue.png` | Expanded week day | Ian Griffiths — [Agenda View blog](http://www.interact-sw.co.uk/iangblog/2014/07/09/agenda-view) |
| `hero_dark_blue.jpg` | Marketing hero | Microsoft Devices Blog — [WP8.1 Calendar](https://blogs.windows.com/devices/2014/04/17/windows-phone-8-1-calendar/) (2014) |

## Agent workflow

1. Read `guides/blueprint.md` for authoritative layout.
2. Open matching image for visual polish.
3. Read `web-resources.md` for behavior context.
4. Cite paths in commits/PRs.

Golden screenshots for verify live in `screenshots/golden/` (captured from emulator, not WP8.1 source).
