# Notifications

**Package:** `com.metro.notifications`  
**Tier:** 0 (Metro Shell)

## Status

**Implemented** — standalone WP8.1 toast overlay. High-importance Android notifications raise a
Metro toast banner instead of the stock heads-up. The overlay window is attached only while a
toast is visible (same attach/detach pattern as volume).

This app does **not** own the status tray, Action Center, or any shade. The tray stays in
`com.metro.statusbar` and is not hidden, shown, or opened by this package.

## App role

Owns WP8.1 **toast banners** (replacing Android heads-up). Light shell overlay, like volume.

## Permissions required

1. **Display over other apps** (`SYSTEM_ALERT_WINDOW`)
2. **Accessibility service** (`NotificationsAccessibilityService`) — `TYPE_ACCESSIBILITY_OVERLAY`
   above the system status bar
3. **Notification access** (`ActionNotificationListenerService`) — observe posted notifications
4. **`WRITE_SECURE_SETTINGS`** (adb grant) — suppress AOSP heads-up via
   `heads_up_notifications_enabled=0`

The setup screen’s **Show notifications** master toggle starts and stops the overlay FGS.
Boot auto-starts only when the toggle is on. Settings → **notifications** launches this
activity (`com.metro.settings` does not host toast setup itself). **toast timeout** is a
ListPicker (3 / 5 / 10 seconds, default 5). **show test toast** raises a sample banner when
the toggle is on and overlay + accessibility are granted.

```bash
adb shell appops set com.metro.notifications SYSTEM_ALERT_WINDOW allow
adb shell pm grant com.metro.notifications android.permission.WRITE_SECURE_SETTINGS
```

## Surface inventory

### 1. Toast

- Accent bar (~52dp) below the status-bar / cutout inset so it clears the notch
- Square app logo + one truncated line; no clock (the tray already shows time)
- Reference: `references/images/toast.png`
- Auto-dismiss after 3 / 5 / 10 seconds (setup ListPicker; default 5s); swipe right dismisses the banner; tap opens the notifying app
- Enter/exit: perspective 3D tile flip (`rotationX` 90° → 0°, reverse on dismiss)

## Commands

```bash
cd apps/notifications

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh notifications
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 | Android | Compromise |
|-------|---------|------------|
| System toast peek | No public pre-peek API | `heads_up_notifications_enabled=0` + Metro toast overlay |
| Heads-up off on all OEMs | Samsung/MIUI may ignore the global setting | Documented; AOSP/emulator is the v1 target |
| Overlay vs Metro tray | Two a11y overlays have undefined z-order | Toast window `y` is the status-bar/cutout inset so it sits below the tray and does not draw under the notch |

## Agent postmortem

_None._
