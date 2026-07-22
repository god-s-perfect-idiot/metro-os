# Agent instructions — metro-system-sdk

**Phase 1 — build after `metro-ui-android`.**

## Purpose

Cross-app system contracts: shared preferences, intents, broadcasts, content provider URIs.

## Package

`com.metro.system`

## Required APIs

### MetroPreferences

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `theme_mode` | `enum { dark, light }` | `dark` | System theme |
| `accent_color` | `String` (hex) | `#1BA1E2` | Accent from palette |
| `font_scale` | `Float` | `1.0` | Font scaling |
| `nav_bar_color` | `String?` | null | Nav bar override |

Storage: `SharedPreferences` file `metro_system` (MODE_MULTI_PROCESS or ContentProvider-backed).

### MetroIntents

| Constant | Action | Extras |
|----------|--------|--------|
| `LAUNCH_APP` | `com.metro.action.LAUNCH_APP` | `package` |
| `SEARCH` | `com.metro.action.SEARCH` | `query` |
| `SHARE` | `com.metro.action.SHARE` | `uri`, `mime` |
| `PIN_TILE` | `com.metro.action.PIN_TILE` | `tile_id` |

### MetroBroadcasts

| Action | Payload |
|--------|---------|
| `com.metro.system.THEME_CHANGED` | `theme_mode`, `accent_color`, `font_scale` |

### ContentProvider

Authority: `com.metro.system` — **hosted by Settings** (`MetroSystemPreferencesProvider`)

| URI path | Returns |
|----------|---------|
| `/preferences` | All system preferences |
| `/preferences/{key}` | Single preference value |
| `/apps` | Installed metro apps registry |

## Rules

- No UI code in this module (types only from metro-ui-android for Color parsing).
- All preference keys are constants — never string literals in apps.
- Settings app is the **writer**; all other apps are **readers** + broadcast receivers.
- Theme change must propagate within 1 frame — use `Flow` or broadcast.

## Verify

```bash
./scripts/verify-toolkit.sh metro-system-sdk
```

## Tests required

- Read/write round-trip for each preference key
- Broadcast received when theme changes
- Intent filter resolution for `LAUNCH_APP`
