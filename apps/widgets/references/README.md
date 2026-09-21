# Widgets references

WP8.1 research and visual aids for the **widgets** homescreen catalog app.

## Reading order

1. [`guides/blueprint.md`](guides/blueprint.md) — authoritative page + tile face spec
2. [`web-resources.md`](web-resources.md) — WP8.1 Battery / Battery Saver / Time / Analog clock / Torch sources
3. [`known-gaps.md`](known-gaps.md) — missing captures and workarounds
4. `images/` — add captures when available (`catalog_dark_cyan.png` preferred)

## Image catalog

| File | Theme / accent | Page | Source |
|------|----------------|------|--------|
| `time_wide_dark_cobalt.png` | dark / cobalt | Time face | User capture — strip weather/location/temp |
| `battery_1x1_dark_crimson.png` | dark / crimson | Battery face | User capture |
| `analog_clock_1x1_dark_crimson.png` | dark / crimson | Analog clock face | User capture |
| `torch_1x1_dark_crimson.png` | dark / crimson | Torch face | Gap — see `known-gaps.md` |

## Notes

- Grid is **4 columns only** (Start default density).
- Footprints: **1×1**, **2×2**, **2×4** (2 tall × 4 wide = Start wide).
- Time defaults to **2×4**; Battery, Analog clock, Torch, and Lock default to **1×1**.
- Long-press a catalog tile pins it to Start (secondary tile).
