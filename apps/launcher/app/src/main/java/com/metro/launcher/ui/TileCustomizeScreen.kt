package com.metro.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.launcher.R
import com.metro.launcher.data.DisplayTile
import com.metro.launcher.data.PinnedTileSize
import com.metro.launcher.data.TileBackgroundMode
import com.metro.launcher.data.TileWidgetOption
import com.metro.launcher.data.supportsCustomWidget
import com.metro.system.MetroAccentPalette
import com.metro.system.MetroPreferences
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Draft values for the tile customize page — applied only when Save is tapped.
 */
data class TileCustomizeDraft(
    val backgroundMode: TileBackgroundMode,
    val customBackgroundHex: String?,
    val useCustomWidget: Boolean,
    val widgetProvider: String?,
)

@Composable
fun TileCustomizeScreen(
    tile: DisplayTile,
    draft: TileCustomizeDraft,
    onDraftChange: (TileCustomizeDraft) -> Unit,
    onOpenColorPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val widgetController = LocalTileAppWidgetController.current
    val showWidgetSection = tile.entry.supportsCustomWidget()
    var widgetOptions by remember(tile.entry.packageName) {
        mutableStateOf<List<TileWidgetOption>>(emptyList())
    }
    var widgetsLoading by remember(tile.entry.packageName) { mutableStateOf(false) }

    // Defer AppWidgetManager + preview decode until the toggle is on — scanning providers
    // on open blocked the customize pivot / app-bar enter.
    LaunchedEffect(draft.useCustomWidget, tile.entry.packageName, widgetController) {
        if (!draft.useCustomWidget || widgetController == null) {
            widgetOptions = emptyList()
            widgetsLoading = false
            return@LaunchedEffect
        }
        widgetsLoading = true
        val options = withContext(Dispatchers.IO) {
            widgetController.listProvidersForPackage(tile.entry.packageName)
        }
        widgetOptions = options
        widgetsLoading = false
        // Auto-select the first provider when enabling with nothing chosen yet.
        if (draft.widgetProvider.isNullOrBlank() && options.isNotEmpty()) {
            onDraftChange(
                draft.copy(widgetProvider = options.first().provider.flattenToString()),
            )
        }
    }

    val backgroundOptions = listOf(
        MetroListPickerOption(
            TileBackgroundMode.Default,
            stringResource(R.string.tile_customize_bg_default),
        ),
        MetroListPickerOption(
            TileBackgroundMode.Accent,
            stringResource(R.string.tile_customize_bg_accent),
        ),
        MetroListPickerOption(
            TileBackgroundMode.Custom,
            stringResource(R.string.tile_customize_bg_custom),
        ),
    )
    val customHex = draft.customBackgroundHex
        ?.let { MetroAccentPalette.normalizeHex(it) }
    val customColor = customHex?.let { MetroPreferences.parseAccentHex(it) }
        ?: MetroTheme.colors.accent
    val customName = customHex?.let { MetroAccentPalette.displayName(it) }
        ?: stringResource(R.string.tile_customize_bg_custom)

    val startBackground = LocalStartBackgroundViewport.current
    val draftRevealsWindow = when (draft.backgroundMode) {
        TileBackgroundMode.Custom -> false
        TileBackgroundMode.Accent -> startBackground != null
        TileBackgroundMode.Default -> tile.revealsStartBackground && startBackground != null
    }
    val draftTileFill = when (draft.backgroundMode) {
        TileBackgroundMode.Accent -> MetroTheme.colors.accent
        TileBackgroundMode.Custom -> customColor
        TileBackgroundMode.Default -> tile.backgroundColor
    }
    val tileAspect = when (tile.entry.size) {
        PinnedTileSize.FourByTwo -> 2f
        else -> 1f
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 88.dp),
    ) {
        // Tight page title — avoid MetroPageHeader's 98dp band under the status bar.
        MetroText(
            text = tile.title,
            style = MetroTextStyle.PageTitle,
            color = MetroTheme.colors.primaryText,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                top = 8.dp,
                bottom = 16.dp,
            ),
        )

        MetroListPicker(
            selected = draft.backgroundMode,
            options = backgroundOptions,
            onSelectedChange = { mode ->
                onDraftChange(draft.copy(backgroundMode = mode))
            },
            label = stringResource(R.string.tile_customize_bg_label),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        if (draft.backgroundMode == TileBackgroundMode.Custom) {
            Spacer(modifier = Modifier.height(20.dp))
            MetroText(
                text = stringResource(R.string.tile_customize_color_picker),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    end = MetroDimens.ScreenHorizontalMargin,
                    bottom = 4.dp,
                ),
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .border(2.dp, MetroTheme.colors.primaryText)
                    .clickable(onClick = onOpenColorPicker)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(customColor),
                )
                Spacer(modifier = Modifier.width(12.dp))
                MetroText(
                    text = customName,
                    style = MetroTextStyle.Body,
                )
            }
        }

        if (showWidgetSection) {
            Spacer(modifier = Modifier.height(28.dp))
            MetroToggleSwitch(
                checked = draft.useCustomWidget,
                onCheckedChange = { enabled ->
                    onDraftChange(
                        draft.copy(
                            useCustomWidget = enabled,
                            widgetProvider = if (enabled) draft.widgetProvider else null,
                        ),
                    )
                },
                label = stringResource(R.string.tile_customize_widget_label),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
            )

            if (draft.useCustomWidget) {
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    widgetsLoading -> Unit
                    widgetOptions.isEmpty() -> {
                        MetroText(
                            text = stringResource(R.string.tile_customize_widget_empty),
                            style = MetroTextStyle.Body,
                            color = MetroTheme.colors.secondaryText,
                            modifier = Modifier.padding(
                                horizontal = MetroDimens.ScreenHorizontalMargin,
                            ),
                        )
                    }
                    else -> {
                        MetroText(
                            text = stringResource(R.string.tile_customize_widget_pick),
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                            modifier = Modifier.padding(
                                start = MetroDimens.ScreenHorizontalMargin,
                                end = MetroDimens.ScreenHorizontalMargin,
                                bottom = 8.dp,
                            ),
                        )
                        widgetOptions.forEach { option ->
                            WidgetProviderPreviewTile(
                                option = option,
                                selected = draft.widgetProvider ==
                                    option.provider.flattenToString(),
                                aspectRatio = tileAspect,
                                useWindowFill = draftRevealsWindow,
                                tileFill = draftTileFill,
                                onClick = {
                                    onDraftChange(
                                        draft.copy(
                                            widgetProvider = option.provider.flattenToString(),
                                        ),
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = MetroDimens.ScreenHorizontalMargin,
                                        vertical = 8.dp,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Selectable widget option framed as a Start tile — transparent wallpaper window or solid
 * Metro fill, matching how the launcher hosts the widget after Save.
 */
@Composable
private fun WidgetProviderPreviewTile(
    option: TileWidgetOption,
    selected: Boolean,
    aspectRatio: Float,
    useWindowFill: Boolean,
    tileFill: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Selected border is thicker so the pick reads clearly on wallpaper previews.
    val borderWidth = if (selected) 4.dp else 2.dp
    val borderColor = if (selected) MetroTheme.colors.accent else MetroTheme.colors.primaryText
    val preview = remember(option.previewBitmap) {
        option.previewBitmap?.asImageBitmap()
    }
    val startBackground = LocalStartBackgroundViewport.current
    val chrome = TileChrome.Standard
    val titleColor = if (useWindowFill) Color.White else MetroTheme.colors.primaryText

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clickable(onClick = onClick)
            .then(
                if (useWindowFill) {
                    Modifier.drawStartBackgroundWindow(startBackground)
                } else {
                    Modifier.background(tileFill)
                },
            )
            .border(borderWidth, borderColor),
    ) {
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = option.label,
                // Fit keeps provider art inside the tile without inventing a Material card plate.
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Bottom-left app title — same placement as Start tile faces.
        TileText(
            text = option.label,
            style = chrome.titleStyle,
            color = titleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    horizontal = chrome.titlePaddingH,
                    vertical = chrome.titlePaddingV,
                ),
        )
    }
}
