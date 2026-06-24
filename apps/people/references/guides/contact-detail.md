# Contact detail — pivot pages

Supplementary detail for [`blueprint.md`](blueprint.md) Pages 5–9.

## Entry

Open detail only via the **profile icon** on a contact row (WP 8.1). Do not navigate here on row/name tap — that places a call.

## Pivot headers

Four pivots in this order:

1. **profile** — default
2. **connect** — linked third-party apps
3. **what's new** — per-person social feed
4. **history** — calls + texts

Headers use lowercase large type. Active pivot: white. Inactive: grey, peeking on the right.

## Profile pivot

**Header block**

- Contact name (all caps or sentence per display setting)
- Source label in grey (`Facebook`, `Outlook`, etc.)

**Photo + status**

- Square photo, no rounded mask.
- Adjacent/overlaid latest social post snippet with source + relative time.

**Action rows**

Verb in grey, value in accent blue:

| Label | Action |
|-------|--------|
| call mobile | Dial default mobile |
| text | SMS intent |
| post to timeline | External app stub (`Facebook`) |
| send email | Mail intent |

**App bar (round icons)**

- Pin to Start
- Link (merge duplicate contacts)
- Edit (pencil)
- … overflow (delete, etc.)

## Connect pivot

- Shows tiles for apps that expose contact bindings (Facebook, Skype, …).
- `add apps` row at bottom when slots remain.
- v1: static placeholder tiles OK; no rich tile image download APIs.

## What's new pivot (per contact)

- Same feed item template as hub what's new, scoped to one person.
- `showing Facebook` filter chip when multiple networks linked.
- Comment count badges are display-only in v1.
- Tap item → external app deep-link stub.

## History pivot

- Unified chronological list of calls and texts with this contact.
- WP 8.1 Phone parity: group multiple calls to same number with `(n)` count.
- Expand group to see individual times + durations.
- Requires `READ_CALL_LOG` / SMS permissions or show empty state with explanation.

## Edit / new contact

- Collapsed field set: Name visible, `+` reveals more fields (last name, company, …).
- Account picker before save — contacts always saved to an account, never "phone only".
- Photo: camera or gallery via `add photo` on edit form.
