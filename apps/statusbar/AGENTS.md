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
| Collapsed tray | Clock only, right-aligned | `references/images/collapsed_dark.png` |
| Expanded tray | All indicators then collapse (default 5s; 3/5/10s from setup) | `references/images/expanded_dark.png` |
| Progress state | Accent spinner in tray | `references/images/progress_dark.png` |

## WP8.1 rules

- Height **32dp**; no Material status bar icons
- Indicator order L→R: cellular + data label, Wi-Fi; battery + clock on the right
- Tap tray / go home → indicators drop in R→L from above; hold **3s / 5s / 10s** (setup ListPicker, default **5000ms**); exit upward R→L
- Per-app: apps request opaque / translucent (0.5) / hidden via `metro-system-sdk` API; fullscreen surfaces use `MetroStatusBarFullscreenEffect`
- Immersive: when Android status bars are hidden, the Metro tray creeps away (same motion as `MODE_HIDDEN`)
- Stub call-forwarding / roaming / Bluetooth / quiet-hours glyphs acceptable in v1 (not in expanded row)

## Primary flows

1. Master **Show status bar** toggle starts/stops the overlay (setup UI); boot respects the same flag. **Match app background** uses the app’s published status-bar/primary theme color for non-Metro apps; Metro suite apps keep the Metro page fill. **Hide icons after** ListPicker sets the auto-collapse hold (3 / 5 / 10 seconds). **Notch position** ListPicker (Center / Left / Right) adds side clearance for corner punch-holes.
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
