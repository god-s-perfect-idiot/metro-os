# Launcher — web resources

Curated links for WP8.1 Start screen look, layout, and interaction.

## General

| Resource | URL | Notes |
|----------|-----|-------|
| scope.md — Launcher | [scope.md](../../../scope.md) | Tile sizes, grid, motion specs |
| App README | [README.md](../README.md) | Screen inventory and guardrails |
| Design checklist | [DESIGN-CHECKLIST.md](../../../docs/DESIGN-CHECKLIST.md) | Pre-ship UI review |
| WP8.1 UI Design and Interaction Guide | _(add PDF to `guides/` or link here)_ | Global Metro patterns |
| Windows Phone 8.1 — App tiles | [MSDN](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202806(v=win.10)) | Tile sizes, templates, badges |

## Start screen (tile grid)

| Resource | URL | Notes |
|----------|-----|-------|
| Tiles on Start | [MSDN — App tiles and badges](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202806(v=win.10)) | Small 159×159, medium 336×336 logical; map to 99/198 dp in scope |
| Live tiles | [MSDN — Live tile overview](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh465391(v=win.10)) | Flip/cycle timing; 600ms turnstile in scope |
| Wallpaper on Start | Search: *Windows Phone 8.1 start screen wallpaper parallax* | Full-bleed background behind tiles; parallax on scroll — **not implemented yet** |

**Layout rules (from scope + references):**

- 6-column grid, **4dp** gap between tiles
- Tile sizes: small 99×99dp, medium 198×99dp, wide 198×198dp (wide behind flag)
- No pure black/white tile fills; accent-driven counters top-right
- Left-aligned Metro typography; no centered page chrome
- Swipe **right** reveals app list (Start is page 0)

## App list

| Resource | URL | Notes |
|----------|-----|-------|
| App list UX | `images/applist_dark_blue.png` | Lowercase letter headers in bordered squares, icon + label rows |
| Search | `images/applist_search_dark_blue.png` | White accent-bordered field; matching letters highlighted in accent |

**Layout rules:**

- Page title **apps** (lowercase), large Segoe/Noto weight
- Rows: app name + optional “pinned to start” subtitle
- No navigation drawer, FAB, or bottom tabs

## Tile edit and pinning

Offline guide: [`guides/startmenu.md`](guides/startmenu.md) — tilt, dimming, corner unpin/resize buttons, exit gestures.

## Adding links

1. Add a `##` section per screen or feature.
2. Prefer official Microsoft docs; save offline copy to `guides/` when URLs break.
3. One sentence in **Notes** explaining what implementers should extract.
