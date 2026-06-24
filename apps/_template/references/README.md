# {{DISPLAY_NAME}} — reference materials

**Start with [`guides/blueprint.md`](guides/blueprint.md)** — authoritative page and interaction spec.

Agents must read the blueprint before changing UI in `apps/{{APP_NAME}}/`.

## Folder layout

```
references/
├── README.md
├── web-resources.md       # External URLs (supplementary)
├── guides/
│   └── blueprint.md         # Authoritative — pages, layout, interactions
├── images/                  # Visual reference only (does not override blueprint)
└── known-gaps.md            # Optional — track implementation debt
```

## Reading order

1. `guides/blueprint.md` — what to build
2. `AGENTS.md` + app `README.md` — contracts and verify gates
3. `images/` — visual polish and inspiration
4. `web-resources.md` — external docs when needed

## Agent workflow

```
Reference: apps/{{APP_NAME}}/references/guides/blueprint.md
Visual: apps/{{APP_NAME}}/references/images/<file>
```

Golden screenshots for verify: `screenshots/golden/`.
