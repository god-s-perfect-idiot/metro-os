# Messaging — known reference gaps

Items the blueprint requires but we do not yet have a high-fidelity capture for in `images/`.

## Missing / low-fidelity screenshots

| File | What we need | Workaround |
|------|--------------|------------|
| `new_message_dark_yellow.jpg` | New-message / composer page — recipient field, message box, send + attach app bar | Behavior documented in `web-resources.md` (Lumia 920 "send a message"); render from blueprint Page 2 composer spec (same gray outgoing bubble). |
| `permission_gate_dark_yellow.jpg` | Permission / demo gate (Page 3) — WP8.1 had no equivalent (Android-only screen) | No WP source exists; implement per blueprint Page 3 using standard Metro full-page text + border buttons. |

Closed: `conversation_dark_orange.png` supplies a real dark-theme conversation capture (orange accent) for bubble geometry, colors, and the gray composer prompt.

## Caveats on shipped images

- **Accent:** shipped captures use the WP8 **yellow** accent, not the repo's usual `dark_blue`. Blueprint accent guidance still wins — treat color as illustrative only.
- **Resolution:** `threads_dark_yellow.jpg` is 300×500 (web-scaled), below the **768×1280** Lumia 925 profile. Use for layout/structure, not pixel measurement.
- **Era:** sources are WP8 / early WP8.1. WP8.1 removed Facebook/Skype chat — ignore any chat-integration UI; v1 is SMS/MMS only.

## How to close gaps

1. Capture from a WP8.1 GDR2+ device or emulator at **768×1280**.
2. Save as `<screen>_dark_<accent>.<ext>` per [`README.md`](README.md).
3. Update this file, the blueprint image table, and `references/README.md` image catalog.
4. Golden emulator screenshots still require human approval per `AGENTS.md`.

## Sourced externally (attribution)

Images in `images/` are community/official references for layout study only; the blueprint wins on conflict. See per-file sources in [`README.md`](README.md) § Image catalog and [`web-resources.md`](web-resources.md) § Reference images.
