# Phone hub — history & speed dial

Supplementary guide for pivot panes. Authoritative layout: [`blueprint.md`](blueprint.md).

## History pane

WP 8.1 Phone opens to **history**, not the dial pad. The list is the primary surface for returning missed calls and redialing.

### Grouping rules

1. Group consecutive log entries by **normalized phone number** (strip spaces, dashes; keep leading `+`).
2. If `READ_CONTACTS` resolves a name, show name as primary label; number moves to detail view only.
3. Sort groups by **most recent call timestamp** descending.
4. Within a group, retain individual `CallEntry` rows for the number view (Page 2).
5. Display `(n)` on the primary label when `n > 1`.

### Row semantics

| Call type | Primary color | Secondary label |
|-----------|---------------|-----------------|
| Incoming (answered) | White | `incoming · <relative time>` |
| Outgoing | White | `outgoing · <relative time>` |
| Missed | Red `#E51400` | `missed · <relative time>` |

The right-side phone icon always initiates an outgoing call to that number — WP users expect one-tap redial.

### Search/filter

Tapping search narrows the visible groups by substring match on name or number. Clearing search restores full list. No separate search results page in v1.

## Speed dial pane

Introduced in WP 8.1 after heavy user demand. Not available on WP 8.0.

- Contacts are **explicitly pinned** — not auto-generated from frequent calls.
- Add via long-press on history row → `add to speed dial`.
- Order is user-controlled (long-press → move up/down — optional in v1; append-only acceptable).
- Tapping a speed-dial row places a call immediately (same as history callback).

## Bottom app bar icons

| Icon | Action |
|------|--------|
| Dial pad (grid/keypad glyph) | Open Page 3 |
| People (address book) | Launch People app |
| Search | Toggle in-app history filter |
| … | Settings stub, blocked calls stub |

App bar is **minimized** (icons only) on hub panes per WP 8.1 Phone.
