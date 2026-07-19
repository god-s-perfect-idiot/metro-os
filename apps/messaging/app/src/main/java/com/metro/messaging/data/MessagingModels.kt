package com.metro.messaging.data

enum class MessageDirection {
    Incoming,
    Outgoing,
}

enum class SendState {
    Draft,
    Sending,
    Sent,
    Failed,
}

data class ConversationThread(
    val id: Long,
    val address: String,
    val displayName: String?,
    val preview: String,
    val timestamp: Long,
    val unreadCount: Int,
)

/** Unread count + optional peek label for the Start live tile. */
data class SmsTilePeek(
    val unreadCount: Int,
    val latestUnreadLabel: String?,
)

data class MessageItem(
    val id: Long,
    val threadId: Long,
    val body: String,
    val timestamp: Long,
    val direction: MessageDirection,
    val sendState: SendState = SendState.Sent,
)

data class DraftState(
    val threadId: Long,
    val text: String,
)

data class ContactSuggestion(
    val displayName: String,
    val phoneNumber: String,
    val normalizedNumber: String,
)
