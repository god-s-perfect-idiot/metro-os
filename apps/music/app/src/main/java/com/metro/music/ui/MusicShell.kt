package com.metro.music.ui

import android.content.Intent
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.metro.music.ytmusic.ArtistAboutClient
import com.metro.music.ytmusic.YtMusicConnectActivity
import com.metro.ui.LocalMetroSubpageExit
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppGlyphs
import com.metro.ui.MetroJumpList
import com.metro.ui.MetroSplashLoadingScreen
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** First dots need a short beat before they read as dancing (matches Start). */
private const val MIN_SPLASH_DOTS_VISIBLE_MS = 700L

/** Dim the clear artist photo so white Metro chrome stays readable. */
private val HubArtScrim = Color.Black.copy(alpha = 0.55f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicShell(
    state: MusicState,
    onComposeSplashReady: () -> Unit = {},
) {
    val context = LocalContext.current
    val connectLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        state.refreshYtAuth()
    }

    // Cold start: keep accent splash + dots until MediaStore finishes so hub brand/panorama
    // enter does not fight library bind. Later syncs update in place (no splash).
    var coldSplashActive by remember { mutableStateOf(true) }
    // Panorama intro once per process — not when returning to hub in-app.
    var panoramaIntroPlayed by remember { mutableStateOf(false) }

    LaunchedEffect(coldSplashActive) {
        if (!coldSplashActive) return@LaunchedEffect
        onComposeSplashReady()
        withFrameNanos { }
        val visibleSince = SystemClock.elapsedRealtime()
        snapshotFlow { state.hasCompletedInitialLoad }.first { ready -> ready }
        val remaining = MIN_SPLASH_DOTS_VISIBLE_MS -
            (SystemClock.elapsedRealtime() - visibleSince)
        if (remaining > 0L) delay(remaining)
        coldSplashActive = false
    }

    // While a track is loaded the hub paints a clear artist photo full-bleed (dimmed), as
    // WP8.1 Xbox Music — not the album cover (that sits in the now-playing square).
    // Draw the wash edge-to-edge (under the status bar); only chrome/content is inset.
    val pageBackground = MetroTheme.colors.background
    val showArtBackdrop = !coldSplashActive &&
        state.route == MusicRoute.Hub &&
        pageBackground.luminance() < 0.5f &&
        state.nowPlayingBackdropArt != null
    // Keep the last URL so fade-out / Coil crossfade still have a model to paint.
    var retainedBackdropArt by remember { mutableStateOf<Any?>(null) }
    val incomingBackdropArt = state.nowPlayingBackdropArt
    SideEffect {
        if (incomingBackdropArt != null) {
            retainedBackdropArt = incomingBackdropArt
        }
    }
    val backdropAlpha by animateFloatAsState(
        targetValue = if (showArtBackdrop) 1f else 0f,
        animationSpec = tween(durationMillis = HubArtMorphMs, easing = EaseOutCubic),
        label = "hubBackdropAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground),
    ) {
        // Stable AsyncImage slot: Coil crossfades art→art; alpha fades presence in/out.
        // (Compose Crossfade + a new AsyncImage per key fought Coil and looked like a snap.)
        if (retainedBackdropArt != null && backdropAlpha > 0.001f) {
            val backdropRequest = remember(retainedBackdropArt) {
                ImageRequest.Builder(context)
                    .data(retainedBackdropArt)
                    // Wikipedia/Wikimedia require a descriptive User-Agent.
                    .addHeader("User-Agent", ArtistAboutClient.USER_AGENT)
                    .crossfade(HubArtMorphMs)
                    .build()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = backdropAlpha },
            ) {
                AsyncImage(
                    model = backdropRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HubArtScrim),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .metroNavBarPadding(),
        ) {
            if (!coldSplashActive) {
                MetroSubpageHost(
                    route = state.route,
                    isRoot = { it == MusicRoute.Hub },
                    parentOf = { it.parentRoute() },
                    loadKeyOf = { subpageLoadKey(it, state) },
                    onGoBack = { state.route = state.route.parentRoute() },
                    modifier = Modifier.fillMaxSize(),
                    rootContent = {
                        val pagerState = rememberPagerState(
                            initialPage = state.hubPage,
                            pageCount = { MusicState.HUB_PAGE_COUNT },
                        )
                        LaunchedEffect(pagerState.currentPage) {
                            state.hubPage = pagerState.currentPage
                        }
                        LaunchedEffect(state.hubPage) {
                            if (pagerState.currentPage != state.hubPage) {
                                pagerState.scrollToPage(state.hubPage)
                            }
                        }
                        MusicHub(
                            state = state,
                            pagerState = pagerState,
                            onOpenCollection = { pivot ->
                                state.openCollectionPivot(pivot)
                            },
                            onOpenExplore = {
                                state.route = MusicRoute.Explore
                            },
                            onOpenSettings = {
                                state.route = MusicRoute.Settings
                            },
                            onOpenRecent = {
                                state.route = MusicRoute.Recent
                            },
                            onOpenTopSongs = {
                                state.route = MusicRoute.TopSongs
                            },
                            onSyncNow = {
                                state.reloadLibrary()
                            },
                            onOpenMusicDirectories = {
                                state.route = MusicRoute.MusicDirectories
                            },
                            skipIntro = panoramaIntroPlayed,
                            onIntroPlayed = { panoramaIntroPlayed = true },
                        )
                    },
                    subpageContent = { route ->
                        val requestExit = LocalMetroSubpageExit.current
                        val onBack = { requestExit?.invoke() }
                        when (route) {
                            MusicRoute.Collection -> CollectionScreen(
                                state = state,
                                onBack = { onBack() },
                            )
                            MusicRoute.AlbumDetail -> {
                                val album = state.selectedAlbum
                                if (album == null) {
                                    state.route = MusicRoute.Collection
                                } else {
                                    AlbumDetailScreen(
                                        state = state,
                                        album = album,
                                        onBack = { onBack() },
                                    )
                                }
                            }
                            MusicRoute.ArtistDetail -> {
                                val artist = state.selectedArtist
                                if (artist == null) {
                                    state.route = MusicRoute.Collection
                                } else {
                                    ArtistDetailScreen(
                                        state = state,
                                        artist = artist,
                                        onBack = { onBack() },
                                    )
                                }
                            }
                            MusicRoute.GenreDetail -> {
                                val genre = state.selectedGenre
                                if (genre == null) {
                                    state.route = MusicRoute.Collection
                                } else {
                                    GenreDetailScreen(
                                        state = state,
                                        genre = genre,
                                        onBack = { onBack() },
                                    )
                                }
                            }
                            MusicRoute.PlaylistDetail -> {
                                val playlist = state.selectedPlaylist
                                if (playlist == null) {
                                    state.route = MusicRoute.Collection
                                } else {
                                    PlaylistDetailScreen(
                                        state = state,
                                        playlist = playlist,
                                        onBack = { onBack() },
                                    )
                                }
                            }
                            MusicRoute.Settings -> SettingsScreen(
                                state = state,
                                onBack = { onBack() },
                                onConnect = {
                                    connectLauncher.launch(
                                        Intent(context, YtMusicConnectActivity::class.java),
                                    )
                                },
                            )
                            MusicRoute.MusicDirectories -> MusicDirectoriesScreen(
                                state = state,
                                onBack = { onBack() },
                            )
                            MusicRoute.Explore -> ExploreScreen(
                                state = state,
                                onBack = { onBack() },
                            )
                            MusicRoute.Recent -> RecentScreen(
                                state = state,
                                onBack = { onBack() },
                            )
                            MusicRoute.TopSongs -> TopSongsScreen(
                                state = state,
                                onBack = { onBack() },
                            )
                            MusicRoute.Queue -> QueueScreen(
                                songs = state.playbackQueue,
                                currentSongId = state.currentSong?.id,
                                onSongSelected = { index ->
                                    state.playQueueIndex(index)
                                },
                                onBack = { onBack() },
                            )
                            MusicRoute.Hub -> Unit
                        }
                    },
                )

                val jumpListOpen = state.route == MusicRoute.Collection && state.jumpListVisible
                // Hub (homescreen) has no ApplicationBar — collection keeps the search/… chrome.
                val appBarVisible = state.route == MusicRoute.Collection && !jumpListOpen
                MetroAppBar(
                    visible = appBarVisible,
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Search,
                            label = "search",
                            onClick = { state.route = MusicRoute.Explore },
                        ),
                    ),
                    menuItems = listOf(
                        MetroAppBarMenuItem("collection") {
                            state.route = MusicRoute.Collection
                        },
                        MetroAppBarMenuItem("settings") {
                            state.route = MusicRoute.Settings
                        },
                        MetroAppBarMenuItem("sync now") {
                            state.reloadLibrary()
                        },
                        MetroAppBarMenuItem("get music") {
                            state.hubPage = MusicState.HUB_GET_MUSIC
                            state.route = MusicRoute.Hub
                        },
                        MetroAppBarMenuItem("local") {
                            state.hubPage = MusicState.HUB_LOCAL
                            state.route = MusicRoute.Hub
                        },
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                // Hosted here so the grid covers the app bar, as in WP8.1.
                if (jumpListOpen) {
                    MetroJumpList(
                        activeLetters = state.collectionJumpLetters,
                        onLetterSelected = { state.jumpToLetter = it },
                        onDismiss = { state.jumpListVisible = false },
                    )
                }
            }
        }

        if (coldSplashActive) {
            // Suite vector — not ic_launcher_foreground (layer-list; Compose cannot load it).
            // Full-bleed like the album wash — under the status bar, not inset with chrome.
            MetroSplashLoadingScreen(
                icon = painterResource(id = MetroAppGlyphs.Music),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun MusicRoute.parentRoute(): MusicRoute = when (this) {
    MusicRoute.AlbumDetail,
    MusicRoute.ArtistDetail,
    MusicRoute.PlaylistDetail,
    MusicRoute.GenreDetail,
    -> MusicRoute.Collection
    MusicRoute.Collection,
    MusicRoute.Settings,
    MusicRoute.MusicDirectories,
    MusicRoute.Explore,
    MusicRoute.Recent,
    MusicRoute.TopSongs,
    MusicRoute.Queue,
    MusicRoute.Hub,
    -> MusicRoute.Hub
}

private fun subpageLoadKey(route: MusicRoute, state: MusicState): Any = when (route) {
    MusicRoute.Collection -> "Collection"
    MusicRoute.AlbumDetail -> "Album:${state.selectedAlbum?.id.orEmpty()}"
    MusicRoute.ArtistDetail -> "Artist:${state.selectedArtist?.id.orEmpty()}"
    MusicRoute.PlaylistDetail -> "Playlist:${state.selectedPlaylist?.id.orEmpty()}"
    MusicRoute.GenreDetail -> "Genre:${state.selectedGenre?.id.orEmpty()}"
    MusicRoute.Settings -> "Settings"
    MusicRoute.MusicDirectories -> "MusicDirectories"
    MusicRoute.Explore -> "Explore"
    MusicRoute.Recent -> "Recent"
    MusicRoute.TopSongs -> "TopSongs"
    MusicRoute.Queue -> "Queue"
    MusicRoute.Hub -> "Hub"
}
