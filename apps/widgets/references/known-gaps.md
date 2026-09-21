# Widgets — known reference gaps

| Missing / low-fidelity file | Should show | Workaround |
|----------------------------|-------------|------------|
| `catalog_dark_cyan.png` | Full widgets catalog: `WIDGETS` overline + 4-col Start grid with Time 2×4, Battery 1×1, Notifier 2×2, Analog clock 1×1, Torch 1×1, Lock 1×1 | Implement from [`guides/blueprint.md`](guides/blueprint.md) § Page 1 + Start grid metrics in `apps/launcher/references/guides/blueprint.md` |
| `torch_1x1_dark_crimson.png` | 1×1 flashlight glyph (off = accent; on = white) | Implement from [`guides/blueprint.md`](guides/blueprint.md) § Torch + `ic_widget_torch` + third-party lamp-tile notes in `web-resources.md` |
| `lock_1x1_dark_crimson.png` | 1×1 padlock lock-device face | Suite `MetroAppGlyphs.Lockscreen` + blueprint § Lock |

Present face refs:

| File | Notes |
|------|-------|
| `time_wide_dark_cobalt.png` | Wide clock — use time/AM-PM only; ignore weather / location / temperature |
| `battery_1x1_dark_crimson.png` | 1×1 vertical battery + digits |
| `analog_clock_1x1_dark_crimson.png` | 1×1 flat analog dial (hour bars, minute ticks, hands) |
