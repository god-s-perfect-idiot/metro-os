# Phone — reference materials

**Start with [`guides/blueprint.md`](guides/blueprint.md)** — authoritative page spec. Images are visual aids only.

## Pages (from blueprint)

| Page | Blueprint | Image | Supplementary |
|------|-----------|-------|---------------|
| History (recent calls) | [blueprint § Page 1](guides/blueprint.md) | `images/history_dark_blue.png` | [`guides/phone-hub.md`](guides/phone-hub.md) |
| Number view (call detail) | [blueprint § Page 2](guides/blueprint.md) | — | [`guides/phone-hub.md`](guides/phone-hub.md) |
| Dial pad | [blueprint § Page 3](guides/blueprint.md) | `images/dialpad_dark_blue.jpg` | [`guides/dial-pad.md`](guides/dial-pad.md) |
| In-call / dialing | [blueprint § Page 4](guides/blueprint.md) | `images/in_call_dark_blue.png` | [`guides/dial-pad.md`](guides/dial-pad.md) |
| Speed dial | [blueprint § Page 5](guides/blueprint.md) | `images/speed_dial_dark_blue.png` | [`guides/phone-hub.md`](guides/phone-hub.md) |

## Folder layout

```
references/
├── README.md              # This file
├── web-resources.md         # Curated links
├── guides/
│   ├── blueprint.md         # Authoritative — read first
│   ├── phone-hub.md         # History + speed dial pivot
│   └── dial-pad.md          # Keypad + in-call
└── images/                  # WP 8.1 screenshots (visual aid only)
```

## Image catalog

Screenshots sourced from WP 8.1 Lumia 920 developer-preview coverage (April 2014) and Stack Overflow WP8 dial-pad capture. Blueprint wins on conflict.

| File | Illustrates |
|------|-------------|
| `history_dark_blue.png` | Grouped call history list |
| `speed_dial_dark_blue.png` | Speed dial pinned contacts |
| `dialpad_dark_blue.jpg` | Numeric keypad with contact autocomplete |
| `in_call_dark_blue.png` | Full-screen outgoing call UI |

## Agent workflow

1. Read `guides/blueprint.md`
2. Read supplementary guides for the page you are building
3. Use `images/` for visual polish only
4. Cite: `apps/dialer/references/guides/blueprint.md`

Golden emulator captures: `screenshots/golden/`.
