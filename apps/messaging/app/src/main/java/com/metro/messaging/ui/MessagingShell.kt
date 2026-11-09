package com.metro.messaging.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.messaging.R
import com.metro.messaging.data.ConversationThread
import com.metro.messaging.data.DefaultSmsApp
import com.metro.system.MetroNavBar
import com.metro.ui.LocalMetroSubpageExit
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroPivot
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTransitions
import com.metro.ui.metroNavBarPadding
import com.metro.ui.rememberMetroNavBarEnabled
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessagingShell(
    state: MessagingState,
    onRequestDefaultApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observeState = generation
    val onThreadList = state.route is MessagingRoute.Threads
    val composing = state.route is MessagingRoute.NewMessage ||
        state.route is MessagingRoute.Conversation
    val viewingImage = state.route is MessagingRoute.ImageViewer

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .then(
                when {
                    composing -> Modifier.composerBottomClearance()
                    viewingImage -> Modifier
                    else -> Modifier.metroNavBarPadding()
                },
            )
            .background(Color.Black),
    ) {
        // Keep the list composed under drill-ins so back does not remount the pivot/LazyColumn
        // (that remount + a fresh threads reload was the list flicker).
        ThreadListPivot(
            threads = state.threads,
            chatThreads = state.chatThreads,
            usingDemoData = state.usingDemoData,
            initialPage = state.pivotPage,
            onPivotPageChange = state::selectPivotPage,
            onOpenThread = state::openThread,
            onDeleteThread = state::deleteThread,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = MetroAppBarDefaults.BarHeight),
        )

        if (!onThreadList) {
            MetroSubpageHost(
                route = state.route,
                // Host is only composed for drill-ins; treat every route as a subpage.
                isRoot = { false },
                parentOf = { route ->
                    when (route) {
                        is MessagingRoute.ImageViewer ->
                            MessagingRoute.Conversation(route.threadId)
                        else -> MessagingRoute.Threads
                    }
                },
                loadKeyOf = { subpageLoadKey(it) },
                onGoBack = state::goBack,
                modifier = Modifier.fillMaxSize(),
                rootContent = { },
                subpageContent = { route ->
                    val requestExit = LocalMetroSubpageExit.current
                    val onBack = { requestExit?.invoke() }
                    when (route) {
                        MessagingRoute.NewMessage -> {
                            NewMessageScreen(
                                recipient = state.newRecipient,
                                body = state.newBody,
                                contactSuggestions = state.contactSuggestions,
                                onRecipientChange = state::updateNewRecipient,
                                onBodyChange = state::updateNewBody,
                                onSelectContact = state::selectContactSuggestion,
                                onSend = state::sendNewMessage,
                                onBack = { onBack() },
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
                                onBack = { onBack() },
                                onComposerChange = state::updateComposer,
                                onSend = state::sendMessage,
                                onOpenImage = { uri -> state.openImage(route.threadId, uri) },
                            )
                        }
                        is MessagingRoute.ImageViewer -> {
                            MmsImageViewerScreen(
                                uri = route.uri,
                                onBack = { onBack() },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        MessagingRoute.Threads -> Unit
                    }
                },
            )
        }

        ThreadListAppBar(
            visible = onThreadList,
            isDefaultSmsApp = state.isDefaultSmsApp,
            onNewMessage = state::startNewMessage,
            onSetDefaultApp = onRequestDefaultApp,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThreadListPivot(
    threads: List<ConversationThread>,
    chatThreads: List<ConversationThread>,
    usingDemoData: Boolean,
    initialPage: Int,
    onPivotPageChange: (Int) -> Unit,
    onOpenThread: (ConversationThread) -> Unit,
    onDeleteThread: (ConversationThread) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val titles = listOf("threads", "chats")
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, titles.lastIndex),
        pageCount = { titles.size },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page -> onPivotPageChange(page) }
    }

    MetroPivot(
        titles = titles,
        pagerState = pagerState,
        modifier = modifier.fillMaxSize(),
        header = { MetroAppTitle(title = stringResource(R.string.app_name)) },
        onTitleClick = { index ->
            scope.launch {
                pagerState.animateScrollToPage(
                    page = index,
                    animationSpec = MetroTransitions.pivotTween(),
                )
            }
        },
    ) { page ->
        when (page) {
            0 -> ThreadListScreen(
                threads = threads,
                usingDemoData = usingDemoData,
                emptyMessage = stringResource(R.string.threads_empty),
                onOpenThread = onOpenThread,
                onDeleteThread = onDeleteThread,
                modifier = Modifier.fillMaxSize(),
            )
            else -> ThreadListScreen(
                threads = chatThreads,
                usingDemoData = usingDemoData,
                emptyMessage = stringResource(R.string.chats_empty),
                onOpenThread = onOpenThread,
                onDeleteThread = onDeleteThread,
                modifier = Modifier.fillMaxSize(),
            )
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
    visible: Boolean,
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
        visible = visible,
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

private fun subpageLoadKey(route: MessagingRoute): Any = when (route) {
    MessagingRoute.Threads -> "Threads"
    MessagingRoute.NewMessage -> "NewMessage"
    is MessagingRoute.Conversation -> "Conversation:${route.threadId}"
    is MessagingRoute.ImageViewer -> "ImageViewer:${route.threadId}:${route.uri}"
}
