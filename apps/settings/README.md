# Settings

**Package:** `com.metro.settings`  
**Tier:** 2

## Status

Implemented v1 — Settings root, start+theme (accent colour combo; Background omitted), accent picker (20 WP8 colours), ease of access (7-step text size). Hosts `content://com.metro.system` preferences provider.

## App role

This app recreates the WP8.1 **Settings** experience and is the authoritative owner of system preference writes for metro-os. It controls theme mode, accent color, font scale, and other shell-wide settings surfaced through `MetroPreferences`.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Shared preference contract in `metro-system-sdk` understood before UI work

## Screen inventory

See [`references/guides/blueprint.md`](references/guides/blueprint.md).

| Screen | Status |
|--------|--------|
| Settings root (`system`) | Done |
| start+theme | Done |
| Accent colour picker | Done (20 official) |
| ease of access (text size) | Done |

## System functions and contracts

### Preference ownership

- This app owns writes to `MetroPreferences` and exports `MetroSystemPreferencesProvider` (`com.metro.system`)
- Other apps read via ContentResolver + observe `THEME_CHANGED`

### Broadcast contract

- Broadcast `THEME_CHANGED` on every relevant preference change (`theme_mode`, `accent_color`, `font_scale`)

### Official setting keys

- `theme_mode`
- `accent_color` (official palette hex)
- `font_scale` (7 discrete steps)

## Commands

```bash
cd apps/settings

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh settings
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| True OS-level ownership of all system visuals | App-layer suite on Android | Settings owns metro-os shared prefs + broadcasts; Android system chrome outside suite remains out of scope |
| Full system settings list | Large OEM surface | v1 implements personalization + ease of access text size only |
| start+theme Background ListPicker | Deferred | Theme stays dark (`theme_mode` default); UI matches Accent colour combo only |

## Agent postmortem

_None._
