package com.metro.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.metro.settings.R
import com.metro.settings.data.InstalledAppEntry
import com.metro.system.MetroAppBranding
import com.metro.system.MetroPreferences
import com.metro.ui.MetroAppGlyphs
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroMultiSelectItem
import com.metro.ui.MetroMultiSelectList
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

private val ConnectedHubRowHeight = 90.dp
private val ConnectedAppIconSize = 40.dp
private val ConnectedAppIconInset = 4.dp
private val ConnectedHubGlyphSize = 52.dp
private val ConnectedSelectedRowHeight = 56.dp

/**
 * Settings → connected apps hub (WP8.1 email+account layout: icon + title + subtitle).
 */
@Composable
fun ConnectedAppsScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        MetroSettingsHeader(pageTitle = stringResource(R.string.settings_connected_apps))
        SettingsBodyText(text = stringResource(R.string.settings_connected_apps_intro))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                ConnectedHubRow(
                    glyphRes = MetroAppGlyphs.Photos,
                    title = stringResource(R.string.settings_gallery_apps),
                    subtitle = stringResource(R.string.settings_gallery_apps_subtitle),
                    onClick = { state.open(SettingsRoute.GalleryApps) },
                )
            }
            item {
                ConnectedHubRow(
                    glyphRes = MetroAppGlyphs.Music,
                    title = stringResource(R.string.settings_music_apps),
                    subtitle = stringResource(R.string.settings_music_apps_subtitle),
                    onClick = { state.open(SettingsRoute.MusicApps) },
                )
            }
        }
    }
}

@Composable
private fun ConnectedHubRow(
    glyphRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    MetroListItem(
        title = title,
        subtitle = subtitle,
        modifier = Modifier.height(ConnectedHubRowHeight),
        leading = {
            Image(
                painter = painterResource(glyphRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(MetroTheme.colors.primaryText),
                modifier = Modifier.size(ConnectedHubGlyphSize),
            )
        },
        onClick = onClick,
    )
}

/**
 * Gallery / Music apps page — WP8.1 apps corner: selected apps + "tap to select apps".
 */
@Composable
fun ConnectedAppListScreen(
    state: SettingsState,
    kind: ConnectedAppKind,
    modifier: Modifier = Modifier,
) {
    val packages = when (kind) {
        ConnectedAppKind.Gallery -> state.galleryAppPackages
        ConnectedAppKind.Music -> state.musicAppPackages
    }
    val selected = remember(packages, state.applicationEntries) {
        packages.mapNotNull { pkg ->
            state.applicationEntries.firstOrNull { it.packageName == pkg }
                ?: state.applications.loadApp(pkg)
        }.sortedBy { it.title.lowercase() }
    }
    val pageTitle = when (kind) {
        ConnectedAppKind.Gallery -> stringResource(R.string.settings_gallery_apps_title)
        ConnectedAppKind.Music -> stringResource(R.string.settings_music_apps_title)
    }
    val intro = when (kind) {
        ConnectedAppKind.Gallery -> stringResource(R.string.settings_gallery_apps_intro)
        ConnectedAppKind.Music -> stringResource(R.string.settings_music_apps_intro)
    }
    val pickerRoute = when (kind) {
        ConnectedAppKind.Gallery -> SettingsRoute.GalleryAppPicker
        ConnectedAppKind.Music -> SettingsRoute.MusicAppPicker
    }

    Column(modifier = modifier.fillMaxSize()) {
        MetroSettingsHeader(pageTitle = pageTitle)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                SettingsBodyText(text = intro)
            }
            if (selected.isEmpty()) {
                item {
                    MetroText(
                        text = stringResource(R.string.settings_connected_apps_empty),
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.padding(
                            horizontal = MetroDimens.ScreenHorizontalMargin,
                            vertical = 8.dp,
                        ),
                    )
                }
            } else {
                items(selected, key = { it.packageName }) { entry ->
                    ConnectedSelectedAppRow(entry = entry)
                }
            }
            item {
                MetroListItem(
                    title = stringResource(R.string.settings_connected_apps_section),
                    subtitle = stringResource(R.string.settings_connected_apps_tap_select),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .height(ConnectedHubRowHeight),
                    onClick = { state.open(pickerRoute) },
                )
            }
        }
    }
}

@Composable
private fun ConnectedSelectedAppRow(entry: InstalledAppEntry) {
    MetroListItem(
        title = entry.title,
        modifier = Modifier.height(ConnectedSelectedRowHeight),
        leading = {
            ConnectedAppIcon(packageName = entry.packageName, label = entry.title)
        },
        verticalPadding = 6.dp,
        oneLineMinHeight = ConnectedSelectedRowHeight,
    )
}

/**
 * Multi-select app picker — WP Apps Corner list via [MetroMultiSelectList].
 */
@Composable
fun ConnectedAppPickerScreen(
    state: SettingsState,
    kind: ConnectedAppKind,
    modifier: Modifier = Modifier,
) {
    val committed = when (kind) {
        ConnectedAppKind.Gallery -> state.galleryAppPackages
        ConnectedAppKind.Music -> state.musicAppPackages
    }
    var draft by remember { mutableStateOf(committed) }
    LaunchedEffect(committed) {
        draft = committed
    }
    val items = remember(state.applicationEntries) {
        state.applicationEntries.map { entry ->
            MetroMultiSelectItem(id = entry.packageName, title = entry.title)
        }
    }

    MetroMultiSelectList(
        title = stringResource(R.string.settings_connected_apps_section),
        items = items,
        selectedIds = draft,
        onSelectionChange = { draft = it },
        onConfirm = {
            when (kind) {
                ConnectedAppKind.Gallery -> state.applyGalleryAppPackages(draft)
                ConnectedAppKind.Music -> state.applyMusicAppPackages(draft)
            }
            state.goBack()
        },
        onCancel = state::goBack,
        modifier = modifier,
        itemLeading = { item ->
            ConnectedAppIcon(packageName = item.id, label = item.title)
        },
        confirmLabel = stringResource(R.string.settings_connected_apps_save),
        cancelLabel = stringResource(R.string.settings_connected_apps_cancel),
    )
}

@Composable
private fun ConnectedAppIcon(
    packageName: String,
    label: String,
) {
    val context = LocalContext.current
    val accent = MetroTheme.colors.accent
    val pixelSize = with(LocalDensity.current) { ConnectedAppIconSize.roundToPx() }.coerceAtLeast(1)
    val tileOverride = MetroAppGlyphs.tileOverrideEntry(packageName)
    val suiteGlyph = MetroAppGlyphs.forPackage(packageName)

    val background = remember(packageName, accent) {
        when {
            tileOverride?.backgroundHex != null ->
                MetroPreferences.parseAccentHex(tileOverride.backgroundHex!!)
            suiteGlyph != null || tileOverride != null ->
                MetroAppBranding.resolveTileBackgroundColor(context, packageName)
            else ->
                MetroAppBranding.loadAppIconAsset(context, packageName).backgroundColor
        }
    }
    val contentColor = MetroColors.tileContentColor(background)
    val glyphRes = tileOverride?.glyphResId ?: suiteGlyph
    val bitmap = remember(packageName, pixelSize, glyphRes) {
        if (glyphRes != null) {
            null
        } else {
            MetroAppBranding.loadAppIcon(context, packageName)
                ?.toBitmap(pixelSize, pixelSize)
                ?.asImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .size(ConnectedAppIconSize)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        when {
            glyphRes != null -> {
                Image(
                    painter = painterResource(glyphRes),
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(contentColor),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ConnectedAppIconInset),
                )
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap,
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ConnectedAppIconInset),
                )
            }
            else -> {
                MetroText(
                    text = label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MetroTextStyle.ListItemTitle,
                    color = contentColor,
                )
            }
        }
    }
}

enum class ConnectedAppKind {
    Gallery,
    Music,
}
