package com.metro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * WP8.1 pivot — filter/categorize similar content (max 7 headers).
 *
 * When [chromeStaggerLoadKey] is non-null, the app header + page titles enter together via
 * [MetroStaggeredPivotEnter] at stagger index 0 (Start-tile continuum). Wrap each list row
 * with [MetroStaggeredPivotEnter] starting at index 1 using the **same stable** [loadKey] —
 * do not change it when the user switches pivot pages. Set [chromeStaggerExiting] to play the
 * top-down exit cascade with the list rows.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetroPivot(
    titles: List<String>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    belowTitleRow: @Composable () -> Unit = {},
    chromeStaggerLoadKey: Any? = null,
    chromeStaggerExiting: Boolean = false,
    chromeStaggerSkipEnter: Boolean = false,
    onTitleClick: ((Int) -> Unit)? = null,
    userScrollEnabled: Boolean = true,
    pageContent: @Composable (Int) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        val chrome: @Composable () -> Unit = {
            header()
            MetroHubTitleRow(
                titles = titles,
                selectedIndex = pagerState.currentPage,
                mode = MetroHubTitleMode.Pivot,
                onTitleClick = onTitleClick,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            belowTitleRow()
        }
        if (chromeStaggerLoadKey != null) {
            MetroStaggeredPivotEnter(
                staggerIndex = 0,
                loadKey = chromeStaggerLoadKey,
                exiting = chromeStaggerExiting,
                skipEnter = chromeStaggerSkipEnter,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    chrome()
                }
            }
        } else {
            chrome()
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            beyondViewportPageCount = 1,
            userScrollEnabled = userScrollEnabled,
            // Pages hang from the title row, never centre in the viewport.
            verticalAlignment = Alignment.Top,
        ) { page ->
            pageContent(page)
        }
    }
}
