package com.metro.hub.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.hub.R
import com.metro.hub.data.ApkInstaller
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun AppDetailScreen(
    state: HubState,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observe = generation

    val asset = state.selectedAsset
    if (asset == null) {
        Column(modifier = modifier.fillMaxSize()) {
            MetroAppTitle(title = stringResource(R.string.app_name))
            MetroText(
                text = stringResource(R.string.apps_detail_missing),
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(12.dp),
            )
        }
        return
    }

    LaunchedEffect(asset.name) {
        state.ensureIcon(asset)
    }

    val downloading = state.downloadingAssetName == asset.name
    val secondary = MetroTheme.colors.secondaryText
    val sizeLabel = ApkInstaller.formatSize(asset.sizeBytes).ifBlank { "—" }
    val versionLabel = asset.versionName?.takeIf { it.isNotBlank() } ?: "—"

    Column(modifier = modifier.fillMaxSize()) {
        MetroAppTitle(title = stringResource(R.string.app_name))
        MetroText(
            text = asset.displayName,
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = MetroAppBarDefaults.BarHeight + ListBottomExtraPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                StoreAppIcon(
                    title = asset.displayName,
                    packageName = asset.packageName,
                    iconUrl = asset.iconUrl,
                    iconPath = state.iconPathFor(asset),
                    logoXml = asset.logoXml,
                    logoPngBase64 = asset.logoPngBase64,
                    backgroundColorHex = asset.backgroundColor,
                    glyphResId = asset.glyphResId,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    MetroText(
                        text = asset.displayName,
                        style = MetroTextStyle.ListItemTitle,
                        maxLines = 2,
                    )
                    BasicText(
                        text = "By: ${asset.publisher}",
                        style = StoreMetaStyle.copy(color = secondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            BasicText(
                text = asset.description,
                style = StoreMetaStyle.copy(color = secondary),
            )

            DetailMetaRow(
                label = stringResource(R.string.apps_detail_version),
                value = versionLabel,
            )
            DetailMetaRow(
                label = stringResource(R.string.apps_detail_download_size),
                value = sizeLabel,
            )
            DetailMetaRow(
                label = stringResource(R.string.apps_detail_category),
                value = asset.category.label,
            )

            if (downloading) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicText(
                        text = stringResource(R.string.apps_downloading),
                        style = StoreMetaStyle.copy(color = secondary),
                    )
                    MetroLoadingDots()
                }
            }

            state.downloadError?.let { error ->
                MetroText(
                    text = error,
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                )
            }
        }
    }
}

@Composable
private fun DetailMetaRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        MetroText(
            text = label.uppercase(),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.secondaryText,
        )
        BasicText(
            text = value,
            style = StoreMetaStyle.copy(color = MetroTheme.colors.primaryText),
        )
    }
}
