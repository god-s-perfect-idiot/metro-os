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
    fun formatContactDisplayName_usesSentenceCaps() {
        assertEquals("John smith", DialerCallLogic.formatContactDisplayName("JOHN SMITH"))
        assertEquals("Alice", DialerCallLogic.formatContactDisplayName("alice"))
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
            CallGroup("5551", "555-1", "Alice", CallDirection.Incoming, 0L, 1, emptyList()),
            CallGroup("5552", "555-2", "Bob", CallDirection.Outgoing, 0L, 1, emptyList()),
        )
        assertEquals(1, DialerCallLogic.filterGroups(groups, "ali").size)
        assertEquals(1, DialerCallLogic.filterGroups(groups, "555-2").size)
        assertEquals(1, DialerCallLogic.filterGroups(groups, "5552").size)
    }

    @Test
    fun filterGroups_matchesFormattedNumbersWithoutFormattingInQuery() {
        val groups = listOf(
            CallGroup("+15551234567", "+1 (555) 123-4567", "Alice", CallDirection.Incoming, 0L, 1, emptyList()),
        )
        assertEquals(1, DialerCallLogic.filterGroups(groups, "5551234").size)
        assertEquals(1, DialerCallLogic.filterGroups(groups, "555-123").size)
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
    fun contactSuggestions_matchesNumberPrefixWithCountryCode() {
        val contacts = listOf(
            ContactSuggestion("Alice", "+1 (555) 123-4567", "+15551234567"),
            ContactSuggestion("Bob", "444-9999", "4449999"),
        )
        val matches = DialerCallLogic.contactSuggestions("555", contacts)
        assertEquals(1, matches.size)
        assertEquals("Alice", matches.first().displayName)
    }

    @Test
    fun contactSuggestions_matchesIndianNumberWithoutCountryCode() {
        val contacts = listOf(
            ContactSuggestion("Alice", "+91 9876543210", "+919876543210"),
            ContactSuggestion("Bob", "444-9999", "4449999"),
        )
        val matches = DialerCallLogic.contactSuggestions("98765", contacts)
        assertEquals(1, matches.size)
        assertEquals("Alice", matches.first().displayName)
    }

    @Test
    fun contactSuggestions_prioritizesNumberMatchOverT9NameMatch() {
        val contacts = listOf(
            ContactSuggestion("Kell", "444-1111", "4441111"),
            ContactSuggestion("Alice", "555-9999", "5559999"),
        )
        val matches = DialerCallLogic.contactSuggestions("555", contacts)
        assertEquals(2, matches.size)
        assertEquals("Alice", matches.first().displayName)
    }

    @Test
    fun mergeContactSuggestions_prefersProviderMatches() {
        val provider = listOf(
            ContactSuggestion("Alice", "555-9999", "5559999"),
        )
        val cache = listOf(
            ContactSuggestion("Kell", "444-1111", "4441111"),
            ContactSuggestion("Alice", "555-9999", "5559999"),
        )
        val merged = DialerCallLogic.mergeContactSuggestions(provider, cache)
        assertEquals(2, merged.size)
        assertEquals("Alice", merged.first().displayName)
    }

    @Test
    fun matchesNumberPrefix_handlesLeadingZeroTrunkPrefix() {
        assertTrue(DialerCallLogic.matchesNumberPrefix("09876543210", "9876543210"))
    }

    @Test
    fun formatDuration_formatsMinutesAndSeconds() {
        assertEquals("1:05", DialerCallLogic.formatDuration(65))
    }

    @Test
    fun matchesT9_findsNameByDigits() {
        assertTrue(DialerCallLogic.matchesT9("Alice", "2543"))
    }

    @Test
    fun isIncomingRinging_trueForIncomingNotConnected() {
        val call = ActiveCall(
            phoneNumber = "5551234",
            displayName = "Alice",
            startedAtMillis = 0L,
            direction = CallDirection.Incoming,
            connected = false,
        )
        assertTrue(DialerCallLogic.isIncomingRinging(call))
    }

    @Test
    fun isIncomingRinging_falseForOutgoingOrConnected() {
        val outgoing = ActiveCall("555", "Bob", 0L, CallDirection.Outgoing, connected = false)
        val answered = ActiveCall("555", "Bob", 0L, CallDirection.Incoming, connected = true)
        assertTrue(!DialerCallLogic.isIncomingRinging(outgoing))
        assertTrue(!DialerCallLogic.isIncomingRinging(answered))
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
