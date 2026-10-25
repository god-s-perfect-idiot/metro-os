# People — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

Target: **Windows Phone 8.1 GDR2+** People hub on a portrait phone (768×1280 / xhdpi reference profile).

## App shell

- **Control model:** `MetroPanorama` for the hub landing (horizontal panes). This is a panorama app, not a pivot app at the top level.
- **Theme:** Black background (`#000000`) on all People surfaces unless a contact photo provides a full-bleed header.
- **Typography:** Noto Sans stand-in for Segoe WP. Contact names are large and left-aligned; section headers use sentence-case lowercase panorama titles (`what's new`, `recent`, `all`).
- **App bar:** Minimized (… overflow) on panorama panes. Standard round icon buttons on contact detail and filter screens.
- **No Material:** No FAB, chips, rounded avatar cards, or bottom sheets.
- **Reference:** `images/hub_panorama_dark_blue.png` — WP People hub panorama (what's new / recent / together / all). metro-os omits together.

## Hub panes (metro-os)

| Pane | Default? | Data |
|------|----------|------|
| **what's new** | **Yes** (landing) | Active Conversations shade posts for Settings → connected apps (`MetroConversationFeed`) |
| **recent** | No | Up to **10** saved contacts who called or messaged (call log + SMS + conversation peers); **2×5** photo grid |
| **all** | No | Contact list; top features newest conversation author + message |

Together / Rooms: out of scope.

## Pages

### Page 1 — People hub · what's new (default landing)

- **Layout:**
  - Panorama pane title: `what's new` (lowercase, large Light). Peek of `recent` on the right.
  - Vertical feed (People hub what's-new UI — match `detail_whatsnew_dark_blue.jpg` / panorama capture):
    - Author name (large white).
    - Message body in quotes.
    - Source + relative time in grey (`WhatsApp 3 minutes ago`).
    - Right: grey square badge with count or `+`.
  - Empty: no Conversations notification access → explanation + open listener settings. Access granted but idle → short empty copy.
- **Navigation:** Tap row → `MetroConversationFeed.open(key)`. Swipe → `recent`.
- **Background:** Black.

### Page 2 — People hub · recent

- **Layout:**
  - Panorama pane title: `recent`.
  - **2×5** square tile grid (WP mosaic). Photo tiles; accent + name when no photo.
  - Max **10** unique **saved** contacts, newest call/message first.
  - Message activity kept in a 10-entry ring (`MessageRecentStore`) — new unique contact pops the oldest.
- **Data:** `CallLog.Calls` (saved only) + `Telephony.Sms` (when permitted) + conversation peers matched to contacts.
- **Navigation:** Tap tile → contact detail. Empty / no call-log permission → allow access.
- **Background:** Black.

### Page 3 — People hub · all

- **Layout:**
  - Panorama pane title: `all`.
  - **Featured conversation** at top when what's new is non-empty: large accent square + author name + quoted newest message + source/time.
  - Then showing filter chip, sticky letter jump, alphabetical contact list (existing rows).
- **Navigation:** Search hides panorama. `+` → new contact. Featured tap opens conversation.
- **Background:** Black.

### Page 4 — Filter contacts

- **Layout:** Full-page settings surface (not a panorama pane).
  - Header: `FILTER CONTACTS` (small caps, 20sp).
  - **Hide contacts without phone numbers** — label + large On/Off state text + `MetroToggleSwitch` right.
  - Helper text below toggle in grey.
  - **show contacts from my** section — checkbox list per synced account (Outlook, Google, Facebook, etc.).
  - Bottom app bar: single Store-style **done** text button (no icons). Back cancels.
- **Navigation:** Reached from `showing` chip or Settings → filter my contact list. Back restores previous list scroll.
- **Interactions:** Toggle/check changes apply on check tap; cancel discards.
- **Reference:** `images/pivot_dark_blue.jpg`
- **Background:** Black.

### Page 4 — Jump list overlay

- **Layout:** Full-screen overlay via toolkit `MetroJumpList`: 4-column grid of `#`, `a`–`z`, and locale globe. Active letters (with contacts) use accent fill; inactive letters use dark gray. Glyphs are lowercase white.
- **Navigation:** Opened from alphabet tile on `all` pane. Tap active letter → scroll list to section and dismiss. Tap inactive → no-op. Tap scrim or Back → dismiss.
- **Background:** Translucent black scrim over the list.

### Page 5 — Contact detail (pivot container)

- **Layout:** Pivot page with header block:
  - Contact name (sentence caps) + source label in grey (`Facebook`).
  - Pivot headers: `profile` | `connect` | `what's new` | `history` (lowercase, large type; active white, inactive grey).
- **Navigation:** Opened from profile icon on contact row (not from name tap). Back → `all` pane preserving scroll.
- **Background:** Black.

### Page 6 — Contact detail · profile pivot

- **Layout:**
  - Square profile photo (large, left or top-left).
  - Latest social blurb beside/below photo (source + relative time).
  - Action list (verb label grey, value accent blue):
    - `call mobile` → number
    - `text` → SMS intent
    - `post to timeline` → external app stub
    - `send email` → address
  - App bar: pin, link, edit icons (round). … overflow.
- **Reference:** `images/detail_dark_blue.jpg`
- **Background:** Black.

### Page 7 — Contact detail · connect pivot

- **Layout:** Grid/list of linked app tiles (Facebook, Skype, etc.) with `add apps` entry point at bottom.
- **Reference:** `images/detail_connect_dark_blue.jpg`
- **Background:** Black.

### Page 8 — Contact detail · what's new pivot

- **Layout:** Per-contact social feed; same item template as hub what's new. Optional `showing <network>` filter chip.
- **Reference:** `images/detail_whatsnew_dark_blue.jpg`
- **Background:** Black.

### Page 9 — Contact detail · history pivot

- **Layout:** Chronological communication list (calls, texts). Group repeat calls with count badge `(3)` per WP 8.1 Phone parity.
- **Data:** Pull from system call log / SMS where permitted; empty state if denied.
- **Background:** Black.

### Page 10 — Add account

- **Layout:** Full-page list of account providers (Exchange, Outlook.com, Google, iCloud, Facebook, Twitter, LinkedIn, …). Icon + name + optional subtitle per row.
- **Navigation:** From `all` pane Add an account row or Settings.
- **Reference:** `images/accounts_dark_blue.jpg`
- **Background:** Black.

### Page 11 — New / edit contact

- **Layout:** Form with Name field visible; `+` expands hidden fields (last name, company, etc.). Account picker before save. Save icon in app bar.
- **Data:** Write through `ContactsContract` to selected account.
- **Background:** Black.

### Page 12 — Permission / empty states

- **No READ_CONTACTS:** Full-page explanation + action to grant permission. No crash, no empty list masquerading as success.
- **No contacts after grant:** Prompt to add account or create contact.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `hub_dark_blue.jpg` | Page 1 — all pane | **Not yet sourced** — see `known-gaps.md`. Use `apps/launcher/references/images/applist_dark_blue.png` for list row styling until captured. |
| `pivot_dark_blue.jpg` | Page 3 — filter contacts | Filter toggles + account checkboxes |
| `detail_dark_blue.jpg` | Page 6 — profile pivot | Primary contact detail reference |
| `detail_connect_dark_blue.jpg` | Page 7 — connect pivot | Linked app tiles |
| `detail_whatsnew_dark_blue.jpg` | Page 8 — what's new pivot | Per-contact social feed |
| `accounts_dark_blue.jpg` | Page 10 — add account | Account provider list |

## Data model (implementation hint)

- `PersonSummary(id, displayName, photoUri, hasPhone, sourceLabel, defaultPhone)`
- `PersonDetail` extends summary with emails, social blurbs, linked apps
- `ContactMethod(type, label, value)`
- `PeopleFilter(hideNoPhone, visibleAccounts)`
- `PeoplePane { WhatsNew, Recent, All }`
- `RecentPerson(contactId, displayName, phoneNumber?, lastKind, lastTimestamp)`
- `WhatsNewItem(key, authorName, body, sourceLabel, postTimeMs, badgeCount)`

## Out of scope (v1)

- Rooms, Groups, Together pane
- Inline social like/comment/reply
- Real Facebook/Twitter/LinkedIn API sync (Conversations shade posts replace that feed)
- Me tile as live Start tile
- Dual-SIM per-contact defaults
- Contact binding / Connect tile rich content APIs
- Full call-history list in People (Dialer owns history; Recent is people-centric, max 10)
