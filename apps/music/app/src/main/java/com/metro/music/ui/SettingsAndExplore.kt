package com.metro.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch

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
            modifier = Modifier.padding(horizontal = 12.dp),
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
fun ExploreScreen(state: MusicState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
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
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        BasicTextField(
            value = state.exploreQuery,
            onValueChange = { state.searchExplore(it) },
            singleLine = true,
            textStyle = MetroTextStyle.ListItemTitle.toTextStyle().copy(
                color = MetroTheme.colors.primaryText,
            ),
            cursorBrush = SolidColor(MetroTheme.colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .background(MetroTheme.colors.secondarySurface)
                .padding(12.dp),
            decorationBox = { inner ->
                if (state.exploreQuery.isEmpty()) {
                    MetroText(
                        text = "search youtube music",
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
                inner()
            },
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
