package com.metro.messaging.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagingLogicTest {
    @Test
    fun normalizeAddress_stripsFormatting() {
        assertEquals("+15551234567", MessagingLogic.normalizeAddress("+1 (555) 123-4567"))
    }

    @Test
    fun threadIdForAddress_isStable() {
        val first = MessagingLogic.threadIdForAddress("+15551234567")
        val second = MessagingLogic.threadIdForAddress("+15551234567")
        assertEquals(first, second)
    }

    @Test
    fun previewText_truncatesLongBodies() {
        val preview = MessagingLogic.previewText("a".repeat(60))
        assertTrue(preview.endsWith("…"))
        assertTrue(preview.length <= 48)
    }

    @Test
    fun mergeThreads_prefersLatestTimestamp() {
        val older = ConversationThread(1L, "+1", "A", "old", 100L, 0)
        val newer = ConversationThread(1L, "+1", "A", "new", 200L, 1)
        val merged = MessagingLogic.mergeThreads(listOf(older), listOf(newer))
        assertEquals(1, merged.size)
        assertEquals("new", merged.first().preview)
        assertEquals(1, merged.first().unreadCount)
    }

    @Test
    fun mergeMessages_ordersChronologically() {
        val messages = MessagingLogic.mergeMessages(
            listOf(message(2L, 200L)),
            listOf(message(1L, 100L)),
        )
        assertEquals(listOf(1L, 2L), messages.map { it.id })
    }

    @Test
    fun markThreadRead_clearsUnreadCount() {
        val threads = listOf(
            ConversationThread(1L, "+1", "A", "hi", 100L, 2),
        )
        val read = MessagingLogic.markThreadRead(threads, 1L)
        assertEquals(0, read.first().unreadCount)
    }

    private fun message(id: Long, timestamp: Long) = MessageItem(
        id = id,
        threadId = 1L,
        body = "test",
        timestamp = timestamp,
        direction = MessageDirection.Incoming,
    )
}
