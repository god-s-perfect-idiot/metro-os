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

- Toast: accent fill, square logo + message copy with ellipsis; group chats put conversation name on top; default single-line `sender: message` (or two-row when enabled); no clock (tray already shows time); 3/5/10s timeout (setup ListPicker, default 5s), swipe right to dismiss, tap to open
- Enter/exit: perspective 3D tile flip (`rotationX` 90° ↔ 0°, camera from banner width); exit is the reverse of enter
- Overlay at y=0; full accent band (tray inset + banner) flips behind an opaque matching Metro tray raised after attach
- Overlay window exists **only** while a toast is visible
- Stock Android heads-up disabled while **Show notifications** is on
  (`heads_up_notifications_enabled=0` + listener hints; restore when toggle off)
- Critical interrupts (CATEGORY_CALL / ALARM, CallStyle, fullScreenIntent, FLAG_INSISTENT)
  temporarily restore stock heads-up and fire `fullScreenIntent` so WhatsApp calls and
  similar peeks are not swallowed
- Group peeks: resolve real MessagingStyle / child copy over "N new messages" summaries;
  debounce identical group-burst copy; suppress shade RemoteInput self-reply echoes;
  skip shade replay on listener connect
- No Action Center, no tray hide/show, no shade contract

## Primary flows

1. Master **Show notifications** toggle starts/stops the overlay FGS
2. High-importance notification → toast at the top of the screen
3. Setup **toast timeout** ListPicker (3 / 5 / 10 seconds) and **show test toast** (toggle + grants required)
4. Setup **Two-row view** toggle stacks `title` / `description` instead of `title: description`
5. Theme broadcast refreshes accent / dark-light

## Verify

```bash
../../scripts/verify-app.sh notifications
```

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| System owns toast peek | No public pre-peek hook | Disable AOSP heads-up via global setting + draw Metro toast |
| True trigger hijack | SystemUI posts HUNs internally | Listener + overlay, not a key filter |
