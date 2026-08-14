package com.metro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Height of the panorama title region — tight to hub title line height. */
val MetroPanoramaTitleHeight = 64.dp

/** WP8.1 panorama — ~40dp of the next pane visible on the right (METRO-UX-LANGUAGE §6.8). */
val MetroPanoramaContentPeek = 40.dp

/**
 * WP8.1 panorama — horizontal hub panes with pane-aligned titles and next-pane peek.
 *
 * Section titles sit one pane-width apart so only a sliver of the next title hangs into
 * view, and each content pane is narrower than the viewport so the next pane's content
 * sticks into the current view.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetroPanorama(
    titles: List<String>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    contentPeek: Dp = MetroPanoramaContentPeek,
    onTitleClick: ((Int) -> Unit)? = null,
    pageContent: @Composable (Int) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val viewportWidth = maxWidth
        val paneWidth = (viewportWidth - contentPeek).coerceAtLeast(0.dp)
        val lastPageIndex = titles.lastIndex.coerceAtLeast(0)
        val density = LocalDensity.current
        val paneStridePx = remember(paneWidth, density) {
            with(density) { paneWidth.roundToPx() }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MetroPanoramaTitleHeight),
                contentAlignment = Alignment.BottomStart,
            ) {
                MetroPanoramaTitleRow(
                    titles = titles,
                    pagerState = pagerState,
                    paneWidth = paneWidth,
                    viewportWidth = viewportWidth,
                    paneStridePx = paneStridePx,
                    onTitleClick = onTitleClick,
                )
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = MetroPanoramaTitleHeight),
                pageSize = PageSize.Fixed(paneWidth),
                // Trailing padding extends the scroll range by [contentPeek] so the last pane
                // can snap flush-left. Without it, max scroll stops [contentPeek] short and the
                // previous pane remains visible on the left edge.
                contentPadding = PaddingValues(end = contentPeek),
                snapPosition = SnapPosition.Start,
                beyondViewportPageCount = 1,
                // Panes hang from the title, never centre in the viewport.
                verticalAlignment = Alignment.Top,
            ) { page ->
                val pageWidth = if (page == lastPageIndex) viewportWidth else paneWidth
                Box(
                    modifier = Modifier
                        .width(pageWidth)
                        .clipToBounds(),
                ) {
                    pageContent(page)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MetroPanoramaTitleRow(
    titles: List<String>,
    pagerState: PagerState,
    paneWidth: Dp,
    viewportWidth: Dp,
    paneStridePx: Int,
    onTitleClick: ((Int) -> Unit)?,
) {
    val scrollOffsetPx by remember(pagerState, paneStridePx) {
        derivedStateOf {
            val progress = pagerState.currentPage + pagerState.currentPageOffsetFraction
            (progress * paneStridePx).roundToInt()
        }
    }
    val activeIndex by remember(pagerState) {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .roundToInt()
                .coerceIn(0, titles.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        titles.forEachIndexed { index, title ->
            val clickModifier = onTitleClick?.let { handler ->
                Modifier.clickable { handler(index) }
            } ?: Modifier
            val slotWidth = if (index == titles.lastIndex) viewportWidth else paneWidth
            Box(
                modifier = Modifier
                    .padding(start = MetroDimens.ScreenHorizontalMargin)
                    .offset {
                        IntOffset(index * paneStridePx - scrollOffsetPx, 0)
                    }
                    .width(slotWidth)
                    .clipToBounds()
                    .then(clickModifier),
                contentAlignment = Alignment.BottomStart,
            ) {
                // BasicText — not MetroText — so the title clips inside its pane slot
                // instead of measuring at unbounded width and overlapping neighbours.
                BasicText(
                    text = title,
                    style = MetroTextStyle.HubTitle.toTextStyle().copy(
                        color = if (index == activeIndex) {
                            MetroTheme.colors.primaryText
                        } else {
                            MetroTheme.colors.secondaryText
                        },
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}
