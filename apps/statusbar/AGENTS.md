# Agent instructions — Status Bar (`com.metro.statusbar`)

**Tier 0 — Metro Shell** | Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

WP8.1 **System Tray** overlay — clock, expandable status indicators, optional in-tray progress.
Runs as overlay service. Does not host Action Center, toasts, or a notification shade.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| Toolkits verified | Yes |
| Launcher installed | Recommended for integrated testing |

## Screens / surfaces

| Surface | Behavior | Reference |
|---------|----------|-----------|
| Collapsed tray | Rightmost layout icon (usually clock) + trailing spacers/padding, trailing-aligned | `references/images/collapsed_dark.png` |
| Expanded tray | All icons freely justified across the tray; then collapse (default 5s; 3/5/10s/never from setup) | `references/images/expanded_dark.png` |
| Progress state | Accent spinner in tray | `references/images/progress_dark.png` |
| Setup | pivot `customise` \| `icons` (customise: toggles + preview + permissions; icons: glyph toggles + preview strip) | `blueprint.md` § Page 4 |
| Configure | Centered tray preview with drag thumbs; add spacer; reorder icons/spacers | `blueprint.md` § Page 4 |

## WP8.1 rules

- Height **32dp**; no Material status bar icons
- Indicator order L→R (default layout): cellular + data label, Wi-Fi, mute, notification app, hotspot, Bluetooth audio, battery, clock — freely justified across the tray (trailing spacers stay when collapsed)
- Tap tray / go home → indicators drop in L→R from above; hold **3s / 5s / 10s** or **Never** (setup ListPicker, default **5000ms**); exit upward L→R until only the rightmost icon remains
- Per-app: apps request opaque / translucent (0.5) / hidden via `metro-system-sdk` API; fullscreen surfaces use `MetroStatusBarFullscreenEffect`
- Immersive: when Android status bars are hidden, the Metro tray creeps away (same motion as `MODE_HIDDEN`)
- Icons-tab toggles gate which glyphs may appear; the rightmost layout icon is always shown when collapsed

## Primary flows

1. Master **Show status bar** toggle starts/stops the overlay (setup UI); boot respects the same flag. **Statusbar background** ListPicker chooses **Default black background** (solid black; default), **Match app background** (Metro suite → black; other apps → icon tile brand / adaptive background), or **Show accent color** (always system accent). **Hide icons after** ListPicker sets the auto-collapse hold (3 / 5 / 10 seconds) or **Never** (indicators stay expanded). **configure statusbar** drills into **configure**: centered tray preview with accent thumbs under each icon/spacer to drag-reorder; **add spacer** / **add tiny spacer** at the bottom (hint: cover notch and active mic areas; trailing spacers stay after auto-hide). On device rotate the tray slides out and back in from the new top.
2. Overlay draws **above the system status bar** via `TYPE_ACCESSIBILITY_OVERLAY`
   (`StatusBarAccessibilityService`); falls back to `TYPE_APPLICATION_OVERLAY` (hidden behind the
   system bar) when the accessibility service is off. `SYSTEM_ALERT_WINDOW` alone is not enough —
   it is layered below the system status bar.
3. Clock updates every minute
4. Tap tray or Start/home expands indicators (staggered drop); auto-collapse after hold
5. Swipe down on tray opens the Android notification shade; Metro overlay hides until the shade closes
6. Immersive / fullscreen apps hide the tray (contract `MODE_HIDDEN` or system status-bar hide)
7. `ThemeChangeReceiver` updates foreground colors

## Golden screenshots

```
screenshots/golden/collapsed_dark_blue.png
screenshots/golden/expanded_dark_blue.png
```

## Permissions

- `SYSTEM_ALERT_WINDOW` (overlay)
- Accessibility service (`BIND_ACCESSIBILITY_SERVICE`) — required to draw over the system status bar
- Foreground service type: `specialUse`

## Verify

```bash
../../scripts/verify-app.sh statusbar
```

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| True signal strength | Privileged OEM internals | App-level `SignalStrength` + `WifiManager` RSSI mapped to WP bar/arc counts |
| Action Center chrome while shade open | Metro Action Center out of scope; a11y overlay paints above SystemUI | Hide Metro tray while Android notification shade is open; swipe-down opens shade via `GLOBAL_ACTION_NOTIFICATIONS` |
