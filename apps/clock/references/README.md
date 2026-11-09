# Clock — reference materials

Visual and behavioral source material for implementing this app to WP8.1 fidelity.

Agents must read this folder **before** changing UI in `apps/clock/`.

## Folder layout

```
references/
├── README.md           # This file — screen index and usage rules
├── web-resources.md    # Curated web guides, docs, and video links
├── images/             # WP8.1 screenshots for this app
│   └── <screen>_<theme>_<accent>.png
└── guides/             # Offline PDFs, saved articles, measurement notes
    └── blueprint.md    # Authoritative page + motion spec
```

## Screens

| Screen | Image | Notes |
|--------|-------|-------|
| Alarms list | `images/alarms_list_dark_amber.png` | ALARMS / all + toggles + Add |
| New / edit alarm | `images/alarms_new_dark_amber.png` | Slide-up editor (omit Sound in v1) |
| Time picker | `images/alarms_time_picker_dark_amber.png` | Three accent tiles |
| Time picker + slider | `images/alarms_time_picker_slider_dark_amber.png` | Minutes neighbor column |
| Alarm repeats on | `images/alarms_repeats_dark_amber.png` | Weekday multi-select |
| World clock list | _(gap)_ | See `known-gaps.md` + blueprint § Page 5 |
| World clock add city | _(gap)_ | See `known-gaps.md` + blueprint § Page 5 |
| Timer | _(gap)_ | See `known-gaps.md` + blueprint § Page 6 |
| Timer duration picker | _(gap)_ | Reuse time-picker tiles; blueprint § Page 8 |
| Stopwatch | _(gap)_ | See `known-gaps.md` + blueprint § Page 7 |

## Image naming

- Pattern: `<screen>_<theme>_<accent>.png`
- Primary device profile: **768×1280** (Lumia 925 / xhdpi) — see `scope.md`

## Web resources

Add links in [`web-resources.md`](web-resources.md). One section per screen or feature area.

## Agent workflow

1. Read [`guides/blueprint.md`](guides/blueprint.md).
2. Open the matching row in **Screens** above.
3. Read `web-resources.md` for behavior notes.
4. Compare implementation against `images/…`.

```
Reference: apps/clock/references/images/alarms_list_dark_amber.png
Guide: apps/clock/references/guides/blueprint.md
```

Golden screenshots for verify live in `screenshots/golden/` (captured from emulator, not WP8.1 source).
