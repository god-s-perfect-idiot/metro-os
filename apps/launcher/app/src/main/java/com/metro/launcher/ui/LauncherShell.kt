package com.metro.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.metro.ui.metroNavBarPadding

/**
 * Two-page shell: Start tiles (page 0) and app menu (page 1).
 * Reference: references/guides/blueprint.md
 */
@OptIn(ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun LauncherShell(
    state: LauncherState,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val editing = state.editingTile != null

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            state.currentPage = page
        }
    }

    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
        if (state.currentPage != 1) {
            state.dismissSearch()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding()
            .semantics { testTagsAsResourceId = true }
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
            userScrollEnabled = !editing,
        ) { page ->
            when (page) {
                0 -> StartScreen(
                    tiles = state.displayTiles,
                    onTileClick = if (editing) ({}) else state::onTileClick,
                    onTileLongPress = state::onTileLongPress,
                    onOpenAppList = { state.currentPage = 1 },
                    editMode = editing,
                    editingTile = state.editingTile,
                    onDismissEdit = state::dismissEdit,
                    onResize = state::resizeEditingTile,
                    onUnpin = state::unpinEditingTile,
                    onDragLayout = state::applyDragLayout,
                    onReorderCommit = state::commitTileOrder,
                    modifier = Modifier.testTag("metro_page_start"),
                )
                1 -> AppListScreen(
                    apps = state.filteredApps,
                    searchActive = state.searchActive,
                    searchQuery = state.searchQuery,
                    onSearchActiveChange = state::onSearchActiveChange,
                    onSearchQueryChange = state::onSearchQueryChange,
                    onAppClick = state::launchApp,
                    onPinToStart = state::pinApp,
                    onUninstall = state::uninstallApp,
                    queryAppOptions = state::queryAppOptions,
                    onLaunchAppOption = state::launchAppOption,
                    modifier = Modifier.testTag("metro_page_app_list"),
                )
            }
        }

        if (state.showNotificationAccessPrompt && state.currentPage == 0 && !editing) {
            NotificationAccessPrompt(
                onGrant = state::openNotificationAccessSettings,
                onDismiss = state::dismissNotificationAccessPrompt,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
