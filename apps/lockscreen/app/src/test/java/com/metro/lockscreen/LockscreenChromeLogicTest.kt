package com.metro.lockscreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import java.util.concurrent.TimeUnit

class LockscreenChromeLogicTest {
    private val zone = ZoneId.of("UTC")
    private val locale = Locale.US

    @Test
    fun formatTime_12h_padsLeadingZero() {
        val now = ZonedDateTime.of(2026, 4, 13, 4, 20, 0, 0, zone)
        assertEquals("04:20", LockscreenChromeLogic.formatTime(now, use24Hour = false, locale))
    }

    @Test
    fun formatTime_24h_padsHour() {
        val now = ZonedDateTime.of(2026, 5, 15, 7, 28, 0, 0, zone)
        assertEquals("07:28", LockscreenChromeLogic.formatTime(now, use24Hour = true, locale))
    }

    @Test
    fun formatDay_fullWeekday() {
        val now = ZonedDateTime.of(2026, 4, 13, 4, 20, 0, 0, zone)
        assertEquals("Monday", LockscreenChromeLogic.formatDay(now, locale))
    }

    @Test
    fun formatDate_usesProvidedPattern() {
        val now = ZonedDateTime.of(2026, 4, 13, 4, 20, 0, 0, zone)
        assertEquals(
            "April 13",
            LockscreenChromeLogic.formatDate(now, locale, datePattern = "MMMM d"),
        )
        assertEquals(
            "13 April",
            LockscreenChromeLogic.formatDate(now, Locale.UK, datePattern = "d MMMM"),
        )
    }

    @Test
    fun formatEventTimeRange_allDay() {
        val occurrence = LockscreenChromeLogic.CalendarOccurrence(
            title = "Birthday",
            location = null,
            startMillis = 0L,
            endMillis = TimeUnit.DAYS.toMillis(1),
            allDay = true,
        )
        assertEquals(
            "All day",
            LockscreenChromeLogic.formatEventTimeRange(occurrence, use24Hour = true, zone),
        )
    }

    @Test
    fun formatEventTimeRange_timed() {
        val start = ZonedDateTime.of(2026, 1, 23, 16, 0, 0, 0, zone).toInstant().toEpochMilli()
        val end = ZonedDateTime.of(2026, 1, 23, 17, 0, 0, 0, zone).toInstant().toEpochMilli()
        val occurrence = LockscreenChromeLogic.CalendarOccurrence(
            title = "Catch-up",
            location = "The Stag",
            startMillis = start,
            endMillis = end,
            allDay = false,
        )
        assertEquals(
            "16:00 - 17:00",
            LockscreenChromeLogic.formatEventTimeRange(occurrence, use24Hour = true, zone, locale),
        )
    }

    @Test
    fun selectNextOccurrence_skipsEndedAndPicksSoonest() {
        val today = LocalDate.of(2026, 1, 23)
        val now = ZonedDateTime.of(2026, 1, 23, 14, 14, 0, 0, zone).toInstant().toEpochMilli()
        val past = occurrence(
            title = "Past",
            startHour = 9,
            endHour = 10,
            day = today,
        )
        val next = occurrence(
            title = "Catch-up with Dan",
            startHour = 16,
            endHour = 17,
            day = today,
            location = "The Stag",
        )
        val later = occurrence(
            title = "Dinner",
            startHour = 19,
            endHour = 20,
            day = today,
        )
        val selected = LockscreenChromeLogic.selectNextOccurrence(
            occurrences = listOf(past, later, next),
            nowMillis = now,
            today = today,
            zoneId = zone,
        )
        assertEquals("Catch-up with Dan", selected?.title)
        assertEquals("The Stag", selected?.location)
    }

    @Test
    fun selectNextOccurrence_keepsInProgress() {
        val today = LocalDate.of(2026, 1, 23)
        val now = ZonedDateTime.of(2026, 1, 23, 16, 30, 0, 0, zone).toInstant().toEpochMilli()
        val inProgress = occurrence(
            title = "Catch-up with Dan",
            startHour = 16,
            endHour = 17,
            day = today,
        )
        val later = occurrence(
            title = "Dinner",
            startHour = 19,
            endHour = 20,
            day = today,
        )
        val selected = LockscreenChromeLogic.selectNextOccurrence(
            occurrences = listOf(later, inProgress),
            nowMillis = now,
            today = today,
            zoneId = zone,
        )
        assertEquals("Catch-up with Dan", selected?.title)
    }

    @Test
    fun selectNextOccurrence_ignoresTomorrow() {
        val today = LocalDate.of(2026, 1, 23)
        val tomorrow = today.plusDays(1)
        val now = ZonedDateTime.of(2026, 1, 23, 20, 0, 0, 0, zone).toInstant().toEpochMilli()
        val tomorrowEvent = occurrence(
            title = "Tomorrow standup",
            startHour = 9,
            endHour = 10,
            day = tomorrow,
        )
        assertNull(
            LockscreenChromeLogic.selectNextOccurrence(
                occurrences = listOf(tomorrowEvent),
                nowMillis = now,
                today = today,
                zoneId = zone,
            ),
        )
    }

    @Test
    fun selectNextOccurrence_returnsNullWhenEmpty() {
        assertNull(
            LockscreenChromeLogic.selectNextOccurrence(
                occurrences = emptyList(),
                nowMillis = System.currentTimeMillis(),
                today = LocalDate.now(zone),
                zoneId = zone,
            ),
        )
    }

    @Test
    fun millisUntilNextMinute_waitsForBoundary() {
        assertEquals(60_000L, LockscreenChromeLogic.millisUntilNextMinute(0L))
        assertEquals(1L, LockscreenChromeLogic.millisUntilNextMinute(59_999L))
        assertTrue(LockscreenChromeLogic.millisUntilNextMinute(1_000L) in 1L..60_000L)
    }

    private fun occurrence(
        title: String,
        startHour: Int,
        endHour: Int,
        day: LocalDate,
        location: String? = null,
        allDay: Boolean = false,
    ): LockscreenChromeLogic.CalendarOccurrence {
        val start = day.atTime(startHour, 0).atZone(zone).toInstant().toEpochMilli()
        val end = day.atTime(endHour, 0).atZone(zone).toInstant().toEpochMilli()
        return LockscreenChromeLogic.CalendarOccurrence(
            title = title,
            location = location,
            startMillis = start,
            endMillis = end,
            allDay = allDay,
        )
    }
}
