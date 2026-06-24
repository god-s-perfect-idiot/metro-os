# metro-ui-android

WP8.1 Compose component library for metro-os.

## UX language

Per-control shape, button, and interaction rules: [`METRO-UX-LANGUAGE.md`](METRO-UX-LANGUAGE.md). Agents must read this before implementing UI.

## Status

**Scaffolded** — P0 components (`MetroTheme`, `MetroText`, `MetroPageHeader`, `MetroTile`, `MetroTransitions`, `MetroColors`).

## Public API (planned)

| Composable | Description |
|------------|-------------|
| `MetroTheme` | Root theme wrapper |
| `MetroAppBar` | Bottom application bar |
| `MetroPivot` | Tabbed pivot navigation |
| `MetroPanorama` | Horizontal hub panorama |
| `MetroListItem` | List row with tilt-on-press |
| `MetroToggleSwitch` | WP8.1 toggle |
| `MetroPageHeader` | Large page title |
| `MetroMessageDialog` | Centered modal dialog |

## Usage (target)

```kotlin
MetroTheme {
    MetroPageHeader(title = "settings")
    MetroPivot(titles = listOf("system", "applications")) { index ->
        when (index) { /* ... */ }
    }
    MetroAppBar(
        buttons = listOf(MetroAppBarIcon(Icons.Add, onClick = {})),
        menuItems = listOf(MetroAppBarMenuItem("about") { }),
    )
}
```

## Build

```bash
./gradlew build
./gradlew test
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)
