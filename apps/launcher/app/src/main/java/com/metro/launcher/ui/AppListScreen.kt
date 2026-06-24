package com.metro.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.drawable.toBitmap
import com.metro.launcher.data.SystemAppPlaceholders
import com.metro.system.MetroAppBranding
import com.metro.system.MetroAppInfo
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroColors
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

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
private val SearchFieldRowHeight = 36.dp
private val AppListHorizontalStartPadding = 12.dp

/**
 * App menu — alphabetical list of installed apps.
 * Reference: references/images/applist.png
 *
 * Column order: search | icon/letter squares | app labels.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListScreen(
    apps: List<MetroAppInfo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAppClick: (MetroAppInfo) -> Unit,
    onPinToStart: (MetroAppInfo) -> Unit,
    onUninstall: (MetroAppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchActive by remember { mutableStateOf(false) }
    var contextMenuApp by remember { mutableStateOf<MetroAppInfo?>(null) }
    var contextMenuIconBounds by remember { mutableStateOf(Rect.Zero) }
    var popupRootBounds by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val menuGapPx = with(density) { ContextMenuGapBelowIcon.roundToPx() }
    val grouped = remember(apps) {
        apps.groupBy { it.label.first().lowercaseChar() }.toSortedMap()
    }
    val dismissSearch = {
        searchActive = false
        onSearchQueryChange("")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                popupRootBounds = coordinates.boundsInWindow()
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(top = 4.dp, start = 12.dp, end = 24.dp),
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
                    onClick = { searchActive = !searchActive },
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
                modifier = Modifier
                    .weight(1f)
                    .padding(start = SearchColumnGap),
                contentPadding = PaddingValues(bottom = ListBottomScrollPadding),
            ) {
                if (searchActive) {
                    item(key = "search-field") {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            textStyle = MetroTextStyle.ListItemTitle.toTextStyle().copy(
                                color = MetroColors.DarkPrimaryText,
                            ),
                            cursorBrush = SolidColor(MetroTheme.colors.accent),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    MetroText(
                                        text = "search",
                                        style = MetroTextStyle.ListItemTitle,
                                        color = MetroColors.DarkSecondaryText,
                                    )
                                }
                                inner()
                            },
                        )
                    }
                }

                grouped.forEach { (letter, sectionApps) ->
                    item(key = "header-$letter") {
                        AppListRowLayout(
                            iconContent = { LetterHeader(letter = letter) },
                            labelContent = {},
                        )
                    }
                    items(sectionApps, key = { it.packageName }) { app ->
                        AppListAppRow(
                            app = app,
                            onAppClick = { onAppClick(app) },
                            onLongClick = { iconBounds ->
                                contextMenuIconBounds = iconBounds
                                contextMenuApp = app
                            },
                        )
                    }
                }
            }
        }

        if (searchActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .padding(
                        start = AppListHorizontalStartPadding + AppListIconSize + SearchColumnGap,
                        top = 4.dp + SearchFieldRowHeight,
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = dismissSearch,
                    ),
            )
        }

        contextMenuApp?.let { app ->
            val menuOffset = IntOffset(
                x = (contextMenuIconBounds.left - popupRootBounds.left).toInt(),
                y = (contextMenuIconBounds.bottom - popupRootBounds.top + menuGapPx).toInt(),
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
    }
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
    onAppClick: () -> Unit,
    onLongClick: (Rect) -> Unit,
) {
    var iconBounds by remember(app.packageName) { mutableStateOf(Rect.Zero) }

    AppListRowLayout(
        modifier = Modifier.combinedClickable(
            onClick = onAppClick,
            onLongClick = { onLongClick(iconBounds) },
        ),
        iconContent = {
            AppListSquareIcon(
                packageName = app.packageName,
                label = app.label,
                onBoundsChange = { iconBounds = it },
            )
        },
        labelContent = {
            MetroText(
                text = app.label,
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
private fun LetterHeader(letter: Char) {
    Box(
        modifier = Modifier
            .size(AppListIconSize)
            .border(1.dp, MetroTheme.colors.accent),
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
    val asset = remember(packageName) { MetroAppBranding.loadAppIconAsset(context, packageName) }
    val placeholderResId = remember(packageName) { SystemAppPlaceholders.iconResId(packageName) }
    val bitmap = remember(packageName, pixelSize) {
        asset.drawable?.toBitmap(pixelSize, pixelSize)?.asImageBitmap()
    }

    Box(
        modifier = Modifier
            .size(AppListIconSize)
            .onGloballyPositioned { coordinates ->
                onBoundsChange(coordinates.boundsInWindow())
            }
            .background(asset.backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
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
        } else {
            val glyph = label.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            MetroText(
                text = glyph,
                style = MetroTextStyle.ListItemTitle,
                color = MetroColors.TileContentOnAccent,
            )
        }
    }
}
