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
import androidx.compose.ui.unit.dp
import com.metro.launcher.R
import com.metro.launcher.data.DisplayTile
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
                            WidgetProviderPreviewCard(
                                option = option,
                                selected = draft.widgetProvider ==
                                    option.provider.flattenToString(),
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
                                        vertical = 6.dp,
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
 * Selectable App Widget preview — square Metro border, provider preview art, label under.
 */
@Composable
private fun WidgetProviderPreviewCard(
    option: TileWidgetOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) MetroTheme.colors.accent else MetroTheme.colors.primaryText
    val aspect = remember(option.minWidth, option.minHeight) {
        val w = option.minWidth.coerceAtLeast(1).toFloat()
        val h = option.minHeight.coerceAtLeast(1).toFloat()
        (w / h).coerceIn(0.75f, 2.5f)
    }
    val preview = remember(option.previewBitmap) {
        option.previewBitmap?.asImageBitmap()
    }

    Column(
        modifier = modifier
            .border(2.dp, borderColor)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center,
        ) {
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = option.label,
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.Medium,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                )
            } else {
                MetroText(
                    text = option.label.take(1).uppercase(),
                    style = MetroTextStyle.PageTitle,
                    color = MetroTheme.colors.secondaryText,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        MetroText(
            text = option.label,
            style = MetroTextStyle.Body,
            color = if (selected) MetroTheme.colors.accent else MetroTheme.colors.primaryText,
        )
    }
}
