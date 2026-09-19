package com.metro.music.ui

import android.content.Intent
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.metro.ui.MetroTransitions
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** First dots need a short beat before they read as dancing (matches Start). */
private const val MIN_SPLASH_DOTS_VISIBLE_MS = 700L

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

    // While a track is loaded the hub takes a darkened album-art colour, as WP8.1 faded the
    // artist image behind the panorama. Reference: `references/images/hub_nowplaying_compare_dark_unknown.jpg`.
    val pageBackground = MetroTheme.colors.background
    val backdrop = state.nowPlayingBackdrop.takeIf {
        !coldSplashActive &&
            state.route == MusicRoute.Hub &&
            pageBackground.luminance() < 0.5f
    }
    val background by animateColorAsState(
        targetValue = backdrop ?: pageBackground,
        animationSpec = tween(durationMillis = MetroTransitions.PageTransitionMs),
        label = "hubBackdrop",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding()
            .background(background),
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
                        pageCount = { 3 },
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
                        MusicRoute.Explore -> ExploreScreen(
                            state = state,
                            onBack = { onBack() },
                        )
                        MusicRoute.Hub -> Unit
                    }
                },
            )

            val jumpListOpen = state.route == MusicRoute.Collection && state.jumpListVisible
            val appBarVisible = (state.route == MusicRoute.Hub || state.route == MusicRoute.Collection) &&
                !jumpListOpen
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

        if (coldSplashActive) {
            // Suite vector — not ic_launcher_foreground (layer-list; Compose cannot load it).
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
    -> MusicRoute.Collection
    MusicRoute.Collection,
    MusicRoute.Settings,
    MusicRoute.Explore,
    MusicRoute.Hub,
    -> MusicRoute.Hub
}

private fun subpageLoadKey(route: MusicRoute, state: MusicState): Any = when (route) {
    MusicRoute.Collection -> "Collection"
    MusicRoute.AlbumDetail -> "Album:${state.selectedAlbum?.id.orEmpty()}"
    MusicRoute.ArtistDetail -> "Artist:${state.selectedArtist?.id.orEmpty()}"
    MusicRoute.PlaylistDetail -> "Playlist:${state.selectedPlaylist?.id.orEmpty()}"
    MusicRoute.Settings -> "Settings"
    MusicRoute.Explore -> "Explore"
    MusicRoute.Hub -> "Hub"
}
