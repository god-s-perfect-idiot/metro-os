package com.metro.calendar.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.CalendarViewType
import com.metro.calendar.data.CalendarRepository
import com.metro.calendar.data.DayBucket
import com.metro.calendar.data.HourSlot
import com.metro.calendar.data.MonthGridCell
import java.time.LocalDate
import java.time.ZoneId

class CalendarState(context: Context) {
    private val repository = CalendarRepository(context)
    internal val appContext = context.applicationContext
    private val zoneId: ZoneId = ZoneId.systemDefault()

    var generation by mutableIntStateOf(0)
        private set

    private fun notifyChanged() {
        generation++
    }

    var hasCalendarPermission: Boolean = false
        private set

    var usingDemoData: Boolean = false
        private set

    var skippedPermissions: Boolean = false
        private set

    val needsPermissionGate: Boolean
        get() = !hasCalendarPermission && !skippedPermissions

    var selectedEpochDay: Long = CalendarLogic.todayEpochDay(zoneId)
        private set

    var viewType: CalendarViewType = CalendarViewType.Day
        private set

    var tabIndex: Int = 0
        private set

    var showTypePicker: Boolean = false
        private set

    var events: List<CalendarEvent> = emptyList()
        private set

    /** First day-tab date; re-anchored when drilling into a day from month view. */
    private var dayPivotStartEpochDay: Long = CalendarLogic.todayEpochDay(zoneId)

    private var loadedStartEpochDay: Long = 0L
    private var loadedEndEpochDay: Long = 0L
    private var rangeLoaded: Boolean = false

    val tabTitles: List<String>
        get() = CalendarLogic.buildTabTitles(
            viewType = viewType,
            zoneId = zoneId,
            dayPivotStartEpochDay = dayPivotStartEpochDay,
        )

    fun epochDayForPage(page: Int): Long =
        CalendarLogic.epochDayForTab(
            viewType = viewType,
            tabIndex = page,
            zoneId = zoneId,
            dayPivotStartEpochDay = dayPivotStartEpochDay,
        )

    val tabCount: Int
        get() = CalendarLogic.tabCountForViewType(viewType)

    val selectedDayEvents: List<CalendarEvent>
        get() = CalendarLogic.eventsForDay(events, selectedEpochDay, zoneId)

    val selectedAllDayEvents: List<CalendarEvent>
        get() = CalendarLogic.allDayEventsForDay(events, selectedEpochDay, zoneId)

    val selectedHourSlots: List<HourSlot>
        get() = CalendarLogic.buildHourSlots(events, selectedEpochDay, zoneId = zoneId)

    val monthGrid: List<MonthGridCell>
        get() {
            val date = LocalDate.ofEpochDay(selectedEpochDay)
            return CalendarLogic.buildMonthGrid(
                year = date.year,
                month = date.monthValue,
                events = events,
                selectedEpochDay = selectedEpochDay,
                zoneId = zoneId,
            )
        }

    val weekDayBuckets: List<DayBucket>
        get() {
            val weekStart = CalendarLogic.weekStartEpochDay(selectedEpochDay, zoneId)
            return CalendarLogic.weekDayEpochDays(weekStart).map { day ->
                DayBucket(
                    epochDay = day,
                    headerLabel = CalendarLogic.dateHeaderLabel(day, zoneId),
                    events = CalendarLogic.eventsForDay(events, day, zoneId),
                )
            }
        }

    val monthYearLabel: String
        get() = CalendarLogic.monthYearLabel(selectedEpochDay, zoneId)

    fun refreshPermission(context: Context) {
        hasCalendarPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
        notifyChanged()
    }

    fun onPermissionResult(granted: Boolean) {
        hasCalendarPermission = granted
        skippedPermissions = false
        if (granted) {
            reloadEvents()
        } else {
            usingDemoData = true
            events = repository.loadDemoEvents()
        }
        notifyChanged()
    }

    fun continueWithDemo() {
        skippedPermissions = true
        usingDemoData = true
        events = repository.loadDemoEvents()
        notifyChanged()
    }

    fun reloadEvents() {
        events = if (hasCalendarPermission) {
            usingDemoData = false
            runCatching {
                repository.loadEventsAround(selectedEpochDay, LOAD_RADIUS_DAYS)
            }.getOrElse {
                usingDemoData = true
                repository.loadDemoEvents()
            }
        } else if (skippedPermissions) {
            usingDemoData = true
            repository.loadDemoEvents()
        } else {
            emptyList()
        }
        if (!usingDemoData && hasCalendarPermission) {
            loadedStartEpochDay = selectedEpochDay - LOAD_RADIUS_DAYS
            loadedEndEpochDay = selectedEpochDay + LOAD_RADIUS_DAYS
            rangeLoaded = true
        } else {
            rangeLoaded = false
        }
        notifyChanged()
    }

    /**
     * Pulls fresh device-calendar events whenever the selected date drifts near the edge of the
     * window we last loaded, so swiping months/years ahead always reflects the real Google/device
     * calendar rather than a stale snapshot.
     */
    private fun ensureRangeLoaded(epochDay: Long) {
        if (!hasCalendarPermission || usingDemoData) return
        if (rangeLoaded &&
            epochDay in (loadedStartEpochDay + RELOAD_MARGIN_DAYS)..(loadedEndEpochDay - RELOAD_MARGIN_DAYS)
        ) {
            return
        }
        events = runCatching {
            repository.loadEventsAround(epochDay, LOAD_RADIUS_DAYS)
        }.getOrElse { return }
        loadedStartEpochDay = epochDay - LOAD_RADIUS_DAYS
        loadedEndEpochDay = epochDay + LOAD_RADIUS_DAYS
        rangeLoaded = true
    }

    fun selectViewType(type: CalendarViewType) {
        viewType = type
        if (type == CalendarViewType.Day) {
            dayPivotStartEpochDay = CalendarLogic.todayEpochDay(zoneId)
        }
        tabIndex = 0
        selectedEpochDay = epochDayForPage(0)
        showTypePicker = false
        ensureRangeLoaded(selectedEpochDay)
        notifyChanged()
    }

    fun selectTab(index: Int) {
        tabIndex = index.coerceIn(0, tabCount - 1)
        selectedEpochDay = epochDayForPage(tabIndex)
        ensureRangeLoaded(selectedEpochDay)
        notifyChanged()
    }

    fun toggleTypePicker() {
        showTypePicker = !showTypePicker
        notifyChanged()
    }

    fun dismissTypePicker() {
        if (showTypePicker) {
            showTypePicker = false
            notifyChanged()
        }
    }

    /** Opens day view for [epochDay] (month-grid drill-down, same pattern as [selectMonth]). */
    fun selectDay(epochDay: Long) {
        viewType = CalendarViewType.Day
        dayPivotStartEpochDay = epochDay
        selectedEpochDay = epochDay
        tabIndex = 0
        ensureRangeLoaded(selectedEpochDay)
        notifyChanged()
    }

    fun goToToday() {
        selectedEpochDay = CalendarLogic.todayEpochDay(zoneId)
        if (viewType == CalendarViewType.Day) {
            dayPivotStartEpochDay = selectedEpochDay
        }
        tabIndex = 0
        ensureRangeLoaded(selectedEpochDay)
        notifyChanged()
    }

    fun selectMonth(year: Int, month: Int) {
        viewType = CalendarViewType.Month
        selectedEpochDay = LocalDate.of(year, month, 1).toEpochDay()
        tabIndex = CalendarLogic.tabIndexForEpochDay(viewType, selectedEpochDay, zoneId)
            .coerceIn(0, tabCount - 1)
        ensureRangeLoaded(selectedEpochDay)
        notifyChanged()
    }

    /** Force a fresh pull from the device calendar provider (Google/Exchange/local accounts). */
    fun syncNow() {
        if (hasCalendarPermission) {
            reloadEvents()
        } else {
            refreshPermission(appContext)
            if (hasCalendarPermission) reloadEvents() else notifyChanged()
        }
    }

    fun showStub(message: String) {
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }

    fun onEventClick(event: CalendarEvent) {
        showStub(event.title)
    }

    private companion object {
        const val LOAD_RADIUS_DAYS = 90
        const val RELOAD_MARGIN_DAYS = 14
    }
}
