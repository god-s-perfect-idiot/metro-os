# Volume — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

## Pages

### Page 1 — Volume HUD collapsed

- Trigger: hardware Volume Up / Down (consumed by this app’s accessibility key filter)
- Placement: overlay window pinned at y=0 over the tray/cutout; charcoal content uses top inset padding so it sits just below the Metro status tray (no notch clipping)
- Layout (L→R): large white zero-padded level `NN`, muted gray `/max`, stream label, far-right **down** chevron
- Stream labels (exact): `Ringer + Notifications` (max **10**), `Media + Apps` (max **30**), or `Call volume` (max **10**) while in-call
- Default stream: in-call → call; else if media is active → media; else → ringer
- Auto-dismiss: **2500ms** after last rocker / touch
- Interactions: rockers adjust the active stream (hold continues stepping after ~400ms); tap strip or chevron → expand
- Show / hide motion: top-anchored height wipe creeping in from 0 and out to 0, **200ms** ease-out (`VolumeHudSpec.SHOW_HIDE_MS` / `MetroTransitions.PageEasing`); overlay window is removed only after the hide wipe finishes
- Expand / collapse motion: top-anchored height wipe, **200ms** ease-out (`VolumeHudSpec.EXPAND_COLLAPSE_MS` / `MetroTransitions.PageEasing`); overlay window height snaps at boundaries (not per-frame)

### Page 1b — Volume HUD music transport (default while playing)

- When an active music session is present (not in-call, not expanded), the **default** rocker view is the WP8.1 Universal Volume Control instead of the thin strip
- Layout (top → bottom):
  1. Same volume header as collapsed: `NN/30 Media + Apps` + down chevron (tap expands to dual sliders)
  2. Left-aligned circular **Previous / Play-Pause / Next** (`MetroMediaTransportButton`, white ring)
  3. Track **title** (white SemiBold 18sp) + **artist** (same size/color, Light weight), left-aligned
- Transport actions reset the auto-dismiss timer; play shows when paused
- Session sources: suite Music via Media3 `SessionToken` (no extra grant); other music apps via optional notification-listener → `MediaSessionManager`
- Height: `VolumeHudSpec.MUSIC_TRANSPORT_HEIGHT_DP` (168); expand still opens Page 2 dual sliders

### Page 2 — Volume HUD expanded

- Dual rows (non-call), **fixed order** (never swap when the other stream becomes active):
  1. **Ringer + Notifications** — header `NN` (large white) `/10` (muted) + gray label; next row: **bell** mute icon **left of** continuous accent slider (0–10)
  2. **Media + Apps** — same pattern with note icon + slider (0–30)
- Actions row: muted-speaker glyph + **SILENT MODE ON** (accent) / **SILENT MODE OFF** (white); gear + **SOUND SETTINGS** (white, opens system sound settings)
- Silent mode ON (one tap): accent highlight + set **all** stream levels to 0 (ringer / media / call); OFF restores prior levels
- Smaller **up** chevron below the actions row, right-aligned (not inline with the actions)
- In-call expanded: single **Call volume** slider only (0–10)
- Mute: tapping the stream icon sets that stream to 0 (or restores the previous level)
- Always dark charcoal chrome, independent of light Start
- Sliders use `MetroBarStepSlider` (solid fill, no tick notches)
- Collapse motion: same 200ms top-anchored height wipe as expand

## System behavior

| Signal | Behavior |
|--------|----------|
| Master toggle | Setup **Show volume controls** starts/stops the overlay FGS; boot respects the same flag |
| Overlay | `SYSTEM_ALERT_WINDOW` FGS, hosted as `TYPE_ACCESSIBILITY_OVERLAY` when a11y connected |
| Keys | When enabled, `FLAG_REQUEST_FILTER_KEY_EVENTS` consumes volume rockers (suppresses Android stock HUD); when off, rockers fall through |
| Lock / AOD | May present over an awake lock screen; never over display-off or AOD/doze — rockers fall through and any attached overlay is removed on screen-off |
| Theme | Observe `THEME_CHANGED` for accent on sliders / silent-mode ON |
| Scales | WP display scales map proportionally onto `STREAM_RING` / `STREAM_MUSIC` / `STREAM_VOICE_CALL`; WP ticks stay source of truth across coarse Android maxima |

## Images

| Image | Page | Notes |
|-------|------|-------|
| `volume_collapsed_ringer_dark_cyan.jpg` | Collapsed | `07/10 Ringer + Notifications` |
| `volume_collapsed_media_dark_cyan.jpg` | Collapsed | `15/30 Media + Apps` |
| `volume_music_transport_dark.png` | Music transport | `15/30 Media + Apps` + prev/pause/next + title/artist |

## Out of scope (v1)

- Headphone / Bluetooth accessory-specific volume rows
- Metro Settings → ringtones+sounds deep-link (SOUND SETTINGS opens Android sound settings for now)
