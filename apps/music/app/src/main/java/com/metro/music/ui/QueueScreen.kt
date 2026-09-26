package com.metro.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.metro.music.data.LibrarySource
import com.metro.music.data.QueueLogic
import com.metro.music.data.Song
import com.metro.ui.MetroAppPickerDefaults
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroListItem
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/**
 * Full-page queue list — same chrome as [com.metro.ui.MetroAppPickerScreen]
 * (secondary surface, small-caps header, dense single-line rows). Current track
 * uses accent; tap jumps to that song and the parent pops back.
 */
@Composable
fun QueueScreen(
    songs: List<Song>,
    currentSongId: String?,
    onSongSelected: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val currentIndex = QueueLogic.indexOfSong(songs, currentSongId).coerceAtLeast(0)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentIndex.coerceAtMost(songs.lastIndex.coerceAtLeast(0)),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.secondarySurface),
    ) {
        MetroAppTitle(title = "queue")
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                items = songs,
                key = { index, song -> "${song.id}#$index" },
            ) { index, song ->
                val selected = song.id == currentSongId
                MetroListItem(
                    title = song.title,
                    titleStyle = MetroTextStyle.ListItemTitle,
                    singleLine = true,
                    oneLineMinHeight = MetroAppPickerDefaults.RowMinHeight,
                    verticalPadding = MetroAppPickerDefaults.RowVerticalPadding,
                    onClick = { onSongSelected(index) },
                    titleColor = if (selected) {
                        MetroTheme.colors.accent
                    } else {
                        MetroTheme.colors.primaryText
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F, widthDp = 360, heightDp = 640)
@Composable
private fun QueueScreenPreview() {
    val songs = listOf(
        Song(
            id = "1",
            title = "Get Lucky",
            artist = "Daft Punk",
            album = "RAM",
            durationMs = 248_000,
            uri = null,
            artworkUri = null,
            source = LibrarySource.Local,
        ),
        Song(
            id = "2",
            title = "Instant Crush",
            artist = "Daft Punk",
            album = "RAM",
            durationMs = 337_000,
            uri = null,
            artworkUri = null,
            source = LibrarySource.Local,
        ),
        Song(
            id = "3",
            title = "Lose Yourself to Dance",
            artist = "Daft Punk",
            album = "RAM",
            durationMs = 353_000,
            uri = null,
            artworkUri = null,
            source = LibrarySource.Local,
        ),
    )
    MetroTheme(darkTheme = true) {
        QueueScreen(
            songs = songs,
            currentSongId = "2",
            onSongSelected = {},
            onBack = {},
        )
    }
}
