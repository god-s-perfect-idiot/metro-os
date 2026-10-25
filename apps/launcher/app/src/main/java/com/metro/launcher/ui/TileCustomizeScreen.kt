package com.metro.launcher.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.launcher.R
import com.metro.launcher.data.DisplayTile
import com.metro.launcher.data.MusicTilePackages
import com.metro.launcher.data.PinnedTileEntry
import com.metro.launcher.data.PinnedTileSize
import com.metro.launcher.data.TileBackgroundMode
import com.metro.launcher.data.TileCustomIcon
import com.metro.launcher.data.TileWidgetOption
import com.metro.launcher.data.resolvedIconPackage
import com.metro.launcher.data.supportsCustomWidget
import com.metro.system.MetroAccentPalette
import com.metro.system.MetroIconPackInfo
import com.metro.system.MetroIconPacks
import com.metro.system.MetroMusicLiveTileStyle
import com.metro.system.MetroPreferences
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption
import com.metro.ui.MetroPivot
import com.metro.ui.MetroSlider
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Preview square matching Settings / lockscreen choose-photo thumbs. */
private val TileIconThumbSize = 108.dp

private val TileIconPlaceholderGray = Color(0xFF6E6E6E)

/** Icon-pack picker grid — same density as Start / accents (4 columns, 8dp gaps). */
private const val IconPackGridColumns = 4
private val IconPackGridGap = 8.dp

/**
 * Draft values for the tile customize page — applied only when Save is tapped.
 */
data class TileCustomizeDraft(
    val backgroundMode: TileBackgroundMode,
    val customBackgroundHex: String?,
    val launchTargetPackage: String?,
    val useCustomIcon: Boolean,
    /**
     * Per-tile icon pack. Null = follow suite global; blank = force system icons;
     * otherwise that pack package name.
     */
    val iconPackPackage: String?,
    val iconScale: Float,
    val customTitle: String,
    val hideTitle: Boolean,
    val useCustomWidget: Boolean,
    val widgetProvider: String?,
    /** Music now-playing face; only applied for connected music pins. */
    val musicLiveTileStyle: MetroMusicLiveTileStyle = MetroMusicLiveTileStyle.DEFAULT,
)

@Composable
fun TileCustomizeScreen(
    tile: DisplayTile,
    draft: TileCustomizeDraft,
    onDraftChange: (TileCustomizeDraft) -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenLaunchTargetPicker: () -> Unit,
    onRequestIconPick: () -> Unit,
    customIconReloadEpoch: Int,
    launchTargetLabel: String,
    modifier: Modifier = Modifier,
) {
    val widgetController = LocalTileAppWidgetController.current
    val showWidgetSection = tile.entry.supportsCustomWidget()
    var widgetOptions by remember(tile.entry.packageName) {
        mutableStateOf<List<TileWidgetOption>>(emptyList())
    }
    var widgetsLoading by remember(tile.entry.packageName) { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val tabCustomise = stringResource(R.string.tile_customize_tab_customise)
    val tabWidgets = stringResource(R.string.tile_customize_tab_widgets)
    val tabTiles = stringResource(R.string.tile_customize_tab_tiles)
    val pivotTitles = remember(tabCustomise, tabWidgets, tabTiles) {
        listOf(tabCustomise, tabWidgets, tabTiles)
    }

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

    MetroPivot(
        titles = pivotTitles,
        pagerState = pagerState,
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
        header = {
            // App-title overline above pivot tabs (Settings / hub pattern).
            MetroAppTitle(title = tile.title)
        },
        onTitleClick = { index ->
            scope.launch { pagerState.animateScrollToPage(index) }
        },
    ) { page ->
        when (page) {
            0 -> CustomisePage(
                tile = tile,
                draft = draft,
                onDraftChange = onDraftChange,
                onOpenColorPicker = onOpenColorPicker,
                onOpenLaunchTargetPicker = onOpenLaunchTargetPicker,
                onRequestIconPick = onRequestIconPick,
                customIconReloadEpoch = customIconReloadEpoch,
                launchTargetLabel = launchTargetLabel,
                backgroundOptions = backgroundOptions,
                customColor = customColor,
                customName = customName,
            )
            1 -> WidgetsCustomizePage(
                tile = tile,
                draft = draft,
                onDraftChange = onDraftChange,
                showWidgetSection = showWidgetSection,
                widgetsLoading = widgetsLoading,
                widgetOptions = widgetOptions,
                tileAspect = tileAspect,
                draftRevealsWindow = draftRevealsWindow,
                draftTileFill = draftTileFill,
            )
            else -> TilesCustomizePage(
                tile = tile,
                draft = draft,
                onDraftChange = onDraftChange,
                draftRevealsWindow = draftRevealsWindow,
                draftTileFill = draftTileFill,
            )
        }
    }
}

@Composable
private fun CustomisePage(
    tile: DisplayTile,
    draft: TileCustomizeDraft,
    onDraftChange: (TileCustomizeDraft) -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenLaunchTargetPicker: () -> Unit,
    onRequestIconPick: () -> Unit,
    customIconReloadEpoch: Int,
    launchTargetLabel: String,
    backgroundOptions: List<MetroListPickerOption<TileBackgroundMode>>,
    customColor: Color,
    customName: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 88.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        MetroText(
            text = stringResource(R.string.tile_customize_app_name_label),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                bottom = 4.dp,
            ),
        )
        MetroTextBox(
            value = draft.customTitle,
            onValueChange = { text ->
                onDraftChange(draft.copy(customTitle = text))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        Spacer(modifier = Modifier.height(28.dp))
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenColorPicker,
                    )
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

        Spacer(modifier = Modifier.height(28.dp))
        MetroListPicker(
            selected = draft.launchTargetPackage,
            options = emptyList<MetroListPickerOption<String?>>(),
            onSelectedChange = {},
            label = stringResource(R.string.tile_customize_launch_target_label),
            placeholder = launchTargetLabel,
            onOpen = onOpenLaunchTargetPicker,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        Spacer(modifier = Modifier.height(28.dp))
        TileIconImagePickerRow(
            packageName = tile.entry.packageName,
            tileId = tile.entry.tileId,
            tileSize = tile.entry.size,
            enabled = draft.useCustomIcon,
            reloadEpoch = customIconReloadEpoch,
            onChoosePhoto = onRequestIconPick,
            onRemove = {
                onDraftChange(draft.copy(useCustomIcon = false))
            },
        )

        Spacer(modifier = Modifier.height(28.dp))
        MetroText(
            text = stringResource(R.string.tile_customize_icon_scale_label),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                bottom = 4.dp,
            ),
        )
        MetroSlider(
            value = draft.iconScale,
            onValueChange = { scale ->
                onDraftChange(
                    draft.copy(iconScale = PinnedTileEntry.clampIconScale(scale)),
                )
            },
            valueRange = PinnedTileEntry.MIN_ICON_SCALE..PinnedTileEntry.MAX_ICON_SCALE,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        // 1×1 faces never draw an app-name footer — only medium / wide.
        if (tile.entry.size != PinnedTileSize.OneByOne) {
            Spacer(modifier = Modifier.height(28.dp))
            MetroToggleSwitch(
                checked = draft.hideTitle,
                onCheckedChange = { hide ->
                    onDraftChange(draft.copy(hideTitle = hide))
                },
                label = stringResource(R.string.tile_customize_hide_title_label),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
            )
        }
    }
}

/**
 * WP8.1 Start-background-style choose photo row for the tile icon.
 */
@Composable
private fun TileIconImagePickerRow(
    packageName: String,
    tileId: String,
    tileSize: PinnedTileSize,
    enabled: Boolean,
    reloadEpoch: Int,
    onChoosePhoto: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var thumbBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val thumbAspect = when (tileSize) {
        PinnedTileSize.FourByTwo -> 2f
        else -> 1f
    }

    LaunchedEffect(enabled, reloadEpoch, packageName, tileId) {
        thumbBitmap = if (enabled) {
            withContext(Dispatchers.IO) {
                TileCustomIcon.decodeForPreview(context, packageName, tileId)
            }
        } else {
            null
        }
    }

    Row(
        modifier = modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .height(TileIconThumbSize)
                .aspectRatio(thumbAspect)
                .background(TileIconPlaceholderGray),
        ) {
            thumbBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = stringResource(R.string.tile_customize_icon_label),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f, fill = false)) {
            MetroText(
                text = stringResource(R.string.tile_customize_icon_label),
                style = MetroTextStyle.Body,
            )
            Spacer(modifier = Modifier.size(10.dp))
            MetroBorderButton(
                text = stringResource(R.string.tile_customize_choose_photo),
                onClick = onChoosePhoto,
            )
            if (enabled) {
                Spacer(modifier = Modifier.size(12.dp))
                val removeLabel = stringResource(R.string.tile_customize_remove_photo)
                MetroText(
                    text = remember(removeLabel) {
                        buildAnnotatedString {
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append(removeLabel)
                            }
                        }
                    },
                    style = MetroTextStyle.Body,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                TileCustomIcon.clearDraft(context, packageName, tileId)
                                onRemove()
                            },
                        )
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun WidgetsCustomizePage(
    tile: DisplayTile,
    draft: TileCustomizeDraft,
    onDraftChange: (TileCustomizeDraft) -> Unit,
    showWidgetSection: Boolean,
    widgetsLoading: Boolean,
    widgetOptions: List<TileWidgetOption>,
    tileAspect: Float,
    draftRevealsWindow: Boolean,
    draftTileFill: Color,
) {
    val context = LocalContext.current
    val showMusicLiveTileStyles = MusicTilePackages.isMusicApp(context, tile.entry.packageName) &&
        tile.entry.supportsCustomWidget()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 88.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        if (showMusicLiveTileStyles) {
            MetroListPicker(
                selected = draft.musicLiveTileStyle,
                options = listOf(
                    MetroListPickerOption(
                        MetroMusicLiveTileStyle.Presentation,
                        stringResource(R.string.tile_customize_music_live_tile_presentation),
                    ),
                    MetroListPickerOption(
                        MetroMusicLiveTileStyle.CompactControls,
                        stringResource(R.string.tile_customize_music_live_tile_compact),
                    ),
                    MetroListPickerOption(
                        MetroMusicLiveTileStyle.SpaciousControls,
                        stringResource(R.string.tile_customize_music_live_tile_spacious),
                    ),
                ),
                onSelectedChange = { style ->
                    onDraftChange(draft.copy(musicLiveTileStyle = style))
                },
                label = stringResource(R.string.tile_customize_music_live_tile_label),
                modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (!showWidgetSection) {
            if (!showMusicLiveTileStyles) {
                MetroText(
                    text = stringResource(R.string.tile_customize_widget_empty),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                )
            }
            return
        }

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
                        color = MetroTheme.colors.accent,
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

/**
 * Per-tile icon-pack picker: each cell is a Start-proportioned preview of this pin under
 * that pack (1×1 / 2×2 / 4×2 span the same columns as Start). Selection updates the
 * customize draft (applied on Save) — not the suite-wide preference.
 */
@Composable
private fun TilesCustomizePage(
    tile: DisplayTile,
    draft: TileCustomizeDraft,
    onDraftChange: (TileCustomizeDraft) -> Unit,
    draftRevealsWindow: Boolean,
    draftTileFill: Color,
) {
    val context = LocalContext.current
    var packs by remember { mutableStateOf<List<MetroIconPackInfo>?>(null) }
    LaunchedEffect(Unit) {
        packs = withContext(Dispatchers.IO) {
            MetroIconPacks.listInstalled(context)
        }
    }

    val noneLabel = stringResource(R.string.tile_customize_tiles_none)
    val chromeOldLabel = stringResource(R.string.tile_customize_tiles_chrome_old)
    val showChromeOldOption = isChromeTilePackage(tile.entry.packageName)
    val installedPacks = packs
    val options: List<IconPackGridOption> = remember(
        installedPacks,
        noneLabel,
        chromeOldLabel,
        showChromeOldOption,
    ) {
        val installed = installedPacks.orEmpty()
        buildList {
            add(IconPackGridOption(packageName = null, label = noneLabel))
            if (showChromeOldOption) {
                add(
                    IconPackGridOption(
                        packageName = PinnedTileEntry.CHROME_LEGACY_FACE,
                        label = chromeOldLabel,
                    ),
                )
            }
            installed.forEach { pack ->
                add(IconPackGridOption(packageName = pack.packageName, label = pack.label))
            }
        }
    }
    val packsLoaded = installedPacks != null

    val tileSize = tile.entry.size
    val colSpan = tileSize.colSpan.coerceIn(1, IconPackGridColumns)
    val iconPackage = tile.entry.resolvedIconPackage()
    val faceTitle = draft.customTitle.takeIf { it.isNotBlank() } ?: tile.title
    val showTitle = tileSize != PinnedTileSize.OneByOne && !draft.hideTitle
    val contentColor = if (draftRevealsWindow) {
        Color.White
    } else {
        MetroTheme.colors.primaryText
    }
    // Null draft = follow suite global — highlight whatever Start would show today.
    val globalPack = LocalIconPackPackage.current
    val highlightPack = when {
        draft.iconPackPackage == PinnedTileEntry.CHROME_LEGACY_FACE ->
            PinnedTileEntry.CHROME_LEGACY_FACE
        draft.iconPackPackage != null -> draft.iconPackPackage.takeIf { it.isNotBlank() }
        else -> globalPack
    }

    if (!packsLoaded) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 88.dp),
    ) {
        if (options.count() <= 1) {
            MetroText(
                text = stringResource(R.string.tile_customize_tiles_empty),
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(
                    horizontal = MetroDimens.ScreenHorizontalMargin,
                    vertical = 12.dp,
                ),
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    horizontal = MetroDimens.ScreenHorizontalMargin,
                    vertical = 12.dp,
                ),
        ) {
            // Unit cell = 1×1 Start column; medium/wide previews span 2 / 4 columns.
            val unitWidth =
                (maxWidth - IconPackGridGap * (IconPackGridColumns - 1)) / IconPackGridColumns
            val (previewWidth, previewHeight) = tilePixelSize(
                unit = unitWidth,
                colSpan = colSpan,
                rowSpan = tileSize.rowSpan,
                gap = IconPackGridGap,
            )
            val previewAspect = previewWidth / previewHeight
            LazyVerticalGrid(
                columns = GridCells.Fixed(IconPackGridColumns),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(IconPackGridGap),
                verticalArrangement = Arrangement.spacedBy(IconPackGridGap),
            ) {
                items(
                    items = options,
                    key = { option: IconPackGridOption -> option.packageName ?: "none" },
                    span = { GridItemSpan(colSpan) },
                ) { option: IconPackGridOption ->
                    val selected = option.packageName == highlightPack ||
                        (option.packageName == null && highlightPack.isNullOrBlank())
                    val chromeLegacy =
                        option.packageName == PinnedTileEntry.CHROME_LEGACY_FACE
                    IconPackPreviewTile(
                        packageName = iconPackage,
                        iconPackPackage = option.packageName,
                        label = option.label,
                        title = faceTitle.takeIf { showTitle },
                        selected = selected,
                        chromeLegacy = chromeLegacy,
                        aspectRatio = previewAspect,
                        cellWidth = previewWidth,
                        tileSize = tileSize,
                        iconScale = draft.iconScale,
                        useWindowFill = draftRevealsWindow && !chromeLegacy,
                        tileFill = draftTileFill,
                        contentColor = contentColor,
                        onClick = {
                            onDraftChange(
                                draft.copy(
                                    // null option → blank = force system for this tile only.
                                    iconPackPackage = option.packageName.orEmpty(),
                                    useCustomIcon = false,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

private data class IconPackGridOption(
    val packageName: String?,
    val label: String,
)

/**
 * Mini Start tile showing [packageName]'s glyph under [iconPackPackage] (null = system),
 * or the legacy Chrome wedge face when [chromeLegacy] is true.
 */
@Composable
private fun IconPackPreviewTile(
    packageName: String,
    iconPackPackage: String?,
    label: String,
    title: String?,
    selected: Boolean,
    chromeLegacy: Boolean,
    aspectRatio: Float,
    cellWidth: Dp,
    tileSize: PinnedTileSize,
    iconScale: Float,
    useWindowFill: Boolean,
    tileFill: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    val chrome = TileChrome.Standard
    val cellHeight = cellWidth / aspectRatio
    val iconSize = chrome.iconSize(cellWidth, cellHeight, tileSize) *
        PinnedTileEntry.clampIconScale(iconScale)
    val borderWidth = if (selected) 4.dp else 0.dp
    val borderColor = MetroTheme.colors.accent
    val startBackground = LocalStartBackgroundViewport.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .then(
                when {
                    chromeLegacy -> Modifier
                    useWindowFill -> Modifier.drawStartBackgroundWindow(startBackground)
                    else -> Modifier.background(tileFill)
                },
            )
            .then(
                if (selected) {
                    Modifier.border(borderWidth, borderColor)
                } else {
                    Modifier
                },
            ),
    ) {
        if (chromeLegacy) {
            // Title is drawn inside ChromeTileContent (white on brand wedges).
            ChromeTileContent(
                title = title,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                // Blank pack string forces system icons for the "none" cell.
                MetroAppIcon(
                    packageName = packageName,
                    size = iconSize,
                    modifier = Modifier.size(iconSize),
                    contentDescription = label,
                    fallbackLabel = title ?: label,
                    fallbackColor = contentColor,
                    iconPackPackage = iconPackPackage.orEmpty(),
                )
            }
            if (!title.isNullOrBlank()) {
                TileText(
                    text = title,
                    style = chrome.titleStyle,
                    color = contentColor,
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
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
