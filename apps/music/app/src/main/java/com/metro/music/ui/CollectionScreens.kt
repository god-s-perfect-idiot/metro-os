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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.metro.music.data.Album
import com.metro.music.data.Artist
import com.metro.music.data.ArtistAboutLogic
import com.metro.music.data.Genre
import com.metro.music.data.LibraryLogic
import com.metro.music.data.Playlist
import com.metro.music.data.ShowingFilter
import com.metro.music.data.Song
import com.metro.music.ytmusic.ArtistAboutClient
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroLoadingScreen
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
                MusicState.COLLECTION_PLAYLISTS ->
                    PlaylistsList(state, jumpTarget, consumeJump, openJumpList)
                MusicState.COLLECTION_GENRES ->
                    GenresList(state, jumpTarget, consumeJump, openJumpList)
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
        LoadingOrEmpty(loading = isLibraryPageLoading(state), emptyMessage = "No artists.")
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
            subtitle = "${artist.songCount} songs discovered",
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
        LoadingOrEmpty(loading = isLibraryPageLoading(state), emptyMessage = "No albums.")
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
        LoadingOrEmpty(loading = isLibraryPageLoading(state), emptyMessage = "No songs.")
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

@Composable
private fun PlaylistsList(
    state: MusicState,
    jumpTarget: Char?,
    onJumpConsumed: () -> Unit,
    onOpenJumpList: () -> Unit,
) {
    val playlists = state.playlists
    if (playlists.isEmpty()) {
        when {
            isLibraryPageLoading(state) -> MetroLoadingScreen()
            state.showingFilter == ShowingFilter.YouTubeMusic && !state.ytConnected ->
                PlaceholderList("Connect YouTube Music in settings.")
            else -> PlaceholderList("No playlists.")
        }
        return
    }
    MusicLetterList(
        items = playlists,
        labelOf = { it.title },
        keyOf = { it.id },
        jumpTarget = jumpTarget,
        onJumpTargetConsumed = onJumpConsumed,
        onLetterMarkerClick = onOpenJumpList,
    ) { playlist ->
        MusicListRow(
            title = playlist.title,
            subtitle = if (playlist.songCount > 0) {
                "${playlist.songCount} songs"
            } else {
                null
            },
            onClick = { state.openPlaylist(playlist) },
        )
    }
}

@Composable
private fun GenresList(
    state: MusicState,
    jumpTarget: Char?,
    onJumpConsumed: () -> Unit,
    onOpenJumpList: () -> Unit,
) {
    val genres = state.genres
    if (genres.isEmpty()) {
        when {
            isLibraryPageLoading(state) -> MetroLoadingScreen()
            state.showingFilter == ShowingFilter.YouTubeMusic ->
                PlaceholderList("YouTube Music library has no genre tags.")
            else -> PlaceholderList("No genres.")
        }
        return
    }
    MusicLetterList(
        items = genres,
        labelOf = { it.name },
        keyOf = { it.id },
        jumpTarget = jumpTarget,
        onJumpTargetConsumed = onJumpConsumed,
        onLetterMarkerClick = onOpenJumpList,
    ) { genre ->
        MusicListRow(
            title = genre.name,
            subtitle = "${genre.songCount} songs",
            onClick = { state.openGenre(genre) },
        )
    }
}

/** Track order list for album / artist / playlist detail — no letter grouping (§6.18 applies to pivots). */
@Composable
fun SongsList(state: MusicState, songs: List<Song>) {
    if (songs.isEmpty()) {
        PlaceholderList("No songs.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(songs, key = { index, song -> "${song.id}#$index" }) { index, song ->
            MusicListRow(
                title = song.title,
                subtitle = song.artist,
                onClick = { state.playSongs(songs, index) },
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
private fun LoadingOrEmpty(loading: Boolean, emptyMessage: String) {
    if (loading) {
        MetroLoadingScreen()
    } else {
        PlaceholderList(emptyMessage)
    }
}

/** True while the filtered library source is still scanning and the list has nothing to show. */
private fun isLibraryPageLoading(state: MusicState): Boolean {
    val waitingLocal = state.libraryLoading && state.showingFilter != ShowingFilter.YouTubeMusic
    val waitingYt = state.ytSyncing && state.ytConnected && state.showingFilter != ShowingFilter.OnDevice
    return waitingLocal || waitingYt
}

@Composable
fun AlbumDetailScreen(state: MusicState, album: Album, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val songs = state.songsForAlbumDetail(album)
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
            modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(Modifier.height(12.dp))
        when {
            state.albumRemoteLoading && songs.isEmpty() ->
                MetroLoadingScreen(modifier = Modifier.weight(1f))
            songs.isEmpty() -> PlaceholderList("No songs.")
            else -> SongsList(state, songs)
        }
    }
}

@Composable
fun PlaylistDetailScreen(state: MusicState, playlist: Playlist, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(bottom = 24.dp),
    ) {
        MetroAppTitle("MUSIC")
        MetroText(
            text = playlist.title,
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(Modifier.height(12.dp))
        when {
            state.playlistLoading && state.playlistSongs.isEmpty() ->
                MetroLoadingScreen(modifier = Modifier.weight(1f))
            state.playlistSongs.isEmpty() -> PlaceholderList("No songs.")
            else -> SongsList(state, state.playlistSongs)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistDetailScreen(state: MusicState, artist: Artist, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(bottom = 24.dp),
    ) {
        MetroPivot(
            titles = listOf("songs", "albums", "about"),
            pagerState = pagerState,
            header = { MetroAppTitle(artist.name.uppercase()) },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> ArtistSongsPage(state, artist)
                1 -> ArtistAlbumsPage(state, artist)
                else -> ArtistAboutPage(state)
            }
        }
    }
}

@Composable
private fun ArtistSongsPage(state: MusicState, artist: Artist) {
    val collection = state.songsForArtist(artist)
    val discover = state.artistDiscoverSongs
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "songs-in-collection-header") {
            AccentSectionHeader("in collection")
        }
        if (collection.isEmpty()) {
            item(key = "songs-in-collection-empty") {
                SectionEmpty("No songs in your collection.")
            }
        } else {
            itemsIndexed(collection, key = { index, song -> "c-${song.id}#$index" }) { index, song ->
                MusicListRow(
                    title = song.title,
                    subtitle = song.album.takeIf { it.isNotBlank() },
                    onClick = { state.playSongs(collection, index) },
                )
            }
        }
        item(key = "songs-discover-header") {
            AccentSectionHeader("discover")
        }
        when {
            state.artistDiscoverLoading && discover.isEmpty() -> {
                item(key = "songs-discover-loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        MetroLoadingScreen()
                    }
                }
            }
            !state.ytConnected && discover.isEmpty() && !state.artistDiscoverLoading -> {
                item(key = "songs-discover-connect") {
                    SectionEmpty("Connect YouTube Music in settings to play discovered songs.")
                }
            }
            discover.isEmpty() -> {
                item(key = "songs-discover-empty") {
                    SectionEmpty("Nothing new to discover right now.")
                }
            }
            else -> {
                itemsIndexed(discover, key = { index, song -> "d-${song.id}#$index" }) { index, song ->
                    MusicListRow(
                        title = song.title,
                        subtitle = song.artist,
                        onClick = { state.playSongs(discover, index) },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ArtistAlbumsPage(state: MusicState, artist: Artist) {
    val collection = state.albumsForArtist(artist)
    val discover = state.artistDiscoverAlbums
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "albums-in-collection-header") {
            AccentSectionHeader("in collection")
        }
        if (collection.isEmpty()) {
            item(key = "albums-in-collection-empty") {
                SectionEmpty("No albums in your collection.")
            }
        } else {
            items(collection, key = { it.id }) { album ->
                MusicListRow(
                    title = album.title,
                    subtitle = "${album.songCount} songs",
                    onClick = { state.openAlbum(album) },
                )
            }
        }
        item(key = "albums-discover-header") {
            AccentSectionHeader("discover")
        }
        when {
            state.artistDiscoverLoading && discover.isEmpty() -> {
                item(key = "albums-discover-loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        MetroLoadingScreen()
                    }
                }
            }
            !state.ytConnected && discover.isEmpty() && !state.artistDiscoverLoading -> {
                item(key = "albums-discover-connect") {
                    SectionEmpty("Connect YouTube Music in settings to open discovered albums.")
                }
            }
            discover.isEmpty() -> {
                item(key = "albums-discover-empty") {
                    SectionEmpty("Nothing new to discover right now.")
                }
            }
            else -> {
                items(discover, key = { it.id }) { album ->
                    MusicListRow(
                        title = album.title,
                        subtitle = album.artist,
                        onClick = { state.openAlbum(album) },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ArtistAboutPage(state: MusicState) {
    when {
        state.artistAboutLoading && state.artistAbout == null -> MetroLoadingScreen()
        else -> {
            val about = state.artistAbout
            if (about == null ||
                (about.error != null && about.summary == null && about.paragraphs.isEmpty())
            ) {
                PlaceholderList(about?.error ?: "No information available.")
                return
            }
            val context = LocalContext.current
            val heroUrl = ArtistAboutLogic.heroImageUrl(about)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
            ) {
                heroUrl?.let { url ->
                    item(key = "about-image") {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(url)
                                .addHeader("User-Agent", ArtistAboutClient.USER_AGENT)
                                .crossfade(true)
                                .build(),
                            contentDescription = about.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 16.dp),
                            contentScale = ContentScale.FillWidth,
                        )
                    }
                }
                about.description?.let { desc ->
                    item(key = "about-desc") {
                        MetroText(
                            text = desc,
                            style = MetroTextStyle.Body,
                            color = MetroTheme.colors.secondaryText,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
                val facts = ArtistAboutLogic.factLines(about)
                if (facts.isNotEmpty()) {
                    item(key = "about-facts") {
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            facts.forEach { (label, value) ->
                                MetroText(
                                    text = label,
                                    style = MetroTextStyle.SectionHeader,
                                    color = MetroTheme.colors.accent,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                                )
                                MetroText(
                                    text = value,
                                    style = MetroTextStyle.Body,
                                )
                            }
                        }
                    }
                }
                val lead = about.paragraphs.ifEmpty {
                    listOfNotNull(about.summary)
                }
                lead.forEachIndexed { index, paragraph ->
                    item(key = "about-lead-$index") {
                        MetroText(
                            text = paragraph,
                            style = MetroTextStyle.Body,
                            modifier = Modifier.padding(bottom = 14.dp),
                        )
                    }
                }
                about.sections.forEach { section ->
                    item(key = "about-section-${section.title}") {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            MetroText(
                                text = section.title.lowercase(),
                                style = MetroTextStyle.SectionHeader,
                                color = MetroTheme.colors.accent,
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                            )
                            section.paragraphs.forEach { paragraph ->
                                MetroText(
                                    text = paragraph,
                                    style = MetroTextStyle.Body,
                                    modifier = Modifier.padding(bottom = 12.dp),
                                )
                            }
                        }
                    }
                }
                about.error?.takeIf {
                    about.summary != null || about.paragraphs.isNotEmpty()
                }?.let { err ->
                    item(key = "about-err") {
                        MetroText(
                            text = err,
                            style = MetroTextStyle.Body,
                            color = MetroTheme.colors.secondaryText,
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun AccentSectionHeader(label: String) {
    MetroText(
        text = label,
        style = MetroTextStyle.SectionHeader,
        color = MetroTheme.colors.accent,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun SectionEmpty(message: String) {
    MetroText(
        text = message,
        style = MetroTextStyle.Body,
        color = MetroTheme.colors.secondaryText,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
fun GenreDetailScreen(state: MusicState, genre: Genre, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val songs = state.songsForGenre(genre)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(bottom = 24.dp),
    ) {
        MetroAppTitle("MUSIC")
        MetroText(
            text = genre.name,
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(Modifier.height(12.dp))
        SongsList(state, songs)
    }
}
