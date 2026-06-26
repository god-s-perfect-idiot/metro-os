package com.metro.messaging.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.messaging.R
import com.metro.messaging.data.ConversationThread
import com.metro.messaging.data.MessageDirection
import com.metro.messaging.data.MessageItem
import com.metro.messaging.data.MessagingLogic
import com.metro.messaging.data.SendState
import com.metro.ui.MetroColors
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

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

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
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
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                .padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun MessageBubble(message: MessageItem) {
    val isOutgoing = message.direction == MessageDirection.Outgoing
    val background = if (isOutgoing) {
        MetroTheme.colors.accent
    } else {
        MetroColors.DarkSecondarySurface
    }
    val textColor = if (isOutgoing) {
        MetroColors.TileContentOnAccent
    } else {
        MetroTheme.colors.primaryText
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
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .background(background)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Column {
                MetroText(
                    text = message.body,
                    style = MetroTextStyle.Body,
                    color = textColor,
                )
                MetroText(
                    text = MessagingLogic.relativeTime(message.timestamp) + statusSuffix,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageComposer(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canSend = text.isNotBlank()
    Column(modifier = modifier) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = MetroTextStyle.Body.toTextStyle().copy(
                color = MetroTheme.colors.primaryText,
            ),
            cursorBrush = SolidColor(MetroTheme.colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            decorationBox = { inner ->
                Column {
                    if (text.isEmpty()) {
                        MetroText(
                            text = stringResource(R.string.composer_hint),
                            style = MetroTextStyle.Body,
                            color = MetroTheme.colors.secondaryText,
                        )
                    }
                    inner()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .background(
                                if (text.isNotEmpty()) {
                                    MetroTheme.colors.accent
                                } else {
                                    MetroTheme.colors.secondaryText
                                },
                            )
                            .padding(vertical = 1.dp),
                    )
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetroText(
                text = stringResource(R.string.send),
                style = MetroTextStyle.ListItemTitle,
                color = if (canSend) MetroTheme.colors.accent else MetroTheme.colors.secondaryText,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = canSend,
                        onClick = onSend,
                    )
                    .padding(vertical = 8.dp),
            )
        }
    }
}
