package com.metro.calendar.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.metro.system.MetroPreferences
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class CalendarRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = MetroPreferences(appContext)
    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun loadEvents(startMillis: Long, endMillis: Long): List<CalendarEvent> =
        // Instances only — a full Events-table range scan was hanging the UI on view switches.
        // Just-saved rows are merged via [loadEventById] / optimistic draft mapping instead.
        queryInstances(startMillis, endMillis)
            .distinctBy { "${it.id}-${it.startMillis}-${it.endMillis}" }

    fun loadEventsAround(epochDay: Long, dayRadius: Int = 90): List<CalendarEvent> {
        val start = CalendarLogic.millisFromEpochDay(epochDay - dayRadius, zoneId)
        val end = CalendarLogic.millisFromEpochDay(epochDay + dayRadius + 1, zoneId)
        return loadEvents(start, end)
    }

    fun loadEventById(eventId: Long): CalendarEvent? {
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.EVENT_COLOR,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.CALENDAR_COLOR,
        )
        appContext.contentResolver.query(
            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            return readEventRow(
                id = cursorLong(cursor, CalendarContract.Events._ID) ?: eventId,
                title = cursorString(cursor, CalendarContract.Events.TITLE),
                begin = cursorLong(cursor, CalendarContract.Events.DTSTART) ?: 0L,
                end = cursorLong(cursor, CalendarContract.Events.DTEND)
                    ?: (cursorLong(cursor, CalendarContract.Events.DTSTART) ?: 0L),
                allDay = cursorInt(cursor, CalendarContract.Events.ALL_DAY) == 1,
                colorInt = cursorInt(cursor, CalendarContract.Events.EVENT_COLOR).takeIf { it != 0 }
                    ?: cursorInt(cursor, CalendarContract.Events.CALENDAR_COLOR),
                calName = cursorString(cursor, CalendarContract.Events.CALENDAR_DISPLAY_NAME),
                location = cursorString(cursor, CalendarContract.Events.EVENT_LOCATION),
            )
        }
        return null
    }

    fun loadDemoEvents(): List<CalendarEvent> = StubCalendarDataSource.demoEvents(zoneId)

    /**
     * Writable calendars from the device provider. Prefer visible, synced, primary calendars
     * (often the user's Google account) so inserts appear in Instances and sync upstream.
     */
    fun loadWritableCalendars(): List<WritableCalendar> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.SYNC_EVENTS,
        )
        // Prefer visible + synced calendars — Instances only expands those. Fall back to any
        // writable calendar if the device has none marked visible/synced yet.
        val strict = queryWritableCalendars(
            projection = projection,
            requireVisibleAndSynced = true,
        )
        val calendars = strict.ifEmpty {
            queryWritableCalendars(
                projection = projection,
                requireVisibleAndSynced = false,
            )
        }
        return calendars.sortedWith(
            compareByDescending<WritableCalendar> { it.isPrimary }
                .thenByDescending { it.accountType == "com.google" }
                .thenBy { it.displayName.lowercase() },
        )
    }

    private fun queryWritableCalendars(
        projection: Array<String>,
        requireVisibleAndSynced: Boolean,
    ): List<WritableCalendar> {
        val selection = buildString {
            append("${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?")
            if (requireVisibleAndSynced) {
                append(" AND ${CalendarContract.Calendars.VISIBLE} = 1")
                append(" AND ${CalendarContract.Calendars.SYNC_EVENTS} = 1")
            }
        }
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        val calendars = mutableListOf<WritableCalendar>()
        appContext.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursorLong(cursor, CalendarContract.Calendars._ID) ?: continue
                val name = cursorString(cursor, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME).orEmpty()
                val account = cursorString(cursor, CalendarContract.Calendars.ACCOUNT_NAME)
                val type = cursorString(cursor, CalendarContract.Calendars.ACCOUNT_TYPE)
                val colorInt = cursorInt(cursor, CalendarContract.Calendars.CALENDAR_COLOR)
                val primary = cursorInt(cursor, CalendarContract.Calendars.IS_PRIMARY) == 1
                calendars += WritableCalendar(
                    id = id,
                    displayName = name.ifBlank { account ?: "Calendar" },
                    accountName = account,
                    accountType = type,
                    colorHex = colorIntToHex(colorInt),
                    isPrimary = primary,
                )
            }
        }
        return calendars
    }

    /**
     * Inserts [draft] into the device calendar provider. Events written to a Google calendar
     * account sync to Google Calendar through the normal account sync — no Google API client.
     */
    fun insertEvent(draft: EventDraft): Long? {
        val calendarId = draft.calendarId
            ?: loadWritableCalendars().firstOrNull()?.id
            ?: return null
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, draft.title.trim())
            val location = draft.location.trim()
            if (location.isEmpty()) {
                putNull(CalendarContract.Events.EVENT_LOCATION)
            } else {
                put(CalendarContract.Events.EVENT_LOCATION, location)
            }
            put(CalendarContract.Events.ALL_DAY, if (draft.allDay) 1 else 0)
            if (draft.allDay) {
                // All-day events are midnight-UTC day boundaries per CalendarContract.
                val startDay = LocalDate.ofEpochDay(draft.epochDay)
                val start = startDay.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                val end = startDay.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                put(CalendarContract.Events.DTSTART, start)
                put(CalendarContract.Events.DTEND, end)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getTimeZone("UTC").id)
            } else {
                val start = LocalDate.ofEpochDay(draft.epochDay)
                    .atTime(LocalTime.of(draft.startHour.coerceIn(0, 23), draft.startMinute.coerceIn(0, 59)))
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
                val end = start + TimeUnit.MINUTES.toMillis(draft.durationMinutes.coerceAtLeast(15).toLong())
                put(CalendarContract.Events.DTSTART, start)
                put(CalendarContract.Events.DTEND, end)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }
            put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            put(CalendarContract.Events.HAS_ALARM, 0)
            put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        }
        val uri = appContext.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?: return null
        appContext.contentResolver.notifyChange(uri, null)
        return ContentUris.parseId(uri)
    }

    /** Deletes the event row. For recurring series this removes the whole series in v1. */
    fun deleteEvent(eventId: Long): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val deleted = appContext.contentResolver.delete(uri, null, null)
        if (deleted > 0) {
            appContext.contentResolver.notifyChange(uri, null)
        }
        return deleted > 0
    }

    fun eventFromDraft(
        id: Long,
        draft: EventDraft,
        calendar: WritableCalendar?,
    ): CalendarEvent {
        val (start, end) = if (draft.allDay) {
            val startDay = LocalDate.ofEpochDay(draft.epochDay)
            val startMillis = startDay.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            val endMillis = startDay.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            startMillis to endMillis
        } else {
            val startMillis = LocalDate.ofEpochDay(draft.epochDay)
                .atTime(LocalTime.of(draft.startHour.coerceIn(0, 23), draft.startMinute.coerceIn(0, 59)))
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
            val endMillis = startMillis +
                TimeUnit.MINUTES.toMillis(draft.durationMinutes.coerceAtLeast(15).toLong())
            startMillis to endMillis
        }
        return CalendarEvent(
            id = id,
            title = draft.title.trim().ifBlank { "(No title)" },
            startMillis = start,
            endMillis = end,
            allDay = draft.allDay,
            calendarColorHex = calendar?.colorHex ?: prefs.accentColorHex,
            calendarName = calendar?.displayName,
            location = draft.location.trim().ifBlank { null },
        )
    }

    private fun queryInstances(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.CALENDAR_COLOR,
        )
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)
        val events = mutableListOf<CalendarEvent>()
        appContext.contentResolver.query(
            builder.build(),
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                events += readEventRow(
                    id = cursorLong(cursor, CalendarContract.Instances.EVENT_ID)
                        ?: cursor.position.toLong(),
                    title = cursorString(cursor, CalendarContract.Instances.TITLE),
                    begin = cursorLong(cursor, CalendarContract.Instances.BEGIN) ?: 0L,
                    end = cursorLong(cursor, CalendarContract.Instances.END)
                        ?: (cursorLong(cursor, CalendarContract.Instances.BEGIN) ?: 0L),
                    allDay = cursorInt(cursor, CalendarContract.Instances.ALL_DAY) == 1,
                    colorInt = cursorInt(cursor, CalendarContract.Instances.EVENT_COLOR).takeIf { it != 0 }
                        ?: cursorInt(cursor, CalendarContract.Instances.CALENDAR_COLOR),
                    calName = cursorString(cursor, CalendarContract.Instances.CALENDAR_DISPLAY_NAME),
                    location = cursorString(cursor, CalendarContract.Instances.EVENT_LOCATION),
                )
            }
        }
        return events
    }

    private fun readEventRow(
        id: Long,
        title: String?,
        begin: Long,
        end: Long,
        allDay: Boolean,
        colorInt: Int,
        calName: String?,
        location: String?,
    ): CalendarEvent = CalendarEvent(
        id = id,
        title = title?.ifBlank { null } ?: "(No title)",
        startMillis = begin,
        endMillis = end,
        allDay = allDay,
        calendarColorHex = colorIntToHex(colorInt),
        calendarName = calName,
        location = location,
    )

    private fun cursorLong(cursor: android.database.Cursor, column: String): Long? {
        val idx = cursor.getColumnIndex(column)
        return if (idx >= 0 && !cursor.isNull(idx)) cursor.getLong(idx) else null
    }

    private fun cursorInt(cursor: android.database.Cursor, column: String): Int {
        val idx = cursor.getColumnIndex(column)
        return if (idx >= 0 && !cursor.isNull(idx)) cursor.getInt(idx) else 0
    }

    private fun cursorString(cursor: android.database.Cursor, column: String): String? {
        val idx = cursor.getColumnIndex(column)
        return if (idx >= 0 && !cursor.isNull(idx)) cursor.getString(idx) else null
    }

    private fun colorIntToHex(color: Int): String {
        if (color == 0) return prefs.accentColorHex
        return String.format("#%06X", color and 0xFFFFFF)
    }
}
