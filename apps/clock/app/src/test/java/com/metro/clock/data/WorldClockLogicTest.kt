package com.metro.clock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class WorldClockLogicTest {
    @Test
    fun catalogIsSortedAndUnique() {
        val ids = WorldClockLogic.Catalog.map { it.id }
        assertEquals(ids.distinct().size, ids.size)
        val names = WorldClockLogic.Catalog.map { it.name.lowercase() }
        assertEquals(names.sorted(), names)
    }

    @Test
    fun formatOffsetLabel_sameZone() {
        val zone = TimeZone.getTimeZone("America/New_York")
        assertEquals(
            "same as local",
            WorldClockLogic.formatOffsetLabel("America/New_York", localZone = zone),
        )
    }

    @Test
    fun formatOffsetLabel_positiveAndNegative() {
        val utc = TimeZone.getTimeZone("UTC")
        val plus = WorldClockLogic.formatOffsetLabel("Asia/Tokyo", localZone = utc)
        assertTrue(plus.startsWith("+"))
        val minus = WorldClockLogic.formatOffsetLabel("America/Los_Angeles", localZone = utc)
        assertTrue(minus.startsWith("-") || minus == "same as local")
    }

    @Test
    fun cityById() {
        assertEquals("Tokyo", WorldClockLogic.cityById("tokyo")?.name)
        assertEquals(null, WorldClockLogic.cityById("nowhere"))
    }
}
