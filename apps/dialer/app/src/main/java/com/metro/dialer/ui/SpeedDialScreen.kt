package com.metro.dialer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.dialer.data.SpeedDialEntry
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpeedDialScreen(
    entries: List<SpeedDialEntry>,
    onCall: (SpeedDialEntry) -> Unit,
    onRemove: (SpeedDialEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        EmptyPane(
            message = stringResource(R.string.speed_dial_empty),
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 12.dp),
    ) {
        items(entries, key = { it.id }) { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 76.dp)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onCall(entry) },
                        onLongClick = { onRemove(entry) },
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpeedDialAvatar(name = entry.displayName)
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    MetroText(
                        text = entry.displayName,
                        style = MetroTextStyle.ListItemTitle,
                    )
                    MetroText(
                        text = entry.phoneNumber,
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
            }
            ListDivider()
        }
    }
}

@Composable
private fun SpeedDialAvatar(name: String, modifier: Modifier = Modifier) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
    Box(
        modifier = modifier
            .size(48.dp)
            .background(MetroTheme.colors.accent),
        contentAlignment = Alignment.Center,
    ) {
        MetroText(
            text = initial,
            style = MetroTextStyle.ListItemTitle,
            color = Color.White,
        )
    }
}
