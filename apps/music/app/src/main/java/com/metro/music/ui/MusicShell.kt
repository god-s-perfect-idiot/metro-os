package com.metro.music.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.metro.music.ytmusic.YtMusicConnectActivity
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroJumpList
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicShell(state: MusicState) {
    val context = LocalContext.current
    val connectLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        state.refreshYtAuth()
    }

    // While a track is loaded the hub takes a darkened album-art colour, as WP8.1 faded the
    // artist image behind the panorama. Reference: `references/images/hub_nowplaying_compare_dark_unknown.jpg`.
    val pageBackground = MetroTheme.colors.background
    val backdrop = state.nowPlayingBackdrop.takeIf {
        state.route == MusicRoute.Hub && pageBackground.luminance() < 0.5f
    }
    val background by animateColorAsState(
        targetValue = backdrop ?: pageBackground,
        animationSpec = tween(durationMillis = MetroTransitions.PageTransitionMs),
        label = "hubBackdrop",
    )

    var showPlaybackLoader by remember { mutableStateOf(false) }
    LaunchedEffect(state.loadingPlayback) {
        if (state.loadingPlayback) {
            delay(400)
            showPlaybackLoader = true
        } else {
            showPlaybackLoader = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding()
            .background(background),
    ) {
        when (state.route) {
            MusicRoute.Hub -> {
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
                )
            }
            MusicRoute.Collection -> CollectionScreen(
                state = state,
                onBack = { state.route = MusicRoute.Hub },
            )
            MusicRoute.AlbumDetail -> {
                val album = state.selectedAlbum
                if (album == null) {
                    state.route = MusicRoute.Collection
                } else {
                    AlbumDetailScreen(
                        state = state,
                        album = album,
                        onBack = { state.route = MusicRoute.Collection },
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
                        onBack = { state.route = MusicRoute.Collection },
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
                        onBack = { state.route = MusicRoute.Collection },
                    )
                }
            }
            MusicRoute.Settings -> SettingsScreen(
                state = state,
                onBack = { state.route = MusicRoute.Hub },
                onConnect = {
                    connectLauncher.launch(Intent(context, YtMusicConnectActivity::class.java))
                },
            )
            MusicRoute.Explore -> ExploreScreen(
                state = state,
                onBack = { state.route = MusicRoute.Hub },
            )
        }

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

        if (showPlaybackLoader) {
            MetroLoadingScreen(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            )
        }
    }
}
