package com.metro.hub.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.hub.R
import com.metro.hub.data.DeviceAppRow
import com.metro.hub.data.HubAppCatalog
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppGlyphs
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/**
 * Installed apps: Store-style square icon + name/description + circular download control.
 */
@Composable
fun DeviceScreen(
    state: HubState,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observe = generation

    Column(modifier = modifier.fillMaxSize()) {
        MetroAppTitle(title = stringResource(R.string.app_name))
        MetroText(
            text = stringResource(R.string.device_title),
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp),
        )

        when {
            state.deviceAppsLoading && state.deviceApps.isEmpty() -> {
                MetroLoadingScreen(
                    message = stringResource(R.string.apps_loading),
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = MetroAppBarDefaults.BarHeight),
                )
            }
            state.deviceApps.isEmpty() -> {
                MetroText(
                    text = stringResource(R.string.device_empty),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(12.dp),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = MetroAppBarDefaults.BarHeight + ListBottomExtraPadding,
                    ),
                ) {
                    items(state.deviceApps, key = { it.packageName }) { app ->
                        DeviceAppRowItem(
                            app = app,
                            iconPath = app.catalogAsset?.let { state.iconPathFor(it) },
                            downloading = state.downloadingAssetName == app.updateAsset?.name,
                            onVisible = {
                                app.catalogAsset?.let { state.ensureIcon(it) }
                            },
                            onUpdate = {
                                val asset = app.updateAsset ?: return@DeviceAppRowItem
                                state.updateDeviceApp(asset)
                            },
                        )
                    }
                }
            }
        }

        state.downloadError?.let { error ->
            MetroText(
                text = error,
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
internal fun DeviceAppRowItem(
    app: DeviceAppRow,
    downloading: Boolean,
    onUpdate: () -> Unit,
    iconPath: String? = null,
    onVisible: () -> Unit = {},
) {
    val primary = MetroTheme.colors.primaryText
    val secondary = MetroTheme.colors.secondaryText
    val versionLabel = app.installedVersionName?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.device_version_unknown)
    val description = when {
        app.hasUpdate -> {
            val remote = app.updateAsset?.versionName?.takeIf { it.isNotBlank() }
            if (remote != null) {
                stringResource(R.string.device_update_available, versionLabel, remote)
            } else {
                stringResource(R.string.device_update_available_short, versionLabel)
            }
        }
        app.canDownload -> stringResource(R.string.device_version_line, versionLabel)
        else -> stringResource(R.string.device_version_line, versionLabel)
    }
    val updateLabel = stringResource(R.string.device_update)
    val catalog = app.catalogAsset
    val tileBgHex = catalog?.backgroundColor
        ?: HubAppCatalog.backgroundColorForPackage(app.packageName)
        ?: String.format("#%06X", MetroTheme.colors.accent.toArgb() and 0xFFFFFF)

    LaunchedEffect(app.packageName, catalog?.name) {
        onVisible()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Same square Store tile language as hub apps list (never adaptive round icons).
        StoreAppIcon(
            title = app.label,
            packageName = app.packageName,
            iconUrl = catalog?.iconUrl,
            iconPath = iconPath,
            logoXml = catalog?.logoXml,
            logoPngBase64 = catalog?.logoPngBase64,
            backgroundColorHex = tileBgHex,
            glyphResId = catalog?.glyphResId ?: MetroAppGlyphs.forPackage(app.packageName),
            iconSize = StoreIconSize,
        )
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = app.label,
                style = MetroTextStyle.ListItemTitle.toTextStyle(
                    fontFamily = MetroTheme.fontFamily,
                ).copy(color = primary),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
            )
            BasicText(
                text = description,
                style = StoreMetaStyle.copy(fontFamily = MetroTheme.fontFamily, color = secondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (app.hasUpdate) {
            if (downloading) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MetroLoadingDots()
                }
            } else {
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .semantics {
                            contentDescription = updateLabel
                            role = Role.Button
                        }
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onUpdate,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    MetroSystemIcon(
                        type = MetroSystemIconType.Save,
                        iconSize = 28.dp,
                        color = primary,
                        showCircle = true,
                    )
                }
            }
        }
    }
}
