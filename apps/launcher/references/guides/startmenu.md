# Tile edit mode (long-press)

Supplements [`blueprint.md`](blueprint.md) § Tiles in start menu. Blueprint is authoritative for layout; this file details edit-mode behavior.

Reference image: *(capture pending — use this guide and live device)*  
See also: [web-resources.md § Tile edit](../web-resources.md#tile-edit-and-pinning)

## Entering edit mode

Press and hold any Live Tile for about one second.

Three immediate visual cues:

1. **Tilt** — The Start screen recedes; tiles look like physical cards floating off the glass.
2. **Dimming** — Background image or accent plane dims to isolate the editing surface.
3. **Control overlays** — The pressed tile lifts higher and gains two circular corner buttons.

## Corner buttons (on the tile, not a menu)

| Corner | Icon | Action |
|--------|------|--------|
| Top-right | Pushpin with strike-through | **Unpin** — tile leaves Start, app remains in app list |
| Bottom-right | Diagonal resize arrow | **Resize** — cycles supported sizes |

### Tile sizes (WP8.1)

| Size | Grid | Notes |
|------|------|-------|
| Small (1×1) | 99×99 dp in metro-os | Compact utility apps |
| Medium (2×1) | 198×99 dp | Standard live tile |
| Wide (2×2) | 198×198 dp | Detailed live content; gated by `BuildConfig.WIDE_TILES` |

## Reordering

While in edit mode, drag a tile to reorder. Surrounding tiles part smoothly (magnet grid). Tiles snap to grid coordinates top-to-bottom.

*Live folders (WP 8.1 Update 1): drag one tile onto another and hold — out of scope for v1.*

## Exiting edit mode

- Tap any blank area on Start, or
- Press Back

Tiles snap flat; live updates resume; layout persists.

## Implementation mapping

| WP8.1 | Android (`TileGrid` edit mode) |
|-------|--------------------------------|
| Dimmed Start behind | Scrim + dimmed non-active tiles in grid |
| Floating idle tiles | Non-active tiles drift independently (`rememberTileIdleFloat`) |
| Lifted tile | Active tile `scale 1.02` in place (stationary) |
| Drag under thumb | Dragged tile follows pointer (`dragPositionPx`), `scale 1.06` |
| Magnet reflow | `insertIndexForPointer` + list reorder; others animate via `rememberAnimatedTileBounds` |
| Unpin top-right | `TileCornerButton` aligned top-end of tile |
| Resize bottom-right | `TileCornerButton` aligned bottom-end of tile |
| Tap blank to dismiss | Scrim `clickable` → `onDismiss` |
