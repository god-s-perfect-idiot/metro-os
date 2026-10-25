package com.metro.people.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleRecentLogicTest {
    @Test
    fun uniqueRecentPeople_mergesCallsAndMessages_savedOnly_newestWins() {
        val calls = listOf(
            call(1, "+15551230001", "Alice", 300L, contactId = 10),
            call(2, "+15551230002", "Bob", 100L, contactId = 11),
            call(3, "+15551239999", null, 400L, contactId = null), // unsaved — drop
        )
        val messages = listOf(
            PeopleMessageTouch(11, "Bob", "+15551230002", 350L),
            PeopleMessageTouch(12, "Carol", "+15551230003", 200L),
        )
        val recent = PeopleRecentLogic.uniqueRecentPeople(calls, messages, limit = 10)
        assertEquals(listOf("Bob", "Alice", "Carol"), recent.map { it.displayName })
        assertEquals(RecentActivityKind.Message, recent[0].lastKind)
        assertEquals(RecentActivityKind.Call, recent[1].lastKind)
    }

    @Test
    fun uniqueRecentPeople_capsAtTen() {
        val calls = (1..15).map { i ->
            call(
                id = i.toLong(),
                number = "+1555123${i.toString().padStart(4, '0')}",
                name = "Person $i",
                timestamp = (100 - i).toLong(),
                contactId = i.toLong(),
            )
        }
        val recent = PeopleRecentLogic.uniqueRecentPeople(calls, emptyList())
        assertEquals(PeopleRecentLogic.MaxRecentPeople, recent.size)
        assertEquals("Person 1", recent.first().displayName)
    }

    @Test
    fun mergeMessageLog_popsOldestWhenOverCapacity() {
        val existing = (1..10).map { i ->
            PeopleMessageTouch(i.toLong(), "P$i", "555$i", timestamp = i.toLong())
        }
        val incoming = listOf(
            PeopleMessageTouch(99, "New", "55599", timestamp = 100L),
        )
        val merged = PeopleRecentLogic.mergeMessageLog(existing, incoming, limit = 10)
        assertEquals(10, merged.size)
        assertEquals(99L, merged.first().contactId)
        assertTrue(merged.none { it.contactId == 1L })
    }

    @Test
    fun whatsNewMeta_formatsSourceAndRelativeTime() {
        val meta = PeopleRecentLogic.whatsNewMeta("WhatsApp", 1_000L, now = 1_000L + 3 * 60_000L)
        assertEquals("WhatsApp 3 minutes ago", meta)
    }

    private fun call(
        id: Long,
        number: String,
        name: String?,
        timestamp: Long,
        contactId: Long?,
    ) = PeopleCallEntry(
        id = id,
        phoneNumber = number,
        normalizedNumber = PeopleRecentLogic.normalizeNumber(number),
        type = CallDirection.Outgoing,
        timestamp = timestamp,
        contactId = contactId,
        contactName = name,
    )
}
