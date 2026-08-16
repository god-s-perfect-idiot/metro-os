# Agent instructions — Notifications (`com.metro.notifications`)

**Tier 0 — Metro Shell** | Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

WP8.1 **toast banners** overlay. Replaces Android heads-up. Light shell app like volume — not
statusbar, and not Action Center.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| Toolkits verified | Yes |
| Statusbar installed | Optional (toasts do not drive the tray) |

## Surfaces

| Surface | Reference |
|---------|-----------|
| Toast banner | `references/images/toast.png` |
| Setup | in-app grants + master toggle (opened from Settings → notifications) |

## WP8.1 rules

- Toast: accent fill, square logo + single-line `sender: message` with ellipsis; no clock (tray already shows time); 3/5/10s timeout (setup ListPicker, default 5s), swipe right to dismiss, tap to open
- Enter/exit: perspective 3D tile flip (`rotationX` 90° ↔ 0°, camera from banner width); exit is the reverse of enter
- Overlay is offset below the status-bar / cutout inset so it does not draw under a notch
- Overlay window exists **only** while a toast is visible
- Stock Android heads-up disabled while **Show notifications** is on
  (`heads_up_notifications_enabled=0` + listener hints; restore when toggle off)
- No Action Center, no tray hide/show, no shade contract

## Primary flows

1. Master **Show notifications** toggle starts/stops the overlay FGS
2. High-importance notification → toast at the top of the screen
3. Setup **toast timeout** ListPicker (3 / 5 / 10 seconds) and **show test toast** (toggle + grants required)
4. Theme broadcast refreshes accent / dark-light

## Verify

```bash
../../scripts/verify-app.sh notifications
```

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| System owns toast peek | No public pre-peek hook | Disable AOSP heads-up via global setting + draw Metro toast |
| True trigger hijack | SystemUI posts HUNs internally | Listener + overlay, not a key filter |
