# Settings

**Package:** `com.metro.settings`  
**Tier:** 2

## Status

Implemented — Settings root with `system` | `applications` pivot (all launchable user + system apps; in-Settings app detail with toggles/open/uninstall), start+theme (accent colour only), accent picker (20 WP8 colours), ease of access (10-step text size), brightness, storage sense, navigation bar / status bar / notifications / volume (launch shell setup apps), keyboard (launches `com.metro.keyboard`), and about (WP8.1 more info device details; Software = metro-os alpha-3). Hosts `content://com.metro.system` preferences provider.

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
| Settings root (`system` \| `applications` pivot) | Done |
| applications list | Done (all launchable user + system apps) |
| application detail | Done (info, background/notifications toggles, open, uninstall) |
| start+theme | Done (accent colour only) |
| Accent colour picker | Done (20 official) |
| ease of access (text size) | Done |
| brightness | Done (`WRITE_SETTINGS` when granted) |
| storage sense | Done — usage bar + open files → `com.metro.files` |
| navigation bar | Done (launches `com.metro.navbar` setup) |
| status bar | Done (launches `com.metro.statusbar` setup) |
| notifications | Done (launches `com.metro.notifications` setup) |
| volume | Done (launches `com.metro.volume` setup) |
| keyboard | Done (launches `com.metro.keyboard` settings) |
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
- `font_scale` (10 discrete steps, 0.625–1.6, default 1.0)

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
| Full system settings list | Large OEM surface | v1 implements personalization + brightness, storage, shell setup launches, keyboard, about + applications pivot |
| Applications hubs (IE, photos+camera, …) live inside Settings | Suite apps lack per-app hubs | Shared application detail page for every launchable package |
| OS notification / background kill for other packages | Privileged AppOps | Toggles store metro-os `metro_app_policy` prefs; suite enforcement when agents exist |
| Uninstall confirmation | Package installer is a system activity | `MetroMessageDialog` then `ACTION_DELETE` (same as launcher); not Android Settings |
| start+theme Background ListPicker | Deferred | Theme stays dark (`theme_mode` default); UI matches Accent colour combo only |
| Brightness write | Needs `WRITE_SETTINGS` app-op | Write `Settings.System` directly; grant via `adb shell appops set com.metro.settings WRITE_SETTINGS allow` |
| Never open Android Settings from Metro Settings | Permission grant UIs are system activities | No in-app “open settings” buttons; grant permissions out-of-band (adb / privileged install) |
| Settings → keyboard inside system Settings hub | Keyboard is a separate suite APK | Root `keyboard` row launches `com.metro.keyboard` (not Android Settings) |
| Settings → navigation bar / status bar / notifications / volume | Overlay + accessibility grants are per shell package | Root rows launch `com.metro.navbar` / `com.metro.statusbar` / `com.metro.notifications` / `com.metro.volume` setup (not Android Settings) |
| About IMEI / MAC / SIM ID | Need telephony / Wi-Fi MAC permissions | Omitted; more info shows Build.* and storage fields without privileged identifiers |
| ease of access Text size has 7 steps (0.85–1.6) | Modern panels are far denser than a 4.5" WVGA Lumia, so 0.85 is still large | Slider keeps the 7 WP8.1 steps and prepends 0.625 / 0.7 / 0.775 at the same 0.075 spacing (10 total, default 1.0 unchanged) |

## Agent postmortem

_None._
