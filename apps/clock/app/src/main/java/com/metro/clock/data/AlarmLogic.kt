package com.metro.clock.data

import java.util.Calendar
import java.util.Locale

object AlarmLogic {
    val Weekdays: List<Pair<Int, String>> = listOf(
        Calendar.SUNDAY to "Sunday",
        Calendar.MONDAY to "Monday",
        Calendar.TUESDAY to "Tuesday",
        Calendar.WEDNESDAY to "Wednesday",
        Calendar.THURSDAY to "Thursday",
        Calendar.FRIDAY to "Friday",
        Calendar.SATURDAY to "Saturday",
    )

    private val ShortDay: Map<Int, String> = mapOf(
        Calendar.SUNDAY to "Sun",
        Calendar.MONDAY to "Mon",
        Calendar.TUESDAY to "Tue",
        Calendar.WEDNESDAY to "Wed",
        Calendar.THURSDAY to "Thu",
        Calendar.FRIDAY to "Fri",
        Calendar.SATURDAY to "Sat",
    )

    fun formatTime(hour24: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
        }
        val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minuteStr = "%02d".format(Locale.US, cal.get(Calendar.MINUTE))
        val meridiem = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        return "$hour12:$minuteStr $meridiem"
    }

    fun formatRepeatSummary(days: Set<Int>): String {
        if (days.isEmpty()) return "only once"
        if (days.size == 7) return "every day"
        val weekdays = setOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY,
        )
        val weekend = setOf(Calendar.SATURDAY, Calendar.SUNDAY)
        if (days == weekdays) return "weekdays"
        if (days == weekend) return "weekends"
        return Weekdays
            .filter { it.first in days }
            .joinToString(", ") { ShortDay.getValue(it.first) }
    }

    fun statusLabel(enabled: Boolean, name: String): String {
        val label = name.ifBlank { "Alarm" }
        return if (enabled) "On: $label" else "Off: $label"
    }

    fun toHour24(hour12: Int, isAm: Boolean): Int {
        val h = hour12.coerceIn(1, 12)
        return when {
            isAm && h == 12 -> 0
            !isAm && h == 12 -> 12
            !isAm -> h + 12
            else -> h
        }
    }

    fun fromHour24(hour24: Int): Pair<Int, Boolean> {
        val h = hour24.coerceIn(0, 23)
        return when (h) {
            0 -> 12 to true
            in 1..11 -> h to true
            12 -> 12 to false
            else -> (h - 12) to false
        }
    }

    /** Next trigger instant for an enabled alarm, or null if disabled. */
    fun nextTriggerMillis(alarm: AlarmItem, nowMillis: Long = System.currentTimeMillis()): Long? {
        if (!alarm.enabled) return null
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val candidate = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
        }
        if (alarm.repeatDays.isEmpty()) {
            if (candidate.timeInMillis <= nowMillis) {
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }
            return candidate.timeInMillis
        }
        // Find the soonest matching weekday (today if still upcoming, else next).
        var best: Long? = null
        for (offset in 0..7) {
            val day = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
            }
            if (day.get(Calendar.DAY_OF_WEEK) !in alarm.repeatDays) continue
            if (day.timeInMillis <= nowMillis) continue
            val t = day.timeInMillis
            if (best == null || t < best) best = t
        }
        return best
    }
}
