# metro-ui-android

WP8.1 Compose component library for metro-os.

## UX language

Per-control shape, button, and interaction rules: [`METRO-UX-LANGUAGE.md`](METRO-UX-LANGUAGE.md). Agents must read this before implementing UI.

## Status

**Scaffolded** — P0 components (`MetroTheme`, `MetroText`, `MetroPageHeader`, `MetroTile`, `MetroTransitions`, `MetroColors`) plus `MetroAppBar` (§6.2 bottom application bar) and `MetroBorderButton` (§6.3 outlined square text button).

## Public API (planned)

| Composable | Description |
|------------|-------------|
| `MetroTheme` | Root theme wrapper |
| `MetroAppBar` | Bottom application bar |
| `MetroBorderButton` | Outlined square text button |
| `MetroPivot` | Tabbed pivot navigation |
| `MetroPanorama` | Horizontal hub panorama |
| `MetroListItem` | List row with tilt-on-press |
| `MetroToggleSwitch` | WP8.1 toggle |
| `MetroPageHeader` | Large page title |
| `MetroJumpList` | Find-by-letter overlay (`#`, a–z, globe); accent = active |
| `MetroLetterTile` | Accent/inactive letter square for list anchors and jump grid |
| `metroStickyLetterHeader` | LazyColumn sticky letter section marker (pins until next letter pushes it) |
| `MetroMessageDialog` | Centered modal dialog |

## Usage (target)

```kotlin
MetroTheme {
    Box(Modifier.fillMaxSize()) {
        Column {
            MetroPageHeader(title = "settings")
            MetroPivot(titles = listOf("system", "applications")) { index ->
                when (index) { /* ... */ }
            }
        }
        // Bottom application bar. Collapsed: icon row + `…`. Expanded: labels + text menu.
        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(MetroSystemIconType.Add, label = "new", onClick = {}),
                MetroAppBarIcon(MetroSystemIconType.Search, label = "search", onClick = {}),
            ),
            menuItems = listOf(MetroAppBarMenuItem("about this app") { }),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
```

`MetroAppBarIcon` also accepts a custom `icon: @Composable (Color) -> Unit` glyph for app-specific
artwork (e.g. a phone or envelope), so every app routes its overflow actions through the same bar.

## Build

```bash
./gradlew build
./gradlew test
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)
