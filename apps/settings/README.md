# Settings

**Package:** `com.metro.settings`  
**Tier:** 2

## Status

Implemented — Settings root with start+theme (accent colour only), accent picker (20 WP8 colours), ease of access (7-step text size), brightness, storage sense, and about (WP8.1 more info device details; Software = metro-os alpha-3). Hosts `content://com.metro.system` preferences provider.

## App role

This app recreates the WP8.1 **Settings** experience and is the authoritative owner of system preference writes for metro-os. It controls accent color, font scale, and surfaces a small set of system pages (brightness, storage, about).

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Shared preference contract in `metro-system-sdk` understood before UI work

## Screen inventory

See [`references/guides/blueprint.md`](references/guides/blueprint.md).

| Screen | Status |
|--------|--------|
| Settings root (`system`) | Done |
| start+theme | Done (accent colour only) |
| Accent colour picker | Done (20 official) |
| ease of access (text size) | Done |
| brightness | Done (`WRITE_SETTINGS` when granted) |
| storage sense | Done |
| about / more info | Done (device information; Software = metro-os alpha-3) |

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
| Full system settings list | Large OEM surface | v1 implements personalization + brightness, storage, about only |
| start+theme Background ListPicker | Deferred | Theme stays dark (`theme_mode` default); UI matches Accent colour combo only |
| Brightness write | Needs `WRITE_SETTINGS` app-op | Write `Settings.System` directly; grant via `adb shell appops set com.metro.settings WRITE_SETTINGS allow` |
| Never open Android Settings from Metro Settings | Permission grant UIs are system activities | No in-app “open settings” buttons; grant permissions out-of-band (adb / privileged install) |
| About IMEI / MAC / SIM ID | Need telephony / Wi-Fi MAC permissions | Omitted; more info shows Build.* and storage fields without privileged identifiers |

## Agent postmortem

_None._
