# Mail

**Package:** `com.metro.mail`  
**Tier:** 2

## Status

Harness docs only — Android project not scaffolded yet. This README is the implementation guide for the WP8.1 mail app.

## App role

This app recreates the WP8.1 **Mail** experience: linked inboxes, folder navigation, and conversation-oriented reading in Metro chrome.

It should feel productivity-focused and text-forward. Avoid turning it into a Material email client with swipe cards, FAB compose patterns, or top-heavy toolbar chrome.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Backend strategy explicitly constrained for v1 before UI work expands

## Screen inventory

### 1. Linked inbox / folder surface

- Primary landing experience
- Supports folder navigation and inbox grouping
- Expected reference: `references/images/inbox_dark_blue.png`

### 2. Conversation view

- Read a thread with Metro list and typography rhythm
- Expected reference: `references/images/conversation_dark_blue.png`

### 3. Compose / reply surface

- Include only if needed for v1 implementation scope
- Keep bottom app bar command style
- Expected reference: `references/images/compose_dark_blue.png`

## System functions and contracts

- Account/backend may be stubbed in v1 if real IMAP/SMTP support is not yet approved
- Define account model, folder model, and message/thread model before UI state explodes
- Keep linked inbox behavior explicit: whether it is true merged ordering or a grouped abstraction
- Avoid app-local hacks for future sync; isolate repository layer

## UI and interaction guardrails

- Use flat lists and text hierarchy
- Prefer app bar commands over gestures copied from modern mail apps
- No Material swipe archives or floating compose button
- Keep folder navigation Metro-appropriate and consistent with references

## Data and state model

- `MailAccount`, `MailFolder`, `MailThread`, `MailMessage`
- Track active account/filter, current folder, selected thread, sync state, and unread counts

## Primary implementation order

1. Define v1 repository/backend stub behavior
2. Build inbox/folder navigation
3. Build thread list and conversation view
4. Add unread state and linked inbox presentation
5. Add compose/reply only if in approved scope

## Test-critical user flows

1. Open inbox and change folders
2. Open a conversation thread and return to list
3. Preserve selected folder/account state across navigation
4. Render unread counts consistently

## Reference and golden expectations

- `references/images/inbox_dark_blue.png`
- `references/images/conversation_dark_blue.png`
- `references/images/compose_dark_blue.png`

## Commands

```bash
cd apps/mail

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh mail
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Full first-party Microsoft mail account ecosystem | Backend/service scope not yet defined | Allow stubbed local/demo mail data in v1 while locking down Metro UI structure and thread behaviors |

## Agent postmortem

_None._
