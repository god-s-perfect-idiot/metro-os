# Hub — reference materials

**Start with [`guides/blueprint.md`](guides/blueprint.md)** — authoritative page and interaction spec.

Agents must read the blueprint before changing UI in `apps/hub/`.

## Folder layout

```
references/
├── README.md
├── web-resources.md
├── known-gaps.md
├── guides/
│   └── blueprint.md
└── images/
    ├── panorama_dark_teal.png
    ├── extras_info_dark_cyan.png
    └── phone_update_dark_red.png
```

## Reading order

1. `guides/blueprint.md` — what to build
2. `AGENTS.md` + app `README.md` — contracts and verify gates
3. `images/` — visual polish
4. `web-resources.md` / `known-gaps.md`

## Image catalog

| File | Theme / accent | Illustrates |
|------|----------------|-------------|
| `panorama_dark_teal.png` | Dark / teal (Music source) | Panorama chrome stand-in for Hub home + apps panes |
| `extras_info_dark_cyan.png` | Dark / cyan (Lumia extras+info) | Title, Software release accent name + info glyph, underlined link, component list, more info border button |
| `phone_update_dark_red.png` | Dark / red (WP phone update) | SETTINGS overline, phone update title, Update status, Learn more, bordered check-for-updates button |

See `known-gaps.md` for missing Hub-specific captures.

## Agent workflow

```
Reference: apps/hub/references/guides/blueprint.md
Visual: apps/hub/references/images/panorama_dark_teal.png
```

Golden screenshots for verify: `screenshots/golden/`.
