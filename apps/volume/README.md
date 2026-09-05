# Volume

**Package:** `com.metro.volume`  
**Tier:** 0 (Metro Shell)

## Status

**Implemented** — standalone WP8.1 volume HUD overlay. Hardware Volume Up/Down (via
accessibility key filter) shows the charcoal collapsed strip; when music is playing the
default view is the Universal Volume Control (media level + prev/pause/next + title/artist).
Expand for dual sliders (ringer 0–10 / media 0–30), mute, silent mode, and sound settings.
In-call shows a single call volume slider.

## App role

Owns the WP8.1 **volume control overlay**. Not part of statusbar — a separate shell APK so
volume key ownership and HUD lifecycle stay independent of the system tray.

## Permissions required

1. **Display over other apps** (`SYSTEM_ALERT_WINDOW`)
2. **Accessibility service** (`VolumeAccessibilityService`) — required for volume key filter
   **and** `TYPE_ACCESSIBILITY_OVERLAY` hosting above the system status bar
3. **Notification access** (optional) — unlocks music transport for third-party players via
   `MediaSessionManager`; suite Music works through a direct Media3 session bind without it

The setup screen’s **Show volume controls** master toggle starts and stops the overlay FGS.
Boot auto-starts only when the toggle is on. Off stops the overlay and lets volume rockers fall
through to Android’s stock HUD.

## Surface inventory

### 1. Collapsed HUD

- `NN/max` + stream label + down chevron
- References: `references/images/volume_collapsed_ringer_dark_cyan.jpg`,
  `references/images/volume_collapsed_media_dark_cyan.jpg`

### 1b. Music transport (default while playing)

- Media volume header + prev / play-pause / next + title / artist
- Reference: `references/images/volume_music_transport_dark.png`

### 2. Expanded HUD

- Dual sliders + mute + silent mode / sound settings (or single call slider in-call)
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
| Accessibility overlay on AOD | `TYPE_ACCESSIBILITY_OVERLAY` can cover Always-On Display | Gate on fully awake display (`STATE_ON`); remove overlay on screen-off / doze; rockers fall through while asleep |
| Ringer/call UI is 0–10 | `AudioManager` maxima are often 5–7; some devices clamp absolute volume writes | Keep WP ticks as HUD source of truth while the rocker/HUD is driving; verify writes and nudge with `adjustStreamVolume` when needed |
| SMTC reads any music app | `MediaSessionManager` needs notification-listener in this package | Suite Music binds via Media3 `SessionToken`; optional Volume notification listener covers other players |

## Agent postmortem

_None._
