package com.metro.messaging.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.metro.messaging.data.ConversationThread
import com.metro.messaging.data.DefaultSmsApp
import com.metro.system.MetroNavBar
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.metroNavBarPadding
import com.metro.ui.rememberMetroNavBarEnabled

@Composable
fun MessagingShell(
    state: MessagingState,
    onRequestDefaultApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observeState = generation
    val composing = state.route is MessagingRoute.NewMessage ||
        state.route is MessagingRoute.Conversation

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .then(if (composing) Modifier.composerBottomClearance() else Modifier.metroNavBarPadding())
            .background(Color.Black),
    ) {
        when (val route = state.route) {
            MessagingRoute.Threads -> {
                if (state.isLoadingThreads && state.threads.isEmpty()) {
                    MetroLoadingScreen()
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MetroText(
                            text = "threads",
                            style = MetroTextStyle.HubTitle,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
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
            }
            MessagingRoute.NewMessage -> {
                NewMessageScreen(
                    recipient = state.newRecipient,
                    body = state.newBody,
                    contactSuggestions = state.contactSuggestions,
                    onRecipientChange = state::updateNewRecipient,
                    onBodyChange = state::updateNewBody,
                    onSelectContact = state::selectContactSuggestion,
                    onSend = state::sendNewMessage,
                    onBack = state::backToThreads,
                )
            }
            is MessagingRoute.Conversation -> {
                if (state.isLoadingMessages) {
                    BackHandler(onBack = state::backToThreads)
                    MetroLoadingScreen()
                } else {
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
}

/**
 * Single bottom inset for composer screens: IME when the keyboard is open, otherwise Metro
 * soft-key clearance. Never stack both — that shoves the header off the top of the screen.
 */
@Composable
private fun Modifier.composerBottomClearance(): Modifier {
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val navBarEnabled by rememberMetroNavBarEnabled()
    return if (imeBottomPx > 0) {
        this.windowInsetsPadding(WindowInsets.ime)
    } else if (navBarEnabled) {
        this.padding(bottom = MetroNavBar.HEIGHT_DP.dp)
    } else {
        this
    }
}

@Composable
private fun ThreadListAppBar(
    isDefaultSmsApp: Boolean,
    onNewMessage: () -> Unit,
    onSetDefaultApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Re-check when default status flips so the overflow item disappears immediately after
    // the user grants the SMS role (and never appears when already default).
    val canRequestDefault = remember(isDefaultSmsApp) {
        !isDefaultSmsApp && DefaultSmsApp.requestIntent(context) != null
    }
    val menuItems = buildList {
        if (canRequestDefault) {
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
