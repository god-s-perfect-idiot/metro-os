# Notifications — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Pages

### Page 1 — Toast banner

- Layout: full-width **accent** band from the top of the screen (tray inset + banner). The whole band flips as one tile behind an opaque accent Metro tray so the pivot is not offset to the safe inset; icon/text sit only in the banner below the tray. Reference: `images/toast.png`.
  - **Square app logo** on the left (manifest / package icon, never a payload image)
  - **Message copy** white text next to the icon:
    - Group chats: **conversation / group name on the top row always** (from MessagingStyle `EXTRA_CONVERSATION_TITLE` when distinct from the sender). Below that:
      - Default: single-line `sender: message`
      - Optional **Two-row view**: `title` then `description`
    - Non-group: default single-line `sender: message`; optional two-row stacks title over description. Each line ellipsizes; banner height grows with row count (1–3).
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
- **Two-row view** toggle — off by default (`title: description` on one line); when on, distinct title/body stack on two rows
- **show test toast** (enabled when the toggle is on and grants are in place) raises a sample banner so the overlay can be checked without a real notification

## System behavior

| Signal | Behavior |
|--------|----------|
| Overlay | `SYSTEM_ALERT_WINDOW` FGS, hosted as `TYPE_ACCESSIBILITY_OVERLAY` when a11y connected |
| Window lifetime | Attach only while a toast is visible; never an always-on hit target |
| Heads-up | While FGS is running, set `heads_up_notifications_enabled=0` (restore on stop) |
| Theme | Observe `THEME_CHANGED` |
| Tray | Toast attaches at y=0 first; then Metro tray tints opaque accent + rehosts above so the full band flips behind the glyphs. Tray accent morphs away with the exit flip. |

## Images

| Image | Page | Notes |
|-------|------|-------|
| `toast.png` | Toast banner | Magenta accent; icon + one truncated line. Clock in the capture is the system tray — omit it on the toast. |

## Out of scope (v1)

- Action Center / notification shade / quick actions
- Driving or hiding the status tray
- Lock-screen toast
- OEM skins that ignore `heads_up_notifications_enabled`
