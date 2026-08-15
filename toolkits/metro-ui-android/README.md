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
| `MetroToggleSwitch` | WP8.1 rectangular toggle (accent fill when on) |
| `MetroListPicker` | WP8.1 ListPicker — bordered field, inline inverted options panel |
| `MetroTextBox` | WP8.1 TextBox — light fill, black text, 3dp accent border when focused |
| `MetroPageHeader` | Large page title |
| `MetroJumpList` | Find-by-letter overlay (`#`, a–z, globe); accent = active |
| `MetroLetterTile` | Accent/inactive letter square for list anchors and jump grid |
| `metroStickyLetterHeader` | LazyColumn sticky letter section marker (pins until next letter pushes it) |
| `MetroMessageDialog` | Centered modal dialog |
| `MetroLoadingScreen` | Full-page await — centered label + dancing dots |
| `MetroLoadingDots` | Inline WP8.1 indeterminate dancing-dots indicator |
| `MetroPagePivotLoad` | Page enter — left-hinge 3D pivot + X slide + fade |
| `MetroPagePivotSwing` | Same hinge `rotateY` + fade as page pivot load, **no** X slide (Start tiles) |
| `MetroAppPivotShell` | Activity wrapper — pivot enter on launch, flip-out on Back then `finish()` |

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

`MetroAppBarIcon` also accepts a custom `icon: @Composable (Color) -> Unit` glyph for
**truly app-unique** artwork. Prefer a `MetroSystemIconType` first — extend the shared
catalog in this toolkit when the glyph will be reused.

### Shared icons

| API | Purpose |
|-----|---------|
| `MetroSystemIcon` / `MetroSystemIconType` | Chrome + common app-bar glyphs |
| `MetroMediaGlyph` / `MetroMediaGlyphIcon` | Music transport / shuffle / queue |
| `MetroAppGlyphs` | Suite app identity + third-party tile overrides (`metro_app_*`, `metro_tile_*`) |

App launcher foregrounds should alias `@drawable/metro_app_<name>` so Start tiles and
adaptive icons stay identical across the suite.

### Loading (await)

```kotlin
if (isLoading) {
    MetroLoadingScreen()                 // default "Loading..."
    // MetroLoadingScreen(message = "syncing…")
} else {
    Content()
}

// Inline indicator (e.g. inside a pane):
MetroLoadingDots()
```

### Page pivot load

```kotlin
MetroPagePivotLoad(
    modifier = Modifier.fillMaxSize(),
    loadKey = destination, // re-run enter when destination changes
) {
    PageContent()
}
```

Enter: `rotateY` 22.5° → 0°, `translationX` +15% width → 0, with fade-in (200ms ease-out). Exit tilt-back: `rotateY` 0° → −28°, `translationX` 0 → −15% width (hinge x +15%, softer camera), fade (280ms ease-out). Set `exiting = true` and `onExitComplete` before pop.

For hinge-only motion (no X slide) — e.g. Start tile enter with a shared page hinge:

```kotlin
MetroPagePivotSwing(
    loadKey = enterWaveKey,
    delayMs = diagonalIndex * 55L,
    cameraWidthPx = pageWidthPx,
    hingeInsetPx = tileLeftInPagePx,
    skipEnter = waveAlreadyPlayed, // keep tile content mounted at rest
) {
    TileContent()
}
```

### App pivot shell

```kotlin
// onCreate, before setContent:
MetroActivities.applyLaunchTransition(this)

MetroTheme { /* … */ ->
    MetroAppPivotShell(
        modifier = Modifier.fillMaxSize(),
        onExit = { MetroActivities.finishWithExitTransition(this@MainActivity) },
    ) {
        SetupScreenContent()
    }
}
```

Wraps an activity with page pivot enter on launch and flip-out on Back before
`finish()`. Call [MetroActivities.applyLaunchTransition] in `onCreate` so the
platform transition does not fight the Compose pivot. Nested `BackHandler`s
inside the content take priority for in-app navigation.

### TextBox

```kotlin
MetroTextBox(
    value = query,
    onValueChange = { query = it },
    placeholder = "search",
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
)
```

Light rectangle, black text, square 3dp border (gray at rest, accent when focused). Never a dark filled chip.

### ListPicker (dropdown)

```kotlin
MetroListPicker(
    label = "Background",
    options = listOf("dark", "light"),
    selectedOptionIndex = 0,
    onSelectOption = { /* ... */ },
)

// Or typed values:
MetroListPicker(
    label = "Background",
    selected = ThemeMode.Dark,
    options = listOf(
        MetroListPickerOption(ThemeMode.Dark, "dark"),
        MetroListPickerOption(ThemeMode.Light, "light"),
    ),
    onSelectedChange = { /* ... */ },
)
```

## Build

```bash
./gradlew build
./gradlew test
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)
