package com.metro.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import androidx.core.graphics.drawable.toBitmap
import android.util.LruCache
import com.metro.launcher.data.AppLauncherOption
import com.metro.system.MetroAppBranding
import com.metro.system.MetroAppInfo
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
private val SearchColumnGap = 20.dp
private val IconInnerPadding = 5.dp
private val IconTextGap = 12.dp
private val ListRowVerticalPadding = 4.dp
private val ListBottomScrollPadding = 180.dp
private val ContextMenuGapBelowIcon = 4.dp
private val ContextMenuHorizontalPadding = 16.dp
private val ContextMenuVerticalPadding = 12.dp
private val ContextMenuMinWidth = 160.dp
private val ContextMenuMaxVisibleItems = 6
/** ListItemTitle line (~24sp) plus top/bottom item padding. */
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

/**
 * App menu — alphabetical list of installed apps.
 * Reference: references/images/applist.png
 *
 * Column order: search | icon/letter squares | app labels.
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
) {
    var jumpListVisible by remember { mutableStateOf(false) }
    var scrollToLetter by remember { mutableStateOf<Char?>(null) }
    var contextMenuApp by remember { mutableStateOf<MetroAppInfo?>(null) }
    var contextMenuIconBounds by remember { mutableStateOf(Rect.Zero) }
    var contextMenuRootBounds by remember { mutableStateOf(Rect.Zero) }
    val contextMenuVisible = remember { MutableTransitionState(false) }
    val popupRootBounds = remember { RectRef() }
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    var imeWasVisibleWhileSearching by remember { mutableStateOf(false) }
    val menuGapPx = with(density) { ContextMenuGapBelowIcon.roundToPx() }
    val openContextMenu: (MetroAppInfo, Rect) -> Unit = { app, iconBounds ->
        contextMenuIconBounds = iconBounds
        contextMenuRootBounds = popupRootBounds.value
        contextMenuApp = app
        contextMenuVisible.targetState = true
    }
    val dismissContextMenu: () -> Unit = {
        contextMenuVisible.targetState = false
    }

    // Drop the host once the shrink animation finishes.
    LaunchedEffect(contextMenuVisible.isIdle, contextMenuVisible.currentState) {
        if (contextMenuVisible.isIdle && !contextMenuVisible.currentState) {
            contextMenuApp = null
        }
    }
    val contextMenuFocusTransition =
        updateTransition(contextMenuVisible, label = "appListContextMenuFocus")
    val contextMenuFocusFraction by contextMenuFocusTransition.animateFloat(
        transitionSpec = { tween(ContextMenuExpandMs) },
        label = "focusFraction",
    ) { visible -> if (visible) 1f else 0f }
    val showLetterMarkers = MetroJumpListLogic.showSectionMarkers(searchActive)
    val grouped = remember(apps) {
        apps.groupBy { MetroJumpListLogic.sortKey(it.label) }.toSortedMap()
    }
    val activeLetters = remember(grouped) { MetroJumpListLogic.activeLetters(grouped.keys) }
    val headerIndices = remember(grouped, showLetterMarkers) {
        var index = 0
        buildMap {
            grouped.forEach { (letter, sectionApps) ->
                if (showLetterMarkers) {
                    put(letter, index)
                    index += 1 + sectionApps.size
                } else {
                    index += sectionApps.size
                }
            }
        }
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                // Ref update only — writing Compose state here would recompose the whole list
                // on every parent layout pass.
                popupRootBounds.value = coordinates.boundsInWindow()
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(top = 4.dp, start = AppListHorizontalStartPadding, end = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .width(AppListIconSize)
                    .fillMaxHeight()
                    .padding(vertical = ListRowVerticalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MetroCircleIconButton(
                    type = MetroSystemIconType.Search,
                    onClick = {
                        if (searchActive) {
                            focusSearchField()
                        } else {
                            onSearchActiveChange(true)
                        }
                    },
                    size = AppListIconSize,
                    contentDescription = "search",
                )
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = SearchColumnGap),
            ) {
                if (searchActive) {
                    AppListSearchField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        focusRequester = searchFocusRequester,
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = ListBottomScrollPadding),
                ) {
                    if (showLetterMarkers) {
                        grouped.forEach { (letter, sectionApps) ->
                            metroStickyLetterHeader(letter = letter) {
                                // Opaque bg so app rows do not show through while pinned.
                                AppListRowLayout(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black),
                                    iconContent = {
                                        LetterHeader(
                                            letter = letter,
                                            onClick = { jumpListVisible = true },
                                        )
                                    },
                                    labelContent = {},
                                )
                            }
                            items(
                                items = sectionApps,
                                key = { it.packageName },
                                contentType = { "app" },
                            ) { app ->
                                AppListAppRow(
                                    app = app,
                                    highlightQuery = "",
                                    contextMenuTarget = contextMenuApp?.packageName == app.packageName,
                                    contextMenuFocusFraction = contextMenuFocusFraction,
                                    onAppClick = { onAppClick(app) },
                                    onLongClick = { iconBounds -> openContextMenu(app, iconBounds) },
                                )
                            }
                        }
                    } else {
                        items(
                            items = apps,
                            key = { it.packageName },
                            contentType = { "app" },
                        ) { app ->
                            AppListAppRow(
                                app = app,
                                highlightQuery = searchQuery,
                                contextMenuTarget = contextMenuApp?.packageName == app.packageName,
                                contextMenuFocusFraction = contextMenuFocusFraction,
                                onAppClick = { onAppClick(app) },
                                onLongClick = { iconBounds -> openContextMenu(app, iconBounds) },
                            )
                        }
                    }
                }
            }
        }

        contextMenuApp?.let { app ->
            var appOptions by remember(app.packageName) { mutableStateOf<List<AppLauncherOption>>(emptyList()) }
            LaunchedEffect(app.packageName) {
                appOptions = queryAppOptions(app.packageName)
            }
            val menuOffset = IntOffset(
                x = (contextMenuIconBounds.left - contextMenuRootBounds.left).toInt(),
                y = (contextMenuIconBounds.bottom - contextMenuRootBounds.top + menuGapPx).toInt(),
            )
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
                            uninstallEnabled = !app.isSystemApp,
                            appOptions = appOptions,
                            onPinToStart = {
                                onPinToStart(app)
                                dismissContextMenu()
                            },
                            onUninstall = {
                                onUninstall(app)
                                dismissContextMenu()
                            },
                            onLaunchAppOption = { option ->
                                onLaunchAppOption(option)
                                dismissContextMenu()
                            },
                        )
                    },
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(
                        constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity),
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
    uninstallEnabled: Boolean,
    appOptions: List<AppLauncherOption>,
    onPinToStart: () -> Unit,
    onUninstall: () -> Unit,
    onLaunchAppOption: (AppLauncherOption) -> Unit,
) {
    val menuEntries = remember(uninstallEnabled, appOptions) {
        buildList {
            add(ContextMenuEntry("pin to start"))
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
            .widthIn(min = ContextMenuMinWidth)
            .background(MetroColors.LightBackground)
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
        MetroText(
            text = text,
            style = MetroTextStyle.ListItemTitle,
            color = if (enabled) {
                MetroColors.LightPrimaryText
            } else {
                MetroColors.LightSecondaryText
            },
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
) {
    Box(
        modifier = Modifier
            .size(AppListIconSize)
            .border(1.dp, MetroTheme.colors.accent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.BottomStart,
    ) {
        MetroText(
            text = letter.toString(),
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.accent,
            modifier = Modifier.padding(start = 5.dp, bottom = 3.dp),
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
    val pixelSize = with(density) { AppListIconSize.roundToPx() }.coerceAtLeast(1)
    val cacheKey = remember(packageName, pixelSize) { "$packageName@$pixelSize" }
    var cached by remember(cacheKey) {
        mutableStateOf(appListIconCache.get(cacheKey))
    }

    LaunchedEffect(cacheKey) {
        if (cached != null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            val asset = MetroAppBranding.loadAppIconAsset(context, packageName)
            val bitmap = asset.drawable?.toBitmap(pixelSize, pixelSize)?.asImageBitmap()
            CachedAppIcon(bitmap = bitmap, backgroundColor = asset.backgroundColor)
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
