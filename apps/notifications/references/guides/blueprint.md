# Notifications — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Pages

### Page 1 — Toast banner

- Layout: full-width **accent** bar (~52dp) pinned **below** the status-bar / cutout inset so the banner never draws under a notch. Reference: `images/toast.png` (clock in that capture is tray chrome — do not draw a second clock on the toast).
  - **Square app logo** on the left (manifest / package icon, never a payload image)
  - **Single-line** white text next to the icon as `sender: message` when both parts exist (MessagingStyle / social title+body); overflow uses ellipsis (no wrap onto a second line)
- Navigation: tap launches the notifying app via the notification content intent.
- Interactions: auto-dismiss after the setup **toast timeout** (3 / 5 / 10 seconds, default **5 seconds**); **swipe right** dismisses the banner. One toast at a time; a new peek replaces the current banner.
- Do not show for ongoing/FGS, shell packages, or active-call notifications.
- Android notification **groups**: allow the alerting post (often the group summary). Debounce identical copy in the same group burst so child + summary do not double-peek; newer content replaces the banner.
- Listener connect re-delivers every shade notification — mark actives as seen so past posts do not replay as toasts; only new / updated peeks raise a banner.
- Motion: **perspective 3D tile flip** enter (`rotationX` 90° → 0°, `JumpListFlipMs` 300ms ease-out, camera distance from banner width so the bar reads as a trapezoid not a squash). Exit is the same flip in reverse (0° → 90°), then the overlay window is removed. Timeout, tap, and swipe-right all use that reverse flip.

### Page 2 — Setup

- Opened from Settings → `notifications` (`com.metro.settings` launches this MainActivity). Does not live inside Settings.
- Overlay grant, accessibility enable, notification access, master **Show notifications** toggle
- Overlay FGS runs when the toggle is on; the WindowManager view is attached only while a toast is visible
- **toast timeout** ListPicker — 3 seconds / 5 seconds / 10 seconds (default 5). Uses `MetroListPicker`, not a Material menu
- **show test toast** (enabled when the toggle is on and grants are in place) raises a sample banner so the overlay can be checked without a real notification

## System behavior

| Signal | Behavior |
|--------|----------|
| Overlay | `SYSTEM_ALERT_WINDOW` FGS, hosted as `TYPE_ACCESSIBILITY_OVERLAY` when a11y connected |
| Window lifetime | Attach only while a toast is visible; never an always-on hit target |
| Heads-up | While FGS is running, set `heads_up_notifications_enabled=0` (restore on stop) |
| Theme | Observe `THEME_CHANGED` |
| Tray | This app does not talk to `com.metro.statusbar`. The toast window is offset by the status-bar/cutout inset (`MetroStatusBar.HEIGHT_DP` minimum) so it sits below the tray and clears the notch. |

## Images

| Image | Page | Notes |
|-------|------|-------|
| `toast.png` | Toast banner | Magenta accent; icon + one truncated line. Clock in the capture is the system tray — omit it on the toast. |

## Out of scope (v1)

- Action Center / notification shade / quick actions
- Driving or hiding the status tray
- Lock-screen toast
- OEM skins that ignore `heads_up_notifications_enabled`
