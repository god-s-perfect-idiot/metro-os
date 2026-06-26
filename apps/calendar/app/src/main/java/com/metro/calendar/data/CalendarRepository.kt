package com.metro.calendar.data

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class CalendarRepository(context: Context) {
    private val appContext = context.applicationContext
    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun loadEvents(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.EVENT_LOCATION,
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)
        val uri = builder.build()

        val events = mutableListOf<CalendarEvent>()
        appContext.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
            val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            val colorIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_COLOR)
            val calNameIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            val locationIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)

            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getLong(idIdx) else cursor.position.toLong()
                val title = if (titleIdx >= 0) cursor.getString(titleIdx).orEmpty() else "(No title)"
                val begin = if (beginIdx >= 0) cursor.getLong(beginIdx) else 0L
                val end = if (endIdx >= 0) cursor.getLong(endIdx) else begin
                val allDay = allDayIdx >= 0 && cursor.getInt(allDayIdx) == 1
                val colorInt = if (colorIdx >= 0 && !cursor.isNull(colorIdx)) cursor.getInt(colorIdx) else 0
                val calName = if (calNameIdx >= 0) cursor.getString(calNameIdx) else null
                val location = if (locationIdx >= 0) cursor.getString(locationIdx) else null

                events += CalendarEvent(
                    id = id,
                    title = title.ifBlank { "(No title)" },
                    startMillis = begin,
                    endMillis = end,
                    allDay = allDay,
                    calendarColorHex = colorIntToHex(colorInt),
                    calendarName = calName,
                    location = location,
                )
            }
        }
        return events.distinctBy { "${it.id}-${it.startMillis}" }
    }

    fun loadEventsAround(epochDay: Long, dayRadius: Int = 90): List<CalendarEvent> {
        val start = CalendarLogic.millisFromEpochDay(epochDay - dayRadius, zoneId)
        val end = CalendarLogic.millisFromEpochDay(epochDay + dayRadius + 1, zoneId)
        return loadEvents(start, end)
    }

    fun loadDemoEvents(): List<CalendarEvent> = StubCalendarDataSource.demoEvents(zoneId)

    private fun colorIntToHex(color: Int): String {
        if (color == 0) return "#1BA1E2"
        return String.format("#%06X", color and 0xFFFFFF)
    }
}
