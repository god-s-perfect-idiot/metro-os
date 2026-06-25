package com.metro.dialer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DialerCallLogicTest {
    @Test
    fun normalizeNumber_stripsFormatting() {
        assertEquals("+15551234567", DialerCallLogic.normalizeNumber("+1 (555) 123-4567"))
    }

    @Test
    fun groupCalls_collapsesByNumber() {
        val entries = listOf(
            callEntry("5551112222", CallDirection.Outgoing, 3000L),
            callEntry("5551112222", CallDirection.Incoming, 2000L),
            callEntry("5559998888", CallDirection.Missed, 1000L),
        )
        val groups = DialerCallLogic.groupCalls(entries)
        assertEquals(2, groups.size)
        assertEquals("5551112222", groups[0].normalizedNumber)
        assertEquals(2, groups[0].callCount)
        assertEquals(CallDirection.Outgoing, groups[0].latestType)
    }

    @Test
    fun primaryLabel_showsCountWhenGrouped() {
        val group = CallGroup(
            normalizedNumber = "555",
            phoneNumber = "555",
            displayName = "Alice",
            latestType = CallDirection.Incoming,
            latestTimestamp = 0L,
            callCount = 3,
            calls = emptyList(),
        )
        assertEquals("Alice (3)", DialerCallLogic.primaryLabel(group))
    }

    @Test
    fun filterGroups_matchesNameOrNumber() {
        val groups = listOf(
            CallGroup("1", "555-1", "Alice", CallDirection.Incoming, 0L, 1, emptyList()),
            CallGroup("2", "555-2", "Bob", CallDirection.Outgoing, 0L, 1, emptyList()),
        )
        assertEquals(1, DialerCallLogic.filterGroups(groups, "ali").size)
        assertEquals(1, DialerCallLogic.filterGroups(groups, "555-2").size)
    }

    @Test
    fun contactSuggestions_matchesNumberPrefix() {
        val contacts = listOf(
            ContactSuggestion("Alice", "555-1234", "5551234"),
            ContactSuggestion("Bob", "444-9999", "4449999"),
        )
        val matches = DialerCallLogic.contactSuggestions("555", contacts)
        assertEquals(1, matches.size)
        assertEquals("Alice", matches.first().displayName)
    }

    @Test
    fun formatDuration_formatsMinutesAndSeconds() {
        assertEquals("1:05", DialerCallLogic.formatDuration(65))
    }

    @Test
    fun matchesT9_findsNameByDigits() {
        assertTrue(DialerCallLogic.matchesT9("Alice", "2543"))
    }

    private fun callEntry(number: String, type: CallDirection, timestamp: Long): CallEntry {
        return CallEntry(
            id = timestamp,
            phoneNumber = number,
            normalizedNumber = DialerCallLogic.normalizeNumber(number),
            type = type,
            timestamp = timestamp,
            durationSeconds = 30,
            contactName = null,
        )
    }
}
