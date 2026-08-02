# Volume

**Package:** `com.metro.volume`  
**Tier:** 0 (Metro Shell)

## Status

**Implemented** — standalone WP8.1 volume HUD overlay. Hardware Volume Up/Down (via
accessibility key filter) shows the charcoal collapsed strip; expand for dual sliders
(ringer 0–10 / media 0–30), mute, and VIBRATE. In-call shows a single call volume slider.

## App role

Owns the WP8.1 **volume control overlay**. Not part of statusbar — a separate shell APK so
volume key ownership and HUD lifecycle stay independent of the system tray.

## Permissions required

1. **Display over other apps** (`SYSTEM_ALERT_WINDOW`)
2. **Accessibility service** (`VolumeAccessibilityService`) — required for volume key filter
   **and** `TYPE_ACCESSIBILITY_OVERLAY` hosting above the system status bar

## Surface inventory

### 1. Collapsed HUD

- `NN/max` + stream label + down chevron
- References: `references/images/volume_collapsed_ringer_dark_cyan.jpg`,
  `references/images/volume_collapsed_media_dark_cyan.jpg`

### 2. Expanded HUD

- Dual sliders + mute + VIBRATE (or single call slider in-call)
- See `references/known-gaps.md` for missing expanded captures

## Commands

```bash
cd apps/volume

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test

# From repo root
../../scripts/verify-app.sh volume
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 | Android | Compromise |
|-------|---------|------------|
| System owns volume keys | Keys only filterable via a11y | Require Volume accessibility service |
| Native HUD never shows | Consuming keys suppresses it | Intentional **only while overlay FGS is running**; otherwise rockers fall through to the system so volume never bricks |
| Accessory-specific streams | Complex routing | v1: ringer / media / in-call only |
| Always-on system volume chrome | Empty overlay windows can steal input after crashes | HUD WindowManager view is attached only while visible |

## Agent postmortem

_None._
