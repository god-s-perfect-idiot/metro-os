package com.metro.photos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.photos.R
import com.metro.photos.data.AlbumGroup
import com.metro.photos.data.DateGroup
import com.metro.photos.data.PhotoItem
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

@Composable
fun AllPicturesPane(
    dateGroups: List<DateGroup>,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    if (dateGroups.isEmpty()) {
        PhotosLoadingOrEmpty(
            loading = loading,
            emptyMessage = stringResource(R.string.empty_all),
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        dateGroups.forEach { group ->
            item(key = "header-${group.label}") {
                MetroText(
                    text = group.label,
                    style = MetroTextStyle.SectionHeader,
                    color = MetroTheme.colors.accent,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }
            items(
                items = group.photos.chunked(4),
                key = { row -> row.firstOrNull()?.id ?: row.hashCode() },
            ) { row ->
                PhotoGridRow(photos = row, onPhotoClick = onPhotoClick)
            }
        }
    }
}

private val PhotoGridGutter = 4.dp

@Composable
private fun PhotoGridRow(
    photos: List<PhotoItem>,
    onPhotoClick: (PhotoItem) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = PhotoGridGutter),
        horizontalArrangement = Arrangement.spacedBy(PhotoGridGutter),
    ) {
        photos.forEach { photo ->
            PhotoThumbnail(
                uri = photo.uri,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clickable { onPhotoClick(photo) },
            )
        }
        repeat((4 - photos.size).coerceAtLeast(0)) {
            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
        }
    }
}

@Composable
fun AlbumsPane(
    albums: List<AlbumGroup>,
    onAlbumClick: (AlbumGroup) -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    if (albums.isEmpty()) {
        PhotosLoadingOrEmpty(
            loading = loading,
            emptyMessage = stringResource(R.string.empty_albums),
            modifier = modifier,
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(albums, key = { it.bucketId }) { album ->
            AlbumTile(album = album, onClick = { onAlbumClick(album) })
        }
    }
}

@Composable
private fun AlbumTile(
    album: AlbumGroup,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color(0xFF333333))
            .clickable(onClick = onClick),
    ) {
        if (album.coverUri != null) {
            PhotoThumbnail(
                uri = album.coverUri,
                modifier = Modifier.fillMaxSize(),
            )
        }
        MetroText(
            text = album.name,
            style = MetroTextStyle.ListItemSubtitle,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
        )
    }
}

@Composable
fun FavoritesPane(
    photos: List<PhotoItem>,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    if (photos.isEmpty()) {
        PhotosLoadingOrEmpty(
            loading = loading,
            emptyMessage = stringResource(R.string.empty_favorites),
            modifier = modifier,
            extra = {
                MetroText(
                    text = stringResource(R.string.empty_favorites_hint),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            },
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(PhotoGridGutter),
        verticalArrangement = Arrangement.spacedBy(PhotoGridGutter),
    ) {
        items(photos, key = { it.id }) { photo ->
            PhotoThumbnail(
                uri = photo.uri,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onPhotoClick(photo) },
            )
        }
    }
}

@Composable
fun AlbumDetailScreen(
    album: AlbumGroup,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .metroNavBarPadding(),
    ) {
        MetroAppTitle(title = stringResource(R.string.photos_label))
        MetroText(
            text = album.name,
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(PhotoGridGutter),
            verticalArrangement = Arrangement.spacedBy(PhotoGridGutter),
        ) {
            items(album.photos, key = { it.id }) { photo ->
                PhotoThumbnail(
                    uri = photo.uri,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onPhotoClick(photo) },
                )
            }
        }
    }
}

@Composable
private fun PhotosLoadingOrEmpty(
    loading: Boolean,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    extra: (@Composable () -> Unit)? = null,
) {
    if (loading) {
        MetroLoadingScreen(modifier = modifier.fillMaxSize())
        return
    }
    if (extra == null) {
        MetroEmptyState(message = emptyMessage, modifier = modifier.fillMaxSize())
        return
    }
    Column(modifier = modifier.fillMaxSize()) {
        MetroEmptyState(message = emptyMessage)
        extra()
    }
}
