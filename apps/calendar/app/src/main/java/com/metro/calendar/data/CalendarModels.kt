package com.metro.calendar.data

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val calendarColorHex: String,
    val calendarName: String?,
    val location: String?,
)

data class DayBucket(
    val epochDay: Long,
    val headerLabel: String,
    val events: List<CalendarEvent>,
)

data class MonthGridCell(
    val dayOfMonth: Int,
    val epochDay: Long,
    val inCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val eventColors: List<String>,
)

data class HourSlot(
    val hour: Int,
    val label: String,
    val events: List<CalendarEvent>,
)

enum class CalendarViewType {
    Day,
    Week,
    Month,
    Year,
    ;

    companion object {
        fun fromIndex(index: Int): CalendarViewType = entries.getOrElse(index) { Day }
    }
}
