package com.metro.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.zIndex
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import kotlin.math.min
import androidx.core.graphics.drawable.toBitmap
import android.util.LruCache
import com.metro.launcher.data.AppLauncherOption
import com.metro.launcher.data.CustomTileBranding
import com.metro.system.MetroAppBranding
import com.metro.system.MetroAppInfo
import com.metro.system.MetroIconPacks
import androidx.core.content.ContextCompat
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroColors
import com.metro.ui.MetroJumpList
import com.metro.ui.MetroJumpListLogic
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.ui.metroStickyLetterHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val AppListIconSize = 48.dp
private val LetterHeaderBorderWidth = 1.5.dp
private val LetterHeaderFontSize = 26.sp
private val LetterHeaderLineHeight = 30.sp
private val SearchColumnGap = 20.dp
private val IconInnerPadding = 5.dp
private val IconTextGap = 12.dp
private val ListRowVerticalPadding = 4.dp
/** Letter marker row: icon square + vertical padding above/below. */
private val LetterMarkerRowHeight = AppListIconSize + ListRowVerticalPadding * 2
private val ListBottomScrollPadding = 180.dp
private val ContextMenuGapBelowIcon = 4.dp
private val ContextMenuHorizontalPadding = 16.dp
private val ContextMenuVerticalPadding = 12.dp
private val ContextMenuMinWidth = 160.dp
private val ContextMenuMaxVisibleItems = 6
/** Match toolkit context menu — slightly under list title (24sp). */
private val ContextMenuLabelSize = 20.sp
private val ContextMenuLabelLineHeight = 24.sp
/** Label line plus top/bottom item padding. */
private val ContextMenuItemRowHeight = 48.dp
private val ContextMenuMaxScrollHeight =
    ContextMenuItemRowHeight * ContextMenuMaxVisibleItems + ContextMenuVerticalPadding * 2
private val ContextMenuExpandMs = MetroTransitions.AppBarSlideMs
private val ContextMenuDimmedAlpha = 0.45f
private val ContextMenuActiveShift = 8.dp
private val SearchFieldRowHeight = 48.dp
private val SearchFieldBorderWidth = 3.dp
private val SearchFieldHorizontalPadding = 10.dp
private val SearchFieldBottomSpacing = 8.dp
private val AppListHorizontalStartPadding = 12.dp
private val AppListHorizontalEndPadding = 12.dp
/** Top inset above search + list (matches prior Row padding). */
private val AppListTopPadding = 4.dp
/**
 * List content starts after search column + gap. LazyColumn itself is full-bleed so
 * exit peels can draw to the screen edge (contentPadding only insets layout, not clip).
 */
private val AppListContentStartInset =
    AppListHorizontalStartPadding + AppListIconSize + SearchColumnGap
/** Fade + height wipe when letter markers hide/show with search mode. */
private val LetterMarkerVisibilityMs = MetroTransitions.AppBarSlideMs

/** Mutable holder so layout callbacks can update without triggering recomposition. */
private class RectRef {
    var value: Rect = Rect.Zero
}

private data class CachedAppIcon(
    val bitmap: ImageBitmap?,
    val backgroundColor: Color,
)

/** Survives LazyColumn recycling so scrolling past apps does not re-decode icons. */
private val appListIconCache = object : LruCache<String, CachedAppIcon>(96) {}

internal fun clearAppListIconCache() {
    appListIconCache.evictAll()
}

/**
 * App menu — alphabetical list of installed apps.
 * Reference: references/images/applist.png
 *
 * Column order: search | icon/letter squares | app labels.
 *
 * Exit peel: bottom→top pivot (search + letters + apps); tapped row last.
 * Enter / resume: static — snap to rest after a peel.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AppListScreen(
    apps: List<MetroAppInfo>,
    searchActive: Boolean,
    searchQuery: String,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAppClick: (MetroAppInfo) -> Unit,
    onPinToStart: (MetroAppInfo) -> Unit,
    onUninstall: (MetroAppInfo) -> Unit,
    queryAppOptions: suspend (String) -> List<AppLauncherOption>,
    onLaunchAppOption: (AppLauncherOption) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Bump to clear launch-exit pose and snap pivot layers to rest
     * (resume / unlock after a row open left rows at alpha 0).
     */
    restPoseRequestId: Int = 0,
) {
    var jumpListVisible by remember { mutableStateOf(false) }
    var scrollToLetter by remember { mutableStateOf<Char?>(null) }
    var contextMenuApp by remember { mutableStateOf<MetroAppInfo?>(null) }
    var contextMenuIconBounds by remember { mutableStateOf(Rect.Zero) }
    var contextMenuRootBounds by remember { mutableStateOf(Rect.Zero) }
    val contextMenuVisible = remember { MutableTransitionState(false) }
    val popupRootBounds = remember { RectRef() }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    var imeWasVisibleWhileSearching by remember { mutableStateOf(false) }
    // Package launch: exit wave (tapped row last).
    var exitingPackage by remember { mutableStateOf<String?>(null) }
    var pendingLaunch by remember { mutableStateOf<(() -> Unit)?>(null) }
    /** Visible wave keys top→bottom at exit start — search + letters + apps. */
    var exitVisibleKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var restKey by remember { mutableIntStateOf(0) }
    val launchInProgress = exitingPackage != null
    val isExiting = exitingPackage != null
    val onAppClickState = rememberUpdatedState(onAppClick)
    val onLaunchAppOptionState = rememberUpdatedState(onLaunchAppOption)
    val menuGapPx = with(density) { ContextMenuGapBelowIcon.roundToPx() }
    val openContextMenu: (MetroAppInfo, Rect) -> Unit = { app, iconBounds ->
        if (!launchInProgress) {
            // WP8.1 app list: short buzz when long-press opens the context menu.
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            contextMenuIconBounds = iconBounds
            contextMenuRootBounds = popupRootBounds.value
            contextMenuApp = app
            contextMenuVisible.targetState = true
        }
    }
    val dismissContextMenu: () -> Unit = {
        contextMenuVisible.targetState = false
    }
    /** Instant close — use when leaving the app list (pin / launch) so the
     * window-level Popup does not collapse mid-pan over Start. */
    val snapDismissContextMenu: () -> Unit = {
        contextMenuVisible.targetState = false
        contextMenuApp = null
    }
    val beginLaunchExit: (String, () -> Unit) -> Unit = { packageName, launch ->
        if (exitingPackage == null) {
            snapDismissContextMenu()
            val listVisible = listState.layoutInfo.visibleItemsInfo.mapNotNull { info ->
                when (val key = info.key) {
                    is String -> key
                    else -> null
                }
            }
            // Search chrome is always in the peel (top of the vertical wave).
            val withSearch = listOf(AppListPivotLogic.SearchWaveKey) + listVisible
            exitVisibleKeys = if (packageName in withSearch) {
                withSearch
            } else {
                withSearch + packageName
            }
            pendingLaunch = launch
            exitingPackage = packageName
        }
    }

    LaunchedEffect(restPoseRequestId) {
        if (restPoseRequestId <= 0) return@LaunchedEffect
        exitingPackage = null
        pendingLaunch = null
        exitVisibleKeys = emptyList()
        restKey++
    }

    // Drop the host once the shrink animation finishes.
    LaunchedEffect(contextMenuVisible.isIdle, contextMenuVisible.currentState) {
        if (contextMenuVisible.isIdle && !contextMenuVisible.currentState) {
            contextMenuApp = null
        }
    }
    val contextMenuFocusTransition =
        updateTransition(contextMenuVisible, label = "appListContextMenuFocus")
    val contextMenuFocusFractionAnimated by contextMenuFocusTransition.animateFloat(
        transitionSpec = { tween(ContextMenuExpandMs) },
        label = "focusFraction",
    ) { visible -> if (visible) 1f else 0f }
    // Host already gone after snap-dismiss — don't keep animating list dim/shift.
    val contextMenuFocusFraction =
        if (contextMenuApp == null) 0f else contextMenuFocusFractionAnimated
    val showLetterMarkers = MetroJumpListLogic.showSectionMarkers(searchActive)
    val letterMarkerVisibility by animateFloatAsState(
        targetValue = if (showLetterMarkers) 1f else 0f,
        animationSpec = tween(
            durationMillis = LetterMarkerVisibilityMs,
            easing = MetroTransitions.PageEasing,
        ),
        label = "letterMarkerVisibility",
    )
    val grouped = remember(apps) {
        apps.groupBy { MetroJumpListLogic.sortKey(it.label) }.toSortedMap()
    }
    val activeLetters = remember(grouped) { MetroJumpListLogic.activeLetters(grouped.keys) }
    // Sticky header slots stay in the list (height wipe) so indices are stable across search.
    val headerIndices = remember(grouped) {
        var index = 0
        buildMap {
            grouped.forEach { (letter, sectionApps) ->
                put(letter, index)
                index += 1 + sectionApps.size
            }
        }
    }
    LaunchedEffect(searchActive) {
        if (searchActive) jumpListVisible = false
    }
    val dismissSearch: () -> Unit = {
        onSearchActiveChange(false)
        keyboardController?.hide()
        Unit
    }
    val focusSearchField: () -> Unit = {
        searchFocusRequester.requestFocus()
        keyboardController?.show()
    }

    BackHandler(enabled = searchActive) {
        dismissSearch()
    }

    // Leave search once the IME has been shown and then closed (e.g. system Back).
    LaunchedEffect(searchActive, imeVisible) {
        if (!searchActive) {
            imeWasVisibleWhileSearching = false
            keyboardController?.hide()
            return@LaunchedEffect
        }
        if (imeVisible) {
            imeWasVisibleWhileSearching = true
        } else if (imeWasVisibleWhileSearching) {
            dismissSearch()
        }
    }

    LaunchedEffect(scrollToLetter, headerIndices) {
        val letter = scrollToLetter ?: return@LaunchedEffect
        val index = headerIndices[MetroJumpListLogic.normalize(letter)] ?: return@LaunchedEffect
        listState.scrollToItem(index)
        scrollToLetter = null
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { coordinates ->
                // Ref update only — writing Compose state here would recompose the whole list
                // on every parent layout pass.
                popupRootBounds.value = coordinates.boundsInWindow()
            },
    ) {
        val pageWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        // Shared page-left hinge offsets (content is inset; list clip is screen-edge).
        val searchLeftInPagePx = with(density) { AppListHorizontalStartPadding.toPx() }
        val listRowLeftInPagePx = with(density) { AppListContentStartInset.toPx() }
        fun exitDelayFor(waveKey: String, exitSelected: Boolean = false): Long {
            if (!isExiting) return 0L
            val exitRowIndex = exitVisibleKeys.indexOf(waveKey)
            if (exitRowIndex < 0) return 0L
            return AppListPivotLogic.exitDelayMs(
                rowIndex = exitRowIndex,
                lastIndex = exitVisibleKeys.lastIndex,
                selected = exitSelected,
            )
        }

        @Composable
        fun LetterMarkerRow(letter: Char, letterKey: String) {
            // Opaque bg so app rows do not show through while pinned.
            // Height wipe + fade when entering/exiting search mode.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LetterMarkerRowHeight * letterMarkerVisibility)
                    .graphicsLayer {
                        alpha = letterMarkerVisibility
                        clip = letterMarkerVisibility < 0.999f
                    }
                    .background(Color.Black),
            ) {
                AppListRowPivot(
                    exitDelayMs = exitDelayFor(letterKey),
                    exitSelected = false,
                    exiting = isExiting,
                    pageWidthPx = pageWidthPx,
                    itemLeftInPagePx = listRowLeftInPagePx,
                    restKey = restKey,
                ) {
                    AppListRowLayout(
                        modifier = Modifier.fillMaxWidth(),
                        iconContent = {
                            LetterHeader(
                                letter = letter,
                                enabled = showLetterMarkers && !launchInProgress,
                                onClick = { jumpListVisible = true },
                            )
                        },
                        labelContent = {},
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = AppListTopPadding),
        ) {
            if (searchActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = AppListHorizontalEndPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.width(AppListContentStartInset))
                    Box(modifier = Modifier.weight(1f)) {
                        AppListSearchField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            focusRequester = searchFocusRequester,
                        )
                    }
                }
            }

            // Full-bleed list: clip edge = screen edge so peels are not cut at the
            // search-column gutter. Rows are inset via contentPadding only.
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !launchInProgress,
                    contentPadding = PaddingValues(
                        start = AppListContentStartInset,
                        end = AppListHorizontalEndPadding,
                        bottom = ListBottomScrollPadding,
                    ),
                ) {
                    grouped.forEach { (letter, sectionApps) ->
                        val letterKey = AppListPivotLogic.letterWaveKey(letter)
                        // During exit, use a normal item so sticky overlay cannot cover
                        // peels (apps were sliding out from under the pinned letter).
                        if (isExiting) {
                            item(key = letterKey, contentType = "metro-letter-header") {
                                LetterMarkerRow(letter = letter, letterKey = letterKey)
                            }
                        } else {
                            metroStickyLetterHeader(letter = letter, key = letterKey) {
                                LetterMarkerRow(letter = letter, letterKey = letterKey)
                            }
                        }
                        items(
                            items = sectionApps,
                            key = { it.packageName },
                            contentType = { "app" },
                        ) { app ->
                            val isExitSelected = exitingPackage == app.packageName
                            AppListRowPivot(
                                exitDelayMs = exitDelayFor(app.packageName, isExitSelected),
                                exitSelected = isExitSelected,
                                exiting = isExiting,
                                pageWidthPx = pageWidthPx,
                                itemLeftInPagePx = listRowLeftInPagePx,
                                restKey = restKey,
                                onExitComplete = if (isExitSelected) {
                                    {
                                        pendingLaunch?.invoke()
                                        pendingLaunch = null
                                    }
                                } else {
                                    null
                                },
                            ) {
                                AppListAppRow(
                                    app = app,
                                    highlightQuery = if (searchActive) searchQuery else "",
                                    contextMenuTarget = contextMenuApp?.packageName == app.packageName,
                                    contextMenuFocusFraction = contextMenuFocusFraction,
                                    onAppClick = {
                                        if (!launchInProgress) {
                                            beginLaunchExit(app.packageName) {
                                                onAppClickState.value(app)
                                            }
                                        }
                                    },
                                    onLongClick = { iconBounds -> openContextMenu(app, iconBounds) },
                                )
                            }
                        }
                    }
                }

                // Search chrome overlays the full-bleed list (same visual slot as before).
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = AppListHorizontalStartPadding)
                        .width(AppListIconSize)
                        .fillMaxHeight()
                        .padding(vertical = ListRowVerticalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppListRowPivot(
                        exitDelayMs = exitDelayFor(AppListPivotLogic.SearchWaveKey),
                        exitSelected = false,
                        exiting = isExiting,
                        pageWidthPx = pageWidthPx,
                        itemLeftInPagePx = searchLeftInPagePx,
                        restKey = restKey,
                    ) {
                        MetroCircleIconButton(
                            type = MetroSystemIconType.Search,
                            onClick = {
                                if (!launchInProgress) {
                                    if (searchActive) {
                                        focusSearchField()
                                    } else {
                                        onSearchActiveChange(true)
                                    }
                                }
                            },
                            size = AppListIconSize,
                            contentDescription = "search",
                        )
                    }
                    if (searchActive) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = dismissSearch,
                                ),
                        )
                    }
                }
            }
        }

        contextMenuApp?.let { app ->
            var appOptions by remember(app.packageName) { mutableStateOf<List<AppLauncherOption>>(emptyList()) }
            LaunchedEffect(app.packageName) {
                appOptions = queryAppOptions(app.packageName)
            }
            // Full-bleed under the list: left and right screen edges. Vertical only under the icon.
            val menuOffset = IntOffset(
                x = 0,
                y = (contextMenuIconBounds.bottom - contextMenuRootBounds.top + menuGapPx).toInt(),
            )
            val menuWidthPx = (contextMenuRootBounds.right - contextMenuRootBounds.left)
                .roundToInt()
                .coerceAtLeast(0)
            // Height-only wipe: expandVertically/shrinkVertically animate IntSize and can
            // collapse width after height finishes — keep measured width fixed throughout.
            val revealFraction = contextMenuFocusFraction
            Popup(
                alignment = Alignment.TopStart,
                offset = menuOffset,
                onDismissRequest = dismissContextMenu,
                properties = PopupProperties(focusable = true),
            ) {
                Layout(
                    modifier = Modifier.clipToBounds(),
                    content = {
                        AppListContextMenu(
                            pinToStartEnabled = AppListContextMenuLogic.pinToStartEnabled(app.isPinned),
                            uninstallEnabled = !app.isSystemApp,
                            appOptions = appOptions,
                            onPinToStart = {
                                snapDismissContextMenu()
                                onPinToStart(app)
                            },
                            onUninstall = {
                                onUninstall(app)
                                dismissContextMenu()
                            },
                            onLaunchAppOption = { option ->
                                beginLaunchExit(app.packageName) {
                                    onLaunchAppOptionState.value(option)
                                }
                            },
                        )
                    },
                ) { measurables, constraints ->
                    val width = if (constraints.hasBoundedWidth) {
                        min(menuWidthPx, constraints.maxWidth)
                    } else {
                        menuWidthPx
                    }
                    val placeable = measurables.first().measure(
                        Constraints(
                            minWidth = width,
                            maxWidth = width,
                            minHeight = 0,
                            maxHeight = Constraints.Infinity,
                        ),
                    )
                    val height = (placeable.height * revealFraction)
                        .roundToInt()
                        .coerceIn(0, placeable.height)
                    layout(placeable.width, height) {
                        placeable.placeRelative(0, 0)
                    }
                }
            }
        }

        if (jumpListVisible && showLetterMarkers) {
            MetroJumpList(
                activeLetters = activeLetters,
                onLetterSelected = { scrollToLetter = it },
                onDismiss = { jumpListVisible = false },
                modifier = Modifier.zIndex(2f),
            )
        }
    }
}

@Composable
private fun AppListSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val accent = MetroTheme.colors.accent
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    // WP8.1 app-list search: white fill, accent border, black text.
    // Reference: references/images/applist_search_dark_blue.png
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SearchFieldBottomSpacing)
            .height(SearchFieldRowHeight)
            .background(MetroColors.LightBackground, RectangleShape)
            .border(SearchFieldBorderWidth, accent, RectangleShape)
            .focusRequester(focusRequester)
            .padding(horizontal = SearchFieldHorizontalPadding),
        textStyle = MetroTextStyle.ListItemTitle.toTextStyle().copy(
            color = MetroColors.LightPrimaryText,
        ),
        cursorBrush = SolidColor(accent),
        singleLine = true,
        decorationBox = { inner ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                inner()
            }
        },
    )
}

@Composable
private fun AppListContextMenu(
    pinToStartEnabled: Boolean,
    uninstallEnabled: Boolean,
    appOptions: List<AppLauncherOption>,
    onPinToStart: () -> Unit,
    onUninstall: () -> Unit,
    onLaunchAppOption: (AppLauncherOption) -> Unit,
) {
    val menuEntries = remember(pinToStartEnabled, uninstallEnabled, appOptions) {
        buildList {
            add(ContextMenuEntry("pin to start", enabled = pinToStartEnabled))
            add(ContextMenuEntry("uninstall", enabled = uninstallEnabled))
            appOptions.forEach { option ->
                add(ContextMenuEntry(option.label.lowercase(), option = option))
            }
        }
    }
    val scrollState = rememberScrollState()
    val needsScroll = menuEntries.size > ContextMenuMaxVisibleItems
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(min = ContextMenuMinWidth)
            .background(MetroColors.LightBackground)
            .clipToBounds()
            .padding(
                horizontal = ContextMenuHorizontalPadding,
                vertical = ContextMenuVerticalPadding,
            )
            .then(
                if (needsScroll) {
                    Modifier
                        .heightIn(max = ContextMenuMaxScrollHeight)
                        .verticalScroll(scrollState)
                } else {
                    Modifier
                },
            ),
    ) {
        menuEntries.forEach { entry ->
            AppListContextMenuItem(
                text = entry.label,
                onClick = {
                    when {
                        entry.option != null -> onLaunchAppOption(entry.option)
                        entry.label == "pin to start" -> onPinToStart()
                        entry.label == "uninstall" -> onUninstall()
                    }
                },
                enabled = entry.enabled,
            )
        }
    }
}

private data class ContextMenuEntry(
    val label: String,
    val enabled: Boolean = true,
    val option: AppLauncherOption? = null,
)

@Composable
private fun AppListContextMenuItem(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(vertical = ContextMenuVerticalPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(
            text = text,
            style = MetroTextStyle.ListItemTitle.toTextStyle().copy(
                fontSize = ContextMenuLabelSize,
                lineHeight = ContextMenuLabelLineHeight,
                color = if (enabled) {
                    MetroColors.LightPrimaryText
                } else {
                    MetroColors.LightSecondaryText
                },
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListAppRow(
    app: MetroAppInfo,
    highlightQuery: String,
    contextMenuTarget: Boolean,
    contextMenuFocusFraction: Float,
    onAppClick: () -> Unit,
    onLongClick: (Rect) -> Unit,
) {
    // Keep bounds in a ref: onGloballyPositioned fires every scroll frame, and writing
    // Compose state from it recomposes every visible row (the main list-scroll jank).
    val iconBounds = remember(app.packageName) { RectRef() }
    val accent = MetroTheme.colors.accent
    val labelText = remember(app.label, highlightQuery, accent) {
        AppListSearchLogic.highlightMatch(app.label, highlightQuery, accent)
    }

    val density = LocalDensity.current
    val activeShiftPx = with(density) { ContextMenuActiveShift.toPx() }
    AppListRowLayout(
        modifier = Modifier
            .graphicsLayer {
                if (contextMenuTarget) {
                    translationX = -contextMenuFocusFraction * activeShiftPx
                    alpha = 1f
                } else {
                    translationX = 0f
                    alpha = 1f - contextMenuFocusFraction * (1f - ContextMenuDimmedAlpha)
                }
            }
            .combinedClickable(
                onClick = onAppClick,
                onLongClick = { onLongClick(iconBounds.value) },
            ),
        iconContent = {
            AppListSquareIcon(
                packageName = app.packageName,
                label = app.label,
                onBoundsChange = { iconBounds.value = it },
            )
        },
        labelContent = {
            MetroText(
                text = labelText,
                style = MetroTextStyle.ListItemTitle,
                color = MetroColors.DarkPrimaryText,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

@Composable
private fun AppListRowLayout(
    iconContent: @Composable () -> Unit,
    labelContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ListRowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(AppListIconSize),
            contentAlignment = Alignment.Center,
        ) {
            iconContent()
        }
        Box(
            modifier = Modifier
                .padding(start = IconTextGap)
                .weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            labelContent()
        }
    }
}

@Composable
private fun LetterHeader(
    letter: Char,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(AppListIconSize)
            .border(LetterHeaderBorderWidth, MetroTheme.colors.accent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.BottomStart,
    ) {
        BasicText(
            text = letter.toString(),
            style = MetroTextStyle.ListItemTitle.toTextStyle().copy(
                color = MetroTheme.colors.accent,
                fontSize = LetterHeaderFontSize,
                lineHeight = LetterHeaderLineHeight,
            ),
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
        )
    }
}

@Composable
private fun AppListSquareIcon(
    packageName: String,
    label: String,
    onBoundsChange: (Rect) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val accentHex = MetroTheme.colors.accent.let { color ->
        // Cache key must change when system accent changes (Metro/system square fills).
        String.format("#%06X", (0xFFFFFF and color.toArgb()))
    }
    val iconPackPackage = LocalIconPackPackage.current
    val pixelSize = with(density) { AppListIconSize.roundToPx() }.coerceAtLeast(1)
    val cacheKey = remember(packageName, pixelSize, accentHex, iconPackPackage) {
        "$packageName@$pixelSize@$accentHex@${iconPackPackage.orEmpty()}"
    }
    var cached by remember(cacheKey) {
        mutableStateOf(appListIconCache.get(cacheKey))
    }

    LaunchedEffect(cacheKey) {
        if (cached != null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            val packDrawable = if (!iconPackPackage.isNullOrBlank()) {
                MetroIconPacks.loadIconForPackage(context, iconPackPackage, packageName)
            } else {
                null
            }
            if (packDrawable != null) {
                val bg = MetroAppBranding.resolveTileBackgroundColor(
                    context,
                    packageName,
                    drawable = packDrawable,
                )
                CachedAppIcon(
                    bitmap = packDrawable.toBitmap(pixelSize, pixelSize).asImageBitmap(),
                    backgroundColor = bg,
                )
            } else {
                val customBg = CustomTileBranding.resolveBackgroundColor(context, packageName)
                val customGlyph = CustomTileBranding.glyphResId(packageName)?.let { resId ->
                    ContextCompat.getDrawable(context, resId)
                }
                if (customGlyph != null && customBg != null) {
                    CachedAppIcon(
                        bitmap = customGlyph.toBitmap(pixelSize, pixelSize).asImageBitmap(),
                        backgroundColor = customBg,
                    )
                } else {
                    val asset = MetroAppBranding.loadAppIconAsset(context, packageName)
                    val bitmap = asset.drawable?.toBitmap(pixelSize, pixelSize)?.asImageBitmap()
                    CachedAppIcon(bitmap = bitmap, backgroundColor = asset.backgroundColor)
                }
            }
        }
        appListIconCache.put(cacheKey, loaded)
        cached = loaded
    }

    Box(
        modifier = Modifier
            .size(AppListIconSize)
            .onGloballyPositioned { coordinates ->
                onBoundsChange(coordinates.boundsInWindow())
            }
            .background(cached?.backgroundColor ?: Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = cached?.bitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = label,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(IconInnerPadding),
            )
        } else if (cached != null) {
            val glyph = label.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            MetroText(
                text = glyph,
                style = MetroTextStyle.ListItemTitle,
                color = MetroColors.TileContentOnAccent,
            )
        }
    }
}
