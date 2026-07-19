package com.metro.calendar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import com.metro.calendar.R
import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.CalendarViewType
import com.metro.calendar.data.DayBucket
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroPivot
import com.metro.ui.MetroSystemIconType
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun CalendarShell(
    state: CalendarState,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observeState = generation

    val tabCount = state.tabCount
    val pagerState = rememberPagerState(
        initialPage = state.tabIndex,
        pageCount = { tabCount },
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.tabIndex, state.viewType) {
        val target = state.tabIndex.coerceIn(0, tabCount - 1)
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != state.tabIndex) {
            state.selectTab(pagerState.currentPage)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding(),
    ) {
        val appTitle = appTitleForPage(state.viewType, state.epochDayForPage(pagerState.currentPage))
        MetroPivot(
            titles = state.tabTitles,
            pagerState = pagerState,
            header = { MetroAppTitle(title = appTitle) },
            onTitleClick = { index ->
                state.selectTab(index)
                scope.launch { pagerState.animateScrollToPage(index) }
            },
            pageContent = { page ->
                val epochDay = state.epochDayForPage(page)
                when (state.viewType) {
                    CalendarViewType.Day -> DayScreen(
                        epochDay = epochDay,
                        allDayEvents = CalendarLogic.allDayEventsForDay(state.events, epochDay),
                        hourSlots = CalendarLogic.buildHourSlots(state.events, epochDay),
                        onEventClick = state::onEventClick,
                    )
                    CalendarViewType.Week -> {
                        val weekStart = CalendarLogic.weekStartEpochDay(epochDay)
                        val buckets = weekBuckets(state, weekStart)
                        WeekScreen(
                            buckets = buckets,
                            usingDemoData = state.usingDemoData,
                            onEventClick = state::onEventClick,
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
                            onSelectDay = state::selectDay,
                            onEventClick = state::onEventClick,
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

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    label = stringResource(R.string.view_type),
                    onClick = state::toggleTypePicker,
                    contentDescription = stringResource(R.string.view_type),
                    showRestOutline = false,
                    icon = { color -> ViewTypeIcon(color = color) },
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Add,
                    label = stringResource(R.string.new_event),
                    onClick = {
                        state.showStub(state.appContext.getString(R.string.create_event_stub))
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
                        state.showStub(state.appContext.getString(R.string.sync_done))
                    },
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        ViewTypePicker(
            visible = state.showTypePicker,
            currentType = state.viewType,
            onSelect = state::selectViewType,
            onDismiss = state::dismissTypePicker,
            modifier = Modifier.align(Alignment.BottomCenter),
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

/**
 * "Switch view" glyph — two opposing horizontal arrows inside the standard circular outline.
 * Circle is drawn here (with [MetroAppBarIcon.showRestOutline] false) so the border matches Add.
 */
@Composable
private fun ViewTypeIcon(color: Color) {
    Canvas(modifier = Modifier.size(MetroAppBarDefaults.IconCircleSize)) {
        val strokeWidth = size.minDimension * 0.05f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawCircle(
            color = color,
            radius = size.minDimension * 0.42f - strokeWidth,
            style = Stroke(width = strokeWidth),
        )

        val cx = size.width / 2f
        val halfLen = size.minDimension * 0.22f
        val offsetY = size.minDimension * 0.10f
        val head = size.minDimension * 0.085f
        val topY = size.height / 2f - offsetY
        val botY = size.height / 2f + offsetY

        drawLine(color, Offset(cx - halfLen, topY), Offset(cx + halfLen, topY), strokeWidth, StrokeCap.Round)
        drawPath(
            Path().apply {
                moveTo(cx + halfLen - head, topY - head)
                lineTo(cx + halfLen, topY)
                lineTo(cx + halfLen - head, topY + head)
            },
            color,
            style = stroke,
        )

        drawLine(color, Offset(cx - halfLen, botY), Offset(cx + halfLen, botY), strokeWidth, StrokeCap.Round)
        drawPath(
            Path().apply {
                moveTo(cx - halfLen + head, botY - head)
                lineTo(cx - halfLen, botY)
                lineTo(cx - halfLen + head, botY + head)
            },
            color,
            style = stroke,
        )
    }
}
