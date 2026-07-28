package com.metro.files.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.files.R
import com.metro.files.data.FileEntry
import com.metro.files.data.FileFilter
import com.metro.files.data.FilesLogic
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroPivot
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilesShell(
    state: FilesState,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val observe = state.generation

    BackHandler(enabled = !state.atVolumeRoot) {
        state.navigateUp()
    }

    val pagerState = rememberPagerState(
        initialPage = state.filter.pivotIndex,
        pageCount = { FileFilter.PIVOT_TITLES.size },
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.filter) {
        if (pagerState.currentPage != state.filter.pivotIndex && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(state.filter.pivotIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .filter { (_, scrolling) -> !scrolling }
            .distinctUntilChanged()
            .collect { (page, _) -> state.setFilter(page) }
    }

    MetroPivot(
        titles = FileFilter.PIVOT_TITLES,
        pagerState = pagerState,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .metroNavBarPadding()
            .background(MetroTheme.colors.background),
        header = {
            MetroAppTitle(title = stringResource(R.string.app_name))
        },
        belowTitleRow = {
            if (state.displaySegments.isNotEmpty()) {
                PathBreadcrumb(
                    segments = state.displaySegments,
                    onSegmentClick = { index -> state.navigateToSegment(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp),
                )
            }
            state.openMessage?.let { message ->
                MetroText(
                    text = message,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp),
                )
            }
        },
        onTitleClick = { index ->
            scope.launch { pagerState.animateScrollToPage(index) }
        },
    ) { page ->
        // Same listing for every page; filter is applied in state when the pivot settles.
        BrowseListPane(
            state = state,
            activePage = page,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PathBreadcrumb(
    segments: List<String>,
    onSegmentClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        segments.forEachIndexed { index, segment ->
            if (index > 0) {
                MetroText(
                    text = " > ",
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.accent,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            val isCurrent = index == segments.lastIndex
            MetroText(
                text = segment,
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.accent,
                modifier = if (isCurrent) {
                    Modifier.padding(vertical = 4.dp)
                } else {
                    Modifier
                        .clickable { onSegmentClick(index) }
                        .padding(vertical = 4.dp)
                },
            )
        }
    }
}

@Composable
private fun BrowseListPane(
    state: FilesState,
    activePage: Int,
) {
    // Force recomposition when generation or page changes.
    @Suppress("UNUSED_VARIABLE")
    val observe = state.generation to activePage

    when {
        state.isLoading && state.entries.isEmpty() -> {
            MetroLoadingScreen(modifier = Modifier.fillMaxSize())
        }
        state.entries.isEmpty() -> {
            val message = when {
                state.atVolumeRoot -> stringResource(R.string.empty_volumes)
                else -> stringResource(R.string.empty_folder)
            }
            MetroEmptyState(message = message)
        }
        else -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.entries, key = { it.id }) { entry ->
                    FileRow(
                        entry = entry,
                        onClick = { state.openEntry(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    entry: FileEntry,
    onClick: () -> Unit,
) {
    MetroListItem(
        title = entry.name,
        subtitle = FilesLogic.fileSubtitle(entry),
        leading = { FileEntryIcon(entry = entry) },
        verticalPadding = 4.dp,
        oneLineMinHeight = 56.dp,
        twoLineMinHeight = 68.dp,
        onClick = onClick,
    )
}
