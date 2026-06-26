package com.metro.photos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.metro.photos.R
import com.metro.photos.data.ViewerCollection
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroPivot
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosShell(
    state: PhotosState,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.route != PhotosRoute.Collection) {
        state.navigateBack()
    }

    when (state.route) {
        PhotosRoute.Collection -> CollectionScreen(state, modifier)
        PhotosRoute.AlbumDetail -> {
            val album = state.selectedAlbum
            if (album == null) {
                LaunchedEffect(Unit) { state.navigateBack() }
            } else {
                AlbumDetailScreen(
                    album = album,
                    onPhotoClick = { photo ->
                        state.openViewer(ViewerCollection.Album, photo.id, album.bucketId)
                    },
                    modifier = modifier.testTag("metro_page_album"),
                )
            }
        }
        PhotosRoute.Viewer -> ViewerScreen(
            state = state,
            modifier = modifier.testTag("metro_page_viewer"),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionScreen(
    state: PhotosState,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = state.pivot.index,
        pageCount = { 3 },
    )
    val scope = rememberCoroutineScope()
    val pivotTitles = listOf(
        stringResource(R.string.pivot_all),
        stringResource(R.string.pivot_albums),
        stringResource(R.string.pivot_favorites),
    )

    LaunchedEffect(state.pivot) {
        if (pagerState.currentPage != state.pivot.index) {
            pagerState.scrollToPage(state.pivot.index)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        state.setPivot(pagerState.currentPage)
    }

    MetroPivot(
        titles = pivotTitles,
        pagerState = pagerState,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .metroNavBarPadding()
            .testTag("metro_page_hub"),
        header = {
            MetroAppTitle(title = stringResource(R.string.photos_label))
        },
        onTitleClick = { index ->
            scope.launch { pagerState.animateScrollToPage(index) }
            state.setPivot(index)
        },
    ) { page ->
        when (CollectionPivot.fromIndex(page)) {
            CollectionPivot.All -> AllPicturesPane(
                dateGroups = state.dateGroups,
                onPhotoClick = { photo ->
                    state.openViewer(ViewerCollection.All, photo.id)
                },
                modifier = Modifier.testTag("metro_page_pivot"),
            )
            CollectionPivot.Albums -> AlbumsPane(
                albums = state.albums,
                onAlbumClick = state::openAlbum,
                modifier = Modifier.testTag("metro_page_pivot"),
            )
            CollectionPivot.Favorites -> FavoritesPane(
                photos = state.favoritePhotos,
                onPhotoClick = { photo ->
                    state.openViewer(ViewerCollection.Favorites, photo.id)
                },
                modifier = Modifier.testTag("metro_page_pivot"),
            )
        }
    }
}
