package com.metro.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.metro.system.MetroAppBranding
import com.metro.system.MetroPreferences
import com.metro.ui.MetroAppGlyphs
import com.metro.ui.MetroColors
import com.metro.ui.MetroMultiSelectItem
import com.metro.ui.MetroMultiSelectList
import com.metro.ui.MetroTheme

private val HiddenAppIconSize = 40.dp
private val HiddenAppIconInset = 6.dp

/**
 * Settings → hidden apps — multi-select which packages are omitted from the launcher app list.
 * Checked = hidden.
 */
@Composable
fun HiddenAppsScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf(state.hiddenAppPackages) }
    LaunchedEffect(state.hiddenAppPackages) {
        draft = state.hiddenAppPackages
    }
    val items = remember(state.applicationEntries) {
        state.applicationEntries.map { entry ->
            MetroMultiSelectItem(id = entry.packageName, title = entry.title)
        }
    }

    MetroMultiSelectList(
        title = stringResource(R.string.settings_hidden_apps_section),
        items = items,
        selectedIds = draft,
        onSelectionChange = { draft = it },
        onConfirm = {
            state.applyHiddenAppPackages(draft)
            state.goBack()
        },
        onCancel = state::goBack,
        modifier = modifier,
        itemLeading = { item ->
            HiddenAppIcon(packageName = item.id, label = item.title)
        },
        confirmLabel = stringResource(R.string.settings_hidden_apps_save),
        cancelLabel = stringResource(R.string.settings_hidden_apps_cancel),
    )
}

@Composable
private fun HiddenAppIcon(
    packageName: String,
    label: String,
) {
    val context = LocalContext.current
    val accent = MetroTheme.colors.accent
    val pixelSize = with(LocalDensity.current) { HiddenAppIconSize.roundToPx() }.coerceAtLeast(1)
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
            .size(HiddenAppIconSize)
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
                        .padding(HiddenAppIconInset),
                )
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap,
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(HiddenAppIconInset),
                )
            }
        }
    }
}
