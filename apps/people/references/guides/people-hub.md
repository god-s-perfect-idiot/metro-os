# People hub — panorama panes

Supplementary detail for [`blueprint.md`](blueprint.md) Pages 1–2. Read the blueprint first.

## Panorama structure (WP 8.1)

People is a **panorama** app (horizontal panes), not a top-level pivot app. In WP 8.1 the panorama was simplified:

| Pane | WP 8.0 | WP 8.1 | Implement |
|------|--------|--------|-----------|
| **all** | Contact list | **Default landing** | Yes |
| **what's new** | Aggregated social feed | Still present; read-only + deep links | Yes (stub feeds) |
| **recent** | Last 8 people | Removed | No |
| **together** | Rooms + Groups | Removed | No |

Swipe left/right between `all` and `what's new`. The inactive pane title peeks on the trailing edge (grey, truncated).

## All pane content order

Top to bottom inside the vertical scroll:

1. **Me** — current user's photo + name at the very top. Tapping opens own profile; Pin icon on profile restores Start Me tile.
2. **Add an account** — WP 8.1 prioritizes this above the contact list.
3. **Import SIM contacts** — when hardware supports it; entry under … → Settings in official UX.
4. **Showing chip** — accent text reflecting active filter (e.g. `showing only contacts with phone numbers`). Tap → filter page.
5. **Jump list anchor** — accent square with current section letter.
6. **Grouped contacts** — alphabetical sections with large letter headers.

## Contact row behavior (8.1-specific)

This is the most common implementation mistake:

| Gesture | WP 8.0 | WP 8.1 |
|---------|--------|--------|
| Tap contact name / main row | Open profile | **Place call** to default mobile |
| Tap profile icon (right) | N/A | Open contact detail pivot |

Always show the profile/details icon on the right when a phone number exists.

## Jump list

- Tap the accent letter tile → fullscreen grid of A–Z, `#`, and locale symbols.
- Tap a letter → list scrolls to that section; overlay closes.
- Alternative: system Search key → type-ahead filter (hidden contacts still searchable).

## What's new pane (hub level)

- Chronological posts from connected accounts.
- `showing <network>` chip filters to one source.
- WP 8.1: tapping a post opens the native app (Facebook, Twitter, etc.) — do not implement inline compose in v1.
- Posts from filtered-out contacts may still appear here unless user also hides them from the social feed in settings.

## Settings (… menu)

Reachable from any panorama pane:

| Setting | Behavior |
|---------|----------|
| filter my contact list | Opens filter page |
| import SIM contacts | SIM transfer wizard (stub OK) |
| sort list by | First name / Last name |
| display names by | First last / Last first |
| hide posts from hidden contacts | Toggle for what's new feed |

## List styling cross-reference

Until `hub_dark_blue.jpg` is captured, match contact row density and alphabet headers to:

`apps/launcher/references/images/applist_dark_blue.png`

People rows add a square avatar and optional right-side profile icon.
