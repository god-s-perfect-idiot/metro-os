# Agent instructions — Status Bar (`com.metro.statusbar`)

**Tier 0 — Metro Shell** | Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

WP8.1 **System Tray** + **Action Center** overlay — clock, expandable status indicators, swipe-down
notification shade with quick actions, optional in-tray progress. Runs as overlay service.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| Toolkits verified | Yes |
| Launcher installed | Recommended for integrated testing |

## Screens / surfaces

| Surface | Behavior | Reference |
|---------|----------|-----------|
| Collapsed tray | Clock only, right-aligned | `references/images/collapsed_dark.png` |
| Expanded tray | All indicators 5s then collapse | `references/images/expanded_dark.png` |
| Progress state | Accent spinner in tray | `references/images/progress_dark.png` |
| Action Center | Quick actions + notifications | `references/images/action_center_dark_cyan.png` |

## WP8.1 rules

- Height **32dp**; no Material status bar icons
- Indicator order L→R: cellular + data label, Wi-Fi; battery + clock on the right
- Tap tray / go home → indicators drop in R→L from above; hold **5000ms**; exit upward R→L
- Swipe down → Action Center; swipe up → close
- Per-app: apps request opaque / translucent (0.5) / hidden via `metro-system-sdk` API
- Stub cellular/Wi-Fi data acceptable in v1 (static icons)

## Primary flows

1. Master **Show status bar** toggle starts/stops the overlay (setup UI); boot respects the same flag
2. Overlay draws **above the system status bar** via `TYPE_ACCESSIBILITY_OVERLAY`
   (`StatusBarAccessibilityService`); falls back to `TYPE_APPLICATION_OVERLAY` (hidden behind the
   system bar) when the accessibility service is off. `SYSTEM_ALERT_WINDOW` alone is not enough —
   it is layered below the system status bar.
3. Clock updates every minute
4. Tap tray or Start/home expands indicators (staggered drop); auto-collapse after hold
5. Swipe down opens Action Center (notifications + quick actions)
6. `ThemeChangeReceiver` updates foreground colors

## Golden screenshots

```
screenshots/golden/collapsed_dark_blue.png
screenshots/golden/expanded_dark_blue.png
```

## Permissions

- `SYSTEM_ALERT_WINDOW` (overlay)
- Accessibility service (`BIND_ACCESSIBILITY_SERVICE`) — required to draw over the system status bar
- Notification listener (`BIND_NOTIFICATION_LISTENER_SERVICE`) — Action Center list / Clear All
- Foreground service type: `specialUse`

## Verify

```bash
../../scripts/verify-app.sh statusbar
```

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| True signal strength | Privileged APIs | Static icon set; document in README |
| Instant radio toggles | Restricted APIs on Android 10+ | Open Settings panels when toggle blocked |
