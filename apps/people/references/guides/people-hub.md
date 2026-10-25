# People hub — panorama panes

Supplementary detail for [`blueprint.md`](blueprint.md) Pages 1–2. Read the blueprint first.

## Panorama structure (metro-os)

People is a **panorama** app (horizontal panes), not a top-level pivot app.

| Pane | Content | Data |
|------|---------|------|
| **what's new** | Conversation feed (People hub UI) | Active shade posts via **People** notification access + connected conversation apps |
| **recent** | **2×5** photo tile grid (max **10**) | Saved contacts who recently **called** or **messaged** (call log + SMS + conversation peers) |
| **all** | Featured newest conversation + alphabetical contacts | ContactsContract + newest what's-new item |

Pane order: `what's new` → `recent` → `all`. Default landing: **what's new**. Inactive pane title peeks on the trailing edge.

Reference capture: `images/hub_panorama_dark_blue.png`.

### Why these panes

- **what's new** — mirrors WP People hub feed layout; source is live shade posts via People's notification listener (connected apps), not Facebook APIs.
- **recent** — WP 8.0-style contact mosaic; filled from call/message activity for **saved** contacts only (10-deep ring).
- **all** — address book; top features the newest conversation author + message (WP “status next to photo” language).
- **together / rooms** — skipped (no Rooms backend).

## What's new pane

- Filter chip: accent `all accounts`.
- Each row: author name, quoted body, `Source · relative time`, right grey badge (`+` or count).
- Tap → open the shade notification’s content intent.
- Empty / no notification access → short explanation + settings action.

## Recent pane

- Up to **10** unique saved contacts, newest activity first.
- Message log is a ring buffer (SharedPreferences): new unique contact pushes out the oldest.
- Sources: `CallLog.Calls` (saved only), `Telephony.Sms` when permitted, conversation peers matched to contacts.
- **2×5** square tiles: photo, or accent + name when no photo.
- Tap → contact detail.

## All pane

- Top: featured newest what's-new conversation (large accent square + name + quote).
- Then showing chip + sticky letter jump + contact list (existing WP 8.1 row behavior).

## Contact row behavior (8.1-specific)

| Gesture | Behavior |
|---------|----------|
| Tap contact name / main row | Open profile (current metro-os) / call when wired |
| Tap profile icon (right) | Open contact detail pivot |

## Jump list

- Tap the accent letter tile → fullscreen grid of A–Z, `#`, and locale symbols.
- Tap a letter → list scrolls to that section; overlay closes.

## Settings (… menu)

| Setting | Behavior |
|---------|----------|
| filter my contact list | Opens filter page |
| import SIM contacts | SIM transfer wizard (stub OK) |
| sort list by | First name / Last name |
| display names by | First last / Last first |
