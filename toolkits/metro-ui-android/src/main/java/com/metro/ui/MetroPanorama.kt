package com.metro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Height of the panorama title region — tight to hub title line height. */
val MetroPanoramaTitleHeight = 64.dp

/**
 * WP8.1 panorama — horizontal hub panes with baseline-aligned titles and next-pane peek.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetroPanorama(
    titles: List<String>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    onTitleClick: ((Int) -> Unit)? = null,
    pageContent: @Composable (Int) -> Unit,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MetroPanoramaTitleHeight),
            contentAlignment = Alignment.BottomStart,
        ) {
            MetroHubTitleRow(
                titles = titles,
                selectedIndex = pagerState.currentPage,
                mode = MetroHubTitleMode.Panorama,
                onTitleClick = onTitleClick,
            )
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = MetroPanoramaTitleHeight),
            beyondViewportPageCount = 1,
        ) { page ->
            pageContent(page)
        }
    }
}
