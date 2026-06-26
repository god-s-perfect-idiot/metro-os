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

## Reference research (do this first — see root `AGENTS.md` § Phase 0)

Before writing any UI code for {{DISPLAY_NAME}}, prefill this folder:

- [ ] Every page in `guides/blueprint.md` is listed here with a reference image path.
- [ ] `web-resources.md` cites a real WP8.1 source per screen (official MS / Lumia docs first).
- [ ] `images/` holds a capture per blueprint page, named `<screen>_<theme>_<accent>.<ext>` and attributed below.
- [ ] Any page without a capture is logged in `known-gaps.md` with a workaround.

`images/` must not be empty when development starts.

## Agent workflow

```
Reference: apps/{{APP_NAME}}/references/guides/blueprint.md
Visual: apps/{{APP_NAME}}/references/images/<file>
```

Golden screenshots for verify: `screenshots/golden/`.
