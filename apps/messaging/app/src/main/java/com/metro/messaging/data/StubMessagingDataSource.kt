package com.metro.messaging.data

object StubMessagingDataSource {
    private val now = System.currentTimeMillis()

    fun demoThreads(): List<ConversationThread> = listOf(
        ConversationThread(
            id = MessagingLogic.threadIdForAddress("+15551234001"),
            address = "+15551234001",
            displayName = "Alex Morgan",
            preview = "See you at the café around 6?",
            timestamp = now - 12 * 60_000L,
            unreadCount = 1,
        ),
        ConversationThread(
            id = MessagingLogic.threadIdForAddress("+15559876543"),
            address = "+15559876543",
            displayName = "Jordan Lee",
            preview = "Thanks for sending the files over.",
            timestamp = now - 3 * 3_600_000L,
            unreadCount = 0,
        ),
        ConversationThread(
            id = MessagingLogic.threadIdForAddress("+15557654321"),
            address = "+15557654321",
            displayName = null,
            preview = "Your verification code is 482910.",
            timestamp = now - 26 * 3_600_000L,
            unreadCount = 0,
        ),
    )

    fun demoMessages(threadId: Long): List<MessageItem> = when (threadId) {
        MessagingLogic.threadIdForAddress("+15551234001") -> listOf(
            message(1, threadId, "Are we still on for tonight?", now - 40 * 60_000L, MessageDirection.Incoming),
            message(2, threadId, "Yes — I'll be there.", now - 35 * 60_000L, MessageDirection.Outgoing),
            message(3, threadId, "See you at the café around 6?", now - 12 * 60_000L, MessageDirection.Incoming),
        )
        MessagingLogic.threadIdForAddress("+15559876543") -> listOf(
            message(4, threadId, "Did you get the invoice?", now - 5 * 3_600_000L, MessageDirection.Outgoing),
            message(5, threadId, "Thanks for sending the files over.", now - 3 * 3_600_000L, MessageDirection.Incoming),
        )
        MessagingLogic.threadIdForAddress("+15557654321") -> listOf(
            message(
                6,
                threadId,
                "Your verification code is 482910.",
                now - 26 * 3_600_000L,
                MessageDirection.Incoming,
            ),
        )
        else -> emptyList()
    }

    fun threadForAddress(address: String, displayName: String? = null): ConversationThread {
        val normalized = MessagingLogic.normalizeAddress(address)
        val id = MessagingLogic.threadIdForAddress(normalized)
        return ConversationThread(
            id = id,
            address = normalized.ifEmpty { address },
            displayName = displayName,
            preview = "",
            timestamp = System.currentTimeMillis(),
            unreadCount = 0,
        )
    }

    private fun message(
        id: Long,
        threadId: Long,
        body: String,
        timestamp: Long,
        direction: MessageDirection,
        sendState: SendState = SendState.Sent,
    ) = MessageItem(
        id = id,
        threadId = threadId,
        body = body,
        timestamp = timestamp,
        direction = direction,
        sendState = sendState,
    )
}
