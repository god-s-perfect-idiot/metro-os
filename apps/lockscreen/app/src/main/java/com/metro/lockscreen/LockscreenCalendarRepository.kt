package com.metro.lockscreen

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * Loads the next calendar appointment for the lock chrome from [CalendarContract].
 */
class LockscreenCalendarRepository(context: Context) {
    private val appContext = context.applicationContext
    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun loadNextGlanceEvent(
        now: ZonedDateTime = ZonedDateTime.now(zoneId),
        use24Hour: Boolean = DateFormat.is24HourFormat(appContext),
        locale: Locale = Locale.getDefault(),
    ): LockscreenChromeLogic.GlanceEvent? {
        if (!hasPermission()) return null
        val occurrence = LockscreenChromeLogic.selectNextOccurrence(
            occurrences = queryOccurrences(now),
            nowMillis = now.toInstant().toEpochMilli(),
            today = now.toLocalDate(),
            zoneId = zoneId,
        ) ?: return null
        return LockscreenChromeLogic.toGlanceEvent(occurrence, use24Hour, zoneId, locale)
    }

    fun loadChromeLabels(
        now: ZonedDateTime = ZonedDateTime.now(zoneId),
    ): LockscreenChromeLogic.ChromeLabels {
        val locale = Locale.getDefault()
        val use24Hour = DateFormat.is24HourFormat(appContext)
        val datePattern = DateFormat.getBestDateTimePattern(locale, "MMMMd")
        return LockscreenChromeLogic.labels(
            now = now,
            use24Hour = use24Hour,
            event = loadNextGlanceEvent(now, use24Hour, locale),
            locale = locale,
            datePattern = datePattern,
        )
    }

    private fun queryOccurrences(now: ZonedDateTime): List<LockscreenChromeLogic.CalendarOccurrence> {
        val today = now.toLocalDate()
        val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
        )
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, start)
        ContentUris.appendId(builder, end)

        val rows = mutableListOf<LockscreenChromeLogic.CalendarOccurrence>()
        try {
            appContext.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val locationIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                while (cursor.moveToNext()) {
                    val begin = if (beginIdx >= 0) cursor.getLong(beginIdx) else 0L
                    val endMillis = if (endIdx >= 0) cursor.getLong(endIdx) else begin
                    val allDay = allDayIdx >= 0 && cursor.getInt(allDayIdx) == 1
                    rows += LockscreenChromeLogic.CalendarOccurrence(
                        title = if (titleIdx >= 0) cursor.getString(titleIdx).orEmpty() else "",
                        location = if (locationIdx >= 0) cursor.getString(locationIdx) else null,
                        startMillis = begin,
                        endMillis = endMillis,
                        allDay = allDay,
                    )
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        }
        return rows
    }
}
