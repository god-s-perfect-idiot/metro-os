# Agent instructions — Volume (`com.metro.volume`)

**Tier 0 — Metro Shell** | Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

WP8.1 **volume HUD** overlay — hardware rocker → collapsed/expanded charcoal panel with
ringer / media / call streams. Standalone shell app (not statusbar).

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| Toolkits verified | Yes |
| Statusbar installed | Recommended (HUD content pads below tray inset) |

## Surfaces

| Surface | Reference |
|---------|-----------|
| Collapsed HUD | `references/images/volume_collapsed_*` |
| Music transport | `references/images/volume_music_transport_dark.png` |
| Expanded / in-call | `blueprint.md` § Page 2 + known-gaps |

## WP8.1 rules

- Always dark charcoal panel; accent-filled sliders
- Ringer **0–10**, media **0–30**, call **0–10**
- Auto-dismiss **2500ms** after last interaction
- While music plays, default rocker view is UVC (transport + metadata), not the thin strip
- Consume volume keys via a11y so Android’s stock HUD does not appear
- Hold rocker auto-steps (a11y often omits system key-repeat; timer in a11y service)
- Only consume rockers while the overlay FGS is running; otherwise fall through
- Never present the HUD while the display is off or in AOD/doze (awake lock screen is OK)
- Show / hide: top-anchored height wipe creep (`SHOW_HIDE_MS`); overlay window exists only while the HUD is visible or exiting
- Expand / collapse: same wipe family (`EXPAND_COLLAPSE_MS`)
## Primary flows

1. Master **Show volume controls** toggle starts/stops the overlay FGS (setup UI); boot respects the same flag
2. Overlay FGS + accessibility overlay host
3. Volume rockers → show / adjust HUD (only while master toggle is on and display is fully awake)
4. Music session active → default music-transport chrome; expand → dual sliders / silent mode + sound settings; in-call → call only
5. Theme broadcast refreshes accent

## Verify

```bash
../../scripts/verify-app.sh volume
```

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| System volume HUD | Keys need a11y filter | Consume rockers only while overlay FGS is running |
| Accessory streams | Complex routing | v1: ringer / media / in-call |
| Empty always-on overlay | Can steal input after faults | Attach WindowManager view only while HUD visible |
| Accessibility overlay on AOD | `TYPE_ACCESSIBILITY_OVERLAY` can draw over Always-On Display | Present only when display is `STATE_ON` + interactive; tear down on screen-off / doze |
| Ringer/call 0–10 | Stream max often 5–7; some OEMs clamp `setStreamVolume` | Keep WP ticks as HUD source of truth; verify/retry with `adjustStreamVolume`; skip re-sync on every rocker while HUD is up |
| SMTC for any player | `MediaSessionManager` needs this package’s notification listener | Suite Music via Media3 bind; optional NLS for others |
