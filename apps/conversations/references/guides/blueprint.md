# Conversations — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Prototype UI — panorama home with Start-style tiles; richer presentation may follow later.

## Pages

### Page 1 — Notification access (setup)

- Layout: `MetroAppTitle` + title **conversations** + body + `MetroBorderButton` **turn on access**
- Navigation: root when notification listener is not enabled
- Interactions: opens system Notification access settings; resume rechecks

### Page 2 — Home (panorama)

- Layout: single panorama page — brand title **conversations** (`MetroPanoramaBrandEnter`) +
  body (`MetroPanoramaBodyEnter`) with a **2-column** vertical tile grid
- Tile 1: **all chats** (system accent + people-group glyph)
- Tile 2: **favorites** (pink + heart glyph)
- Following tiles: one per notifying app (Start tile fill from `MetroAppBranding`, app icon,
  app label) — SMS / suite Messaging excluded; **Gmail included** even without RemoteInput
- Last tile when apps exist: **clear** (red + trash glyph) — dismisses all Conversations-tracked
  shade posts; omitted when the home grid has no app tiles; always occupies the last 2-up slot
  (slides in with the first app tile; later apps push it to the new last slot)
- Tile size: ~88% of half-width square; enter: Hub extras+info right-slide stagger
- Interactions: tap app tile → Page 3; tap clear → staggered slide-out of app tiles + clear, then refresh

### Page 3 — Conversation list

- **all chats:** `MetroPivot` — **all chats** | **favorites**; favorites show active shade chats
  whose peer title was saved as a favorite for that app
- **per app:** that app’s conversations only, subtitle = notification preview
- Navigation: `MetroSubpageHost` drill-in from home; Back → home
- Interactions: tap row → Page 4; long-press → `MetroContextMenuPopup` **favorite** /
  **unfavorite** / **dismiss** (single notification)

### Page 4 — Thread (read + reply)

- Layout: app label + conversation title; message list; `MetroTextBox` + **send** / **open app**;
  bottom `MetroAppBar` with round **heart** (favorite) / **heart-slash** (unfavorite)
- Navigation: drill-in from list; Back → list
- Interactions: RemoteInput reply; open fires `contentIntent`; heart toggles favorite
  (package + peer title)

### Live tile

- Provider: `ConversationsTileProvider` (`com.metro.conversations.tiles`)
- Front: Conversations glyph + count of active replyable chats
- Flip: cycles each active conversation (title = peer, body = preview, footer = app)
- Updates on notification listener changes via `TILE_UPDATE`

## Images

No WP8.1 Conversations product — see [`known-gaps.md`](../known-gaps.md).

## Out of scope (v1)

- SMS / `com.metro.messaging` (and stock Messages packages)
- Action Center chrome
- Multi-pane panorama sections (home is one page with panorama *intro* motion only)
