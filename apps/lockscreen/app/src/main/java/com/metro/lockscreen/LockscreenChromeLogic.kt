package com.metro.lockscreen

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Pure formatting + event selection for the WP8.1 lock chrome
 * (large time, weekday, date, next calendar appointment).
 */
object LockscreenChromeLogic {
    data class GlanceEvent(
        val title: String,
        val location: String?,
        val timeLabel: String,
    )

    data class ChromeLabels(
        val time: String,
        val day: String,
        val date: String,
        val event: GlanceEvent?,
    )

    /**
     * Raw calendar row used only for selection/formatting — kept free of Android types
     * so unit tests can drive it without ContentResolver.
     */
    data class CalendarOccurrence(
        val title: String,
        val location: String?,
        val startMillis: Long,
        val endMillis: Long,
        val allDay: Boolean,
    )

    fun labels(
        now: ZonedDateTime,
        use24Hour: Boolean,
        event: GlanceEvent?,
        locale: Locale = Locale.getDefault(),
        datePattern: String = "d MMMM",
    ): ChromeLabels = ChromeLabels(
        time = formatTime(now, use24Hour, locale),
        day = formatDay(now, locale),
        date = formatDate(now, locale, datePattern),
        event = event,
    )

    fun formatTime(
        now: ZonedDateTime,
        use24Hour: Boolean,
        locale: Locale = Locale.getDefault(),
    ): String {
        val pattern = if (use24Hour) "HH:mm" else "hh:mm"
        return now.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    fun formatDay(
        now: ZonedDateTime,
        locale: Locale = Locale.getDefault(),
    ): String = now.dayOfWeek.getDisplayName(TextStyle.FULL, locale)

    /**
     * Month+day without year — matches WP lock chrome (e.g. `23 January`, `April 13`).
     * [datePattern] should come from `DateFormat.getBestDateTimePattern(locale, "MMMMd")`
     * on Android; defaults to day-then-month for tests.
     */
    fun formatDate(
        now: ZonedDateTime,
        locale: Locale = Locale.getDefault(),
        datePattern: String = "d MMMM",
    ): String = now.format(DateTimeFormatter.ofPattern(datePattern, locale))

    fun formatEventTimeRange(
        occurrence: CalendarOccurrence,
        use24Hour: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): String {
        if (occurrence.allDay) return "All day"
        val pattern = if (use24Hour) "HH:mm" else "hh:mm"
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        val start = Instant.ofEpochMilli(occurrence.startMillis).atZone(zoneId).format(formatter)
        val end = Instant.ofEpochMilli(occurrence.endMillis).atZone(zoneId).format(formatter)
        return "$start - $end"
    }

    fun toGlanceEvent(
        occurrence: CalendarOccurrence,
        use24Hour: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): GlanceEvent = GlanceEvent(
        title = occurrence.title.ifBlank { "(No title)" },
        location = occurrence.location?.takeIf { it.isNotBlank() },
        timeLabel = formatEventTimeRange(occurrence, use24Hour, zoneId, locale),
    )

    /**
     * Next appointment **for [today] only**: still ongoing or upcoming today.
     * Skips ended events and anything that starts after local midnight tonight.
     * Returns null when nothing remains on the current day.
     */
    fun selectNextOccurrence(
        occurrences: List<CalendarOccurrence>,
        nowMillis: Long,
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): CalendarOccurrence? {
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return occurrences
            .filter { occurrence ->
                if (occurrence.endMillis <= nowMillis) return@filter false
                // Must overlap local today — never promote tomorrow's next event.
                occurrence.startMillis < dayEnd && occurrence.endMillis > dayStart
            }
            .minWithOrNull(
                compareBy<CalendarOccurrence> { it.startMillis }
                    .thenBy { it.endMillis },
            )
    }

    /** Milliseconds until the next minute boundary (for clock ticks). */
    fun millisUntilNextMinute(nowMillis: Long): Long {
        val remainder = nowMillis % 60_000L
        return if (remainder == 0L) 60_000L else 60_000L - remainder
    }
}
