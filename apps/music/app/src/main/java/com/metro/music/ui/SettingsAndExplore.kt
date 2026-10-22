package com.metro.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.metro.music.data.Song
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroMultiSelectDefaults
import com.metro.ui.MetroMultiSelectItem
import com.metro.ui.MetroMultiSelectList
import com.metro.ui.MetroPivot
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.MetroSystemIconType
import com.metro.ui.drawMetroSystemIconGlyph
@Composable
fun SettingsScreen(
    state: MusicState,
    onBack: () -> Unit,
    onConnect: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(bottom = 24.dp),
    ) {
        MetroAppTitle("SETTINGS")
        MetroText(
            text = "music",
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        MetroText(
            text = "Connect to YouTube Music",
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        MetroToggleSwitch(
            checked = state.ytConnected,
            onCheckedChange = { checked ->
                if (checked) onConnect() else state.disconnectYt()
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        if (state.ytSyncMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            MetroText(
                text = state.ytSyncMessage.orEmpty(),
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        MetroBorderButton(
            text = "YouTube Music account",
            onClick = onConnect,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        MetroBorderButton(
            text = if (state.ytSyncing) "syncing…" else "sync now",
            onClick = { state.refreshYtLibrary() },
            enabled = state.ytConnected && !state.ytSyncing,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
fun RecentScreen(state: MusicState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(bottom = 24.dp),
    ) {
        MetroAppTitle("MUSIC")
        MetroText(
            text = "recent",
            style = MetroTextStyle.HubTitle,
            modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (state.recentSongs.isEmpty()) {
            MetroText(
                text = "Nothing played yet",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        } else {
            LazyColumn {
                items(state.recentSongs, key = { it.id }) { song ->
                    MusicListRow(
                        title = song.title,
                        subtitle = song.artist,
                        onClick = {
                            state.playSongs(
                                listOf(song) + state.recentSongs.filter { it.id != song.id },
                                0,
                            )
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopSongsScreen(state: MusicState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val pagerState = rememberPagerState(pageCount = { 2 })
    LaunchedEffect(Unit) {
        state.loadTopSongs()
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1) {
            state.loadBillboard100()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(bottom = 24.dp),
    ) {
        MetroAppTitle("MUSIC")
        MetroPivot(
            titles = listOf("top songs", "billboard 100"),
            pagerState = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (page) {
                0 -> ChartSongsPane(
                    songs = state.topSongs,
                    loading = state.topSongsLoading,
                    error = state.topSongsError,
                    emptyMessage = "No top songs available",
                    onPlay = { songs, index -> state.playSongs(songs, index) },
                )
                else -> ChartSongsPane(
                    songs = state.billboardSongs,
                    loading = state.billboardLoading,
                    error = state.billboardError,
                    emptyMessage = "No Billboard songs available",
                    onPlay = { songs, index -> state.playSongs(songs, index) },
                )
            }
        }
    }
}

@Composable
private fun ChartSongsPane(
    songs: List<Song>,
    loading: Boolean,
    error: String?,
    emptyMessage: String,
    onPlay: (List<Song>, Int) -> Unit,
) {
    when {
        loading && songs.isEmpty() -> {
            MetroLoadingScreen(modifier = Modifier.fillMaxSize())
        }
        songs.isEmpty() -> {
            MetroText(
                text = error ?: emptyMessage,
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        else -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    MusicStreamingSongRow(
                        title = song.title,
                        artist = song.artist,
                        onClick = { onPlay(songs, index) },
                    )
                }
            }
        }
    }
}

@Composable
fun ExploreScreen(state: MusicState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        searchFocus.requestFocus()
        keyboard?.show()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(bottom = 24.dp),
    ) {
        MetroAppTitle("MUSIC")
        MetroText(
            text = "explore",
            style = MetroTextStyle.HubTitle,
            modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetroTextBox(
            value = state.exploreQuery,
            onValueChange = { state.searchExplore(it) },
            placeholder = "search",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .focusRequester(searchFocus),
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (!state.ytConnected) {
            MetroText(
                text = "Sign in from settings for library sync. Search still works as guest when available.",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        if (state.exploreLoading && state.exploreResults.isEmpty()) {
            MetroLoadingScreen(modifier = Modifier.weight(1f))
        } else {
            LazyColumn {
                items(state.exploreResults, key = { it.id }) { song ->
                    MusicListRow(
                        title = song.title,
                        subtitle = song.artist,
                        onClick = {
                            val q = state.exploreResults
                            state.playSongs(q, q.indexOf(song).coerceAtLeast(0))
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionScreen(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(24.dp),
    ) {
        MetroAppTitle("MUSIC")
        MetroText(text = "music library access", style = MetroTextStyle.PageTitle)
        Spacer(modifier = Modifier.height(16.dp))
        MetroText(
            text = "Music needs access to audio files on this device to build your collection.",
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
        )
        Spacer(modifier = Modifier.height(24.dp))
        MetroBorderButton(text = "allow access", onClick = onGrant)
    }
}

/**
 * Checkbox list of MediaStore folders that contribute local tracks — same pattern as
 * Settings → connected apps picker ([MetroMultiSelectList]).
 */
@Composable
fun MusicDirectoriesScreen(
    state: MusicState,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val directories = state.musicDirectories
    val committedSelected = remember(directories, state.excludedMusicDirectoryIds) {
        directories.map { it.id }.toSet() - state.excludedMusicDirectoryIds
    }
    var draft by remember { mutableStateOf(committedSelected) }
    LaunchedEffect(committedSelected) {
        draft = committedSelected
    }
    val items = remember(directories) {
        directories.map { dir ->
            val countLabel = if (dir.songCount == 1) "1 song" else "${dir.songCount} songs"
            MetroMultiSelectItem(
                id = dir.id,
                title = "${dir.title} · $countLabel",
            )
        }
    }

    if (directories.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MetroTheme.colors.background)
                .padding(bottom = 24.dp),
        ) {
            MetroAppTitle("FOLDERS")
            MetroText(
                text = "music directories",
                style = MetroTextStyle.PageTitle,
                modifier = Modifier.padding(start = 12.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            when {
                state.libraryLoading -> {
                    MetroLoadingScreen(modifier = Modifier.weight(1f))
                }
                !state.hasAudioPermission -> {
                    MetroText(
                        text = "Allow music access to see folders on this device.",
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
                else -> {
                    MetroText(
                        text = "No music folders found on this device.",
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        }
        return
    }

    MetroMultiSelectList(
        title = "folders",
        items = items,
        selectedIds = draft,
        onSelectionChange = { draft = it },
        onConfirm = {
            state.applyMusicDirectorySelection(draft)
            onBack()
        },
        onCancel = onBack,
        itemLeading = {
            // Folder chrome glyph is sized for app-bar rings (~0.42); boost to fill the
            // 40dp leading slot like connected-app tiles.
            val color = MetroTheme.colors.primaryText
            Canvas(modifier = Modifier.size(MetroMultiSelectDefaults.LeadingSize)) {
                val boost = 0.85f / 0.42f
                withTransform({
                    scale(scaleX = boost, scaleY = boost, pivot = center)
                }) {
                    drawMetroSystemIconGlyph(MetroSystemIconType.Folder, color)
                }
            }
        },
        confirmLabel = "done",
        cancelLabel = "cancel",
    )
}
