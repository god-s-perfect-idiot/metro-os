package com.metro.calendar.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.CalendarPreferences
import com.metro.calendar.data.CalendarViewType
import com.metro.calendar.data.CalendarRepository
import com.metro.calendar.data.DayBucket
import com.metro.calendar.data.DayWeather
import com.metro.calendar.data.EventDraft
import com.metro.calendar.data.HourSlot
import com.metro.calendar.data.MonthGridCell
import com.metro.calendar.data.WritableCalendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

sealed class CalendarRoute {
    data object Hub : CalendarRoute()
    data object NewEvent : CalendarRoute()
    data object Settings : CalendarRoute()
}

enum class EventSaveResult {
    Saved,
    NeedsWritePermission,
    Invalid,
}

class CalendarState(context: Context) {
    private val repository = CalendarRepository(context)
    private val preferences = CalendarPreferences(context)
    internal val appContext = context.applicationContext
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null

    var generation by mutableIntStateOf(0)
        private set

    private fun notifyChanged() {
        generation++
    }

    var useFahrenheit: Boolean = preferences.useFahrenheit
        private set
    var showWeather: Boolean = preferences.showWeather
        private set
    var use24HourTime: Boolean = preferences.use24HourTime
        private set

    var hasCalendarPermission: Boolean = false
        private set

    var hasWritePermission: Boolean = false
        private set

    var usingDemoData: Boolean = false
        private set

    var skippedPermissions: Boolean = false
        private set

    /** False until the first [refreshPermission] completes — avoids flashing the permission gate. */
    var permissionsChecked: Boolean = false
        private set

    val needsPermissionGate: Boolean
        get() = !hasCalendarPermission && !skippedPermissions

    var selectedEpochDay: Long = CalendarLogic.todayEpochDay(zoneId)
        private set

    var viewType: CalendarViewType = preferences.defaultViewType
        private set

    var tabIndex: Int =
        if (viewType == CalendarViewType.Day) {
            CalendarLogic.dayPastTabOffset()
        } else {
            CalendarLogic.pastTabOffset(viewType)
        }
        private set

    var showTypePicker: Boolean = false
        private set

    var route: CalendarRoute = CalendarRoute.Hub
        private set

    var events: List<CalendarEvent> = emptyList()
        private set

    var writableCalendars: List<WritableCalendar> = emptyList()
        private set

    var eventDraft: EventDraft = EventDraft(epochDay = selectedEpochDay)
        private set

    /** Metro toast message pending display via [MetroToastHost]. */
    var toastMessage: String? = null
        private set

    /** True when Add was tapped before WRITE_CALENDAR was granted. */
    private var pendingOpenNewEvent: Boolean = false

    /** First day-tab date; re-anchored when drilling into a day from month view. */
    private var dayPivotStartEpochDay: Long = CalendarLogic.defaultDayPivotStartEpochDay(zoneId)

    /** Tab-0 period for week/month/year; recentered on today or when drilling far into the past. */
    private var periodPivotStartEpochDay: Long =
        CalendarLogic.defaultPeriodPivotStartEpochDay(
            if (viewType == CalendarViewType.Day) CalendarViewType.Month else viewType,
            zoneId,
        )

    private var loadedStartEpochDay: Long = 0L
    private var loadedEndEpochDay: Long = 0L
    private var rangeLoaded: Boolean = false

    val tabTitles: List<String>
        get() = CalendarLogic.buildTabTitles(
            viewType = viewType,
            zoneId = zoneId,
            dayPivotStartEpochDay = dayPivotStartEpochDay,
            periodPivotStartEpochDay = periodPivotStartEpochDay,
        )

    fun epochDayForPage(page: Int): Long =
        CalendarLogic.epochDayForTab(
            viewType = viewType,
            tabIndex = page,
            zoneId = zoneId,
            dayPivotStartEpochDay = dayPivotStartEpochDay,
            periodPivotStartEpochDay = periodPivotStartEpochDay,
        )

    val tabCount: Int
        get() = CalendarLogic.tabCountForViewType(viewType)

    val selectedDayEvents: List<CalendarEvent>
        get() = CalendarLogic.eventsForDay(events, selectedEpochDay, zoneId)

    val selectedAllDayEvents: List<CalendarEvent>
        get() = CalendarLogic.allDayEventsForDay(events, selectedEpochDay, zoneId)

    val selectedHourSlots: List<HourSlot>
        get() = CalendarLogic.buildHourSlots(
            events = events,
            epochDay = selectedEpochDay,
            startHour = 0,
            endHour = 23,
            zoneId = zoneId,
            use24Hour = use24HourTime,
        )

    fun weatherForDay(epochDay: Long): DayWeather? =
        if (showWeather) DayWeather.forEpochDay(epochDay) else null

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
        hasWritePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
        permissionsChecked = true
        notifyChanged()
    }

    fun onPermissionResult(granted: Boolean) {
        hasCalendarPermission = granted
        skippedPermissions = false
        if (granted) {
            hasWritePermission = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.WRITE_CALENDAR,
            ) == PackageManager.PERMISSION_GRANTED
            reloadEvents()
            refreshWritableCalendars()
        } else {
            usingDemoData = true
            events = repository.loadDemoEvents()
            writableCalendars = emptyList()
        }
        notifyChanged()
    }

    fun onWritePermissionResult(granted: Boolean) {
        hasWritePermission = granted
        if (granted) {
            refreshWritableCalendars()
            if (route == CalendarRoute.NewEvent && eventDraft.calendarId == null) {
                eventDraft = eventDraft.copy(calendarId = writableCalendars.firstOrNull()?.id)
            }
            if (pendingOpenNewEvent) {
                pendingOpenNewEvent = false
                openNewEvent()
                return
            }
        } else {
            pendingOpenNewEvent = false
            if (route == CalendarRoute.NewEvent) {
                showToast(appContext.getString(com.metro.calendar.R.string.event_needs_calendar_access))
            }
        }
        notifyChanged()
    }

    fun requestOpenNewEvent(): Boolean {
        if (usingDemoData || !hasCalendarPermission) {
            showToast(appContext.getString(com.metro.calendar.R.string.event_needs_calendar_access))
            return true
        }
        if (!hasWritePermission) {
            pendingOpenNewEvent = true
            return false
        }
        openNewEvent()
        return true
    }

    fun continueWithDemo() {
        skippedPermissions = true
        usingDemoData = true
        events = repository.loadDemoEvents()
        writableCalendars = emptyList()
        notifyChanged()
    }

    fun reloadEvents(retainEvents: List<CalendarEvent> = emptyList()) {
        if (!hasCalendarPermission) {
            events = if (skippedPermissions) {
                usingDemoData = true
                repository.loadDemoEvents()
            } else {
                emptyList()
            }
            writableCalendars = emptyList()
            rangeLoaded = false
            notifyChanged()
            return
        }
        val around = selectedEpochDay
        loadJob?.cancel()
        loadJob = scope.launch {
            usingDemoData = false
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    repository.loadEventsAround(around, LOAD_RADIUS_DAYS) to
                        repository.loadWritableCalendars()
                }
            }
            result.fold(
                onSuccess = { (loaded, calendars) ->
                    events = mergeRetained(loaded, retainEvents)
                    writableCalendars = calendars
                    loadedStartEpochDay = around - LOAD_RADIUS_DAYS
                    loadedEndEpochDay = around + LOAD_RADIUS_DAYS
                    rangeLoaded = true
                },
                onFailure = {
                    usingDemoData = true
                    events = repository.loadDemoEvents()
                    writableCalendars = emptyList()
                    rangeLoaded = false
                },
            )
            notifyChanged()
        }
    }

    private fun mergeRetained(
        loaded: List<CalendarEvent>,
        retainEvents: List<CalendarEvent>,
    ): List<CalendarEvent> {
        if (retainEvents.isEmpty()) return loaded
        val missing = retainEvents.filter { retained ->
            loaded.none { it.id == retained.id && it.startMillis == retained.startMillis }
        }
        return if (missing.isEmpty()) loaded else (loaded + missing).sortedBy { it.startMillis }
    }

    private fun refreshWritableCalendars() {
        if (!hasCalendarPermission || usingDemoData) {
            writableCalendars = emptyList()
            return
        }
        scope.launch {
            writableCalendars = withContext(Dispatchers.IO) {
                runCatching { repository.loadWritableCalendars() }.getOrDefault(emptyList())
            }
            notifyChanged()
        }
    }

    /**
     * Pulls fresh device-calendar events whenever the selected date drifts near the edge of the
     * window we last loaded. Always runs off the main thread — a sync provider query here was
     * freezing the UI when switching day/week/month/year.
     */
    private fun ensureRangeLoaded(epochDay: Long) {
        if (!hasCalendarPermission || usingDemoData) return
        if (rangeLoaded &&
            epochDay in (loadedStartEpochDay + RELOAD_MARGIN_DAYS)..(loadedEndEpochDay - RELOAD_MARGIN_DAYS)
        ) {
            return
        }
        loadJob?.cancel()
        loadJob = scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                runCatching { repository.loadEventsAround(epochDay, LOAD_RADIUS_DAYS) }.getOrNull()
            } ?: return@launch
            events = loaded
            loadedStartEpochDay = epochDay - LOAD_RADIUS_DAYS
            loadedEndEpochDay = epochDay + LOAD_RADIUS_DAYS
            rangeLoaded = true
            notifyChanged()
        }
    }

    fun selectViewType(type: CalendarViewType) {
        showTypePicker = false
        if (type == viewType) {
            notifyChanged()
            return
        }
        viewType = type
        if (type == CalendarViewType.Day) {
            dayPivotStartEpochDay = CalendarLogic.defaultDayPivotStartEpochDay(zoneId)
            tabIndex = CalendarLogic.dayPastTabOffset()
            selectedEpochDay = CalendarLogic.todayEpochDay(zoneId)
        } else {
            periodPivotStartEpochDay = CalendarLogic.defaultPeriodPivotStartEpochDay(type, zoneId)
            tabIndex = CalendarLogic.pastTabOffset(type)
            selectedEpochDay = epochDayForPage(tabIndex)
        }
        ensureRangeLoaded(selectedEpochDay)
        notifyChanged()
    }

    fun selectTab(index: Int) {
        val next = index.coerceIn(0, (tabCount - 1).coerceAtLeast(0))
        val nextDay = epochDayForPage(next)
        if (next == tabIndex && nextDay == selectedEpochDay) {
            ensureRangeLoaded(nextDay)
            return
        }
        tabIndex = next
        selectedEpochDay = nextDay
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
            dayPivotStartEpochDay = CalendarLogic.defaultDayPivotStartEpochDay(zoneId)
            tabIndex = CalendarLogic.dayPastTabOffset()
        } else {
            periodPivotStartEpochDay = CalendarLogic.defaultPeriodPivotStartEpochDay(viewType, zoneId)
            tabIndex = CalendarLogic.pastTabOffset(viewType)
        }
        ensureRangeLoaded(selectedEpochDay)
        notifyChanged()
    }

    fun selectMonth(year: Int, month: Int) {
        viewType = CalendarViewType.Month
        selectedEpochDay = LocalDate.of(year, month, 1).toEpochDay()
        periodPivotStartEpochDay = CalendarLogic.periodPivotStartForFocus(
            viewType = CalendarViewType.Month,
            epochDay = selectedEpochDay,
            zoneId = zoneId,
        )
        tabIndex = CalendarLogic.tabIndexForEpochDay(
            viewType = viewType,
            epochDay = selectedEpochDay,
            zoneId = zoneId,
            periodPivotStartEpochDay = periodPivotStartEpochDay,
        )
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

    fun openSettings() {
        showTypePicker = false
        route = CalendarRoute.Settings
        notifyChanged()
    }

    fun setUseFahrenheit(value: Boolean) {
        useFahrenheit = value
        preferences.useFahrenheit = value
        notifyChanged()
    }

    fun setShowWeather(value: Boolean) {
        showWeather = value
        preferences.showWeather = value
        notifyChanged()
    }

    fun setUse24HourTime(value: Boolean) {
        use24HourTime = value
        preferences.use24HourTime = value
        notifyChanged()
    }

    fun setDefaultViewType(type: CalendarViewType) {
        preferences.defaultViewType = type
        notifyChanged()
    }

    fun preferencesDefaultViewType(): CalendarViewType = preferences.defaultViewType

    fun openNewEvent() {
        showTypePicker = false
        val now = LocalTime.now(zoneId)
        val startHour = when {
            now.hour >= 22 -> 9
            now.minute == 0 -> now.hour
            else -> (now.hour + 1).coerceAtMost(23)
        }
        refreshWritableCalendars()
        eventDraft = EventDraft(
            epochDay = selectedEpochDay,
            startHour = startHour,
            startMinute = 0,
            durationMinutes = 60,
            calendarId = writableCalendars.firstOrNull()?.id,
        )
        route = CalendarRoute.NewEvent
        notifyChanged()
    }

    fun updateEventDraft(draft: EventDraft) {
        eventDraft = draft
        notifyChanged()
    }

    fun closeOverlay() {
        if (route != CalendarRoute.Hub) {
            route = CalendarRoute.Hub
            notifyChanged()
        }
    }

    /**
     * Saves [eventDraft] into the device calendar provider. Leaves [route] alone so the subpage
     * host can animate out; caller should [closeOverlay] after [EventSaveResult.Saved].
     */
    fun saveEvent(): EventSaveResult {
        val title = eventDraft.title.trim()
        if (title.isEmpty()) {
            showToast(appContext.getString(com.metro.calendar.R.string.event_title_required))
            return EventSaveResult.Invalid
        }
        if (!hasWritePermission || usingDemoData) {
            return EventSaveResult.NeedsWritePermission
        }
        if (writableCalendars.isEmpty()) {
            refreshWritableCalendars()
        }
        val draft = if (eventDraft.calendarId == null) {
            eventDraft.copy(calendarId = writableCalendars.firstOrNull()?.id)
        } else {
            eventDraft
        }
        if (draft.calendarId == null) {
            showToast(appContext.getString(com.metro.calendar.R.string.event_no_calendar))
            return EventSaveResult.Invalid
        }
        val id = runCatching { repository.insertEvent(draft) }.getOrNull()
        if (id == null) {
            showToast(appContext.getString(com.metro.calendar.R.string.event_save_failed))
            return EventSaveResult.Invalid
        }
        selectedEpochDay = draft.epochDay
        if (viewType == CalendarViewType.Day) {
            // Keep the day pivot window covering the saved day when possible.
            val inWindow = draft.epochDay in dayPivotStartEpochDay until (dayPivotStartEpochDay + tabCount)
            if (inWindow) {
                tabIndex = (draft.epochDay - dayPivotStartEpochDay).toInt()
            } else {
                dayPivotStartEpochDay = draft.epochDay
                tabIndex = 0
            }
        } else {
            periodPivotStartEpochDay = CalendarLogic.periodPivotStartForFocus(
                viewType = viewType,
                epochDay = selectedEpochDay,
                zoneId = zoneId,
            )
            tabIndex = CalendarLogic.tabIndexForEpochDay(
                viewType = viewType,
                epochDay = selectedEpochDay,
                zoneId = zoneId,
                periodPivotStartEpochDay = periodPivotStartEpochDay,
            )
        }
        // Provider Instances can lag right after insert — keep the saved row visible across the
        // async reload so the day/month grids don't flash empty.
        val calendar = writableCalendars.firstOrNull { it.id == draft.calendarId }
        val optimistic = repository.eventFromDraft(id, draft, calendar)
        val fromProvider = runCatching { repository.loadEventById(id) }.getOrNull()
        val saved = fromProvider ?: optimistic
        mergeEvent(saved)
        reloadEvents(retainEvents = listOf(saved))
        showToast(appContext.getString(com.metro.calendar.R.string.event_saved))
        return EventSaveResult.Saved
    }

    private fun mergeEvent(event: CalendarEvent) {
        events = (events.filterNot { it.id == event.id && it.startMillis == event.startMillis } + event)
            .sortedBy { it.startMillis }
        notifyChanged()
    }

    fun showToast(message: String) {
        toastMessage = message
        notifyChanged()
    }

    fun consumeToast() {
        toastMessage = null
    }

    fun onEventClick(event: CalendarEvent) {
        showToast(event.title)
    }

    fun deleteEvent(event: CalendarEvent) {
        if (usingDemoData || !hasCalendarPermission) {
            events = events.filterNot { it.id == event.id && it.startMillis == event.startMillis }
            showToast(appContext.getString(com.metro.calendar.R.string.event_deleted))
            notifyChanged()
            return
        }
        if (!hasWritePermission) {
            showToast(appContext.getString(com.metro.calendar.R.string.event_needs_calendar_access))
            return
        }
        loadJob?.cancel()
        loadJob = scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { repository.deleteEvent(event.id) }.getOrDefault(false)
            }
            if (ok) {
                events = events.filterNot { it.id == event.id }
                showToast(appContext.getString(com.metro.calendar.R.string.event_deleted))
            } else {
                showToast(appContext.getString(com.metro.calendar.R.string.event_delete_failed))
            }
            notifyChanged()
        }
    }

    private companion object {
        const val LOAD_RADIUS_DAYS = 90
        const val RELOAD_MARGIN_DAYS = 14
    }
}
