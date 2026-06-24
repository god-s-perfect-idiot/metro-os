# Agent instructions — Launcher (`com.metro.launcher`)

**Tier 0 — Metro Shell** | Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## Authoritative spec

**[`references/guides/blueprint.md`](references/guides/blueprint.md)** — pages, grid, navigation, edit mode. Images do not override the blueprint.

## App role

WP8.1 **Start screen** — live tiles, 4-column grid, app menu, pin/unpin tiles. Default home app via `ROLE_HOME`.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| `metro-ui-android` verified | Yes |
| `metro-system-sdk` verified | Yes |
| Tier 0 shell | N/A (this is shell) |

## Pages (blueprint)

| Page | Layout | Navigation |
|------|--------|------------|
| Start (tiles) | 4 cols × n rows; sizes 1×1, 2×2, 4×2; black bg; inner gap | → arrow bottom-right → app menu |
| App menu | Simple app list | Swipe left / back → Start |
| Tile edit | Long-press: dim grid, focus tile, corner unpin/resize | Tap blank or back to exit |

Supplementary: [`references/guides/startmenu.md`](references/guides/startmenu.md), [`references/images/`](references/images/).

## WP8.1 rules specific to launcher

- Resize cycle: **1×1 → 2×2 → 4×2 → 1×1**
- Never use wallpaper on Start (black background)
- Long-press: in-place edit mode, not a separate menu screen
- No FAB; no Material decorations

## Verify

```bash
../../scripts/verify-app.sh launcher
```

## Platform exceptions

| scope.md | Blueprint | Compromise |
|----------|-----------|------------|
| 6-column grid, 99/198dp sizes | 4-column, 1×1/2×2/4×2 | **Blueprint wins** for launcher layout |
