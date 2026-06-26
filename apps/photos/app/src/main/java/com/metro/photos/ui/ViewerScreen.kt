package com.metro.photos.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.photos.R
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

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
    var chromeVisible by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage) {
        state.updateViewerIndex(pagerState.currentPage)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { chromeVisible = !chromeVisible },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            PhotoFullImage(
                uri = photos[page].uri,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (chromeVisible) {
            val current = photos.getOrNull(pagerState.currentPage)
            val favorite = current?.let { state.isFavorite(it.id) } == true
            MetroText(
                text = stringResource(
                    if (favorite) R.string.remove_favorite else R.string.add_favorite,
                ),
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.accent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .metroNavBarPadding()
                    .padding(bottom = 24.dp)
                    .clickable {
                        current?.let { state.toggleFavorite(it.id) }
                    },
            )
        }
    }
}
