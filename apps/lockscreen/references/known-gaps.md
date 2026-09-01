# Lock screen — known gaps

| Missing / low-fidelity | Should show | Workaround |
|------------------------|-------------|------------|
| Alarm glyph beside the clock | Small alarm icon when an alarm is set | Clock time only |
| Separate capture of the password / PIN entry after swipe-up | Stock WP credential UI after drag-up | Documented in Lumia user guide; Android uses `requestDismissKeyguard` for the system unlock screen |
| Glance (always-on) screen | Minimal always-on time / notifications | Implemented via **Glance lockscreen** setup toggle — black AMOLED chrome over system AOD; no reference capture yet |
| Bing wallpaper while offline | Fresh Bing picture of the day | Last cached JPEG if present; otherwise accent fill until network succeeds |

Acceptance: accent / custom / Bing fill + time/day/date/(optional event) over keyguard + biometrics still unlock + swipe up → system unlock UI.
