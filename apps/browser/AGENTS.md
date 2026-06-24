# Agent instructions — Browser (`com.metro.browser`)

**Tier 1** | Package: `com.metro.browser`

Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

**IE Mobile** for WP8.1 — address bar, tabs, favorites, navigation, reading view.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| Toolkits verified | Yes |
| Tier 0 shell passes verify | **Yes** |

## Screens

| Screen | Pattern | Reference |
|--------|---------|-----------|
| Browser (single tab) | Page + bottom app bar | `references/images/browse_dark_blue.png` |
| Tabs | Pivot | `references/images/tabs_dark_blue.png` |
| Favorites | List | `references/images/favorites_dark_blue.png` |

## WP8.1 rules

- Address bar at top of content area (not Material Omnibox chrome)
- Bottom `MetroAppBar`: back, forward, add favorite, tabs
- Tabs pivot: open tabs as pivot items or dedicated tabs page per reference
- Page transition 300ms horizontal within app chrome
- Use WebView; no Chrome Custom Tabs

## Primary flows

1. Enter URL → load page
2. Open new tab; switch via tabs pivot
3. Add/remove favorite
4. Back key navigates WebView history then exits

## Golden screenshots

```
screenshots/golden/browse_dark_blue.png
screenshots/golden/tabs_dark_blue.png
```

## Verify

```bash
../../scripts/verify-app.sh browser
```
