package com.metro.calendar.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import com.metro.calendar.R
import com.metro.calendar.data.CalendarEvent
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.CalendarViewType
import com.metro.calendar.data.DayBucket
import com.metro.ui.LocalMetroSubpageExit
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroPivot
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroToastHost
import com.metro.ui.metroNavBarPadding
import com.metro.ui.rememberMetroToastController
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun CalendarShell(
    state: CalendarState,
    onRequestWritePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observeState = generation
    val toast = rememberMetroToastController()

    LaunchedEffect(state.toastMessage) {
        val message = state.toastMessage ?: return@LaunchedEffect
        toast.show(message)
        state.consumeToast()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        MetroSubpageHost(
            route = state.route,
            isRoot = { it is CalendarRoute.Hub },
            parentOf = { CalendarRoute.Hub },
            onGoBack = state::closeOverlay,
            modifier = Modifier.fillMaxSize(),
            rootContent = {
                CalendarHub(
                    state = state,
                    onRequestWritePermission = onRequestWritePermission,
                )
            },
            subpageContent = { route ->
                val requestExit = LocalMetroSubpageExit.current
                when (route) {
                    CalendarRoute.Hub -> Unit
                    CalendarRoute.NewEvent -> NewEventScreen(
                        draft = state.eventDraft,
                        calendars = state.writableCalendars,
                        onDraftChange = state::updateEventDraft,
                        onSave = {
                            when (state.saveEvent()) {
                                EventSaveResult.Saved -> requestExit?.invoke()
                                EventSaveResult.NeedsWritePermission -> onRequestWritePermission()
                                EventSaveResult.Invalid -> Unit
                            }
                        },
                        onCancel = { requestExit?.invoke() },
                        modifier = Modifier.fillMaxSize(),
                    )
                    CalendarRoute.Settings -> SettingsScreen(
                        useFahrenheit = state.useFahrenheit,
                        showWeather = state.showWeather,
                        use24HourTime = state.use24HourTime,
                        defaultViewType = state.preferencesDefaultViewType(),
                        onUseFahrenheitChange = state::setUseFahrenheit,
                        onShowWeatherChange = state::setShowWeather,
                        onUse24HourTimeChange = state::setUse24HourTime,
                        onDefaultViewTypeChange = state::setDefaultViewType,
                        onBack = { requestExit?.invoke() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
        )

        MetroToastHost(
            controller = toast,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun CalendarHub(
    state: CalendarState,
    onRequestWritePermission: () -> Unit,
) {
    // state.generation is a plain counter — read it here so picker / toast / event updates
    // recompose this hub (parent may otherwise skip us when the State object identity is unchanged).
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observeState = generation
    val tabCount = state.tabCount
    val showTypePicker = state.showTypePicker
    val menuState = rememberCalendarEventContextMenuState()
    val contextMenuFocusFraction = rememberContextMenuFocusFraction(menuState.visibleState)
    val popupRootBounds = remember { EventBoundsRef() }
    val openEventMenu: (CalendarEvent, Rect) -> Unit = { event, bounds ->
        menuState.open(event, bounds, popupRootBounds.value)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                popupRootBounds.value = coordinates.boundsInWindow()
            },
    ) {
        // Recreate pager when view type changes so currentPage never exceeds the new pageCount
        // (e.g. month view has 12 tabs, year view has 5 — swiping to the last month then
        // switching to year left the pager on page 11 and crashed).
        key(state.viewType) {
            val pagerState = rememberPagerState(
                initialPage = state.tabIndex.coerceIn(0, (tabCount - 1).coerceAtLeast(0)),
                pageCount = { tabCount },
            )
            val scope = rememberCoroutineScope()

            // Keep the pager on the state's tab after Today / view switches. Use scrollToPage
            // (not animate) so view-type changes can't fight a settling animation and hang.
            LaunchedEffect(state.tabIndex, state.viewType) {
                val target = state.tabIndex.coerceIn(0, (tabCount - 1).coerceAtLeast(0))
                if (pagerState.currentPage != target) {
                    pagerState.scrollToPage(target)
                }
            }

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
                    .collect { (page, scrolling) ->
                        if (!scrolling && page != state.tabIndex) {
                            state.selectTab(page)
                        }
                    }
            }

            val appTitle = appTitleForPage(state.viewType, state.epochDayForPage(pagerState.currentPage))
            MetroPivot(
                titles = state.tabTitles,
                pagerState = pagerState,
                header = { MetroAppTitle(title = appTitle) },
                onTitleClick = { index ->
                    state.selectTab(index)
                    scope.launch { pagerState.scrollToPage(index) }
                },
                pageContent = { page ->
                    val epochDay = state.epochDayForPage(page)
                    when (state.viewType) {
                        CalendarViewType.Day -> DayScreen(
                            epochDay = epochDay,
                            allDayEvents = CalendarLogic.allDayEventsForDay(state.events, epochDay),
                            hourSlots = CalendarLogic.buildHourSlots(
                                events = state.events,
                                epochDay = epochDay,
                                startHour = 0,
                                endHour = 23,
                                use24Hour = state.use24HourTime,
                            ),
                            weather = state.weatherForDay(epochDay),
                            useFahrenheit = state.useFahrenheit,
                            contextMenuEvent = menuState.event,
                            contextMenuFocusFraction = contextMenuFocusFraction,
                            onEventClick = state::onEventClick,
                            onEventLongPress = openEventMenu,
                        )
                        CalendarViewType.Week -> {
                            val weekStart = CalendarLogic.weekStartEpochDay(epochDay)
                            val buckets = weekBuckets(state, weekStart)
                            WeekScreen(
                                buckets = buckets,
                                usingDemoData = state.usingDemoData,
                                contextMenuEvent = menuState.event,
                                contextMenuFocusFraction = contextMenuFocusFraction,
                                onEventClick = state::onEventClick,
                                onEventLongPress = openEventMenu,
                            )
                        }
                        CalendarViewType.Month -> {
                            val date = LocalDate.ofEpochDay(epochDay)
                            MonthScreen(
                                epochDay = epochDay,
                                grid = CalendarLogic.buildMonthGrid(
                                    year = date.year,
                                    month = date.monthValue,
                                    events = state.events,
                                    selectedEpochDay = state.selectedEpochDay,
                                ),
                                selectedDayEvents = if (epochDay == state.selectedEpochDay) {
                                    state.selectedDayEvents
                                } else {
                                    emptyList()
                                },
                                contextMenuEvent = menuState.event,
                                contextMenuFocusFraction = contextMenuFocusFraction,
                                onSelectDay = state::selectDay,
                                onEventClick = state::onEventClick,
                                onEventLongPress = openEventMenu,
                            )
                        }
                        CalendarViewType.Year -> {
                            val year = LocalDate.ofEpochDay(epochDay).year
                            val months = CalendarLogic.monthsInYear(year).map { month ->
                                month to CalendarLogic.monthNameLower(
                                    LocalDate.of(year, month, 1).toEpochDay(),
                                )
                            }
                            YearScreen(
                                year = year,
                                months = months,
                                events = state.events,
                                onSelectMonth = { month -> state.selectMonth(year, month) },
                            )
                        }
                    }
                },
            )
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Refresh,
                    label = stringResource(R.string.view_type),
                    onClick = state::toggleTypePicker,
                    contentDescription = stringResource(R.string.view_type),
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Add,
                    label = stringResource(R.string.new_event),
                    onClick = {
                        if (!state.requestOpenNewEvent()) {
                            onRequestWritePermission()
                        }
                    },
                ),
            ),
            menuItems = listOf(
                MetroAppBarMenuItem(
                    text = stringResource(R.string.today),
                    onClick = state::goToToday,
                ),
                MetroAppBarMenuItem(
                    text = stringResource(R.string.sync_calendars),
                    onClick = {
                        state.syncNow()
                        state.showToast(state.appContext.getString(R.string.sync_done))
                    },
                ),
                MetroAppBarMenuItem(
                    text = stringResource(R.string.settings),
                    onClick = state::openSettings,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        ViewTypePicker(
            visible = showTypePicker,
            currentType = state.viewType,
            onSelect = state::selectViewType,
            onDismiss = state::dismissTypePicker,
            // Above the app bar so the panel/scrim receive taps after open; zero-size when hidden.
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(if (showTypePicker) Modifier.fillMaxSize() else Modifier),
        )

        CalendarEventContextMenu(
            menuState = menuState,
            onDeleteConfirmed = state::deleteEvent,
        )
    }
}

/**
 * The WP8.1 pivot overline above the tab row. Reflects the period currently in view: the month and
 * year for day/week, and the year for month/year views. Recomputes as the user swipes pages.
 */
private fun appTitleForPage(viewType: CalendarViewType, epochDay: Long): String =
    when (viewType) {
        CalendarViewType.Day -> CalendarLogic.monthYearLabel(epochDay)
        CalendarViewType.Week -> CalendarLogic.monthYearLabel(
            CalendarLogic.weekStartEpochDay(epochDay),
        )
        CalendarViewType.Month -> CalendarLogic.yearLabel(epochDay)
        CalendarViewType.Year -> LocalDate.ofEpochDay(epochDay).year.toString()
    }

private fun weekBuckets(state: CalendarState, weekStart: Long): List<DayBucket> =
    CalendarLogic.weekDayEpochDays(weekStart).map { day ->
        DayBucket(
            epochDay = day,
            headerLabel = CalendarLogic.dateHeaderLabel(day),
            events = CalendarLogic.eventsForDay(state.events, day),
        )
    }
