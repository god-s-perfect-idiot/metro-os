# Global reference assets

Repo-wide WP8.1 design materials shared across apps. **Per-app screenshots and web guides live in each app folder**, not here.

## Per-app references (primary)

Every app has its own reference bundle. **`guides/blueprint.md` is authoritative**; images are visual aids only.

```
apps/<name>/references/
├── guides/blueprint.md   # Pages, layout, interactions — read first
├── README.md
├── web-resources.md
├── images/
└── known-gaps.md         # optional
```

Bootstrap or refresh:

```bash
./scripts/bootstrap-references.sh          # all apps
./scripts/bootstrap-references.sh launcher # one app
```

Agents implement UI against `apps/<name>/references/images/` and cite paths in commits. Emulator goldens for verify stay in `apps/<name>/screenshots/golden/`.

## Global assets (this folder)

Shared materials not tied to a single app:

```
references/
├── README.md              # This file
├── design-guide/          # Official WP UI guides (PDF)
├── devices/               # Per-device screenshot sets (optional)
│   ├── lumia-925/
│   └── lumia-520/
└── fonts/                 # Optional Noto reference copies (bundled in metro-ui-android)
```

## Adding global assets

1. Capture from Lumia device running **WP8.1 GDR2+** or use licensed marketing assets.
2. Name device dumps: `<screen>_<theme>_<accent>.png`
3. Resolution should match device profile in `scope.md` (primary: 768×1280).

## Agent usage

Every new screen implementation must cite reference paths:

```
Reference: apps/launcher/references/images/start_dark_blue.png
Guide: apps/launcher/references/web-resources.md#start-screen-tile-grid
```

Compare against goldens in `apps/<name>/screenshots/golden/` during verify.

## Fonts

Noto Sans is bundled in `metro-ui-android` (OFL). Optional reference copies in `references/fonts/`.

## Not in git (large binaries)

- Raw device dumps > 5MB
- Video captures

Document where humans store them if omitted from repo.
