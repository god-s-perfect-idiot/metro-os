package com.metro.calendar.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.concurrent.TimeUnit

object CalendarLogic {
    private val locale: Locale = Locale.US

    fun epochDayFromMillis(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.ofInstant(Instant.ofEpochMilli(millis), zoneId).toEpochDay()

    fun millisFromEpochDay(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun todayEpochDay(zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zoneId).toEpochDay()

    fun dateHeaderLabel(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", locale)).uppercase(locale)
    }

    fun dayNameLower(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.ofEpochDay(epochDay)
            .dayOfWeek
            .getDisplayName(TextStyle.FULL, locale)
            .lowercase(locale)

    fun dayNameShort(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.ofEpochDay(epochDay)
            .dayOfWeek
            .getDisplayName(TextStyle.SHORT, locale)
            .lowercase(locale)

    fun monthYearLabel(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)).uppercase(locale)
    }

    fun monthNameLower(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.ofEpochDay(epochDay)
            .month
            .getDisplayName(TextStyle.FULL, locale)
            .lowercase(locale)

    fun yearLabel(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.ofEpochDay(epochDay).year.toString()

    fun todayButtonLabel(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val date = LocalDate.ofEpochDay(epochDay)
        val month = date.month.getDisplayName(TextStyle.SHORT, locale)
        return "${date.dayOfMonth} $month"
    }

    fun formatEventTime(event: CalendarEvent, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (event.allDay) return "All day"
        val time = Instant.ofEpochMilli(event.startMillis)
            .atZone(zoneId)
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern("HH:mm", locale))
        return time
    }

    fun formatEventDuration(event: CalendarEvent, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (event.allDay) {
            // Android stores all-day boundaries in UTC regardless of local zone.
            val utc = ZoneId.of("UTC")
            val startDay = epochDayFromMillis(event.startMillis, utc)
            val endDay = epochDayFromMillis(event.endMillis, utc)
            val days = (endDay - startDay).coerceAtLeast(1)
            return if (days == 1L) "1 day" else "$days days"
        }
        val minutes = ((event.endMillis - event.startMillis) / 60_000L).coerceAtLeast(0)
        return when {
            minutes < 60 -> "$minutes minutes"
            minutes % 60 == 0L -> "${minutes / 60} hour${if (minutes / 60 == 1L) "" else "s"}"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }

    fun eventsForDay(events: List<CalendarEvent>, epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): List<CalendarEvent> {
        val dayStart = millisFromEpochDay(epochDay, zoneId)
        val dayEnd = millisFromEpochDay(epochDay + 1, zoneId)
        return events.filter { event ->
            event.endMillis > dayStart && event.startMillis < dayEnd
        }.sortedBy { it.startMillis }
    }

    fun allDayEventsForDay(events: List<CalendarEvent>, epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): List<CalendarEvent> =
        eventsForDay(events, epochDay, zoneId).filter { it.allDay }

    fun timedEventsForDay(events: List<CalendarEvent>, epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): List<CalendarEvent> =
        eventsForDay(events, epochDay, zoneId).filterNot { it.allDay }

    fun groupIntoAgendaBuckets(
        events: List<CalendarEvent>,
        startEpochDay: Long,
        dayCount: Int = 60,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<DayBucket> {
        val endEpochDay = startEpochDay + dayCount
        val grouped = events
            .filter { event ->
                val eventDay = epochDayFromMillis(event.startMillis, zoneId)
                eventDay in startEpochDay until endEpochDay
            }
            .groupBy { event -> epochDayFromMillis(event.startMillis, zoneId) }
            .toSortedMap()

        return grouped.map { (day, dayEvents) ->
            DayBucket(
                epochDay = day,
                headerLabel = dateHeaderLabel(day, zoneId),
                events = dayEvents.sortedBy { it.startMillis },
            )
        }
    }

    fun buildHourSlots(
        events: List<CalendarEvent>,
        epochDay: Long,
        startHour: Int = 8,
        endHour: Int = 20,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<HourSlot> {
        val timed = timedEventsForDay(events, epochDay, zoneId)
        return (startHour..endHour).map { hour ->
            val hourStart = millisFromEpochDay(epochDay, zoneId) + TimeUnit.HOURS.toMillis(hour.toLong())
            val hourEnd = hourStart + TimeUnit.HOURS.toMillis(1)
            val label = formatHourLabel(hour)
            val inHour = timed.filter { event ->
                event.startMillis < hourEnd && event.endMillis > hourStart
            }
            HourSlot(hour = hour, label = label, events = inHour)
        }
    }

    fun buildMonthGrid(
        year: Int,
        month: Int,
        events: List<CalendarEvent>,
        selectedEpochDay: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<MonthGridCell> {
        val firstOfMonth = LocalDate.of(year, month, 1)
        val today = todayEpochDay(zoneId)
        val startOffset = (firstOfMonth.dayOfWeek.value - 1).coerceAtLeast(0)
        val gridStart = firstOfMonth.minusDays(startOffset.toLong())

        return (0 until 42).map { index ->
            val date = gridStart.plusDays(index.toLong())
            val epochDay = date.toEpochDay()
            val dayEvents = eventsForDay(events, epochDay, zoneId)
            MonthGridCell(
                dayOfMonth = date.dayOfMonth,
                epochDay = epochDay,
                inCurrentMonth = date.monthValue == month,
                isToday = epochDay == today,
                isSelected = epochDay == selectedEpochDay,
                eventColors = dayEvents.take(3).map { it.calendarColorHex },
            )
        }
    }

    fun eventsTodayCount(events: List<CalendarEvent>, zoneId: ZoneId = ZoneId.systemDefault()): Int =
        eventsForDay(events, todayEpochDay(zoneId), zoneId).size

    fun nextUpcomingEvent(
        events: List<CalendarEvent>,
        nowMillis: Long = System.currentTimeMillis(),
    ): CalendarEvent? =
        events
            .filter { it.endMillis > nowMillis }
            .minByOrNull { it.startMillis }

    fun nextEventTimeLabel(event: CalendarEvent, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (event.allDay) return "All day"
        return formatEventTime(event, zoneId)
    }

    /** Short capitalised weekday for a live-tile date badge, e.g. `Thu`. */
    fun tileDayLabel(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.ofEpochDay(epochDay)
            .dayOfWeek
            .getDisplayName(TextStyle.SHORT, locale)

    /** Day-of-month for a live-tile date badge, e.g. `15`. */
    fun tileDayNumber(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.ofEpochDay(epochDay).dayOfMonth.toString()

    /** 12-hour time range for a live-tile event line, e.g. `3:00 PM - 4:00 PM` or `All day`. */
    fun tileTimeRange(event: CalendarEvent, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (event.allDay) return "All day"
        val formatter = DateTimeFormatter.ofPattern("h:mm a", locale)
        val start = Instant.ofEpochMilli(event.startMillis).atZone(zoneId).toLocalTime().format(formatter)
        val end = Instant.ofEpochMilli(event.endMillis).atZone(zoneId).toLocalTime().format(formatter)
        return "$start - $end"
    }

    /**
     * Event content lines for the WP8.1 Calendar live tile, in priority order:
     * `[title, location?, time]`. When the event is not today the time line is prefixed with its
     * weekday (e.g. `Mon: All day`), matching the device tile.
     */
    fun tileEventLines(
        event: CalendarEvent,
        todayEpochDay: Long = todayEpochDay(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<String> {
        val eventDay = epochDayFromMillis(event.startMillis, zoneId)
        val timeText = tileTimeRange(event, zoneId)
        val timeLine = if (eventDay != todayEpochDay) {
            "${tileDayLabel(eventDay, zoneId)}: $timeText"
        } else {
            timeText
        }
        return buildList {
            add(event.title)
            event.location?.takeIf { it.isNotBlank() }?.let { add(it) }
            add(timeLine)
        }
    }

    private fun formatHourLabel(hour: Int): String {
        val normalized = hour % 24
        return when {
            normalized == 0 -> "12 AM"
            normalized < 12 -> "$normalized AM"
            normalized == 12 -> "12 PM"
            else -> "${normalized - 12} PM"
        }
    }

    fun weekStartEpochDay(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
    }

    fun tabCountForViewType(viewType: CalendarViewType): Int = when (viewType) {
        CalendarViewType.Year -> 5
        else -> 12
    }

    fun buildTabTitles(
        viewType: CalendarViewType,
        tabCount: Int = tabCountForViewType(viewType),
        zoneId: ZoneId = ZoneId.systemDefault(),
        dayPivotStartEpochDay: Long? = null,
    ): List<String> {
        val today = LocalDate.now(zoneId)
        val dayStart = dayPivotStartEpochDay?.let(LocalDate::ofEpochDay) ?: today
        return (0 until tabCount).map { index ->
            tabTitle(viewType, index, today, dayStart, zoneId)
        }
    }

    fun epochDayForTab(
        viewType: CalendarViewType,
        tabIndex: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
        dayPivotStartEpochDay: Long? = null,
    ): Long {
        val today = LocalDate.now(zoneId)
        val dayStart = dayPivotStartEpochDay?.let(LocalDate::ofEpochDay) ?: today
        return when (viewType) {
            CalendarViewType.Day -> dayStart.plusDays(tabIndex.toLong()).toEpochDay()
            CalendarViewType.Week -> {
                val thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                thisWeekStart.plusWeeks(tabIndex.toLong()).toEpochDay()
            }
            CalendarViewType.Month -> {
                today.withDayOfMonth(1).plusMonths(tabIndex.toLong()).toEpochDay()
            }
            CalendarViewType.Year -> {
                LocalDate.of(today.year + tabIndex, 1, 1).toEpochDay()
            }
        }
    }

    fun tabIndexForEpochDay(
        viewType: CalendarViewType,
        epochDay: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        dayPivotStartEpochDay: Long? = null,
    ): Int {
        val today = LocalDate.now(zoneId)
        val date = LocalDate.ofEpochDay(epochDay)
        val dayStart = dayPivotStartEpochDay?.let(LocalDate::ofEpochDay) ?: today
        return when (viewType) {
            CalendarViewType.Day -> {
                ChronoUnit.DAYS.between(dayStart, date).toInt().coerceAtLeast(0)
            }
            CalendarViewType.Week -> {
                val thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val targetWeekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ChronoUnit.WEEKS.between(thisWeekStart, targetWeekStart).toInt().coerceAtLeast(0)
            }
            CalendarViewType.Month -> {
                val months = (date.year - today.year) * 12 + (date.monthValue - today.monthValue)
                months.coerceAtLeast(0)
            }
            CalendarViewType.Year -> {
                (date.year - today.year).coerceAtLeast(0)
            }
        }
    }

    fun weekDayEpochDays(weekStartEpochDay: Long): List<Long> =
        (0 until 7).map { weekStartEpochDay + it }

    fun monthsInYear(year: Int): List<Int> = (1..12).toList()

    private fun tabTitle(
        viewType: CalendarViewType,
        tabIndex: Int,
        today: LocalDate,
        dayStart: LocalDate,
        zoneId: ZoneId,
    ): String = when (viewType) {
        CalendarViewType.Day -> {
            val date = dayStart.plusDays(tabIndex.toLong())
            when (date) {
                today -> "today"
                today.plusDays(1) -> "tomorrow"
                else -> "${dayNameLower(date.toEpochDay(), zoneId)} ${date.dayOfMonth}"
            }
        }
        CalendarViewType.Week -> when (tabIndex) {
            0 -> "this week"
            1 -> "next week"
            else -> {
                val weekStart = today
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .plusWeeks(tabIndex.toLong())
                "${monthNameLower(weekStart.toEpochDay(), zoneId)} ${weekStart.dayOfMonth}"
            }
        }
        CalendarViewType.Month -> {
            val monthDate = today.withDayOfMonth(1).plusMonths(tabIndex.toLong())
            monthNameLower(monthDate.toEpochDay(), zoneId)
        }
        CalendarViewType.Year -> (today.year + tabIndex).toString()
    }
}
