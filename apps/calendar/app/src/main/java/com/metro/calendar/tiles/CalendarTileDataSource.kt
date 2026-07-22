package com.metro.calendar.tiles

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.CalendarRepository
import com.metro.calendar.data.StubCalendarDataSource
import com.metro.system.MetroAppRegistry
import com.metro.system.MetroPreferences
import com.metro.system.MetroTileAgenda
import com.metro.system.MetroTileData

object CalendarTileLogic {
    fun buildTileData(
        events: List<CalendarEvent>,
        packageName: String,
        accentHex: String,
    ): MetroTileData {
        val next = CalendarLogic.nextUpcomingEvent(events)
        val todayCount = CalendarLogic.eventsTodayCount(events)
        val label = MetroAppRegistry.label(packageName) ?: "Calendar"
        val today = CalendarLogic.todayEpochDay()

        val agenda = MetroTileAgenda(
            lines = next?.let { CalendarLogic.tileEventLines(it, today) } ?: emptyList(),
            dayLabel = CalendarLogic.tileDayLabel(today),
            dayNumber = CalendarLogic.tileDayNumber(today),
            footer = label,
        )

        return MetroTileData(
            title = label,
            backgroundColorHex = accentHex,
            counter = todayCount.takeIf { it > 0 },
            backFaceTitle = next?.let { event ->
                "${CalendarLogic.nextEventTimeLabel(event)} — ${event.title}"
            },
            deepLinkUri = null,
            agenda = agenda,
        )
    }
}

class CalendarTileDataSource(context: Context) {
    private val appContext = context.applicationContext

    fun buildTileData(): MetroTileData {
        val accentHex = MetroPreferences(appContext).accentColorHex
        val events = if (hasCalendarPermission()) {
            runCatching {
                val repo = CalendarRepository(appContext)
                val today = CalendarLogic.todayEpochDay()
                repo.loadEventsAround(today, dayRadius = 30)
            }.getOrDefault(StubCalendarDataSource.demoEvents())
        } else {
            StubCalendarDataSource.demoEvents()
        }
        return CalendarTileLogic.buildTileData(
            events = events,
            packageName = appContext.packageName,
            accentHex = accentHex,
        )
    }

    private fun hasCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
}
