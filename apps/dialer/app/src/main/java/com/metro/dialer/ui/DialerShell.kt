package com.metro.dialer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroHubTitleMode
import com.metro.ui.MetroHubTitleRow
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerShell(
    state: DialerState,
    modifier: Modifier = Modifier,
) {
    when (state.route) {
        DialerRoute.Main -> {
            val pagerState = rememberPagerState(
                initialPage = when (state.pivot) {
                    PhonePivot.History -> 0
                    PhonePivot.DialPad -> 1
                    PhonePivot.SpeedDial -> 2
                },
                pageCount = { 3 },
            )
            val scope = rememberCoroutineScope()
            val onDialPad = pagerState.currentPage == 1

            LaunchedEffect(state.pivot) {
                val targetPage = when (state.pivot) {
                    PhonePivot.History -> 0
                    PhonePivot.DialPad -> 1
                    PhonePivot.SpeedDial -> 2
                }
                if (pagerState.currentPage != targetPage) {
                    pagerState.scrollToPage(targetPage)
                }
            }

            LaunchedEffect(pagerState.currentPage) {
                state.setPivot(pagerState.currentPage)
            }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .metroNavBarPadding()
                    .background(Color.Black),
            ) {
                if (state.searchVisible) {
                    SearchBar(
                        query = state.searchQuery,
                        onQueryChange = state::updateSearchQuery,
                    )
                }

                MetroAppTitle(title = stringResource(R.string.app_name))

                MetroHubTitleRow(
                    titles = listOf("history", "dialpad", "speed dial"),
                    selectedIndex = pagerState.currentPage,
                    mode = MetroHubTitleMode.Pivot,
                    onTitleClick = { index ->
                        scope.launch { pagerState.animateScrollToPage(index) }
                        state.setPivot(index)
                    },
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                if (onDialPad) {
                    DialNumberField(
                        dialString = state.dialString,
                        onDelete = state::deleteDialChar,
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    beyondViewportPageCount = 1,
                ) { page ->
                    when (page) {
                        0 -> HistoryScreen(
                            groups = state.filteredGroups,
                            hasPermission = state.hasCallLogPermission,
                            onOpenDetail = state::openCallDetail,
                            onCallBack = { group ->
                                state.placeCall(group.phoneNumber, group.displayName)
                            },
                            onAddSpeedDial = state::addToSpeedDial,
                            onPinToStart = state::pinToStart,
                            canPinToStart = state::canPinToStart,
                        )
                        1 -> DialPadPane(
                            suggestions = state.t9Suggestions,
                            onAppend = state::appendDialChar,
                            onLongPressZero = { state.appendDialChar('+') },
                            onSuggestionClick = state::replaceDialString,
                            onCall = {
                                state.placeCall(state.dialString)
                            },
                            onSave = state::saveContactInPeople,
                        )
                        else -> SpeedDialScreen(
                            entries = state.speedDialEntries,
                            onCall = { entry ->
                                state.placeCall(entry.phoneNumber, entry.displayName)
                            },
                            onPinToStart = state::pinToStart,
                            canPinToStart = state::canPinToStart,
                            onRemove = state::removeSpeedDial,
                        )
                    }
                }
            }
        }
        DialerRoute.CallDetail -> state.selectedGroup?.let { group ->
            CallDetailScreen(
                group = group,
                onBack = state::closeOverlay,
                onCall = {
                    state.placeCall(group.phoneNumber, group.displayName)
                },
                onMessage = {
                    state.sendMessage(group.phoneNumber)
                },
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        MetroText(
            text = stringResource(R.string.search_history),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.secondaryText,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(Color(0xFF1F1F1F))
                .padding(12.dp),
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = MetroTextStyle.ListItemTitle.toTextStyle().copy(
                    color = MetroTheme.colors.primaryText,
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        MetroText(
                            text = stringResource(R.string.search_history),
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                        )
                    }
                    inner()
                },
            )
        }
    }
}
