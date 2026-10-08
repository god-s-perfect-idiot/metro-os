package mihon.feature.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.util.isTabletUi
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import mihon.feature.upcoming.components.UpcomingItem
import mihon.feature.upcoming.components.calendar.Calendar
import tachiyomi.core.common.Constants
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.TwoPanelBox
import tachiyomi.presentation.core.i18n.stringResource

private val ContentBottomClearance = MetroAppBarDefaults.BarHeight + 32.dp

@Composable
fun UpcomingScreenContent(
    state: UpcomingViewModel.State,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickUpcoming: (manga: Manga) -> Unit,
    onClickFilter: () -> Unit,
    hasActiveFilters: Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val onClickDay: (LocalDate, Int) -> Unit = { date, offset ->
        state.headerIndexes[date]?.let {
            scope.launch {
                listState.animateScrollToItem(it + offset)
            }
        }
    }

    MetroSystemTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .metroNavBarPadding()
                .background(MetroTheme.colors.background),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MetroSettingsHeader(
                    pageTitle = stringResource(MR.strings.label_upcoming).lowercase(),
                    appTitle = "metron",
                )
                if (isTabletUi()) {
                    UpcomingScreenLargeImpl(
                        listState = listState,
                        items = state.items,
                        events = state.events,
                        contentPadding = PaddingValues(bottom = ContentBottomClearance),
                        selectedYearMonth = state.selectedYearMonth,
                        setSelectedYearMonth = setSelectedYearMonth,
                        onClickDay = { onClickDay(it, 0) },
                        onClickUpcoming = onClickUpcoming,
                    )
                } else {
                    UpcomingScreenSmallImpl(
                        listState = listState,
                        items = state.items,
                        events = state.events,
                        contentPadding = PaddingValues(bottom = ContentBottomClearance),
                        selectedYearMonth = state.selectedYearMonth,
                        setSelectedYearMonth = setSelectedYearMonth,
                        onClickDay = { onClickDay(it, 1) },
                        onClickUpcoming = onClickUpcoming,
                    )
                }
            }

            UpcomingToolbar(
                hasFilters = hasActiveFilters,
                onClickFilter = onClickFilter,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun UpcomingToolbar(
    hasFilters: Boolean,
    onClickFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val accent = MetroTheme.colors.accent

    MetroAppBar(
        icons = listOf(
            MetroAppBarIcon(
                label = stringResource(MR.strings.action_filter).lowercase(),
                onClick = onClickFilter,
                icon = { color ->
                    MetroSystemIcon(
                        type = MetroSystemIconType.Filter,
                        iconSize = MetroAppBarDefaults.GlyphSize,
                        color = if (hasFilters) accent else color,
                        showCircle = false,
                    )
                },
            ),
        ),
        menuItems = listOf(
            MetroAppBarMenuItem(
                text = stringResource(MR.strings.upcoming_guide).lowercase(),
                onClick = { uriHandler.openUri(Constants.URL_HELP_UPCOMING) },
            ),
        ),
        modifier = modifier,
    )
}

@Composable
private fun DateHeading(
    date: LocalDate,
    mangaCount: Int,
) {
    MetroText(
        text = "${relativeDateText(date).lowercase()} · $mangaCount",
        style = MetroTextStyle.SectionHeader,
        color = MetroTheme.colors.accent,
        modifier = Modifier.padding(
            horizontal = MetroDimens.ScreenHorizontalMargin,
            vertical = 12.dp,
        ),
    )
}

@Composable
private fun UpcomingScreenSmallImpl(
    listState: LazyListState,
    items: List<UpcomingUIModel>,
    events: Map<LocalDate, Int>,
    contentPadding: PaddingValues,
    selectedYearMonth: YearMonth,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickDay: (LocalDate) -> Unit,
    onClickUpcoming: (manga: Manga) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding,
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "upcoming-calendar") {
            Calendar(
                selectedYearMonth = selectedYearMonth,
                events = events,
                setSelectedYearMonth = setSelectedYearMonth,
                onClickDay = onClickDay,
            )
        }
        items(
            items = items,
            key = { "upcoming-${it.hashCode()}" },
            contentType = {
                when (it) {
                    is UpcomingUIModel.Header -> "header"
                    is UpcomingUIModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is UpcomingUIModel.Item -> {
                    UpcomingItem(
                        upcoming = item.manga,
                        onClick = { onClickUpcoming(item.manga) },
                    )
                }

                is UpcomingUIModel.Header -> {
                    DateHeading(
                        date = item.date,
                        mangaCount = item.mangaCount,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingScreenLargeImpl(
    listState: LazyListState,
    items: List<UpcomingUIModel>,
    events: Map<LocalDate, Int>,
    contentPadding: PaddingValues,
    selectedYearMonth: YearMonth,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickDay: (LocalDate) -> Unit,
    onClickUpcoming: (manga: Manga) -> Unit,
) {
    TwoPanelBox(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        startContent = {
            Calendar(
                selectedYearMonth = selectedYearMonth,
                events = events,
                setSelectedYearMonth = setSelectedYearMonth,
                onClickDay = onClickDay,
            )
        },
        endContent = {
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                items(
                    items = items,
                    key = { "upcoming-${it.hashCode()}" },
                    contentType = {
                        when (it) {
                            is UpcomingUIModel.Header -> "header"
                            is UpcomingUIModel.Item -> "item"
                        }
                    },
                ) { item ->
                    when (item) {
                        is UpcomingUIModel.Item -> {
                            UpcomingItem(
                                upcoming = item.manga,
                                onClick = { onClickUpcoming(item.manga) },
                            )
                        }

                        is UpcomingUIModel.Header -> {
                            DateHeading(
                                date = item.date,
                                mangaCount = item.mangaCount,
                            )
                        }
                    }
                }
            }
        },
    )
}
