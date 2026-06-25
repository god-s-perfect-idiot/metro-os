package com.metro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * WP8.1 pivot — filter/categorize similar content (max 7 headers).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetroPivot(
    titles: List<String>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    onTitleClick: ((Int) -> Unit)? = null,
    pageContent: @Composable (Int) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        header()
        MetroHubTitleRow(
            titles = titles,
            selectedIndex = pagerState.currentPage,
            mode = MetroHubTitleMode.Pivot,
            onTitleClick = onTitleClick,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            beyondViewportPageCount = 1,
        ) { page ->
            pageContent(page)
        }
    }
}
