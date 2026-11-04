# Feedback — reference materials

**Start with [`guides/blueprint.md`](guides/blueprint.md)** — authoritative page and interaction spec.

Agents must read the blueprint before changing UI in `apps/feedback/`.

## Folder layout

```
references/
├── README.md
├── web-resources.md
├── known-gaps.md
├── guides/
│   └── blueprint.md
└── images/
    └── sdk_leak_collage_storage_battery_dark.jpg
```

## Reading order

1. `guides/blueprint.md` — what to build
2. `AGENTS.md` + app `README.md` — contracts and verify gates
3. `images/` — visual polish
4. `web-resources.md` — external docs
5. `known-gaps.md` — missing captures

## Reference research checklist

- [x] Every page in `guides/blueprint.md` is listed with a reference image path or known gap
- [x] `web-resources.md` cites real WP8.1 / GitHub sources per pane
- [x] `images/` holds a Storage Sense / Battery Sense collage for metrics
- [x] Feedback form capture gap logged in `known-gaps.md`

## Image catalog

| File | Theme / accent | Page | Attribution |
|------|----------------|------|-------------|
| `sdk_leak_collage_storage_battery_dark.jpg` | dark / mixed | metrics | [NPowerUser WP8.1 SDK collage](https://nokiapoweruser.com/new-wp-8-1-features-speed-dial-browser-password-manager-camera-ui-cortana-battery-power-sense-storage-sense-screenshots/) |

## Agent workflow

```
Reference: apps/feedback/references/guides/blueprint.md
Visual: apps/feedback/references/images/<file>
```

Golden screenshots for verify: `screenshots/golden/`.
