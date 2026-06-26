package com.metro.messaging.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.messaging.R
import com.metro.messaging.data.ConversationThread
import com.metro.messaging.data.MessagingLogic
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun ThreadListScreen(
    threads: List<ConversationThread>,
    usingDemoData: Boolean,
    onOpenThread: (ConversationThread) -> Unit,
    onDeleteThread: (ConversationThread) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (threads.isEmpty()) {
        EmptyPane(
            message = stringResource(R.string.threads_empty),
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
    ) {
        if (usingDemoData) {
            item(key = "demo-banner") {
                MetroText(
                    text = stringResource(R.string.demo_data_banner),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
        items(threads, key = { it.id }) { thread ->
            ThreadRow(
                thread = thread,
                onOpenThread = { onOpenThread(thread) },
                onDeleteThread = { onDeleteThread(thread) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThreadRow(
    thread: ConversationThread,
    onOpenThread: () -> Unit,
    onDeleteThread: () -> Unit,
) {
    val titleColor = if (thread.unreadCount > 0) {
        MetroTheme.colors.accent
    } else {
        MetroTheme.colors.primaryText
    }
    val subtitle = buildString {
        append(MessagingLogic.previewText(thread.preview))
        append(" · ")
        append(MessagingLogic.relativeTime(thread.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenThread,
                onLongClick = onDeleteThread,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MetroText(
                text = MessagingLogic.displayLabel(thread),
                style = MetroTextStyle.ListItemTitle,
                color = titleColor,
            )
            MetroText(
                text = subtitle,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
            )
        }
        if (thread.unreadCount > 0) {
            MetroText(
                text = thread.unreadCount.toString(),
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.accent,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenThread,
                    )
                    .padding(start = 8.dp),
            )
        }
    }
}
