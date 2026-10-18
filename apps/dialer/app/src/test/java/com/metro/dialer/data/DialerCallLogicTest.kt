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
            // "Kell" T9 is 5355 — must not steal the slot from Alice's number prefix.
            ContactSuggestion("Kell", "444-1111", "4441111"),
            ContactSuggestion("Alice", "555-9999", "5559999"),
        )
        val matches = DialerCallLogic.contactSuggestions("555", contacts)
        assertEquals(1, matches.size)
        assertEquals("Alice", matches.first().displayName)
    }

    @Test
    fun contactSuggestions_matchesT9NamePrefix() {
        val contacts = listOf(
            ContactSuggestion("Andrew Hill", "5550001", "5550001"),
            ContactSuggestion("Arturo Lopez", "5550005", "5550005"),
            ContactSuggestion("Chris Sells", "5550003", "5550003"),
            ContactSuggestion("Dana", "4449999", "4449999"),
        )
        val matches = DialerCallLogic.contactSuggestions("2", contacts)
        assertEquals(3, matches.size)
        assertEquals(
            listOf("Andrew Hill", "Arturo Lopez", "Chris Sells"),
            matches.map { it.displayName },
        )
    }

    @Test
    fun contactSuggestions_keepsMultipleContactsWithSameDisplayName() {
        val contacts = listOf(
            ContactSuggestion("John Smith", "5551111", "5551111", contactId = 10L),
            ContactSuggestion("John Smith", "5552222", "5552222", contactId = 11L),
            ContactSuggestion("John Smith", "5553333", "5553333", contactId = 12L),
            ContactSuggestion("Bob", "4449999", "4449999", contactId = 20L),
        )
        // 56467 ≈ "johns…" T9 prefix
        val matches = DialerCallLogic.contactSuggestions("56467", contacts)
        assertEquals(3, matches.size)
        assertTrue(matches.all { it.displayName == "John Smith" })
        assertEquals(
            setOf("5551111", "5552222", "5553333"),
            matches.map { it.normalizedNumber }.toSet(),
        )
    }

    @Test
    fun mergeContactSuggestions_keepsSameNameDifferentContacts() {
        val primary = listOf(
            ContactSuggestion("John Smith", "5551111", "5551111", contactId = 10L),
        )
        val secondary = listOf(
            ContactSuggestion("John Smith", "5552222", "5552222", contactId = 11L),
            ContactSuggestion("John Smith", "5551111", "5551111", contactId = 10L),
        )
        val merged = DialerCallLogic.mergeContactSuggestions(primary, secondary)
        assertEquals(2, merged.size)
    }

    @Test
    fun contactSuggestions_matchesT9LastNameWord() {
        val contacts = listOf(
            ContactSuggestion("John Smith", "5551111", "5551111"),
            ContactSuggestion("Bob", "4449999", "4449999"),
        )
        // 76 = SM… (Smith)
        val matches = DialerCallLogic.contactSuggestions("76", contacts)
        assertEquals(1, matches.size)
        assertEquals("John Smith", matches.first().displayName)
    }

    @Test
    fun contactSuggestions_matchesT9Initials() {
        val contacts = listOf(
            ContactSuggestion("John Smith", "5551111", "5551111"),
            ContactSuggestion("Bob", "4449999", "4449999"),
        )
        // 57 = J… S… (John Smith initials)
        val matches = DialerCallLogic.contactSuggestions("57", contacts)
        assertEquals(1, matches.size)
        assertEquals("John Smith", matches.first().displayName)
    }

    @Test
    fun contactSuggestions_returnsScrollableListBeyondThree() {
        val contacts = (1..12).map { index ->
            ContactSuggestion("Alice $index", "555000$index", "555000$index")
        }
        val matches = DialerCallLogic.contactSuggestions("555", contacts)
        assertEquals(12, matches.size)
    }

    @Test
    fun matchesT9_matchesInitialsAndCompactedName() {
        assertTrue(DialerCallLogic.matchesT9("John Smith", "57"))
        assertTrue(DialerCallLogic.matchesT9("John Smith", "56467")) // johns…
        assertTrue(!DialerCallLogic.matchesT9("John Smith", "58"))
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
    fun numberPrefixMatches_filtersNonPrefixHits() {
        val contacts = listOf(
            ContactSuggestion("Alice", "555-9999", "5559999"),
            ContactSuggestion("Bob", "444-9999", "4449999"),
        )
        val matches = DialerCallLogic.numberPrefixMatches("555", contacts)
        assertEquals(1, matches.size)
        assertEquals("Alice", matches.first().displayName)
    }

    @Test
    fun matchesNumberPrefix_handlesLeadingZeroTrunkPrefix() {
        assertTrue(DialerCallLogic.matchesNumberPrefix("09876543210", "9876543210"))
    }

    @Test
    fun matchesNumberPrefix_matchesTrunkZeroAgainstPlusCountryCode() {
        // Contact stored as +91…; user dials local trunk form 0…
        assertTrue(DialerCallLogic.matchesNumberPrefix("+919876543210", "098765"))
        assertTrue(DialerCallLogic.matchesNumberPrefix("+919876543210", "09876543210"))
    }

    @Test
    fun matchesNumberPrefix_matchesCountryCodeWithoutPlus() {
        assertTrue(DialerCallLogic.matchesNumberPrefix("+919876543210", "91"))
        assertTrue(DialerCallLogic.matchesNumberPrefix("+919876543210", "9198765"))
        assertTrue(DialerCallLogic.matchesNumberPrefix("+919876543210", "919876543210"))
    }

    @Test
    fun matchesNumberPrefix_matchesIddDoubleZeroPrefix() {
        assertTrue(DialerCallLogic.matchesNumberPrefix("+919876543210", "009198765"))
    }

    @Test
    fun matchesNumberPrefix_matchesNationalDigitsAgainstPlusNumber() {
        assertTrue(DialerCallLogic.matchesNumberPrefix("+919876543210", "98765"))
        assertTrue(DialerCallLogic.matchesNumberPrefix("+15551234567", "555123"))
    }

    @Test
    fun numbersEquivalent_treatsPlusTrunkAndCountryVariantsAsSame() {
        assertTrue(DialerCallLogic.numbersEquivalent("+919876543210", "919876543210"))
        assertTrue(DialerCallLogic.numbersEquivalent("+919876543210", "09876543210"))
        assertTrue(DialerCallLogic.numbersEquivalent("919876543210", "09876543210"))
        assertTrue(DialerCallLogic.numbersEquivalent("+15551234567", "5551234567"))
        assertTrue(!DialerCallLogic.numbersEquivalent("+919876543210", "91"))
        assertTrue(!DialerCallLogic.numbersEquivalent("+919876543210", "+919876543211"))
    }

    @Test
    fun contactSuggestions_matchesTrunkZeroDialing() {
        val contacts = listOf(
            ContactSuggestion("Alice", "+91 98765 43210", "+919876543210"),
            ContactSuggestion("Bob", "444-9999", "4449999"),
        )
        val matches = DialerCallLogic.contactSuggestions("098765", contacts)
        assertEquals(1, matches.size)
        assertEquals("Alice", matches.first().displayName)
    }

    @Test
    fun formatDuration_formatsMinutesAndSeconds() {
        assertEquals("1:05", DialerCallLogic.formatDuration(65))
    }

    @Test
    fun matchesT9_findsNameByDigits() {
        assertTrue(DialerCallLogic.matchesT9("Alice", "25423"))
        assertTrue(DialerCallLogic.matchesT9("Alice", "25"))
        assertTrue(!DialerCallLogic.matchesT9("Alice", "23"))
    }

    @Test
    fun matchesT9_zakkuIs92558() {
        // Z=9 A=2 K=5 K=5 U=8
        assertTrue(DialerCallLogic.matchesT9("zakku", "92558"))
        assertTrue(DialerCallLogic.matchesT9("Zakku", "92558"))
        assertTrue(DialerCallLogic.matchesT9("Zakku Kumar", "92558"))
        assertEquals(
            listOf("Zakku"),
            DialerCallLogic.contactSuggestions(
                "92558",
                listOf(
                    ContactSuggestion("Zakku", "5550001", "5550001", contactId = 1L),
                    ContactSuggestion("Bob", "4449999", "4449999", contactId = 2L),
                ),
            ).map { it.displayName },
        )
    }

    @Test
    fun matchesT9_matchesEachWordPrefix() {
        assertTrue(DialerCallLogic.matchesT9("Chris Sells", "2"))
        assertTrue(DialerCallLogic.matchesT9("Chris Sells", "73557"))
        assertTrue(!DialerCallLogic.matchesT9("Chris Sells", "3"))
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
