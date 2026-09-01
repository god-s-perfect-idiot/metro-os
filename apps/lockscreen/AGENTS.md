# Agent instructions — Lock screen (`com.metro.lockscreen`)

**Tier 0 — Metro Shell** | Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

WP8.1 **lock screen** overlay — presents a Metro surface **above** the Android system
keyguard via `TYPE_ACCESSIBILITY_OVERLAY`. Accent / custom / Bing fill + lock chrome (time, day,
date, next calendar event). Fingerprint / face unlock pass through; swipe up reveals the system
unlock (PIN / pattern / password) UI.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| Toolkits verified | Yes |
| Other shell apps | Optional |

## Surfaces

| Surface | Reference |
|---------|-----------|
| Lock (accent / custom / Bing + chrome) | `blueprint.md` § Page 1 + `images/lock_bing_homepage_dark.jpg` |
| Setup | `blueprint.md` § Page 2 |

## WP8.1 rules

- Lock surface is full-bleed (no Material cards / FABs)
- Transparent Metro status tray over the fill (no opaque bar); hides `com.metro.statusbar` while up
- Swipe **up** → system credential UI (WP: drag lock screen up)
- Do **not** dismiss the keyguard until the user authenticates (biometric or system unlock)
- Biometrics must remain usable while the Metro surface is visible
- Fill = setup **Background** choice (accent / custom photo / Bing); chrome text contrasts via
  `tileContentColor` on accent, white on photos
- Setup **Background** ListPicker; Custom uses Start-background choose-photo / crop / remove

## Primary flows

1. Enable Lock screen accessibility + master **Show lock screen** toggle (starts host FGS)
2. Screen on + keyguard locked → attach accessibility overlay (chosen background + chrome)
3. Fingerprint / face success → remove overlay
4. Swipe up (finger-tracking slide) past threshold → SystemUI bouncer via `requestDismissKeyguard` only
5. Screen off → remove overlay
6. Optional: grant calendar → next appointment appears under the date
7. Setup **Background** ListPicker → Accent / Custom (choose photo + crop) / Bing (fetch + cache)
8. Optional: **Glance lockscreen** → black AMOLED surface over system AOD when keyguard locked

## Verify

```bash
../../scripts/verify-app.sh lockscreen
```

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| True system lock chrome | Cannot replace Keyguard as a normal app | `TYPE_ACCESSIBILITY_OVERLAY` above stock keyguard |
| Activity over lock from FGS | Background activity starts blocked | Accessibility overlay (not `startActivity`) for the fill |
| Drag up → password | No public “show bouncer only” API | Transparent trampoline + `requestDismissKeyguard`; a11y `startActivity` first (API 35+ must not put BAL mode on `PendingIntent.getActivity`) |
| Biometrics under custom lock | Overlay may cover fingerprint hint UI | HAL still unlocks; tear down on unlock |
| Photo wallpaper + notification glyphs | Bottom glyphs deferred | Accent / custom / Bing fill + tray + chrome; see known-gaps |
| Accessibility overlay on AOD | `TYPE_ACCESSIBILITY_OVERLAY` can cover Always-On Display | Lock fill only when `STATE_ON` + interactive; optional **Glance lockscreen** presents black chrome over `STATE_DOZE` / `STATE_DOZE_SUSPEND` |
| Live cellular on lock tray | Needs `READ_PHONE_STATE` | Setup **allow signal strength**; Wi-Fi + battery always |

## Agent postmortem

_(Agents append here after 5 failed verify iterations — see docs/TROUBLESHOOTING.md)_
