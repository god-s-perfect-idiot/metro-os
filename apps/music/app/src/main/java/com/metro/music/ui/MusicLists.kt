package com.metro.music.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.music.data.LibraryLogic
import com.metro.ui.MetroDimens
import com.metro.ui.MetroJumpListLogic
import com.metro.ui.MetroLetterTile
import com.metro.ui.MetroListItem
import com.metro.ui.MetroTheme
import com.metro.ui.metroStickyLetterHeader

/** Xbox Music packs song/album/artist rows tighter than the 76/90dp `MetroListItem` defaults. */
private val RowVerticalPadding = 4.dp
private val RowOneLineMinHeight = 48.dp
private val RowTwoLineMinHeight = 60.dp
private val LetterMarkerVerticalPadding = 4.dp

/** Dense collection row — songs, albums, artists, and streaming search results. */
@Composable
fun MusicListRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    MetroListItem(
        title = title,
        subtitle = subtitle,
        verticalPadding = RowVerticalPadding,
        oneLineMinHeight = RowOneLineMinHeight,
        twoLineMinHeight = RowTwoLineMinHeight,
        singleLine = true,
        onClick = onClick,
    )
}

/**
 * Collection list grouped under sticky letter markers (METRO-UX-LANGUAGE §6.18).
 *
 * Tapping a marker asks the host to open `MetroJumpList`; the picked letter comes back as
 * [jumpTarget] and scrolls that section to the top.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> MusicLetterList(
    items: List<T>,
    labelOf: (T) -> String,
    keyOf: (T) -> Any,
    jumpTarget: Char?,
    onJumpTargetConsumed: () -> Unit,
    onLetterMarkerClick: () -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (T) -> Unit,
) {
    val grouped = remember(items) { LibraryLogic.groupByJumpKey(items, labelOf) }
    val listState = rememberLazyListState()
    val headerIndices = remember(grouped) {
        var index = 0
        buildMap {
            grouped.forEach { (letter, sectionRows) ->
                put(letter, index)
                index += 1 + sectionRows.size
            }
        }
    }

    LaunchedEffect(jumpTarget, headerIndices) {
        val letter = jumpTarget ?: return@LaunchedEffect
        headerIndices[MetroJumpListLogic.normalize(letter)]?.let { listState.scrollToItem(it) }
        onJumpTargetConsumed()
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        grouped.forEach { (letter, sectionRows) ->
            metroStickyLetterHeader(letter) {
                // Opaque background so rows do not show through while the marker is pinned.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MetroTheme.colors.background)
                        .padding(
                            horizontal = MetroDimens.ScreenHorizontalMargin,
                            vertical = LetterMarkerVerticalPadding,
                        ),
                ) {
                    MetroLetterTile(letter = letter, onClick = onLetterMarkerClick)
                }
            }
            items(sectionRows, key = keyOf) { item -> row(item) }
        }
    }
}
