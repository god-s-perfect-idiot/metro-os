package com.metro.settings.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.settings.R
import com.metro.settings.data.InstalledAppEntry
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroToggleSwitch

@Composable
fun AppDetailScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    val app = state.selectedApp
    if (app == null) {
        SettingsDetailScaffold(
            pageTitle = stringResource(R.string.settings_applications_title),
            modifier = modifier,
        ) {
            SettingsBodyText(text = stringResource(R.string.settings_app_missing))
        }
        return
    }

    SettingsDetailScaffold(
        pageTitle = app.title,
        modifier = modifier,
    ) {
        AppInfoFields(app = app)

        SettingsSpacer(height = 20)

        MetroToggleSwitch(
            checked = state.appBackgroundAllowed,
            onCheckedChange = state::applyAppBackgroundAllowed,
            label = stringResource(R.string.settings_app_background),
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )
        SettingsHelpText(text = stringResource(R.string.settings_app_background_help))

        SettingsSpacer(height = 8)

        MetroToggleSwitch(
            checked = state.appNotificationsAllowed,
            onCheckedChange = state::applyAppNotificationsAllowed,
            label = stringResource(R.string.settings_app_notifications),
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )
        SettingsHelpText(text = stringResource(R.string.settings_app_notifications_help))

        SettingsSpacer(height = 24)

        MetroBorderButton(
            text = stringResource(R.string.settings_app_open),
            onClick = state::openSelectedApp,
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        if (app.canUninstall) {
            SettingsSpacer(height = 12)
            MetroBorderButton(
                text = stringResource(R.string.settings_app_uninstall),
                onClick = state::requestUninstallSelectedApp,
                modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
            )
        } else {
            SettingsSpacer(height = 12)
            SettingsHelpText(text = stringResource(R.string.settings_app_uninstall_blocked))
        }
    }

    if (state.showUninstallConfirm) {
        MetroMessageDialog(
            title = stringResource(R.string.settings_app_uninstall_title),
            body = stringResource(R.string.settings_app_uninstall_body, app.title),
            confirmLabel = stringResource(R.string.settings_app_uninstall),
            onConfirm = state::confirmUninstallSelectedApp,
            dismissLabel = stringResource(R.string.settings_app_uninstall_cancel),
            onDismissRequest = state::dismissUninstallConfirm,
        )
    }
}

@Composable
private fun AppInfoFields(app: InstalledAppEntry) {
    AboutInfoField(
        label = stringResource(R.string.settings_app_version),
        value = app.versionName,
    )
    AboutInfoField(
        label = stringResource(R.string.settings_app_size),
        value = app.sizeLabel,
    )
    AboutInfoField(
        label = stringResource(R.string.settings_app_type),
        value = app.typeLabel,
    )
    AboutInfoField(
        label = stringResource(R.string.settings_app_package),
        value = app.packageName,
    )
}
