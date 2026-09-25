# Conversations

**Package:** `com.metro.conversations`  
**Tier:** 2

## Status

**Prototype** — panorama home with 2-up Start-style app tiles; lists + RemoteInput reply.
Excludes SMS / suite Messaging notifications.

## App role

Reply inbox over shade notifications with free-form reply, grouped by app. Not Action Center.
Not SMS (use Messaging for that).

## Screen inventory

1. Notification access setup
2. Home — title **conversations**, panorama intro, 2-col tiles (all chats, favorites, each app)
3. Conversation list (all chats + favorites pivot, or one app)
4. Thread — read + reply; favorite via app-bar heart
5. Live tile — flips through active replyable shade chats

## Commands

```bash
cd apps/conversations

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test

# From repo root
../../scripts/verify-app.sh conversations
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| No WP Conversations app | N/A | metro-os original over Android RemoteInput |
| Start tile branding | Icon packs / adaptive icons | `MetroAppBranding` same as launcher |

## Agent postmortem

_None._
