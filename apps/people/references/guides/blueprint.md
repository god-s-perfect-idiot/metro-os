# People — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

Target: **Windows Phone 8.1 GDR2+** People hub on a portrait phone (768×1280 / xhdpi reference profile).

## App shell

- **Control model:** `MetroPanorama` for the hub landing (horizontal panes). This is a panorama app, not a pivot app at the top level.
- **Theme:** Black background (`#000000`) on all People surfaces unless a contact photo provides a full-bleed header.
- **Typography:** Noto Sans stand-in for Segoe WP. Contact names are large and left-aligned; section headers use sentence-case lowercase panorama titles (`all`, `what's new`).
- **App bar:** Minimized (… overflow) on panorama panes. Standard round icon buttons on contact detail and filter screens.
- **No Material:** No FAB, chips, rounded avatar cards, or bottom sheets.

## WP 8.1 deltas (vs WP 8.0)

| WP 8.0 | WP 8.1 (build this) |
|--------|---------------------|
| Opens to What's new | Opens to **all** (contact list) |
| Recent pane (last 8 people) | **Removed** — do not implement |
| Together pane (Rooms / Groups) | **Removed** — do not implement |
| In-hub Facebook like/comment | **Read-only feed**; tap opens external app |
| Tap contact row → profile | Tap contact **name** → **call**; separate icon → profile |
| Built-in Facebook/Twitter sync | App-linked accounts via Add an account |

## Pages

### Page 1 — People hub · all (default landing)

- **Layout:**
  - Panorama pane title: lowercase `all` in large Light type (64sp class), flush left.
  - Peek of next pane (`what's new`) visible on right edge (~40dp).
  - Top of scrollable content (in order):
    1. **Me row** — user's own square photo + display name. Tapping opens own profile (Me surface; may stub in v1).
    2. **Add an account** row — launches account picker (`accounts_dark_blue.jpg`).
    3. **Import SIM contacts** row — shown when SIM present; may stub in v1.
    4. **Showing filter chip** — accent-colored label (e.g. `showing only contacts with phone numbers`). Tap opens filter contacts page.
    5. **Alphabet jump tile** — small square accent tile with current letter or `#`. Tap opens jump list overlay.
    6. **Contact list** — grouped by first letter (or last name per settings). Each row:
       - Square avatar (48dp), left.
       - Display name, large (20sp SemiBold), left-aligned.
       - **Profile icon** (small contact-card glyph) at row right — tap opens contact detail.
       - **Row tap on name/primary area** — initiate call to default mobile number (WP 8.1 behavior).
  - Rows separated by subtle 1px dividers at 20% white; no card elevation.
- **Navigation:**
  - Horizontal swipe → `what's new` pane.
  - System Search key → in-app contact search overlay.
  - … menu → Settings (sort/display name options, filter link, import SIM).
  - `+` app bar icon → new contact form.
- **Interactions:**
  - Long-press contact row → context menu (pin to Start, link, delete) — implement pin + delete in v1; link optional.
  - Scroll preserves position when returning from detail.
- **Background:** Solid black.

### Page 2 — People hub · what's new

- **Layout:**
  - Panorama pane title: `what's new` (lowercase, large Light).
  - Optional network filter chip at top: `showing Facebook` (accent text). Tap cycles connected networks.
  - Vertical feed of social posts from aggregated accounts. Each item:
    - Poster name + square avatar.
    - Post text (wrap).
    - Optional link preview card.
    - Source + relative time in grey (`Facebook · 34 minutes ago`).
    - Comment count badge (grey speech bubble) on right — display only in v1.
  - Empty state when no accounts connected: centred message + link to Add an account.
- **Navigation:** Swipe back to `all`. Tap post → deep-link stub (toast "Open in Facebook" / external intent placeholder).
- **Interactions:** No inline like/comment in v1 (WP 8.1 app-first model).
- **Background:** Black.

### Page 3 — Filter contacts

- **Layout:** Full-page settings surface (not a panorama pane).
  - Header: `FILTER CONTACTS` (small caps, 20sp).
  - **Hide contacts without phone numbers** — label + large On/Off state text + `MetroToggleSwitch` right.
  - Helper text below toggle in grey.
  - **show contacts from my** section — checkbox list per synced account (Outlook, Google, Facebook, etc.).
  - Bottom app bar: check (save) and X (cancel) round buttons; … overflow right.
- **Navigation:** Reached from `showing` chip or Settings → filter my contact list. Back restores previous list scroll.
- **Interactions:** Toggle/check changes apply on check tap; cancel discards.
- **Reference:** `images/pivot_dark_blue.jpg`
- **Background:** Black.

### Page 4 — Jump list overlay

- **Layout:** Full-screen overlay grid of alphanumeric tiles (A–Z, `#`, and locale bucket). Tiles are accent-colored squares with letter centered.
- **Navigation:** Opened from alphabet tile on `all` pane. Tap letter → scroll list to section and dismiss. Tap outside or Back → dismiss.
- **Background:** Black with slight scrim optional.

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
- `PeoplePane { All, WhatsNew }`

## Out of scope (v1)

- Rooms, Groups, Together pane (WP 8.0 only)
- Recent pane
- Inline social like/comment/reply
- Real Facebook/Twitter/LinkedIn API sync (stub feeds + external deep links only)
- Me tile as live Start tile
- Dual-SIM per-contact defaults
- Contact binding / Connect tile rich content APIs
