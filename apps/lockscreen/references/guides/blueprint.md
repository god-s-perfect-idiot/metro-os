# Lock screen — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Goal (v1)

Render a Metro lock **surface above the Android system keyguard** without replacing biometric unlock. Swipe up hands control to the system credential UI (PIN / pattern / password). Fingerprint and face unlock continue to work while the Metro surface is visible.

## Pages

### Page 1 — Lock surface

- **Layout:** Full-bleed solid color fill (system accent) with WP8.1 lock chrome:
  - Transparent Metro status tray at the top (safe status-bar / cutout inset; **no bar background** — icons sit on the fill). Left: cellular + Wi-Fi; right: battery (+ charging plug). No tray clock.
  - Large light time (left-aligned, mid/lower-left)
  - Weekday on the next line
  - Month + day (no year) on the next line
  - Optional next calendar appointment below (title, optional location, time range / `All day`)
  - **Quick status:** up to five user-chosen apps along the bottom edge; each shows a white monochrome glyph and a naked count (capped at `99+`) when notification access is granted and the app has unread notifications
- **Presentation:** `TYPE_ACCESSIBILITY_OVERLAY` hosted by `LockscreenAccessibilityService` (attached from `LockscreenHostService`) so it draws **over** the system lock screen when the keyguard is locked and the **default display is fully awake** (`Display.STATE_ON` + interactive). Never present over screen-off or AOD/doze (unless **Glance lockscreen** is enabled — see below). Retries on screen-on / keyguard-locked because keyguard state can lag briefly.
- **Navigation:** Not a navigable app page. Requires accessibility enabled + master toggle.
- **Interactions:**
  - **Fingerprint / Face unlock:** Pass through to the system. On successful biometric unlock, remove the overlay (`USER_PRESENT` / keyguard unlocked).
  - **Swipe up:** Lock fill **tracks the finger** upward. Release **below** the unlock threshold → spring bounce back (no SystemUI / biometric). Release **at or past** threshold → panel animates fully off-screen, then SystemUI’s credential / biometric bouncer is shown via `KeyguardManager.requestDismissKeyguard` (transparent trampoline only — **no custom auth UI**). After a committed swipe, the Metro fill stays suppressed until the next screen-off / lock — biometric fail or cancel must not restore it.
  - **Screen off:** Remove the overlay; re-attach on next screen-on while locked.
- **Background:** Chosen in setup via **Background** ListPicker:
  - **Accent colour** — solid system accent (`MetroPreferences` accent). Chrome uses `MetroColors.tileContentColor(accent)`.
  - **Custom background** — cropped user photo (same choose-photo / pan-pinch crop / remove UI as Settings Start background). Falls back to accent until a photo is saved.
  - **Bing wallpaper** — Bing picture of the day (HPImageArchive), cached locally and refreshed ~daily. Falls back to accent when no cache and offline.
  Photo modes use white chrome.
- **Calendar:** Next remaining appointment from device `CalendarContract` when `READ_CALENDAR` is granted; omit the event block when permission is missing or no upcoming event exists. Clock ticks on the minute boundary.

### Page 1b — Glance surface (optional)

- **Layout:** Same chrome as Page 1 (time, weekday, date, optional next calendar event) on a **pure black** (`#000000`) fill — no wallpaper, no status tray. Quick-status notification icons remain along the bottom edge.
- **Presentation:** When **Glance lockscreen** is enabled in setup, draws over the system Always-On Display while the keyguard is locked and the display is not fully awake (`!STATE_ON` + interactive). Also covers the brief screen-off transition before doze engages. Non-touchable; tap-to-wake passes through to the system. Never presents while battery saver is on or the display is fully off (`STATE_OFF`).
- **Interactions:** Passive only — no swipe-up. Lifts when the display becomes fully awake (regular lock surface takes over) or the keyguard unlocks.

### Page 2 — Setup

- **Layout:** Fixed `LOCK SCREEN` app overline + hub title `customisation` (do not scroll). Scrollable body: master toggle + **Glance lockscreen** toggle (visible when master is on) + **Background** ListPicker + **notifications** quick-status slot row (five bordered squares; empty slots show `+`; tap opens **choose an app** page) + accent **permissions** section with body copy and accessibility / notifications / notification access / calendar / phone-state / full-screen unlock grants.
- **Navigation:** Launcher → Lock screen app.
- **Interactions:** Enable accessibility, then master **Show lock screen** toggle starts/stops the host FGS. Boot respects the same flag. Calendar permission is optional (enables the appointment line). **Background** ListPicker selects Accent colour / Custom background / Bing wallpaper. Choosing **Custom background** reveals the Start-background-style thumb + **choose photo** (+ **remove** when set) → system photo picker → crop page. **Quick status:** tap each of five slot buttons → `MetroAppPickerScreen` (`none` + all launchable apps) → persist package per slot; overlay reads counts via `LockscreenNotificationListenerService` when notification access is on.
- **Background:** Theme background via `MetroTheme`.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `images/lock_bing_homepage_dark.jpg` | Page 1 | Official Lumia WP8.1 lock screen (Bing wallpaper). Chrome + Bing fill target; Accent / Custom modes also supported. |

## Out of scope (v1)

- Detailed status app picker (calendar event line uses `READ_CALENDAR` directly)
- Alarm glyph beside the clock
- Camera / flashlight quick actions
- Replacing Android keyguard / Device Owner lock replacement
