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
            beyondViewportPageCount = 1,
            userScrollEnabled = !editing,
        ) { page ->
            when (page) {
                0 -> StartScreen(
                    tiles = state.displayTiles,
                    onTileClick = if (editing) ({}) else state::onTileClick,
                    onTileLongPress = if (editing) ({}) else state::onTileLongPress,
                    onOpenAppList = { state.currentPage = 1 },
                    editMode = editing,
                    editingTile = state.editingTile,
                    onDismissEdit = state::dismissEdit,
                    onResize = state::resizeEditingTile,
                    onUnpin = state::unpinEditingTile,
                    modifier = Modifier.testTag("metro_page_start"),
                )
                1 -> AppListScreen(
                    apps = state.filteredApps,
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = state::onSearchQueryChange,
                    onAppClick = state::launchApp,
                    onPinToStart = state::pinApp,
                    onUninstall = state::uninstallApp,
                    modifier = Modifier.testTag("metro_page_app_list"),
                )
            }
        }
    }
}
