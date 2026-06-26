package com.metro.calendar.data

import com.metro.calendar.data.CalendarViewType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class CalendarLogicTest {
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun formatEventTime_allDay() {
        val event = sampleEvent(allDay = true)
        assertEquals("All day", CalendarLogic.formatEventTime(event, zoneId))
    }

    @Test
    fun formatEventTime_timed() {
        val start = LocalDate.of(2026, 6, 26).atTime(14, 30).atZone(zoneId).toInstant().toEpochMilli()
        val event = sampleEvent(allDay = false, startMillis = start, endMillis = start + TimeUnit.HOURS.toMillis(1))
        assertEquals("14:30", CalendarLogic.formatEventTime(event, zoneId))
    }

    @Test
    fun groupIntoAgendaBuckets_groupsByDay() {
        val day = LocalDate.of(2026, 6, 26).toEpochDay()
        val start = LocalDate.ofEpochDay(day).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val events = listOf(
            sampleEvent(id = 1, startMillis = start, endMillis = start + 3_600_000),
            sampleEvent(id = 2, startMillis = start + 7_200_000, endMillis = start + 10_800_000),
        )
        val buckets = CalendarLogic.groupIntoAgendaBuckets(events, day, dayCount = 1, zoneId = zoneId)
        assertEquals(1, buckets.size)
        assertEquals(2, buckets.first().events.size)
    }

    @Test
    fun buildMonthGrid_has42Cells() {
        val day = LocalDate.of(2026, 6, 15).toEpochDay()
        val grid = CalendarLogic.buildMonthGrid(2026, 6, emptyList(), day, zoneId)
        assertEquals(42, grid.size)
    }

    @Test
    fun buildMonthGrid_marksSelectedDay() {
        val day = LocalDate.of(2026, 6, 15).toEpochDay()
        val grid = CalendarLogic.buildMonthGrid(2026, 6, emptyList(), day, zoneId)
        assertTrue(grid.any { it.isSelected && it.dayOfMonth == 15 })
    }

    @Test
    fun eventsForDay_filtersCorrectly() {
        val day = LocalDate.of(2026, 6, 26).toEpochDay()
        val start = LocalDate.ofEpochDay(day).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nextDay = LocalDate.ofEpochDay(day + 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val events = listOf(
            sampleEvent(id = 1, startMillis = start + 3_600_000, endMillis = start + 7_200_000),
            sampleEvent(id = 2, startMillis = nextDay, endMillis = nextDay + 3_600_000),
        )
        val filtered = CalendarLogic.eventsForDay(events, day, zoneId)
        assertEquals(1, filtered.size)
        assertEquals(1L, filtered.first().id)
    }

    @Test
    fun nextUpcomingEvent_returnsEarliestFuture() {
        val now = LocalDate.of(2026, 6, 26).atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()
        val past = now - TimeUnit.HOURS.toMillis(2)
        val future1 = now + TimeUnit.HOURS.toMillis(1)
        val future2 = now + TimeUnit.HOURS.toMillis(3)
        val events = listOf(
            sampleEvent(id = 1, startMillis = past, endMillis = past + 3_600_000),
            sampleEvent(id = 2, startMillis = future2, endMillis = future2 + 3_600_000),
            sampleEvent(id = 3, startMillis = future1, endMillis = future1 + 3_600_000),
        )
        val next = CalendarLogic.nextUpcomingEvent(events, now)
        assertEquals(3L, next?.id)
    }

    @Test
    fun formatEventDuration_allDaySingle() {
        val day = LocalDate.of(2026, 6, 26).toEpochDay()
        val start = LocalDate.ofEpochDay(day).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = LocalDate.ofEpochDay(day + 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val event = sampleEvent(allDay = true, startMillis = start, endMillis = end)
        assertEquals("1 day", CalendarLogic.formatEventDuration(event))
    }

    @Test
    fun buildHourSlots_coversRequestedRange() {
        val day = LocalDate.of(2026, 6, 26).toEpochDay()
        val slots = CalendarLogic.buildHourSlots(emptyList(), day, startHour = 8, endHour = 12, zoneId = zoneId)
        assertEquals(5, slots.size)
        assertEquals("8 AM", slots.first().label)
        assertFalse(slots.any { it.events.isNotEmpty() })
    }

    @Test
    fun buildTabTitles_dayStartsWithToday() {
        val titles = CalendarLogic.buildTabTitles(CalendarViewType.Day, tabCount = 3, zoneId = zoneId)
        assertEquals("today", titles[0])
        assertEquals("tomorrow", titles[1])
    }

    @Test
    fun epochDayForTab_dayIncrements() {
        val day0 = CalendarLogic.epochDayForTab(CalendarViewType.Day, 0, zoneId)
        val day1 = CalendarLogic.epochDayForTab(CalendarViewType.Day, 1, zoneId)
        assertEquals(day0 + 1, day1)
    }

    @Test
    fun buildTabTitles_monthStartsWithCurrentMonth() {
        val today = LocalDate.now(zoneId)
        val titles = CalendarLogic.buildTabTitles(CalendarViewType.Month, tabCount = 2, zoneId = zoneId)
        assertEquals(CalendarLogic.monthNameLower(today.toEpochDay(), zoneId), titles[0])
        assertEquals(
            CalendarLogic.monthNameLower(today.plusMonths(1).toEpochDay(), zoneId),
            titles[1],
        )
    }

    @Test
    fun weekStartEpochDay_isMonday() {
        val friday = LocalDate.of(2026, 6, 26).toEpochDay()
        val weekStart = CalendarLogic.weekStartEpochDay(friday, zoneId)
        assertEquals(LocalDate.of(2026, 6, 22).toEpochDay(), weekStart)
    }

    private fun sampleEvent(
        id: Long = 1L,
        allDay: Boolean = false,
        startMillis: Long = 0L,
        endMillis: Long = startMillis + 3_600_000,
    ) = CalendarEvent(
        id = id,
        title = "Test event",
        startMillis = startMillis,
        endMillis = endMillis,
        allDay = allDay,
        calendarColorHex = "#1BA1E2",
        calendarName = "Test",
        location = null,
    )
}
