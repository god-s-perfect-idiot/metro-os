# Agent instructions — Widgets (`com.metro.widgets`)

**Tier 2** | Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## Authoritative spec

**[`references/guides/blueprint.md`](references/guides/blueprint.md)** — catalog grid, footprints, faces.

## App role

Homescreen **widget catalog** — Start-style **4-column** grid of custom live tiles (Time, Battery, Storage Sense). Preview only in v1 (no pin yet).

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
- No pin / resize / edit in v1

## Verify

```bash
../../scripts/verify-app.sh widgets
```

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| First-party Time Start tile | Did not exist on WP8.1 | TimeMe / Clock Hub–style digital face |
| Battery Saver shield glyph when saver on | No WP Battery Saver mode API | Show shield / red fill at ≤20% or while charging low |
