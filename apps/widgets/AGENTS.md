# Agent instructions — Widgets (`com.metro.widgets`)

**Tier 2** | Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## Authoritative spec

**[`references/guides/blueprint.md`](references/guides/blueprint.md)** — catalog grid, footprints, faces.

## App role

Homescreen **widget catalog** — Start-style **4-column** grid of custom live tiles (Time, Battery, Notifier, Analog clock, Torch, Lock). Long-press pins a secondary Start tile with live face + in-place tap via `MetroTileWidgetFace`.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| `metro-ui-android` verified | Yes |
| `metro-system-sdk` verified | Yes |
| Tier 0 shell | Recommended for visual parity |
| Tier 1 apps | No |

## Screens

| Screen | Layout | Reference |
|--------|--------|-----------|
| Widget catalog | `MetroAppTitle` + 4-col tile grid | `references/guides/blueprint.md` |

## WP8.1 rules specific to this app

- Black Start background; 4 columns only; gaps/gutters match launcher Start
- Tile sizes only **1×1 / 2×2 / 2×4** (2×4 = Start wide)
- `MetroAppTitle("widgets")` — no Material chrome
- Long-press any catalog tile → pin secondary Start tile (catalog footprint); tap actions unchanged
- No resize / edit in catalog

## Verify

```bash
../../scripts/verify-app.sh widgets
```

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| First-party Time Start tile | Did not exist on WP8.1 | TimeMe / Clock Hub–style digital face |
| First-party Torch Start tile | Action Center quick action only | 1×1 LED toggle via `CameraManager.setTorchMode` |
| First-party Lock Start tile | None | 1×1 padlock via lockscreen a11y `GLOBAL_ACTION_LOCK_SCREEN` |
