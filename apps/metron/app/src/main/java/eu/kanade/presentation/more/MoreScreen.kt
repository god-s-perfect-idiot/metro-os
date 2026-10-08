package eu.kanade.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroListItem
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import tachiyomi.core.common.Constants
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickSettings: () -> Unit,
    onClickSupport: () -> Unit,
    onClickAbout: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val downloadQueueState = downloadQueueStateProvider()
    val downloadSubtitle = when (downloadQueueState) {
        DownloadQueueState.Stopped -> stringResource(MR.strings.information_no_downloads)
        is DownloadQueueState.Paused -> {
            val pending = downloadQueueState.pending
            if (pending == 0) {
                stringResource(MR.strings.paused)
            } else {
                "${stringResource(MR.strings.paused)} • ${
                    pluralStringResource(
                        MR.plurals.download_queue_summary,
                        count = pending,
                        pending,
                    )
                }"
            }
        }
        is DownloadQueueState.Downloading -> {
            val pending = downloadQueueState.pending
            pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = MetroAppBarDefaults.BarHeight + 32.dp),
    ) {
        MetroSettingsHeader(
            pageTitle = stringResource(MR.strings.label_more).lowercase(),
            appTitle = "metron",
        )

        MetroToggleSwitch(
            checked = downloadedOnly,
            onCheckedChange = onDownloadedOnlyChange,
            label = stringResource(MR.strings.label_downloaded_only).lowercase(),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        MetroText(
            text = stringResource(MR.strings.downloaded_only_summary).lowercase(),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        MetroToggleSwitch(
            checked = incognitoMode,
            onCheckedChange = onIncognitoModeChange,
            label = stringResource(MR.strings.pref_incognito_mode).lowercase(),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        MetroText(
            text = stringResource(MR.strings.pref_incognito_mode_summary).lowercase(),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )

        Spacer(modifier = Modifier.height(4.dp))

        MetroListItem(
            title = stringResource(MR.strings.label_download_queue).lowercase(),
            subtitle = downloadSubtitle.lowercase(),
            oneLineMinHeight = 64.dp,
            twoLineMinHeight = 72.dp,
            verticalPadding = 8.dp,
            onClick = onClickDownloadQueue,
        )
        MetroListItem(
            title = stringResource(MR.strings.categories).lowercase(),
            subtitle = stringResource(MR.strings.pref_library_summary).lowercase(),
            oneLineMinHeight = 64.dp,
            twoLineMinHeight = 72.dp,
            verticalPadding = 8.dp,
            onClick = onClickCategories,
        )
        MetroListItem(
            title = stringResource(MR.strings.label_stats).lowercase(),
            subtitle = "reading statistics",
            oneLineMinHeight = 64.dp,
            twoLineMinHeight = 72.dp,
            verticalPadding = 8.dp,
            onClick = onClickStats,
        )
        MetroListItem(
            title = stringResource(MR.strings.label_data_storage).lowercase(),
            subtitle = stringResource(MR.strings.pref_backup_summary).lowercase(),
            oneLineMinHeight = 64.dp,
            twoLineMinHeight = 72.dp,
            verticalPadding = 8.dp,
            onClick = onClickDataAndStorage,
        )

        Spacer(modifier = Modifier.height(4.dp))

        MetroListItem(
            title = stringResource(MR.strings.label_settings).lowercase(),
            subtitle = "appearance, library, reader, downloads",
            oneLineMinHeight = 64.dp,
            twoLineMinHeight = 72.dp,
            verticalPadding = 8.dp,
            onClick = onClickSettings,
        )
        MetroListItem(
            title = stringResource(MR.strings.label_support_us).lowercase(),
            subtitle = "support development",
            oneLineMinHeight = 64.dp,
            twoLineMinHeight = 72.dp,
            verticalPadding = 8.dp,
            onClick = onClickSupport,
        )
        MetroListItem(
            title = stringResource(MR.strings.pref_category_about).lowercase(),
            subtitle = "${stringResource(MR.strings.app_name)} ${AboutScreen.getVersionName(withBuildDate = false)}".lowercase(),
            oneLineMinHeight = 64.dp,
            twoLineMinHeight = 72.dp,
            verticalPadding = 8.dp,
            onClick = onClickAbout,
        )
        MetroListItem(
            title = stringResource(MR.strings.label_help).lowercase(),
            subtitle = Constants.URL_HELP.removePrefix("https://").lowercase(),
            oneLineMinHeight = 64.dp,
            twoLineMinHeight = 72.dp,
            verticalPadding = 8.dp,
            onClick = { uriHandler.openUri(Constants.URL_HELP) },
        )
    }
}
