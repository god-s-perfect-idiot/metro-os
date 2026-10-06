package com.metro.news.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewsShell(
    state: NewsState,
    modifier: Modifier = Modifier,
) {
    var panoramaIntroPlayed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        state.ensureLoaded()
    }

    MetroSubpageHost(
        route = state.route,
        isRoot = { it == NewsRoute.Hub },
        parentOf = { it.parentRoute() },
        onGoBack = state::goBack,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding()
            .background(MetroTheme.colors.background),
        rootContent = {
            Box(modifier = Modifier.fillMaxSize()) {
                val pagerState = rememberPagerState(
                    initialPage = state.hubPage,
                    pageCount = { NewsState.HUB_PAGE_COUNT },
                )
                LaunchedEffect(pagerState.currentPage) {
                    state.hubPage = pagerState.currentPage
                }
                LaunchedEffect(state.hubPage) {
                    if (pagerState.currentPage != state.hubPage) {
                        pagerState.scrollToPage(state.hubPage)
                    }
                }
                NewsPanorama(
                    state = state,
                    pagerState = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    skipIntro = panoramaIntroPlayed,
                    onIntroPlayed = { panoramaIntroPlayed = true },
                )
                MetroAppBar(
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Refresh,
                            label = "refresh",
                            onClick = state::refresh,
                        ),
                    ),
                    menuItems = listOf(
                        MetroAppBarMenuItem("topics") { state.openTopics() },
                        MetroAppBarMenuItem("sources") { state.openSources() },
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        },
        subpageContent = { route ->
            when (route) {
                is NewsRoute.Article -> {
                    val story = state.storiesById[route.storyId]
                    ArticleScreen(
                        story = story,
                        onOpen = { story?.let(state::openInBrowser) },
                        onShare = { story?.let(state::shareStory) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                NewsRoute.Topics -> TopicsScreen(
                    state = state,
                    mode = TopicsMode.Topics,
                    modifier = Modifier.fillMaxSize(),
                )
                NewsRoute.Sources -> TopicsScreen(
                    state = state,
                    mode = TopicsMode.Sources,
                    modifier = Modifier.fillMaxSize(),
                )
                NewsRoute.Hub -> Unit
            }
        },
    )
}
