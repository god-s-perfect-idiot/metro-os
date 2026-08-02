# Volume — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

## Pages

### Page 1 — Volume HUD collapsed

- Trigger: hardware Volume Up / Down (consumed by this app’s accessibility key filter)
- Placement: dark charcoal strip pinned just below the system / Metro status tray
- Layout (L→R): large white zero-padded level `NN`, muted gray `/max`, stream label, far-right **down** chevron
- Stream labels (exact): `Ringer + Notifications` (max **10**), `Media + Apps` (max **30**), or `Call volume` (max **10**) while in-call
- Default stream: in-call → call; else if media is active → media; else → ringer
- Auto-dismiss: **2500ms** after last rocker / touch
- Interactions: rockers adjust the active stream; tap strip or chevron → expand

### Page 2 — Volume HUD expanded

- Dual rows (non-call):
  1. **Ringer + Notifications** — `NN/10`, bell mute icon, accent slider (0–10)
  2. **Media + Apps** — `NN/30`, note mute icon, accent slider (0–30)
- Bottom row: **VIBRATE** toggle (left); **up** chevron (right) to collapse
- In-call expanded: single **Call volume** slider only (0–10)
- Mute: tapping the stream icon sets that stream to 0 (or restores the previous level)
- Always dark charcoal chrome, independent of light Start

## System behavior

| Signal | Behavior |
|--------|----------|
| Overlay | `SYSTEM_ALERT_WINDOW` FGS, hosted as `TYPE_ACCESSIBILITY_OVERLAY` when a11y connected |
| Keys | `FLAG_REQUEST_FILTER_KEY_EVENTS` consumes volume rockers (suppresses Android stock HUD) |
| Theme | Observe `THEME_CHANGED` for accent on sliders / VIBRATE |
| Scales | WP display scales map proportionally onto `STREAM_RING` / `STREAM_MUSIC` / `STREAM_VOICE_CALL` |

## Images

| Image | Page | Notes |
|-------|------|-------|
| `volume_collapsed_ringer_dark_cyan.jpg` | Collapsed | `07/10 Ringer + Notifications` |
| `volume_collapsed_media_dark_cyan.jpg` | Collapsed | `15/30 Media + Apps` |

## Out of scope (v1)

- Headphone / Bluetooth accessory-specific volume rows
- SMTC / media transport chrome under the volume HUD
- Settings → ringtones+sounds deep-link from the HUD
