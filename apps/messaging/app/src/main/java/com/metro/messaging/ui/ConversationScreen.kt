package com.metro.messaging.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metro.messaging.data.ConversationThread
import com.metro.messaging.data.MessageDirection
import com.metro.messaging.data.MessageItem
import com.metro.messaging.data.MessagingLogic
import com.metro.messaging.data.SendState
import com.metro.ui.MetroColors
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationScreen(
    thread: ConversationThread,
    messages: List<MessageItem>,
    composerText: String,
    onBack: () -> Unit,
    onComposerChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val listState = rememberLazyListState()
    val noExtraBringIntoView = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float,
            ): Float = 0f
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    CompositionLocalProvider(LocalBringIntoViewSpec provides noExtraBringIntoView) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(top = 4.dp, bottom = 8.dp),
        ) {
            MetroText(
                text = MessagingLogic.displayLabel(thread).uppercase(),
                style = MetroTextStyle.SectionHeader,
            )
            MetroText(
                text = MessagingLogic.formatAddress(thread.address),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.accent,
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }

        MessageComposer(
            text = composerText,
            onTextChange = onComposerChange,
            onSend = onSend,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
    }
}

@Composable
private fun MessageBubble(message: MessageItem) {
    val isOutgoing = message.direction == MessageDirection.Outgoing
    val kind = if (isOutgoing) MessageBubbleKind.Outgoing else MessageBubbleKind.Incoming
    val background = if (isOutgoing) {
        outgoingBubbleColor(MetroTheme.colors.accent)
    } else {
        incomingBubbleColor(MetroTheme.colors.accent)
    }
    val statusSuffix = when (message.sendState) {
        SendState.Sending -> " · sending"
        SendState.Failed -> " · failed"
        else -> ""
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
    ) {
        MessageBubbleChrome(
            kind = kind,
            color = background,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MetroText(
                        text = message.body,
                        style = MetroTextStyle.Body,
                        color = MetroColors.TileContentOnAccent,
                        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
                    )
                    MetroText(
                        text = MessagingLogic.bubbleTime(message.timestamp) + statusSuffix,
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroColors.TileContentOnAccent.copy(alpha = 0.75f),
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
        }
    }
}
