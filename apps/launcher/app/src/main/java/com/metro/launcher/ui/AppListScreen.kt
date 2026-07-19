package com.metro.launcher.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.drawable.toBitmap
import android.util.LruCache
import com.metro.launcher.data.SystemAppPlaceholders
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
    modifier: Modifier = Modifier,
) {
    var jumpListVisible by remember { mutableStateOf(false) }
    var scrollToLetter by remember { mutableStateOf<Char?>(null) }
    var contextMenuApp by remember { mutableStateOf<MetroAppInfo?>(null) }
    var contextMenuIconBounds by remember { mutableStateOf(Rect.Zero) }
    var contextMenuRootBounds by remember { mutableStateOf(Rect.Zero) }
    val popupRootBounds = remember { RectRef() }
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    var imeWasVisibleWhileSearching by remember { mutableStateOf(false) }
    val menuGapPx = with(density) { ContextMenuGapBelowIcon.roundToPx() }
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
                .padding(top = 4.dp, start = AppListHorizontalStartPadding, end = 24.dp),
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

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = SearchColumnGap),
                contentPadding = PaddingValues(bottom = ListBottomScrollPadding),
            ) {
                if (searchActive) {
                    item(key = "search-field") {
                        AppListSearchField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            focusRequester = searchFocusRequester,
                        )
                    }
                }

                if (showLetterMarkers) {
                    grouped.forEach { (letter, sectionApps) ->
                        item(key = "header-$letter", contentType = "header") {
                            AppListRowLayout(
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
                                onAppClick = { onAppClick(app) },
                                onLongClick = { iconBounds ->
                                    contextMenuIconBounds = iconBounds
                                    contextMenuRootBounds = popupRootBounds.value
                                    contextMenuApp = app
                                },
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
                            onAppClick = { onAppClick(app) },
                            onLongClick = { iconBounds ->
                                contextMenuIconBounds = iconBounds
                                contextMenuRootBounds = popupRootBounds.value
                                contextMenuApp = app
                            },
                        )
                    }
                }
            }
        }

        contextMenuApp?.let { app ->
            val menuOffset = IntOffset(
                x = (contextMenuIconBounds.left - contextMenuRootBounds.left).toInt(),
                y = (contextMenuIconBounds.bottom - contextMenuRootBounds.top + menuGapPx).toInt(),
            )
            Popup(
                alignment = Alignment.TopStart,
                offset = menuOffset,
                onDismissRequest = { contextMenuApp = null },
                properties = PopupProperties(focusable = true),
            ) {
                AppListContextMenu(
                    uninstallEnabled = !app.isSystemApp,
                    onPinToStart = {
                        onPinToStart(app)
                        contextMenuApp = null
                    },
                    onUninstall = {
                        onUninstall(app)
                        contextMenuApp = null
                    },
                )
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
    onPinToStart: () -> Unit,
    onUninstall: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(min = ContextMenuMinWidth)
            .background(MetroColors.LightBackground)
            .padding(
                horizontal = ContextMenuHorizontalPadding,
                vertical = ContextMenuVerticalPadding,
            ),
    ) {
        AppListContextMenuItem(
            text = "pin to start",
            onClick = onPinToStart,
        )
        AppListContextMenuItem(
            text = "uninstall",
            onClick = onUninstall,
            enabled = uninstallEnabled,
        )
    }
}

@Composable
private fun AppListContextMenuItem(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
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

    AppListRowLayout(
        modifier = Modifier.combinedClickable(
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
    val placeholderResId = remember(packageName) { SystemAppPlaceholders.iconResId(packageName) }
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
        } else if (placeholderResId != null) {
            Image(
                painter = painterResource(placeholderResId),
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
