# metro-system-sdk

Inter-app communication and system preference layer for metro-os.

## Status

## Status

**Scaffolded** — `MetroPreferences`, `MetroIntents`, `MetroBroadcasts`, preference keys.

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Integration (target)

```kotlin
// Read theme
val dark = MetroPreferences.getThemeMode(context) == ThemeMode.DARK
val accent = MetroPreferences.getAccentColor(context)

// Listen for changes
MetroPreferences.themeFlow(context).collect { /* recompose */ }

// Launch another metro app
startActivity(MetroIntents.launchApp("com.metro.browser"))
```
