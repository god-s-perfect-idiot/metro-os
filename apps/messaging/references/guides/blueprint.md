# Messaging — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

Target: **Windows Phone 8.1** Messaging app on a portrait phone (768×1280 / xhdpi reference profile).

## App shell

- **Navigation model:** Pivot filter + list-with-details drilldown. WP8.1 Messaging was SMS-only without a chats pivot; metro-os adds a `chats` page for saved-contact threads while keeping `threads` as the full inbox.
- **Theme:** Black background (`#000000`) on all surfaces.
- **Typography:** Noto Sans stand-in for Segoe WP. Thread titles and hub header use large left-aligned type.
- **No Material:** No FAB, rounded chat bubbles, snackbars, or bottom sheets.
- **Session:** Re-opening the app restores the last open conversation (cleared when the user backs to the list). Pivot page (`threads` / `chats`) is remembered.

## Pages

### Page 1 — Thread list (landing)

- **Layout:**
  - App title overline: `MESSAGING` (`MetroAppTitle`).
  - Pivot titles: `threads` | `chats` (HubTitle / PivotTab).
    - **threads:** every conversation (saved contacts + unknown numbers).
    - **chats:** only conversations whose peer resolved to a saved contact display name.
  - Vertical list of conversation threads. Each row:
    - **Primary line:** contact display name or formatted phone number (24sp).
    - **Secondary line:** message preview + relative time (16sp grey), separated by ` · `.
    - **Unread:** primary line and optional count use accent color.
  - Rows min height 76dp; 24dp horizontal margin.
  - Demo-data banner when SMS permission not granted.
  - Empty state when no threads / no saved-contact chats.
  - **Bottom app bar** (`MetroAppBar`): `+` new message; `…` expands to reveal labels and overflow text items (`set as default messaging app`, `select`, `settings` stubs).
- **Navigation:** Tap row → Page 2 (conversation). Long-press → delete stub toast (v1). App bar `new` → Page 4 (new message).
- **Background:** Solid black.
- **Reference:** `images/threads_dark_yellow.jpg` (official WP8 dark-theme capture; note app bar order: `+` new, people picker, list/select, `…`). Pivot is a metro-os extension — see `known-gaps.md`.

### Page 2 — Conversation

- **Layout:**
  - Header: contact name (20sp caps section style) + phone number in accent subtitle.
  - Chronological message list (oldest at top, auto-scroll to latest) — SMS **and** downloaded MMS.
  - **Message bubbles** (WP8.1 chat chrome — square body + right-triangle tail):
    - **Received (left):** full system **accent** fill, white text; triangular tail on the **top-left** corner pointing up/left.
    - **Sent (right):** darkened accent (~70% of accent RGB), white text; triangular tail on the **bottom-right** corner pointing down/right.
    - MMS image parts render inside the bubble (fit, square chrome); tap → fullscreen black image viewer (tap/Back returns to conversation).
    - Timestamp (`4:55pm`) + send state (`sending`, `failed`) bottom-right inside each bubble.
  - **Composer:** light-gray (`#D0D0D0`) bubble matching the **outgoing** shape (bottom-right tail); placeholder `type a text message` in dark gray; square-border `send` button below (also IME Send).
- **Navigation:** System Back → thread list preserving read state. Draft text persists per thread. Image viewer Back → conversation.
- **Interactions:** Type message, tap `send` (or SIP Send) to send (real SMS when permitted, local simulation otherwise). Open MMS images already stored on-device.
- **Background:** Black.
- **Reference:** `images/conversation_dark_orange.png` (real WP dark-theme conversation — orange accent bubbles + gray composer). Schematic: `images/conversation_schematic.png`.

### Page 3 — Permission / demo gate

- **Layout:** Full-page explanation + `allow access` (requests READ_SMS, SEND_SMS, READ_CONTACTS) + `continue with demo data`.
- **Navigation:** Grant → thread list with device SMS. Demo → thread list with stub conversations. Already the default SMS app → skip gate and read Telephony directly.
- **Note:** `smsto:` intents auto-skip gate and open the target thread.

### Page 4 — New message (compose)

- **Layout:** Section header `new message`; underline `to` field (name or phone) + contact suggestion list while typing; bottom-anchored outgoing message composer (same light-gray outgoing bubble + square-border `send` as Page 2; send enabled when both recipient and body are non-empty).
- **Navigation:** System Back → thread list. Successful send → Page 2 for the new/existing thread.
- **Interactions:** Enter recipient (name or number) + body; matching contacts appear under `to` (name + number); tap a suggestion to fill the phone number; tap composer `send` to send (real SMS when permitted). Unique contact-name recipients resolve to that contact’s number on send.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `threads_dark_yellow.jpg` | Page 1 — thread list | Official WP8 capture — conversation list + bottom app bar |
| `conversation_dark_orange.png` | Page 2 — conversation | Real WP dark-theme capture — accent received / darkened sent bubbles + gray composer |
| `conversation_schematic.png` | Page 2 — conversation | Official layout schematic — sent/received bubble alignment + MMS |
| `settings_dark_yellow.jpg` | (settings, out of v1 scope) | Toggle-list styling reference |
| `tile_yellow.jpg` | Messaging live tile (unread wink glyph + count) | Messaging tile glyph + unread count |

## Data model (implementation hint)

- `ConversationThread(id, address, displayName?, preview, timestamp, unreadCount)`
- `MessageItem(id, threadId, body, timestamp, direction, sendState, imageUris, isMms)`
- `MessageDirection { Incoming, Outgoing }`
- `SendState { Draft, Sending, Sent, Failed }`
- `MessagingRoute { Threads, Conversation(threadId), ImageViewer(threadId, uri) }`

## System integration

- `READ_SMS` / `SEND_SMS` / `RECEIVE_SMS` for device SMS via `Telephony` provider.
- `READ_CONTACTS` optional for display names.
- `ACTION_SENDTO` + `smsto:` intent filter (People app text action).
- **Default SMS app role:** manifest declares `SMS_DELIVER`, `WAP_PUSH_DELIVER`, and `RESPOND_VIA_MESSAGE` components; app bar overflow offers `set as default messaging app` via `RoleManager` (API 29+).
- When default, the app reads threads/messages directly from the Telephony SMS provider (no demo fallback), outgoing messages are written to `Telephony.Sms.Sent`, and inbound `SMS_DELIVER` is persisted to the inbox.
- Repository seam: `SmsMessagingDataSource` + `StubMessagingDataSource` + `LocalMessageStore` overlay. Access gate is `READ_SMS` **or** default-SMS role (`canAccessSystemSms`). Thread list loads from `Telephony.Threads` (`simple=true`, one row per conversation) on a background dispatcher; falls back to grouping `Telephony.Sms.CONTENT_URI` only when the threads table is unavailable. Never use the OEM-flaky `Sms.Conversations` view.

## Out of scope (v1)

- Composing/sending new MMS (attach + MMSC upload) and downloading undownloaded WAP-push notifications
- Facebook / Skype chat integration (removed in WP8.1)
- Multi-select delete
