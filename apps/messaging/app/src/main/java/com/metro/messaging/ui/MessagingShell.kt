package com.metro.messaging.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metro.messaging.data.ConversationThread
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.metroNavBarPadding

@Composable
fun MessagingShell(
    state: MessagingState,
    onRequestDefaultApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observeState = generation

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding()
            .background(Color.Black),
    ) {
        when (val route = state.route) {
            MessagingRoute.Threads -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    MetroText(
                        text = "threads",
                        style = MetroTextStyle.HubTitle,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(top = 8.dp, bottom = 12.dp),
                    )
                    ThreadListScreen(
                        threads = state.threads,
                        usingDemoData = state.usingDemoData,
                        onOpenThread = state::openThread,
                        onDeleteThread = state::deleteThread,
                        modifier = Modifier.padding(bottom = MetroAppBarDefaults.BarHeight),
                    )
                }
                ThreadListAppBar(
                    isDefaultSmsApp = state.isDefaultSmsApp,
                    onNewMessage = state::startNewMessage,
                    onSetDefaultApp = onRequestDefaultApp,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            MessagingRoute.NewMessage -> {
                NewMessageScreen(
                    recipient = state.newRecipient,
                    body = state.newBody,
                    onRecipientChange = state::updateNewRecipient,
                    onBodyChange = state::updateNewBody,
                    onSend = state::sendNewMessage,
                    onBack = state::backToThreads,
                )
            }
            is MessagingRoute.Conversation -> {
                val thread = state.threads.firstOrNull { it.id == route.threadId }
                    ?: ConversationThread(
                        id = route.threadId,
                        address = route.threadId.toString(),
                        displayName = null,
                        preview = "",
                        timestamp = 0L,
                        unreadCount = 0,
                    )
                ConversationScreen(
                    thread = thread,
                    messages = state.messages,
                    composerText = state.composerText,
                    onBack = state::backToThreads,
                    onComposerChange = state::updateComposer,
                    onSend = state::sendMessage,
                )
            }
        }
    }
}

@Composable
private fun ThreadListAppBar(
    isDefaultSmsApp: Boolean,
    onNewMessage: () -> Unit,
    onSetDefaultApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val menuItems = buildList {
        if (!isDefaultSmsApp) {
            add(MetroAppBarMenuItem("set as default messaging app", onClick = onSetDefaultApp))
        }
        add(MetroAppBarMenuItem("select", enabled = false) {})
        add(MetroAppBarMenuItem("settings", enabled = false) {})
    }
    MetroAppBar(
        icons = listOf(
            MetroAppBarIcon(
                type = MetroSystemIconType.Add,
                label = "new",
                onClick = onNewMessage,
                contentDescription = "new message",
            ),
        ),
        menuItems = menuItems,
        modifier = modifier,
    )
}
