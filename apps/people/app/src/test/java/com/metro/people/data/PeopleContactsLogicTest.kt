package com.metro.people.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleContactsLogicTest {
    @Test
    fun applyFilter_hidesContactsWithoutPhoneWhenEnabled() {
        val people = listOf(
            person(1, "Alice", hasPhone = true),
            person(2, "Bob", hasPhone = false),
        )
        val filtered = PeopleContactsLogic.applyFilter(
            people,
            PeopleFilter(hideWithoutPhone = true, visibleAccounts = setOf("Device")),
            setOf("Device"),
        )
        assertEquals(1, filtered.size)
        assertEquals("Alice", filtered.first().displayName)
    }

    @Test
    fun groupBySortKey_ordersAlphabetically() {
        val grouped = PeopleContactsLogic.groupBySortKey(
            listOf(
                person(1, "Zoe"),
                person(2, "Amy"),
                person(3, "Mike"),
            ),
        )
        assertEquals(listOf('A', 'M', 'Z'), grouped.keys.toList())
    }

    @Test
    fun filterLabel_reflectsPhoneOnlySetting() {
        assertTrue(
            PeopleContactsLogic.filterLabel(PeopleFilter(hideWithoutPhone = true))
                .contains("phone numbers"),
        )
    }

    private fun person(
        id: Long,
        name: String,
        hasPhone: Boolean = true,
    ) = PersonSummary(
        id = id,
        displayName = name,
        photoUri = null,
        hasPhone = hasPhone,
        defaultPhone = if (hasPhone) "+15551234567" else null,
        defaultEmail = null,
        sourceLabel = "Device",
        sortKey = name.first(),
    )
}
