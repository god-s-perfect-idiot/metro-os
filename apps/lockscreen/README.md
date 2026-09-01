# Lock screen

**Package:** `com.metro.lockscreen`  
**Tier:** 0 (Metro Shell)

## Status

**Implemented** — lock fill (accent / custom photo / Bing wallpaper) over the system keyguard with
WP8.1 lock chrome (transparent status tray icons, time, day, date, next calendar event, quick-status
glyphs with counts). Swipe up reveals the system unlock screen; fingerprint / face unlock continue to work.

## App role

Owns the WP8.1 **lock screen** surface drawn above Android’s keyguard. Does not replace
biometric or credential authentication — those stay with the system.

## Permissions required

1. **Accessibility service** (`LockscreenAccessibilityService`) — required to host a
   `TYPE_ACCESSIBILITY_OVERLAY` above the system keyguard (same approach as volume/statusbar)
2. **Foreground service** (`FOREGROUND_SERVICE` / `SPECIAL_USE`) — listens for screen /
   keyguard state and attaches/removes the overlay
3. **Notifications** (`POST_NOTIFICATIONS`) — needed for the full-screen-intent fallback that
   starts the SystemUI unlock trampoline when accessibility activity starts are blocked
4. **Full-screen intent** (`USE_FULL_SCREEN_INTENT`, Android 14+) — same FSI fallback path
5. **Calendar** (`READ_CALENDAR`) — optional; shows the next appointment on the lock chrome
6. **Boot completed** — restart host when the master toggle is on
7. **Notification listener** (`LockscreenNotificationListenerService`) — optional; drives quick-status counts

A plain `showWhenLocked` activity started from the host FGS is blocked by Android
background-activity restrictions most of the time. The lock **fill** uses
`TYPE_ACCESSIBILITY_OVERLAY`; swipe-up prefers accessibility `startActivity`, then
`PendingIntent.send`, then an FSI notification. The trampoline is transparent and only calls
`requestDismissKeyguard` (no custom auth UI).

**Android 15+:** do not pass `pendingIntentBackgroundActivityStartMode` into
`PendingIntent.getActivity` — that throws and kills the process mid-swipe.

## Surface inventory

### 1. Lock surface

- Full-bleed fill: system accent, cropped custom photo, or Bing picture of the day
- Transparent Metro status tray at the top (safe inset; no background bar)
- Large time + weekday + date; optional next calendar event; quick-status row (up to 5 apps + counts)
- References: `references/images/lock_bing_homepage_dark.jpg`

### 2. Setup

- Master **Show lock screen** toggle
- **Background** ListPicker (Accent colour / Custom background / Bing wallpaper)
- **Quick status** — five bordered app slots (+ when empty); tap opens **choose an app** page
- Custom background: Start-background-style thumb + choose photo / remove + crop
- Accessibility / notifications / notification access / calendar / full-screen unlock grants

## Commands

```bash
cd apps/lockscreen

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test

# From repo root
../../scripts/verify-app.sh lockscreen
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 | Android | Compromise |
|-------|---------|------------|
| System owns lock chrome | Cannot replace Keyguard as a normal app | Accessibility overlay above stock keyguard |
| Activity over lock from FGS | Background activity starts blocked | `TYPE_ACCESSIBILITY_OVERLAY` for the fill |
| Drag up → password | No public bouncer-only API | Remove overlay + dismiss helper |
| Biometrics under custom UI | Fingerprint hint may be covered | Sensor still works; tear down on unlock |
| Photo wallpaper + glyphs | Bottom notification glyphs deferred | Quick-status row (5 apps + 99+ counts) when notification access granted |
| Live cellular bars | Needs `READ_PHONE_STATE` | Setup grants phone state; Wi-Fi + battery always |

## Agent postmortem

_None._
