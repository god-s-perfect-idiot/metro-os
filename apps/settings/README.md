# Settings

**Package:** `com.metro.settings`  
**Tier:** 2

## Status

Harness docs only — Android project not scaffolded yet. This README is the implementation brief for the WP8.1 settings app.

## App role

This app recreates the WP8.1 **Settings** experience and is the authoritative owner of system preference writes for metro-os. It controls theme mode, accent color, nav bar color, and other shell-wide settings surfaced through `MetroPreferences`.

This is one of the most important apps in the suite because its writes affect every other app. Treat its contracts as platform-level, not app-local convenience logic.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Shared preference contract in `metro-system-sdk` understood before UI work

## Screen inventory

### 1. Settings list / hierarchy root

- Entry point mirroring WP8.1 settings structure
- Expected reference: `references/images/root_dark_blue.png`

### 2. Theme settings

- Dark/light theme selection
- Expected reference: `references/images/theme_dark_blue.png`

### 3. Accent color picker

- Official palette only
- Expected reference: `references/images/accent_dark_blue.png`

### 4. Navigation bar color settings

- Controls shell nav bar appearance
- Expected reference: `references/images/navbar_dark_blue.png`

### 5. Font size / related display settings

- Allows user-facing text scaling within approved bounds
- Expected reference: `references/images/font_dark_blue.png`

## System functions and contracts

### Preference ownership

- This app owns writes to `MetroPreferences`
- Other apps read and observe; they should not invent competing write flows for these keys

### Broadcast contract

- Broadcast `THEME_CHANGED` on every relevant preference change
- Changes must propagate suite-wide immediately

### Official setting keys

- `theme_mode`
- `accent_color`
- `nav_bar_color`
- `font_scale`

Do not create ad hoc preference names without first updating shared contracts.

## UI and interaction guardrails

- Mirror WP8.1 settings hierarchy and plain list presentation
- No Material switches or preference screens
- Use Metro controls only
- Accent picker must use the exact official palette from `scope.md`
- Avoid Android-specific affordances that break the WP8.1 illusion

## Data and state model

- `SettingsSection`, `SettingItem`, `ThemeMode`, `AccentOption`
- Keep persisted preference values and transient picker UI state separate
- Apply validation for font scale and color values at the repository layer

## Primary implementation order

1. Implement shared settings repository over `MetroPreferences`
2. Build settings root hierarchy
3. Implement theme mode selection
4. Implement accent picker
5. Implement nav bar color settings
6. Implement font size and any approved display options
7. Verify cross-app propagation with launcher, statusbar, and another consumer app

## Test-critical user flows

1. Change dark/light theme and observe suite-wide update
2. Change accent color and observe suite-wide update
3. Change nav bar color and observe navbar update
4. Change font scale and observe compliant app updates
5. Relaunch app and retain settings

## Reference and golden expectations

- `references/images/root_dark_blue.png`
- `references/images/theme_dark_blue.png`
- `references/images/accent_dark_blue.png`
- `references/images/navbar_dark_blue.png`
- `references/images/font_dark_blue.png`

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
| True OS-level ownership of all system visuals | This is an app-layer suite running on Android | Settings owns the metro-os shared preference surface and broadcasts changes, while Android system visuals outside suite control remain out of scope |

## Agent postmortem

_None._
