# Agent instructions — Status Bar (`com.metro.statusbar`)

**Tier 0 — Metro Shell** | Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

WP8.1 **System Tray** overlay — clock, expandable status indicators, optional in-tray progress. Runs as overlay service.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| Toolkits verified | Yes |
| Launcher installed | Recommended for integrated testing |

## Screens / surfaces

| Surface | Behavior | Reference |
|---------|----------|-----------|
| Collapsed tray | Clock only, right-aligned | `references/images/collapsed_dark.png` |
| Expanded tray | All indicators 8s then collapse | `references/images/expanded_dark.png` |
| Progress state | Accent spinner in tray | `references/images/progress_dark.png` |

## WP8.1 rules

- Height **32dp**; no Material status bar icons
- Indicator order L→R: cellular, Wi-Fi, Bluetooth, alarm, location, battery
- Tap tray → expand 200ms; auto-collapse after **8000ms**
- Per-app: apps request opaque / translucent (0.5) / hidden via `metro-system-sdk` API
- Stub cellular/Wi-Fi data acceptable in v1 (static icons)

## Primary flows

1. Overlay draws **above the system status bar** via `TYPE_ACCESSIBILITY_OVERLAY`
   (`StatusBarAccessibilityService`); falls back to `TYPE_APPLICATION_OVERLAY` (hidden behind the
   system bar) when the accessibility service is off. `SYSTEM_ALERT_WINDOW` alone is not enough —
   it is layered below the system status bar.
2. Clock updates every minute
3. Tap expands/collapses indicators
4. `ThemeChangeReceiver` updates foreground colors

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
| True signal strength | Privileged APIs | Static icon set; document in README |
