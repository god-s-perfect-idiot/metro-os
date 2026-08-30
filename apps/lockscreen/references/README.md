# Lock screen — reference materials

**Start with [`guides/blueprint.md`](guides/blueprint.md)** — authoritative page and interaction spec.

Agents must read the blueprint before changing UI in `apps/lockscreen/`.

## Folder layout

```
references/
├── README.md
├── web-resources.md
├── guides/
│   └── blueprint.md
├── images/
└── known-gaps.md
```

## Reading order

1. `guides/blueprint.md` — what to build
2. `AGENTS.md` + app `README.md` — contracts and verify gates
3. `images/` — visual polish and inspiration
4. `web-resources.md` — external docs when needed

## Reference research (Phase 0)

- [x] Every page in `guides/blueprint.md` is listed with a reference image path or known-gap.
- [x] `web-resources.md` cites real WP8.1 / Lumia sources.
- [x] `images/` holds a capture for the lock surface.
- [x] Full WP lock chrome beyond the PoC solid fill is logged in `known-gaps.md`.

## Image catalog

| File | Theme / accent | Source | Illustrates |
|------|----------------|--------|-------------|
| `images/lock_bing_homepage_dark.jpg` | Dark / Bing photo | [Microsoft Devices Blog — Lumia screens explained](https://blogs.windows.com/devices/2015/01/26/lumia-screens-explained-glance-lock-start-app-list/) | WP8.1 lock screen with wallpaper; swipe up for password |

## Agent workflow

```
Reference: apps/lockscreen/references/guides/blueprint.md
Visual: apps/lockscreen/references/images/lock_bing_homepage_dark.jpg
```

Golden screenshots for verify: `screenshots/golden/`.
