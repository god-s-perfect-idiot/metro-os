package com.metro.conversations.ui

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.metro.conversations.R
import com.metro.conversations.data.ConversationMessage
import com.metro.conversations.data.ConversationsLogic
import com.metro.conversations.data.HomeTile
import com.metro.conversations.data.ReplyableConversation
import com.metro.system.MetroAppBranding
import com.metro.ui.LocalMetroSubpageExit
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroColors
import com.metro.ui.MetroContextMenuActiveShift
import com.metro.ui.MetroContextMenuClearOnDismiss
import com.metro.ui.MetroContextMenuDimmedAlpha
import com.metro.ui.MetroContextMenuItem
import com.metro.ui.MetroContextMenuPopup
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroMessageBubble
import com.metro.ui.MetroMessageBubbleKind
import com.metro.ui.MetroPanoramaBodyEnter
import com.metro.ui.MetroPanoramaBrandEnter
import com.metro.ui.MetroPivot
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.ui.metroIncomingBubbleColor
import com.metro.ui.metroNavBarPadding
import com.metro.ui.metroOutgoingBubbleColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val HomeTileGap = 8.dp
private val HomeTileInset = 8.dp
/** Slightly under a full half-width square — “a touch smaller” than edge-to-edge 2-up. */
private const val HomeTileWidthScale = 0.88f
private val HomeTileSlideStart = 72.dp
private const val HomeTileEnterMs = 320
private const val HomeTileStaggerMs = 70L
private val HomeTileEnterEasing = CubicBezierEasing(0.3f, 1f, 0.2f, 1f)
private const val ChatDismissSlideMs = 220
private const val ChatDismissPlacementMs = 220
private const val HomeClearSlideOutMs = 280
private const val HomeClearSlideUpMs = 220

private val HomeTitleStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 80.sp,
    lineHeight = 84.sp,
    letterSpacing = (-1).sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private val HomeTileTitleStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 22.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Composable
fun ConversationsShell(
    state: ConversationsState,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_VARIABLE")
    val observe = state.generation

    var panoramaIntroPlayed by remember { mutableStateOf(false) }
    val composing = state.route is ConversationsRoute.Thread

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .then(
                if (composing) {
                    Modifier.windowInsetsPadding(WindowInsets.ime)
                } else {
                    Modifier
                        .navigationBarsPadding()
                        .metroNavBarPadding()
                },
            )
            .background(MetroTheme.colors.background),
    ) {
        MetroSubpageHost(
            route = state.route,
            isRoot = { it is ConversationsRoute.Home },
            parentOf = { route ->
                when (route) {
                    ConversationsRoute.Home -> ConversationsRoute.Home
                    is ConversationsRoute.AppList -> ConversationsRoute.Home
                    is ConversationsRoute.Thread -> ConversationsRoute.AppList(
                        packageName = route.listPackageName,
                        startOnFavorites = route.listFavorites,
                    )
                }
            },
            loadKeyOf = { route ->
                when (route) {
                    ConversationsRoute.Home -> "home"
                    is ConversationsRoute.AppList -> when {
                        route.packageName != null -> "list:${route.packageName}"
                        route.startOnFavorites -> "list:favorites"
                        else -> "list:all"
                    }
                    is ConversationsRoute.Thread -> "thread:${route.key}"
                }
            },
            onGoBack = state::goBack,
            modifier = Modifier.fillMaxSize(),
            rootContent = {
                HomeScreen(
                    tiles = state.homeTiles,
                    skipIntro = panoramaIntroPlayed,
                    onIntroPlayed = { panoramaIntroPlayed = true },
                    onOpenAllApps = state::openAllApps,
                    onOpenFavorites = state::openFavorites,
                    onOpenApp = state::openApp,
                    onClear = state::clearAllConversations,
                )
            },
            subpageContent = { route ->
                val requestExit = LocalMetroSubpageExit.current
                val onBack = { requestExit?.invoke() ?: state.goBack() }
                when (route) {
                    ConversationsRoute.Home -> Unit
                    is ConversationsRoute.AppList -> {
                        AppListScreen(
                            packageName = route.packageName,
                            title = listTitle(route.packageName, state),
                            conversations = state.listConversations(route.packageName),
                            favoriteConversations = state.favoriteConversations(),
                            startOnFavorites = route.startOnFavorites,
                            isFavorite = state::isFavorite,
                            onToggleFavorite = state::toggleFavorite,
                            onDismiss = state::dismissConversation,
                            onOpenThread = { key, fromFavorites ->
                                state.openThread(key, fromFavorites = fromFavorites)
                            },
                            onBack = onBack,
                        )
                    }
                    is ConversationsRoute.Thread -> {
                        val conversation = state.selectedConversation
                        if (conversation == null) {
                            LaunchedEffect(route.key) { onBack() }
                        } else {
                            ThreadScreen(
                                conversation = conversation,
                                isFavorite = state.isFavorite(conversation),
                                composerText = state.composerText,
                                sending = state.sending,
                                onComposerChange = state::updateComposer,
                                onSend = state::sendReply,
                                onOpenApp = state::openInApp,
                                onToggleFavorite = { state.toggleFavorite(conversation) },
                                onBack = onBack,
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun listTitle(packageName: String?, state: ConversationsState): String {
    if (packageName == null) return stringResource(R.string.all_chats)
    return state.groups.firstOrNull { it.packageName == packageName }?.appLabel
        ?: packageName
}

@Composable
private fun HomeScreen(
    tiles: List<HomeTile>,
    skipIntro: Boolean,
    onIntroPlayed: () -> Unit,
    onOpenAllApps: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenApp: (String) -> Unit,
    onClear: () -> Unit,
) {
    // Remember after this home visit so in-app return skips the panorama intro.
    DisposableEffect(Unit) {
        onDispose { onIntroPlayed() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 12.dp),
    ) {
        MetroPanoramaBrandEnter(skipEnter = skipIntro) {
            BasicText(
                text = stringResource(R.string.home_title),
                style = HomeTitleStyle.copy(
                    fontFamily = MetroTheme.fontFamily,
                    color = MetroTheme.colors.primaryText,
                ),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
                modifier = Modifier
                    .padding(start = 12.dp, top = 8.dp, bottom = 16.dp),
            )
        }

        MetroPanoramaBodyEnter(skipEnter = skipIntro) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
            ) {
                HomeTilesGrid(
                    tiles = tiles,
                    onOpenAllApps = onOpenAllApps,
                    onOpenFavorites = onOpenFavorites,
                    onOpenApp = onOpenApp,
                    onClear = onClear,
                )
            }
        }
    }
}

/**
 * Start-menu–style 2-up tiles. Enter order left→right, top→bottom —
 * each sliding in from the right (same motion as Hub extras+info support tiles).
 * Clear slides app tiles + the clear tile out one-by-one (same stagger as enter).
 * Clear is omitted when there are no app groups left.
 */
@Composable
private fun HomeTilesGrid(
    tiles: List<HomeTile>,
    onOpenAllApps: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenApp: (String) -> Unit,
    onClear: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var clearing by remember { mutableStateOf(false) }
    val leading = tiles.filter { it is HomeTile.AllApps || it is HomeTile.Favorites }
    val apps = tiles.filterIsInstance<HomeTile.App>()
    val showClear = tiles.any { it is HomeTile.Clear }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tileSize = (maxWidth - HomeTileGap) / 2 * HomeTileWidthScale
        Column(
            modifier = Modifier.animateContentSize(
                animationSpec = tween(
                    durationMillis = HomeClearSlideUpMs,
                    easing = HomeTileEnterEasing,
                ),
            ),
            verticalArrangement = Arrangement.spacedBy(HomeTileGap),
        ) {
            TileChunkRows(
                tiles = leading,
                tileSize = tileSize,
                baseEnterIndex = 0,
                onOpenAllApps = onOpenAllApps,
                onOpenFavorites = onOpenFavorites,
                onOpenApp = onOpenApp,
                onClear = {},
            )
            apps.chunked(2).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HomeTileGap),
                ) {
                    row.forEachIndexed { colIndex, tile ->
                        val enterIndex = leading.size + rowIndex * 2 + colIndex
                        val exitStaggerIndex = rowIndex * 2 + colIndex
                        HomeAppTile(
                            title = tile.appLabel,
                            packageName = tile.packageName,
                            glyphResId = null,
                            faceColorOverride = null,
                            enterIndex = enterIndex,
                            exiting = clearing,
                            exitStaggerIndex = exitStaggerIndex,
                            onClick = { onOpenApp(tile.packageName) },
                            modifier = Modifier.size(tileSize),
                        )
                    }
                }
            }
            if (showClear) {
                val clearEnterIndex = leading.size + apps.size
                HomeAppTile(
                    title = stringResource(R.string.clear),
                    packageName = null,
                    glyphResId = R.drawable.ic_clear,
                    faceColorOverride = MetroColors.AccentRed,
                    enterIndex = clearEnterIndex,
                    exiting = clearing,
                    exitStaggerIndex = apps.size,
                    onClick = {
                        if (!clearing && apps.isNotEmpty()) {
                            clearing = true
                            scope.launch {
                                val waitMs =
                                    apps.size * HomeTileStaggerMs + HomeClearSlideOutMs
                                delay(waitMs)
                                onClear()
                                clearing = false
                            }
                        }
                    },
                    modifier = Modifier.size(tileSize),
                )
            }
        }
    }
}

@Composable
private fun TileChunkRows(
    tiles: List<HomeTile>,
    tileSize: androidx.compose.ui.unit.Dp,
    baseEnterIndex: Int,
    onOpenAllApps: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenApp: (String) -> Unit,
    onClear: () -> Unit,
) {
    tiles.chunked(2).forEachIndexed { rowIndex, row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HomeTileGap),
        ) {
            row.forEachIndexed { colIndex, tile ->
                val enterIndex = baseEnterIndex + rowIndex * 2 + colIndex
                when (tile) {
                    HomeTile.AllApps -> {
                        HomeAppTile(
                            title = stringResource(R.string.all_chats),
                            packageName = null,
                            glyphResId = R.drawable.ic_all_chats,
                            faceColorOverride = null,
                            enterIndex = enterIndex,
                            onClick = onOpenAllApps,
                            modifier = Modifier.size(tileSize),
                        )
                    }
                    HomeTile.Favorites -> {
                        HomeAppTile(
                            title = stringResource(R.string.favorites),
                            packageName = null,
                            glyphResId = R.drawable.ic_favorites,
                            faceColorOverride = MetroColors.AccentPink,
                            enterIndex = enterIndex,
                            onClick = onOpenFavorites,
                            modifier = Modifier.size(tileSize),
                        )
                    }
                    is HomeTile.App -> {
                        HomeAppTile(
                            title = tile.appLabel,
                            packageName = tile.packageName,
                            glyphResId = null,
                            faceColorOverride = null,
                            enterIndex = enterIndex,
                            onClick = { onOpenApp(tile.packageName) },
                            modifier = Modifier.size(tileSize),
                        )
                    }
                    HomeTile.Clear -> {
                        HomeAppTile(
                            title = stringResource(R.string.clear),
                            packageName = null,
                            glyphResId = R.drawable.ic_clear,
                            faceColorOverride = MetroColors.AccentRed,
                            enterIndex = enterIndex,
                            onClick = onClear,
                            modifier = Modifier.size(tileSize),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeAppTile(
    title: String,
    packageName: String?,
    glyphResId: Int?,
    faceColorOverride: Color?,
    enterIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    exiting: Boolean = false,
    exitStaggerIndex: Int = 0,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val accent = MetroTheme.colors.accent
    val asset = remember(packageName, accent) {
        if (packageName == null) {
            null
        } else {
            MetroAppBranding.loadAppIconAsset(context, packageName)
        }
    }
    val faceColor = faceColorOverride ?: asset?.backgroundColor ?: accent
    val contentColor = MetroColors.tileContentColor(faceColor)
    val slideStartPx = with(density) { HomeTileSlideStart.toPx() }
    // Stable across enterIndex changes so clear doesn't re-slide after apps leave.
    val staggerIndex = remember { enterIndex }
    val translationX = remember { Animatable(slideStartPx) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        translationX.snapTo(slideStartPx)
        alpha.snapTo(0f)
        delay(staggerIndex * HomeTileStaggerMs)
        alpha.snapTo(1f)
        translationX.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = HomeTileEnterMs,
                easing = HomeTileEnterEasing,
            ),
        )
    }

    LaunchedEffect(exiting) {
        if (!exiting) return@LaunchedEffect
        delay(exitStaggerIndex * HomeTileStaggerMs)
        launch {
            translationX.animateTo(
                targetValue = slideStartPx,
                animationSpec = tween(
                    durationMillis = HomeClearSlideOutMs,
                    easing = EaseOutCubic,
                ),
            )
        }
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = HomeClearSlideOutMs,
                easing = EaseOutCubic,
            ),
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .graphicsLayer {
                this.translationX = translationX.value
                this.alpha = alpha.value
            }
            .background(faceColor)
            .clickable(onClick = onClick)
            .semantics { contentDescription = title },
    ) {
        val iconSize = minOf(maxWidth, maxHeight) * 0.42f
        if (glyphResId != null) {
            Image(
                painter = painterResource(id = glyphResId),
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(iconSize)
                    .align(Alignment.Center),
            )
        } else {
            val bitmap = remember(asset?.drawable, iconSize, density) {
                asset?.drawable?.toCenteredBitmap(
                    sizePx = with(density) { iconSize.roundToPx() }.coerceAtLeast(1),
                )
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(iconSize)
                        .align(Alignment.Center),
                )
            }
        }

        BasicText(
            text = title,
            style = HomeTileTitleStyle.copy(
                fontFamily = MetroTheme.fontFamily,
                color = contentColor,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(HomeTileInset),
        )
    }
}

private fun Drawable.toCenteredBitmap(sizePx: Int): android.graphics.Bitmap {
    if (this is BitmapDrawable && bitmap != null && bitmap.width == sizePx && bitmap.height == sizePx) {
        return bitmap
    }
    return toBitmap(sizePx, sizePx)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListScreen(
    packageName: String?,
    title: String,
    conversations: List<ReplyableConversation>,
    favoriteConversations: List<ReplyableConversation>,
    startOnFavorites: Boolean,
    isFavorite: (ReplyableConversation) -> Boolean,
    onToggleFavorite: (ReplyableConversation) -> Unit,
    onDismiss: (ReplyableConversation) -> Unit,
    onOpenThread: (key: String, fromFavorites: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    if (packageName == null) {
        AllChatsPivotScreen(
            conversations = conversations,
            favoriteConversations = favoriteConversations,
            startOnFavorites = startOnFavorites,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onDismiss = onDismiss,
            onOpenThread = onOpenThread,
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroText(
                text = title,
                style = MetroTextStyle.HubTitle,
                modifier = Modifier
                    .padding(start = 12.dp, top = 8.dp, bottom = 12.dp),
            )
            ConversationListPane(
                conversations = conversations,
                showAppLine = false,
                emptyMessage = stringResource(R.string.empty_hub),
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                onDismiss = onDismiss,
                onOpenThread = { key -> onOpenThread(key, false) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllChatsPivotScreen(
    conversations: List<ReplyableConversation>,
    favoriteConversations: List<ReplyableConversation>,
    startOnFavorites: Boolean,
    isFavorite: (ReplyableConversation) -> Boolean,
    onToggleFavorite: (ReplyableConversation) -> Unit,
    onDismiss: (ReplyableConversation) -> Unit,
    onOpenThread: (key: String, fromFavorites: Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val titles = listOf(
        stringResource(R.string.all_chats),
        stringResource(R.string.favorites),
    )
    val pagerState = rememberPagerState(
        initialPage = if (startOnFavorites) 1 else 0,
        pageCount = { titles.size },
    )
    MetroPivot(
        titles = titles,
        pagerState = pagerState,
        modifier = Modifier.fillMaxSize(),
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
            0 -> ConversationListPane(
                conversations = conversations,
                showAppLine = true,
                emptyMessage = stringResource(R.string.empty_hub),
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                onDismiss = onDismiss,
                onOpenThread = { key -> onOpenThread(key, false) },
                modifier = Modifier.fillMaxSize(),
            )
            else -> ConversationListPane(
                conversations = favoriteConversations,
                showAppLine = true,
                emptyMessage = stringResource(R.string.empty_favorites),
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                onDismiss = onDismiss,
                onOpenThread = { key -> onOpenThread(key, true) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Mutable holder so layout callbacks can update without triggering recomposition. */
private class RectRef {
    var value: Rect = Rect.Zero
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationListPane(
    conversations: List<ReplyableConversation>,
    showAppLine: Boolean,
    emptyMessage: String,
    isFavorite: (ReplyableConversation) -> Boolean,
    onToggleFavorite: (ReplyableConversation) -> Unit,
    onDismiss: (ReplyableConversation) -> Unit,
    onOpenThread: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (conversations.isEmpty()) {
        MetroEmptyState(
            message = emptyMessage,
            modifier = modifier,
        )
        return
    }

    var menuConversation by remember { mutableStateOf<ReplyableConversation?>(null) }
    var menuAnchor by remember { mutableStateOf(Rect.Zero) }
    var exitingKeys by remember { mutableStateOf(setOf<String>()) }
    val popupRootBounds = remember { RectRef() }
    val contextMenuVisible = remember { MutableTransitionState(false) }
    val contextMenuFocusFraction by updateTransition(
        contextMenuVisible,
        label = "chatListContextMenuFocus",
    ).animateFloat(
        transitionSpec = { tween(MetroTransitions.AppBarSlideMs) },
        label = "focus",
    ) { visible -> if (visible) 1f else 0f }

    val openContextMenu: (ReplyableConversation, Rect) -> Unit = { conversation, bounds ->
        if (conversation.key !in exitingKeys) {
            menuConversation = conversation
            menuAnchor = bounds
            contextMenuVisible.targetState = true
        }
    }
    val dismissContextMenu: () -> Unit = {
        contextMenuVisible.targetState = false
    }
    MetroContextMenuClearOnDismiss(contextMenuVisible) {
        menuConversation = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                popupRootBounds.value = coordinates.boundsInWindow()
            },
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(conversations, key = { it.key }) { conversation ->
                val visibleState = remember(conversation.key) {
                    MutableTransitionState(true)
                }
                LaunchedEffect(conversation.key in exitingKeys) {
                    if (conversation.key in exitingKeys) {
                        visibleState.targetState = false
                    }
                }
                LaunchedEffect(
                    visibleState.isIdle,
                    visibleState.currentState,
                    conversation.key,
                ) {
                    if (
                        conversation.key in exitingKeys &&
                        visibleState.isIdle &&
                        !visibleState.currentState
                    ) {
                        onDismiss(conversation)
                        exitingKeys = exitingKeys - conversation.key
                    }
                }
                AnimatedVisibility(
                    visibleState = visibleState,
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null,
                        placementSpec = tween(
                            durationMillis = ChatDismissPlacementMs,
                            easing = HomeTileEnterEasing,
                        ),
                    ),
                    exit = slideOutHorizontally(
                        animationSpec = tween(
                            durationMillis = ChatDismissSlideMs,
                            easing = EaseOutCubic,
                        ),
                        targetOffsetX = { fullWidth -> fullWidth },
                    ) + shrinkVertically(
                        animationSpec = tween(
                            durationMillis = ChatDismissSlideMs,
                            easing = EaseOutCubic,
                        ),
                        shrinkTowards = Alignment.Top,
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = ChatDismissSlideMs,
                            easing = EaseOutCubic,
                        ),
                    ),
                ) {
                    ChatListRow(
                        conversation = conversation,
                        showAppLine = showAppLine,
                        contextMenuTarget = menuConversation?.key == conversation.key,
                        contextMenuFocusFraction = contextMenuFocusFraction,
                        onClick = { onOpenThread(conversation.key) },
                        onLongPress = { bounds -> openContextMenu(conversation, bounds) },
                    )
                }
            }
        }

        menuConversation?.let { conversation ->
            val favorited = isFavorite(conversation)
            val favoriteLabel = stringResource(
                if (favorited) R.string.unfavorite else R.string.favorite,
            )
            val dismissLabel = stringResource(R.string.dismiss)
            MetroContextMenuPopup(
                visibleState = contextMenuVisible,
                anchorBounds = menuAnchor,
                rootBounds = popupRootBounds.value,
                items = listOf(
                    MetroContextMenuItem(
                        label = favoriteLabel,
                        onClick = {
                            onToggleFavorite(conversation)
                            dismissContextMenu()
                        },
                    ),
                    MetroContextMenuItem(
                        label = dismissLabel,
                        onClick = {
                            exitingKeys = exitingKeys + conversation.key
                            dismissContextMenu()
                        },
                    ),
                ),
                onDismissRequest = dismissContextMenu,
            )
        }
    }
}

private val ChatRowVerticalPadding = 6.dp
private val SenderAvatarSize = 64.dp

private val ChatTitleStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 22.sp,
    lineHeight = 26.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private val ChatBodyStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 20.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatListRow(
    conversation: ReplyableConversation,
    showAppLine: Boolean,
    contextMenuTarget: Boolean,
    contextMenuFocusFraction: Float,
    onClick: () -> Unit,
    onLongPress: (Rect) -> Unit,
) {
    val preview = conversation.preview.trim().takeIf { it.isNotEmpty() }
    val appLabel = conversation.appLabel.trim().takeIf { it.isNotEmpty() }
    val timeMs = conversation.messages.asReversed()
        .firstOrNull { it.timestampMs > 0L }
        ?.timestampMs
        ?: conversation.postTimeMs
    val timeLabel = ConversationsLogic.bubbleTime(timeMs)
    val rowBounds = remember(conversation.key) { RectRef() }
    val density = LocalDensity.current
    val activeShiftPx = with(density) { MetroContextMenuActiveShift.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                if (contextMenuTarget) {
                    translationX = -contextMenuFocusFraction * activeShiftPx
                    alpha = 1f
                } else {
                    translationX = 0f
                    alpha = 1f - contextMenuFocusFraction * (1f - MetroContextMenuDimmedAlpha)
                }
            }
            .onGloballyPositioned { coordinates ->
                rowBounds.value = coordinates.boundsInWindow()
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = { onLongPress(rowBounds.value) },
            )
            .padding(horizontal = 12.dp, vertical = ChatRowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SenderAvatar(
            photo = conversation.senderPhoto,
            title = conversation.title,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
                .padding(end = if (timeLabel.isNotEmpty()) 8.dp else 0.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText(
                text = conversation.title,
                style = ChatTitleStyle.copy(
                    fontFamily = MetroTheme.fontFamily,
                    color = MetroTheme.colors.primaryText,
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
            if (preview != null) {
                BasicText(
                    text = preview,
                    style = ChatBodyStyle.copy(
                        fontFamily = MetroTheme.fontFamily,
                        color = MetroTheme.colors.secondaryText,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            if (showAppLine && appLabel != null) {
                BasicText(
                    text = appLabel,
                    style = ChatBodyStyle.copy(
                        fontFamily = MetroTheme.fontFamily,
                        color = MetroTheme.colors.accent,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        if (timeLabel.isNotEmpty()) {
            BasicText(
                text = timeLabel,
                style = ChatBodyStyle.copy(
                    fontFamily = MetroTheme.fontFamily,
                    color = MetroTheme.colors.secondaryText,
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.align(Alignment.Top).padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SenderAvatar(
    photo: android.graphics.Bitmap?,
    title: String,
) {
    val accent = MetroTheme.colors.accent
    val letter = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(SenderAvatarSize)
            .clip(RectangleShape)
            .background(if (photo != null) Color.Black else accent),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            MetroText(
                text = letter,
                style = MetroTextStyle.ListItemTitle,
                color = MetroColors.tileContentColor(accent),
            )
        }
    }
}

@Composable
private fun ThreadScreen(
    conversation: ReplyableConversation,
    isFavorite: Boolean,
    composerText: String,
    sending: Boolean,
    onComposerChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenApp: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val listState = rememberLazyListState()
    val messages = conversation.messages
    val showSenderNames = ConversationsLogic.isMultiSenderThread(messages)
    val favoriteLabel = stringResource(
        if (isFavorite) R.string.unfavorite else R.string.favorite,
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = MetroAppBarDefaults.BarHeight + navBottomInset),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 4.dp, bottom = 8.dp),
            ) {
                MetroText(
                    text = conversation.appLabel.uppercase(),
                    style = MetroTextStyle.SectionHeader,
                    color = MetroTheme.colors.accent,
                )
                MetroText(
                    text = conversation.title,
                    style = MetroTextStyle.HubTitle,
                )
            }

            if (messages.isEmpty()) {
                val emptyHub = stringResource(R.string.empty_hub)
                val preview = conversation.preview.ifBlank { emptyHub }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "preview") {
                        IncomingMessageBubble(
                            text = preview,
                            timestampMs = conversation.postTimeMs,
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(messages, key = { it.id }) { message ->
                        ThreadMessageBubble(
                            message = message,
                            showSenderName = showSenderNames,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (conversation.canReply) {
                MetroTextBox(
                    value = composerText,
                    onValueChange = onComposerChange,
                    placeholder = stringResource(R.string.reply_hint),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    enabled = !sending,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (conversation.canReply) {
                    MetroBorderButton(
                        text = stringResource(R.string.send),
                        onClick = onSend,
                        enabled = !sending && composerText.isNotBlank(),
                    )
                }
                MetroBorderButton(
                    text = stringResource(R.string.open_app),
                    onClick = onOpenApp,
                )
            }
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    label = favoriteLabel,
                    onClick = onToggleFavorite,
                    contentDescription = favoriteLabel,
                    icon = { color ->
                        MetroSystemIcon(
                            type = if (isFavorite) {
                                MetroSystemIconType.HeartSlash
                            } else {
                                MetroSystemIconType.Heart
                            },
                            iconSize = MetroAppBarDefaults.GlyphSize,
                            color = color,
                            showCircle = false,
                        )
                    },
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ThreadMessageBubble(
    message: ConversationMessage,
    showSenderName: Boolean,
) {
    val senderLabel = message.sender
        ?.trim()
        ?.takeIf { showSenderName && it.isNotEmpty() && !ConversationsLogic.isSelfSender(it) }
    if (message.fromSelf) {
        OutgoingMessageBubble(
            text = message.text,
            timestampMs = message.timestampMs,
            senderLabel = senderLabel,
            image = message.image,
        )
    } else {
        IncomingMessageBubble(
            text = message.text,
            timestampMs = message.timestampMs,
            senderLabel = senderLabel,
            image = message.image,
        )
    }
}

@Composable
private fun IncomingMessageBubble(
    text: String,
    timestampMs: Long,
    senderLabel: String? = null,
    image: android.graphics.Bitmap? = null,
) {
    val background = metroIncomingBubbleColor(MetroTheme.colors.accent)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        MetroMessageBubble(
            kind = MetroMessageBubbleKind.Incoming,
            color = background,
        ) {
            BubbleInner(
                text = text,
                timestampMs = timestampMs,
                senderLabel = senderLabel,
                image = image,
            )
        }
    }
}

@Composable
private fun OutgoingMessageBubble(
    text: String,
    timestampMs: Long,
    senderLabel: String? = null,
    image: android.graphics.Bitmap? = null,
) {
    val background = metroOutgoingBubbleColor(MetroTheme.colors.accent)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        MetroMessageBubble(
            kind = MetroMessageBubbleKind.Outgoing,
            color = background,
        ) {
            BubbleInner(
                text = text,
                timestampMs = timestampMs,
                senderLabel = senderLabel,
                image = image,
            )
        }
    }
}

@Composable
private fun BubbleInner(
    text: String,
    timestampMs: Long,
    senderLabel: String? = null,
    image: android.graphics.Bitmap? = null,
) {
    val time = ConversationsLogic.bubbleTime(timestampMs)
    val showFooter = !senderLabel.isNullOrEmpty() || time.isNotEmpty()
    val showText = text.isNotBlank()
    val metaColor = MetroColors.TileContentOnAccent.copy(alpha = 0.75f)
    Column(modifier = Modifier.fillMaxWidth()) {
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = if (showText || showFooter) 0.dp else 8.dp,
                    )
                    .heightIn(max = 220.dp),
            )
        }
        if (showText || showFooter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (showText) {
                    MetroText(
                        text = text,
                        style = MetroTextStyle.Body,
                        color = MetroColors.TileContentOnAccent,
                        modifier = Modifier.padding(
                            end = 4.dp,
                            bottom = if (showFooter) 4.dp else 0.dp,
                        ),
                    )
                }
                if (showFooter) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        if (!senderLabel.isNullOrEmpty()) {
                            MetroText(
                                text = senderLabel,
                                style = MetroTextStyle.DialogBody,
                                color = metaColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .padding(end = if (time.isNotEmpty()) 8.dp else 0.dp),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        if (time.isNotEmpty()) {
                            MetroText(
                                text = time,
                                style = MetroTextStyle.DialogBody,
                                color = metaColor,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
