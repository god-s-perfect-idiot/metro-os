package com.metro.people.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.metro.people.R
import com.metro.people.data.PeoplePane
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroPanorama
import com.metro.ui.MetroPanoramaBodyEnter
import com.metro.ui.MetroPanoramaBrandEnter
import com.metro.ui.MetroPanoramaBrandGradients
import com.metro.ui.MetroPanoramaBrandTitle
import kotlinx.coroutines.launch

private const val PeopleBrandText = "people"
private val PeopleBrandBrush = MetroPanoramaBrandGradients.People

/**
 * People hub — Music/Metron-style panoramic brand + [MetroPanorama] body.
 */
@Composable
fun PeopleHub(
    state: PeopleState,
    pagerState: PagerState,
    scrollToLetter: Char?,
    onScrollConsumed: () -> Unit,
    onRequestCallLogPermission: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    skipIntro: Boolean = false,
    onIntroPlayed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val paneTitles = listOf(
        stringResource(R.string.pane_whats_new),
        stringResource(R.string.pane_recent),
        stringResource(R.string.pane_all),
    )

    DisposableEffect(Unit) {
        onDispose { onIntroPlayed() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        MetroPanoramaBrandEnter(skipEnter = skipIntro) {
            MetroPanoramaBrandTitle(
                text = PeopleBrandText,
                pageProgress = pagerState.currentPage + pagerState.currentPageOffsetFraction,
                pageCount = pagerState.pageCount,
                brush = PeopleBrandBrush,
            )
        }

        MetroPanoramaBodyEnter(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            skipEnter = skipIntro,
        ) {
            MetroPanorama(
                titles = paneTitles,
                pagerState = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = MetroAppBarDefaults.BarHeight),
                onTitleClick = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                pageContent = { page ->
                    when (PeoplePane.entries.getOrNull(page) ?: PeoplePane.WhatsNew) {
                        PeoplePane.WhatsNew -> WhatsNewPane(
                            hasNotificationAccess = state.hasConversationAccess,
                            items = state.whatsNewItems,
                            onRequestNotificationAccess = onRequestNotificationAccess,
                            onOpenItem = state::openWhatsNewItem,
                            isLoading = state.whatsNewLoading,
                            modifier = Modifier.fillMaxSize(),
                        )
                        PeoplePane.Recent -> RecentPane(
                            hasCallLogPermission = state.hasCallLogPermission,
                            people = state.recentPeople,
                            onRequestCallLogPermission = onRequestCallLogPermission,
                            onOpenPerson = state::openRecentPerson,
                            isLoading = state.recentLoading,
                            modifier = Modifier.fillMaxSize(),
                        )
                        PeoplePane.All -> AllPane(
                            filterLabel = state.filterLabel,
                            grouped = state.groupedContacts,
                            flatContacts = state.visibleContacts,
                            isLoading = state.contactsLoading,
                            searchActive = false,
                            featuredConversation = state.featuredConversation,
                            featuredContact = state.featuredContact,
                            onFeaturedClick = {
                                state.featuredConversation?.let(state::openWhatsNewItem)
                            },
                            onFilterClick = state::openFilter,
                            onJumpClick = state::toggleJumpList,
                            onOpenDetail = { state.openDetail(it.id) },
                            onAddToSpeedDial = state::addToSpeedDial,
                            onPinToStart = state::pinToStart,
                            onDeleteContact = state::deleteContact,
                            scrollToLetter = scrollToLetter,
                            onScrollConsumed = onScrollConsumed,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                },
            )
        }
    }
}
