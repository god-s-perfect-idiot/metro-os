package com.metro.hub.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.hub.R
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

private val StatusBodyStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private val LearnMoreStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    textDecoration = TextDecoration.Underline,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/**
 * WP8.1 Settings → phone update language: overline, large title, Update status,
 * Learn more, bordered update all — without notification toggles or install time.
 */
@Composable
fun UpdaterScreen(
    state: HubState,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observe = generation

    val tag = state.release?.tagName?.takeIf { it.isNotBlank() }
    val primary = MetroTheme.colors.primaryText
    val secondary = MetroTheme.colors.secondaryText
    val loading = state.catalogLoadMode == CatalogLoadMode.Loading || state.deviceAppsLoading
    val needing = state.suiteAppsNeedingUpdate
    val needCount = needing.size
    val suiteTotal = state.suiteCatalogAssets.size
    val busy = state.suiteBatchUpdating || state.downloadingAssetName != null
    val updateEnabled = !busy && !loading && needCount > 0

    val statusText = when {
        loading && tag == null -> stringResource(R.string.updater_status_checking)
        tag == null -> stringResource(R.string.updater_status_unknown)
        suiteTotal == 0 -> stringResource(R.string.updater_status_unknown)
        needCount > 0 -> stringResource(R.string.updater_status_needs, tag, needCount, suiteTotal)
        else -> stringResource(R.string.updater_status_current, tag)
    }

    val progressName = state.downloadingAssetName?.let { name ->
        val label = needing.find { it.name == name }?.displayName
            ?: state.suiteCatalogAssets.find { it.name == name }?.displayName
            ?: name.removeSuffix(".apk")
        stringResource(R.string.updater_downloading_app, label)
    }

    Column(modifier = modifier.fillMaxSize()) {
        MetroAppTitle(title = stringResource(R.string.updater_overline))
        MetroText(
            text = stringResource(R.string.updater_title),
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 20.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
        ) {
            MetroText(
                text = stringResource(R.string.updater_status_header),
                style = MetroTextStyle.SectionHeader,
                color = secondary,
                modifier = Modifier.padding(bottom = 10.dp),
            )

            BasicText(
                text = statusText,
                style = StatusBodyStyle.copy(fontFamily = MetroTheme.fontFamily, color = primary),
            )

            Spacer(modifier = Modifier.height(8.dp))

            BasicText(
                text = stringResource(R.string.updater_learn_more),
                style = LearnMoreStyle.copy(fontFamily = MetroTheme.fontFamily, color = primary),
                modifier = Modifier
                    .clickable {
                        val url = if (tag != null) {
                            HubState.releaseUrlForTag(tag)
                        } else {
                            HubState.GITHUB_RELEASES_LATEST_URL
                        }
                        state.openExternalUrl(url)
                    }
                    .padding(vertical = 4.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            MetroBorderButton(
                text = stringResource(R.string.updater_update_all),
                enabled = updateEnabled,
                onClick = state::updateAll,
            )

            if (busy) {
                Spacer(modifier = Modifier.height(16.dp))
                MetroLoadingDots()
                Spacer(modifier = Modifier.height(8.dp))
                MetroText(
                    text = progressName ?: stringResource(R.string.apps_downloading),
                    style = MetroTextStyle.Body,
                    color = secondary,
                )
            }

            state.downloadError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                MetroText(
                    text = error,
                    style = MetroTextStyle.Body,
                    color = secondary,
                )
            }
        }
    }
}
