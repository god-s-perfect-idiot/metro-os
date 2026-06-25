# Phone — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

Target: **Windows Phone 8.1 GDR2+** Phone app on a portrait phone (768×1280 / xhdpi reference profile).

## App shell

- **Control model:** `MetroPivot` with two headers: `history` | `speed dial`. Horizontal swipe between panes.
- **Theme:** Black background (`#000000`) on all Phone surfaces.
- **Typography:** Noto Sans stand-in for Segoe WP. Contact names and dialed numbers are large and left-aligned.
- **App bar:** Minimized bottom bar on history/speed-dial panes with round icon buttons: dial pad, people, search, … overflow.
- **No Material:** No FAB, chips, rounded cards, or bottom sheets.

## WP 8.1 deltas (vs WP 8.0)

| WP 8.0 | WP 8.1 (build this) |
|--------|---------------------|
| Flat call log (every call listed) | **Grouped history** — repeat calls from same contact/number collapsed with `(n)` count |
| Call/Save on bottom app bar | **Call and save in dial-pad grid** |
| No speed dial | **Speed dial pane** with pinned contacts |
| Smaller in-call UI | **Full-screen outgoing call** with large name/photo and bottom end-call control |

## Pages

### Page 1 — History (default landing, pivot pane 0)

- **Layout:**
  - Pivot header: `history` (active, accent underline) | `speed dial` (inactive grey).
  - Vertical list of grouped call events. Each row:
    - **Primary line:** contact display name or formatted phone number (24sp Regular, left).
    - **Secondary line:** relative time + call direction label (`incoming`, `outgoing`, `missed`) in grey (16sp).
    - **Count badge:** `(3)` suffix on primary line when grouped count > 1.
    - **Missed calls:** primary text uses accent red (`#E51400`) for missed type.
    - **Callback affordance:** phone-hand glyph in accent circle at row right — tap calls default number.
  - Rows separated by 1px dividers at 20% white; 76dp min height.
  - Empty state: centred grey message when no call log permission or no entries.
- **Navigation:**
  - Swipe right → speed dial pane.
  - Dial-pad icon in app bar → Page 3 (dial pad).
  - People icon → launch `com.metro.people` (or system contacts fallback).
  - Search icon → in-app filter overlay (filter history by typed name/number).
  - Tap row primary area → Page 2 (number / call detail for that entry).
  - Tap callback icon → place call (Page 4).
- **Interactions:**
  - Long-press row → context menu stub (`add to speed dial`, `block number` — speed dial wired in v1).
- **Background:** Solid black.
- **Reference:** `images/history_dark_blue.png`

### Page 2 — Number view (call detail for one contact/number)

- **Layout:** Full-page drill-in (not a pivot pane).
  - Header: contact name or formatted number (20sp caps section label style).
  - Subheader: phone number in accent when name is shown.
  - Chronological list of individual call attempts:
    - Direction icon (incoming/outgoing/missed).
    - Date + time (grey).
    - Duration (`mm:ss`) for answered calls; `missed` / `declined` labels otherwise.
  - Bottom app bar: phone icon (call this number), … overflow.
- **Navigation:** System Back → history pane preserving scroll. Call icon → Page 4.
- **Background:** Black.

### Page 3 — Dial pad

- **Layout:**
  - Top: dialed number field (32sp+, left-aligned, accent when non-empty).
  - Below number: T9 contact suggestion list (max 3 rows) — name + number; tap fills number and calls or selects.
  - Key grid (square tiles, no rounded corners):
    - Rows 1–3: digits 1–9 with letter hints (`2 ABC`, etc.) in smaller grey sub-label.
    - Row 4: `*` | `0` with `+` hint | `#`.
    - Row 5: wide **call** tile (accent fill) | wide **save** tile (border button).
  - Long-press `0` inserts `+`.
  - Backspace: tap number field area or dedicated delete affordance when digits present.
  - Bottom app bar: people icon, … overflow (no dial-pad icon while on this page).
- **Navigation:** Back → history. Call → Page 4. Save → add-to-contact stub (toast in v1). People icon → People app.
- **Interactions:** Haptic optional; tile press uses accent flash (150ms).
- **Background:** Black.
- **Reference:** `images/dialpad_dark_blue.jpg`

### Page 4 — In-call (dialing / active call screen)

- **Layout:**
  - Full-bleed black screen.
  - Upper region: square contact photo (200dp) when resolved, else blank.
  - Large display name or number (48sp Light), centred-left with 24dp margin.
  - Status line: `calling…` then elapsed `mm:ss` timer once connected (simulated connect after brief delay in v1 stub).
  - Bottom fixed region:
    - **End call** — large circular red control centred above system nav (`#E51400`).
    - Optional **video** stub icon (grey, disabled in v1).
  - No in-app back button; End ends call and returns to previous route.
- **Navigation:** End call → previous screen. System Back same as End during outgoing ring.
- **Background:** Black.
- **Reference:** `images/in_call_dark_blue.png`

### Page 5 — Speed dial (pivot pane 1)

- **Layout:**
  - Pivot header: `history` | `speed dial` (active).
  - List of pinned contacts: square avatar (48dp), name (24sp), default number subtitle (grey).
  - Empty state: message + accent `+` hint to add from history long-press.
  - Bottom app bar: same as history (dial pad, people, search, …).
- **Navigation:** Swipe left → history. Tap row → call. Long-press → remove from speed dial.
- **Background:** Black.
- **Reference:** `images/speed_dial_dark_blue.png`

### Page 6 — Permission / empty states

- **No READ_CALL_LOG:** Full-page explanation + grant action. Show empty history (not fake data).
- **No READ_CONTACTS:** History still works with raw numbers; dial-pad T9 suggestions disabled.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `history_dark_blue.png` | Page 1 — history | Grouped call log, Lumia 920 dev preview |
| `speed_dial_dark_blue.png` | Page 5 — speed dial | Pinned contacts list |
| `dialpad_dark_blue.jpg` | Page 3 — dial pad | Keypad + autocomplete suggestions |
| `in_call_dark_blue.png` | Page 4 — in-call | Outgoing call full-screen UI |

## Data model (implementation hint)

- `CallGroup(id, displayName, phoneNumber, latestType, latestTimestamp, callCount, calls: List<CallEntry>)`
- `CallEntry(id, phoneNumber, type, timestamp, durationSeconds, contactName?)`
- `CallDirection { Incoming, Outgoing, Missed }`
- `SpeedDialEntry(id, displayName, phoneNumber, photoUri?)`
- `DialerRoute { Main, DialPad, CallDetail, InCall, Search }`

## System integration

- Read call log via `CallLog.Calls` (requires `READ_CALL_LOG`, `READ_CONTACTS` for names).
- Place calls via `Intent.ACTION_CALL` when `CALL_PHONE` granted; else `ACTION_DIAL`.
- Register `ACTION_DIAL` and `ACTION_VIEW` (`tel:`) intent filters so Metro Phone can become default dialer later.
- Launch People via `com.metro.people` package intent.

## Out of scope (v1)

- Visual voicemail pane
- Dual-SIM smart switcher (SIM 1 / SIM 2 badges)
- Video call app picker
- Blocked numbers management screen
- Real add-contact form (save shows stub toast)
- Cortana voice dial
