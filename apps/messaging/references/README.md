# Messaging — reference materials

Visual and behavioral source material for implementing this app to WP8.1 fidelity.

Agents must read this folder **before** changing UI in `apps/messaging/`.

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
| Thread list | `images/threads_dark_yellow.jpg` | Landing page — conversation list + bottom app bar (official) |
| Conversation | `images/conversation_dark_orange.png` | Real WP dark-theme thread — bubble tails, accent/darkened fills, gray composer |
| Conversation (schematic) | `images/conversation_schematic.png` | Message thread layout — bubble alignment + MMS (official schematic) |
| Settings (ref only) | `images/settings_dark_yellow.jpg` | Toggle-list styling; settings page is out of v1 scope |
| Live tile | `images/tile_yellow.jpg` | Unread wink glyph + count |

## Image naming

- Pattern: `<screen>_<theme>_<accent>.<ext>` (use the real theme/accent of the capture, not an assumed one).
- Examples: `threads_dark_yellow.jpg`, `start_dark_blue.png`, `applist_light_teal.png`
- Primary device profile: **768×1280** (Lumia 925 / xhdpi) — see `scope.md`
- Capture from WP8.1 GDR2+ device or use licensed/official marketing assets

## Image catalog (attribution)

Current `images/` are community/official references (visual aids only — `guides/blueprint.md` wins on conflict). The shipped captures use the WP8 **dark** theme with the **yellow** accent; the blueprint's accent guidance still applies.

| File | Illustrates | Source |
|------|-------------|--------|
| `threads_dark_yellow.jpg` | Threads landing list + bottom app bar | Microsoft Devices Blog — [WP8 Messaging deep dive](https://blogs.windows.com/devices/2014/03/06/windows-phone-8-messaging-deep-dive/) (2014) |
| `settings_dark_yellow.jpg` | Messaging settings toggle list | Microsoft Devices Blog — WP8 Messaging deep dive (2014) |
| `tile_yellow.jpg` | Messaging live tile glyph + unread count | Microsoft Devices Blog — WP8 Messaging deep dive (2014) |
| `conversation_dark_orange.png` | Conversation bubbles (accent received / darkened sent) + gray composer | WP dark-theme capture (orange accent); used for bubble geometry and composer chrome |
| `conversation_schematic.png` | Conversation bubble layout (sent/received + MMS) | Nokia/Microsoft Lumia user guide ([helpdoc.net](https://nokia-lumia-521.helpdoc.net/en-us/people-messaging/messages/read-a-message/)) |

Open gaps (e.g. a real dark-theme conversation capture at 768×1280) are tracked in [`known-gaps.md`](known-gaps.md).

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
Reference: apps/messaging/references/images/threads_dark_yellow.jpg
Guide: apps/messaging/references/web-resources.md#thread-list
```

Golden screenshots for verify live in `screenshots/golden/` (captured from emulator, not WP8.1 source).

## Large binaries

Raw dumps > 5MB or video captures may live outside git. Document the storage location in this file if omitted.
