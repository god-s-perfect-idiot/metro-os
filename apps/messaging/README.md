# Messaging

**Package:** `com.metro.messaging`  
**Tier:** 2

## Status

Harness docs only — Android project not scaffolded yet. This README is the implementation brief for the WP8.1 messaging app.

## App role

This app recreates the WP8.1 **Messaging** experience with thread list and conversation pages for SMS/MMS-style communication.

It should prioritize clarity, thread continuity, and Metro styling. Avoid chat-app-modernization drift such as floating action buttons, bubbly Material surfaces, or oversized composer chrome.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- SMS/MMS permission approach reviewed before feature work begins

## Screen inventory

### 1. Thread list

- Primary list of conversations
- Expected reference: `references/images/threads_dark_blue.png`

### 2. Conversation page

- Individual thread view with sent/received message styling
- Expected reference: `references/images/conversation_dark_blue.png`

## System functions and contracts

- Requires SMS-related permissions if using real device messaging
- Define repository seams so v1 can support either stub data or real SMS sources cleanly
- Draft handling, send status, and message ordering must be explicit in the model
- MMS/media scope should be clearly documented if deferred

## UI and interaction guardrails

- Sent messages may use accent styling, but keep the overall surface flat and WP-like
- No Material message bubbles, attachment trays, or FAB compose controls
- Thread list should favor large typography and simple secondary metadata
- Composer should be understated and integrated into Metro page chrome

## Data and state model

- `ConversationThread`, `MessageItem`, `DraftState`, `SendState`
- Track current draft per thread, last-read timestamp, message direction, and failure state

## Primary implementation order

1. Define data source and permission strategy
2. Build thread list
3. Build conversation page and composer
4. Add draft persistence
5. Add send/failure state and any approved MMS support

## Test-critical user flows

1. Load thread list
2. Open thread and render ordered messages
3. Compose and persist a draft
4. Send or simulate sending a message
5. Return to thread list without losing read state

## Reference and golden expectations

- `references/images/threads_dark_blue.png`
- `references/images/conversation_dark_blue.png`

## Commands

```bash
cd apps/messaging

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh messaging
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Deep phone-integrated messaging behavior | SMS/MMS APIs and permissions can vary and may be sensitive in development | Allow stub/demo data path in early v1 while preserving exact thread and conversation UX |

## Agent postmortem

_None._
