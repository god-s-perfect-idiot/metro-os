package com.metro.calendar.data

import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object StubCalendarDataSource {
    private const val DEFAULT_ACCENT = "#A200FF"

    fun demoEvents(zoneId: ZoneId = ZoneId.systemDefault()): List<CalendarEvent> {
        val today = LocalDate.now(zoneId)
        val tomorrow = today.plusDays(1)
        val dayAfter = today.plusDays(2)

        return listOf(
            allDayEvent(
                id = 1L,
                title = "Team standup week",
                date = today,
                days = 1,
                color = "#1BA1E2",
                zoneId = zoneId,
            ),
            timedEvent(
                id = 2L,
                title = "Lunch at the Willow",
                date = today,
                hour = 12,
                minute = 0,
                durationMinutes = 60,
                color = "#1BA1E2",
                zoneId = zoneId,
            ),
            timedEvent(
                id = 3L,
                title = "Project review",
                date = today,
                hour = 15,
                minute = 30,
                durationMinutes = 45,
                color = "#E51400",
                zoneId = zoneId,
            ),
            allDayEvent(
                id = 4L,
                title = "Caroline's birthday",
                date = tomorrow,
                days = 1,
                color = DEFAULT_ACCENT,
                zoneId = zoneId,
            ),
            timedEvent(
                id = 5L,
                title = "Deflange frobnicator",
                date = tomorrow,
                hour = 9,
                minute = 0,
                durationMinutes = 30,
                color = DEFAULT_ACCENT,
                zoneId = zoneId,
            ),
            timedEvent(
                id = 6L,
                title = "Call brother about the weekend",
                date = dayAfter,
                hour = 10,
                minute = 0,
                durationMinutes = 15,
                color = "#F09609",
                zoneId = zoneId,
            ),
        )
    }

    private fun allDayEvent(
        id: Long,
        title: String,
        date: LocalDate,
        days: Int,
        color: String,
        zoneId: ZoneId,
    ): CalendarEvent {
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(days.toLong()).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return CalendarEvent(
            id = id,
            title = title,
            startMillis = start,
            endMillis = end,
            allDay = true,
            calendarColorHex = color,
            calendarName = "Demo",
            location = null,
        )
    }

    private fun timedEvent(
        id: Long,
        title: String,
        date: LocalDate,
        hour: Int,
        minute: Int,
        durationMinutes: Int,
        color: String,
        zoneId: ZoneId,
    ): CalendarEvent {
        val start = date.atTime(hour, minute).atZone(zoneId).toInstant().toEpochMilli()
        val end = start + TimeUnit.MINUTES.toMillis(durationMinutes.toLong())
        return CalendarEvent(
            id = id,
            title = title,
            startMillis = start,
            endMillis = end,
            allDay = false,
            calendarColorHex = color,
            calendarName = "Demo",
            location = null,
        )
    }
}
