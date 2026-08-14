package com.metro.photos.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.metro.photos.R
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    state: PhotosState,
    modifier: Modifier = Modifier,
) {
    val photos = state.viewerPhotos
    if (photos.isEmpty()) {
        LaunchedEffect(Unit) { state.navigateBack() }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = state.viewerIndex.coerceIn(0, photos.lastIndex),
        pageCount = { photos.size },
    )
    val scope = rememberCoroutineScope()
    var chromeVisible by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage) {
        state.updateViewerIndex(pagerState.currentPage)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Pager scroll is disabled — ZoomablePhoto owns pinch and page swipes so
        // HorizontalPager cannot steal two-finger gestures.
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            ZoomablePhoto(
                uri = photos[page].uri,
                isActive = page == pagerState.currentPage,
                modifier = Modifier.fillMaxSize(),
                onTap = { chromeVisible = !chromeVisible },
                onSwipeToAdjacent = { direction ->
                    val target = (pagerState.currentPage + direction)
                        .coerceIn(0, photos.lastIndex)
                    if (target != pagerState.currentPage) {
                        scope.launch { pagerState.animateScrollToPage(target) }
                    }
                },
            )
        }

        if (chromeVisible) {
            val current = photos.getOrNull(pagerState.currentPage)
            val favorite = current?.let { state.isFavorite(it.id) } == true
            val favoriteLabel = stringResource(R.string.add_favorite)
            val unfavoriteLabel = stringResource(R.string.remove_favorite)
            MetroAppBar(
                icons = listOf(
                    if (favorite) {
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Close,
                            label = unfavoriteLabel,
                            onClick = { current?.let { state.removeFavorite(it.id) } },
                            contentDescription = unfavoriteLabel,
                            enabled = current != null,
                        )
                    } else {
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Heart,
                            label = favoriteLabel,
                            onClick = { current?.let { state.addFavorite(it.id) } },
                            contentDescription = favoriteLabel,
                            enabled = current != null,
                        )
                    },
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .metroNavBarPadding(),
            )
        }
    }
}
