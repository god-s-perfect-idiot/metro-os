package com.metro.messaging.data

import java.util.Calendar
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
    fun mergeMessages_dedupesLocalOverlayAgainstProviderSend() {
        val fromProvider = MessageItem(
            id = 42L,
            threadId = 1L,
            body = "hello",
            timestamp = 1_000L,
            direction = MessageDirection.Outgoing,
            sendState = SendState.Sent,
        )
        val fromLocal = MessageItem(
            id = 9_999_999L,
            threadId = 1L,
            body = "hello",
            timestamp = 1_000L,
            direction = MessageDirection.Outgoing,
            sendState = SendState.Sent,
        )
        val merged = MessagingLogic.mergeMessages(listOf(fromProvider), listOf(fromLocal))
        assertEquals(listOf(42L), merged.map { it.id })
    }

    @Test
    fun markThreadRead_clearsUnreadCount() {
        val threads = listOf(
            ConversationThread(1L, "+1", "A", "hi", 100L, 2),
        )
        val read = MessagingLogic.markThreadRead(threads, 1L)
        assertEquals(0, read.first().unreadCount)
    }

    @Test
    fun threadsFromSmsRows_groupsByThreadNewestFirst() {
        val rows = sequenceOf(
            MessagingLogic.SmsRow(10L, "+15551111111", "latest A", 300L, read = true),
            MessagingLogic.SmsRow(20L, "+15552222222", "latest B", 250L, read = false),
            MessagingLogic.SmsRow(10L, "+15551111111", "older A", 100L, read = false),
            MessagingLogic.SmsRow(20L, "+15552222222", "older B", 50L, read = false),
        )
        val threads = MessagingLogic.threadsFromSmsRows(rows) { address ->
            if (address == "+15551111111") "Alice" else null
        }
        assertEquals(listOf(10L, 20L), threads.map { it.id })
        assertEquals("latest A", threads[0].preview)
        assertEquals("Alice", threads[0].displayName)
        assertEquals(1, threads[0].unreadCount)
        assertEquals("latest B", threads[1].preview)
        assertEquals(2, threads[1].unreadCount)
    }

    @Test
    fun threadsFromSmsRows_skipsBlankAddresses() {
        val rows = sequenceOf(
            MessagingLogic.SmsRow(1L, null, "orphan", 100L, read = true),
            MessagingLogic.SmsRow(2L, "  ", "blank", 90L, read = true),
            MessagingLogic.SmsRow(3L, "+15553333333", "ok", 80L, read = true),
        )
        val threads = MessagingLogic.threadsFromSmsRows(rows)
        assertEquals(listOf(3L), threads.map { it.id })
    }

    @Test
    fun contactSuggestions_matchesNameAndNumber() {
        val contacts = listOf(
            ContactSuggestion("Alice Smith", "(555) 111-2222", "5551112222"),
            ContactSuggestion("Bob Jones", "555-9999", "5559999"),
            ContactSuggestion("Alicia", "+15553334444", "15553334444"),
        )
        val byName = MessagingLogic.contactSuggestions("ali", contacts)
        assertEquals(listOf("Alice Smith", "Alicia"), byName.map { it.displayName })

        val byNumber = MessagingLogic.contactSuggestions("5551", contacts)
        assertEquals(listOf("Alice Smith"), byNumber.map { it.displayName })
    }

    @Test
    fun resolveRecipientAddress_mapsUniqueNameToNumber() {
        val contacts = listOf(
            ContactSuggestion("Alice", "555-1111", "5551111"),
            ContactSuggestion("Bob", "555-2222", "5552222"),
        )
        assertEquals("555-1111", MessagingLogic.resolveRecipientAddress("Alice", contacts))
        assertEquals("555-9999", MessagingLogic.resolveRecipientAddress("555-9999", contacts))
    }

    @Test
    fun bubbleTime_formatsWpStyle() {
        val cal = java.util.Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 55)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("4:55pm", MessagingLogic.bubbleTime(cal.timeInMillis))

        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 5)
        assertEquals("9:05am", MessagingLogic.bubbleTime(cal.timeInMillis))
    }

    private fun message(id: Long, timestamp: Long) = MessageItem(
        id = id,
        threadId = 1L,
        body = "test",
        timestamp = timestamp,
        direction = MessageDirection.Incoming,
    )
}
