package com.metro.files.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.files.R
import com.metro.files.data.FileEntry
import com.metro.files.data.FileFilter
import com.metro.files.data.FileKind
import com.metro.files.data.FilesLogic
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroContextMenuActiveShift
import com.metro.ui.MetroContextMenuClearOnDismiss
import com.metro.ui.MetroContextMenuDimmedAlpha
import com.metro.ui.MetroContextMenuItem
import com.metro.ui.MetroContextMenuPopup
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroPivot
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** Mutable holder so layout callbacks can update without triggering recomposition. */
private class RectRef {
    var value: Rect = Rect.Zero
}

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

    Box(modifier = modifier.fillMaxSize()) {
        MetroPivot(
            titles = FileFilter.PIVOT_TITLES,
            pagerState = pagerState,
            modifier = Modifier
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

        state.pendingDelete?.let { entry ->
            MetroMessageDialog(
                title = stringResource(R.string.delete_confirm_title, entry.name),
                body = stringResource(R.string.delete_confirm_body),
                confirmLabel = stringResource(R.string.delete_confirm_yes),
                onConfirm = state::confirmDelete,
                dismissLabel = stringResource(R.string.delete_confirm_no),
                onDismissRequest = state::dismissDeleteConfirm,
            )
        }
    }
}

/**
 * Single-line left-aligned path breadcrumb. When the path overflows the row width,
 * scroll stays pinned to the end so the current folder remains visible.
 */
@Composable
private fun PathBreadcrumb(
    segments: List<String>,
    onSegmentClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(segments) {
        scrollState.scrollTo(scrollState.maxValue)
    }
    LaunchedEffect(scrollState.maxValue) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        segments.forEachIndexed { index, segment ->
            if (index > 0) {
                MetroText(
                    text = " > ",
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.accent,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            val isCurrent = index == segments.lastIndex
            MetroText(
                text = segment,
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.accent,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
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

    var contextMenuEntry by remember { mutableStateOf<FileEntry?>(null) }
    var contextMenuAnchor by remember { mutableStateOf(Rect.Zero) }
    var contextMenuRoot by remember { mutableStateOf(Rect.Zero) }
    val contextMenuVisible = remember { MutableTransitionState(false) }
    val popupRootBounds = remember { RectRef() }
    val contextMenuFocusTransition =
        updateTransition(contextMenuVisible, label = "filesContextMenuFocus")
    val contextMenuFocusFraction by contextMenuFocusTransition.animateFloat(
        transitionSpec = { tween(MetroTransitions.AppBarSlideMs) },
        label = "focusFraction",
    ) { visible -> if (visible) 1f else 0f }

    val openContextMenu: (FileEntry, Rect) -> Unit = { entry, bounds ->
        contextMenuAnchor = bounds
        contextMenuRoot = popupRootBounds.value
        contextMenuEntry = entry
        contextMenuVisible.targetState = true
    }
    val dismissContextMenu: () -> Unit = {
        contextMenuVisible.targetState = false
    }
    MetroContextMenuClearOnDismiss(contextMenuVisible) {
        contextMenuEntry = null
    }

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        popupRootBounds.value = coordinates.boundsInWindow()
                    },
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.entries, key = { it.id }) { entry ->
                        FileRow(
                            entry = entry,
                            contextMenuTarget = contextMenuEntry?.id == entry.id,
                            contextMenuFocusFraction = contextMenuFocusFraction,
                            onClick = { state.openEntry(entry) },
                            onLongPress = { bounds ->
                                if (entry.kind != FileKind.VOLUME) {
                                    openContextMenu(entry, bounds)
                                }
                            },
                        )
                    }
                }

                contextMenuEntry?.let { entry ->
                    val openLabel = stringResource(R.string.action_open)
                    val shareLabel = stringResource(R.string.action_share)
                    val deleteLabel = stringResource(R.string.action_delete)
                    MetroContextMenuPopup(
                        visibleState = contextMenuVisible,
                        anchorBounds = contextMenuAnchor,
                        rootBounds = contextMenuRoot,
                        items = listOf(
                            MetroContextMenuItem(
                                label = openLabel,
                                onClick = {
                                    dismissContextMenu()
                                    state.openEntry(entry)
                                },
                            ),
                            MetroContextMenuItem(
                                label = shareLabel,
                                enabled = entry.kind == FileKind.FILE,
                                onClick = {
                                    dismissContextMenu()
                                    state.shareEntry(entry)
                                },
                            ),
                            MetroContextMenuItem(
                                label = deleteLabel,
                                onClick = {
                                    dismissContextMenu()
                                    state.requestDelete(entry)
                                },
                            ),
                        ),
                        onDismissRequest = dismissContextMenu,
                    )
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    entry: FileEntry,
    contextMenuTarget: Boolean,
    contextMenuFocusFraction: Float,
    onClick: () -> Unit,
    onLongPress: (Rect) -> Unit,
) {
    val rowBounds = remember(entry.id) { RectRef() }
    val density = LocalDensity.current
    val activeShiftPx = with(density) { MetroContextMenuActiveShift.toPx() }
    val supportsContextMenu = entry.kind != FileKind.VOLUME

    MetroListItem(
        title = entry.name,
        subtitle = FilesLogic.fileSubtitle(entry),
        leading = { FileEntryIcon(entry = entry) },
        verticalPadding = 4.dp,
        oneLineMinHeight = 56.dp,
        twoLineMinHeight = 68.dp,
        singleLine = true,
        modifier = Modifier
            .graphicsLayer {
                if (contextMenuTarget) {
                    translationX = -contextMenuFocusFraction * activeShiftPx
                    alpha = 1f
                } else {
                    translationX = 0f
                    alpha = 1f - contextMenuFocusFraction * (1f - MetroContextMenuDimmedAlpha)
                }
            }
            .onGloballyPositioned { coordinates ->
                rowBounds.value = coordinates.boundsInWindow()
            }
            .metroClickable(
                onClick = onClick,
                onLongClick = if (supportsContextMenu) {
                    { onLongPress(rowBounds.value) }
                } else {
                    null
                },
            ),
    )
}
