# Notifications — web resources

WP8.1 sources used for toast banners.

## Toast banner

| Source | URL | What it specifies |
|--------|-----|-------------------|
| Toast notification overview (Windows Runtime) | https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh779727(v=win.10) | Phone toast at top of screen; tap / dismiss / ignore; no buttons |
| Toast template catalog | https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh761494(v=win.10) | Phone renders ToastText02 variant + Square 150 logo, no payload image |
| UX interactions — toast | https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202884(v=vs.105) | Opaque accent bar, app icon, bold title + subtitle, ~10s |
| Toast ↔ Action Center | https://nokiapoweruser.com/this-is-how-toast-notifications-action-centre-interact-in-windows-phone-8-1/ | Tap / ignore / swipe-right dismiss (Action Center itself is out of scope for this app) |

## Reference images

| File | Source | Notes |
|------|--------|-------|
| `images/toast.png` | User-provided WP8.1 toast capture (Facebook, magenta accent) | Icon + one truncated line. Clock in the capture is the system tray — do not duplicate it on the toast. |
| `images/action_center_dark_cyan.png` | Microsoft WP8.1 Action Center marketing capture (user-provided) | App-glyph treatment only; shade UI is out of scope |
