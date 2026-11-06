package com.metro.statusbar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TrayClockFormatterTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun format_12Hour_omitsLeadingZero() {
        val now = ZonedDateTime.of(2026, 11, 6, 9, 5, 0, 0, zone)
        assertEquals("9:05", TrayClockFormatter.format(now, use24Hour = false))
    }

    @Test
    fun format_24Hour_usesHourOfDay() {
        val morning = ZonedDateTime.of(2026, 11, 6, 9, 5, 0, 0, zone)
        assertEquals("9:05", TrayClockFormatter.format(morning, use24Hour = true))
        val evening = ZonedDateTime.of(2026, 11, 6, 21, 41, 0, 0, zone)
        assertEquals("21:41", TrayClockFormatter.format(evening, use24Hour = true))
    }

    @Test
    fun format_12Hour_afternoonIsUnpadded() {
        val now = ZonedDateTime.of(2026, 11, 6, 15, 30, 0, 0, zone)
        assertEquals("3:30", TrayClockFormatter.format(now, use24Hour = false))
    }
}
