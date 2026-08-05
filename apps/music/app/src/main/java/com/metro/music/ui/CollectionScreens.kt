package com.metro.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.metro.music.data.Album
import com.metro.music.data.Artist
import com.metro.music.data.LibraryLogic
import com.metro.music.data.ShowingFilter
import com.metro.music.data.Song
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroPivot
import com.metro.ui.MetroShowingLabel
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionScreen(state: MusicState, onBack: () -> Unit) {
    var showFilterPicker by remember { mutableStateOf(false) }
    BackHandler(enabled = !showFilterPicker, onBack = onBack)
    val pagerState = rememberPagerState(initialPage = state.collectionPage, pageCount = { 5 })
    LaunchedEffect(pagerState.currentPage) {
        state.collectionPage = pagerState.currentPage
    }
    LaunchedEffect(state.collectionPage) {
        if (pagerState.currentPage != state.collectionPage) {
            pagerState.scrollToPage(state.collectionPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(bottom = 72.dp),
    ) {
        MetroAppTitle("MUSIC")
        MetroPivot(
            titles = listOf("artists", "albums", "songs", "playlists", "genres"),
            pagerState = pagerState,
            belowTitleRow = {
                MetroShowingLabel(
                    label = LibraryLogic.showingLabel(state.showingFilter),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    onClick = { showFilterPicker = true },
                )
            },
        ) { page ->
            // Adjacent pivot pages stay composed, so only the visible one may consume a jump.
            val jumpTarget = state.jumpToLetter.takeIf { pagerState.currentPage == page }
            val openJumpList = { state.jumpListVisible = true }
            val consumeJump = { state.jumpToLetter = null }
            when (page) {
                MusicState.COLLECTION_ARTISTS ->
                    ArtistsList(state, jumpTarget, consumeJump, openJumpList)
                MusicState.COLLECTION_ALBUMS ->
                    AlbumsList(state, jumpTarget, consumeJump, openJumpList)
                MusicState.COLLECTION_SONGS ->
                    CollectionSongsList(state, jumpTarget, consumeJump, openJumpList)
                MusicState.COLLECTION_PLAYLISTS -> PlaceholderList("No playlists yet.")
                else -> PlaceholderList("No genres yet.")
            }
        }
    }

    if (showFilterPicker) {
        ShowingFilterPicker(
            current = state.showingFilter,
            onDismiss = { showFilterPicker = false },
            onSelect = {
                state.showingFilter = it
                showFilterPicker = false
            },
        )
    }
}

/**
 * Page 7 — showing filter menu. `FILTER BY:` header, tight option stack with the active filter in
 * accent, and a bordered cancel button. Rows turnstile in from the left edge on open.
 * Reference: `references/images/showing_menu_dark_teal.jpg`.
 */
@Composable
private fun ShowingFilterPicker(
    current: ShowingFilter,
    onDismiss: () -> Unit,
    onSelect: (ShowingFilter) -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val options = listOf(
        ShowingFilter.All to "all music",
        ShowingFilter.OnDevice to "on this device",
        ShowingFilter.YouTubeMusic to "youtube music",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(horizontal = 12.dp, vertical = 24.dp),
    ) {
        TurnstileIn(index = 0) {
            MetroText(text = "FILTER BY:", style = MetroTextStyle.SectionHeader)
        }
        Spacer(Modifier.height(20.dp))
        options.forEachIndexed { index, (filter, label) ->
            TurnstileIn(index = index + 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(filter) },
                        )
                        .defaultMinSize(minHeight = 44.dp)
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    MetroText(
                        text = label,
                        style = MetroTextStyle.ListItemTitle,
                        color = if (filter == current) {
                            MetroTheme.colors.accent
                        } else {
                            MetroTheme.colors.primaryText
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        TurnstileIn(index = options.size + 1) {
            MetroBorderButton(text = "cancel", onClick = onDismiss)
        }
    }
}

/** WP8.1 turnstile feather-in — rotate around the left edge, staggered per row. */
private const val TurnstileStartDegrees = -70f
private const val TurnstileStaggerMs = 45L
private const val TurnstileCameraDistance = 20f

@Composable
private fun TurnstileIn(index: Int, content: @Composable () -> Unit) {
    val rotationY = remember { Animatable(TurnstileStartDegrees) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * TurnstileStaggerMs)
        launch { alpha.animateTo(1f, MetroTransitions.pivotTween()) }
        rotationY.animateTo(0f, MetroTransitions.pageTween())
    }
    Box(
        modifier = Modifier.graphicsLayer {
            this.rotationY = rotationY.value
            this.alpha = alpha.value
            transformOrigin = TransformOrigin(0f, 0.5f)
            cameraDistance = TurnstileCameraDistance * density
        },
    ) {
        content()
    }
}

@Composable
private fun ArtistsList(
    state: MusicState,
    jumpTarget: Char?,
    onJumpConsumed: () -> Unit,
    onOpenJumpList: () -> Unit,
) {
    val artists = state.artists
    if (artists.isEmpty()) {
        PlaceholderList("No artists.")
        return
    }
    MusicLetterList(
        items = artists,
        labelOf = { it.name },
        keyOf = { it.id },
        jumpTarget = jumpTarget,
        onJumpTargetConsumed = onJumpConsumed,
        onLetterMarkerClick = onOpenJumpList,
    ) { artist ->
        MusicListRow(
            title = artist.name,
            subtitle = "${artist.songCount} songs",
            onClick = { state.openArtist(artist) },
        )
    }
}

@Composable
private fun AlbumsList(
    state: MusicState,
    jumpTarget: Char?,
    onJumpConsumed: () -> Unit,
    onOpenJumpList: () -> Unit,
) {
    val albums = state.albums
    if (albums.isEmpty()) {
        PlaceholderList("No albums.")
        return
    }
    MusicLetterList(
        items = albums,
        labelOf = { it.title },
        keyOf = { it.id },
        jumpTarget = jumpTarget,
        onJumpTargetConsumed = onJumpConsumed,
        onLetterMarkerClick = onOpenJumpList,
    ) { album ->
        MusicListRow(
            title = album.title,
            subtitle = album.artist,
            onClick = { state.openAlbum(album) },
        )
    }
}

@Composable
private fun CollectionSongsList(
    state: MusicState,
    jumpTarget: Char?,
    onJumpConsumed: () -> Unit,
    onOpenJumpList: () -> Unit,
) {
    val songs = state.visibleSongs
    if (songs.isEmpty()) {
        PlaceholderList("No songs.")
        return
    }
    MusicLetterList(
        items = songs,
        labelOf = { it.title },
        keyOf = { it.id },
        jumpTarget = jumpTarget,
        onJumpTargetConsumed = onJumpConsumed,
        onLetterMarkerClick = onOpenJumpList,
    ) { song ->
        MusicListRow(
            title = song.title,
            subtitle = song.artist,
            onClick = { state.playSongs(songs, songs.indexOf(song).coerceAtLeast(0)) },
        )
    }
}

/** Track order list for album / artist detail — no letter grouping (§6.18 applies to pivots). */
@Composable
fun SongsList(state: MusicState, songs: List<Song>) {
    if (songs.isEmpty()) {
        PlaceholderList("No songs.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(songs, key = { it.id }) { song ->
            MusicListRow(
                title = song.title,
                subtitle = song.artist,
                onClick = { state.playSongs(songs, songs.indexOf(song).coerceAtLeast(0)) },
            )
        }
    }
}

@Composable
fun PlaceholderList(message: String) {
    Column(modifier = Modifier.padding(24.dp)) {
        MetroText(
            text = message,
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
        )
    }
}

@Composable
fun AlbumDetailScreen(state: MusicState, album: Album, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val songs = state.songsForAlbum(album)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(bottom = 24.dp),
    ) {
        MetroAppTitle(album.artist.uppercase())
        MetroText(
            text = album.title,
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(12.dp))
        SongsList(state, songs)
    }
}

@Composable
fun ArtistDetailScreen(state: MusicState, artist: Artist, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val songs = state.songsForArtist(artist)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(bottom = 24.dp),
    ) {
        MetroAppTitle(artist.name.uppercase())
        MetroText(
            text = "songs",
            style = MetroTextStyle.HubTitle,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(12.dp))
        SongsList(state, songs)
    }
}
