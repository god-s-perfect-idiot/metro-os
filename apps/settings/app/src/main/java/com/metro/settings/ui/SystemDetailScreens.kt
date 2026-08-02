package com.metro.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.settings.R
import com.metro.settings.data.SettingsLogic
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSlider
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun BrightnessScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    SettingsDetailScaffold(
        pageTitle = stringResource(R.string.settings_brightness),
        modifier = modifier,
    ) {
        SettingsFieldLabel(text = stringResource(R.string.settings_brightness_label))
        MetroSlider(
            value = state.brightness,
            onValueChange = state::applyBrightness,
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )
        SettingsHelpText(text = stringResource(R.string.settings_brightness_help))
    }
}

@Composable
fun StorageSenseScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    val storage = state.system.storageSnapshot()
    SettingsDetailScaffold(
        pageTitle = stringResource(R.string.settings_storage_sense),
        modifier = modifier,
    ) {
        if (storage == null) {
            SettingsBodyText(text = stringResource(R.string.settings_storage_unavailable))
        } else {
            StorageUsageBar(
                usedFraction = storage.usedFraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                    .padding(bottom = 16.dp),
            )
            SettingsStatusRow(
                label = stringResource(R.string.settings_storage_used),
                value = SettingsLogic.formatBytes(storage.usedBytes),
            )
            SettingsStatusRow(
                label = stringResource(R.string.settings_storage_free),
                value = SettingsLogic.formatBytes(storage.freeBytes),
            )
            SettingsStatusRow(
                label = stringResource(R.string.settings_storage_total),
                value = SettingsLogic.formatBytes(storage.totalBytes),
            )
        }

        SettingsSpacer(height = 24)
        MetroBorderButton(
            text = stringResource(R.string.settings_storage_open_files),
            onClick = state::openFiles,
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )
    }
}

/** WP8.1 Storage Sense–style determinate bar: accent used fill on a muted track. */
@Composable
private fun StorageUsageBar(
    usedFraction: Float,
    modifier: Modifier = Modifier,
) {
    val fraction = usedFraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(8.dp)
            .background(
                color = MetroTheme.colors.secondaryText.copy(alpha = 0.35f),
                shape = RectangleShape,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(MetroTheme.colors.accent, RectangleShape),
        )
    }
}

/**
 * extras+info: suite blurb plus phone information fields (formerly About → more info).
 */
@Composable
fun AboutScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    val info = remember(state.route) { state.system.deviceInfoSnapshot() }

    SettingsDetailScaffold(
        pageTitle = stringResource(R.string.settings_about),
        modifier = modifier,
    ) {
        SettingsBodyText(text = stringResource(R.string.settings_about_intro))

        MetroText(
            text = stringResource(R.string.settings_about_phone_information),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent,
            modifier = Modifier.padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 8.dp,
            ),
        )

        AboutInfoField(label = stringResource(R.string.settings_about_name), value = info.name)
        AboutInfoField(label = stringResource(R.string.settings_about_model), value = info.model)
        AboutInfoField(
            label = stringResource(R.string.settings_about_manufacturer),
            value = info.manufacturer,
        )
        AboutInfoField(label = stringResource(R.string.settings_about_carrier), value = info.carrier)
        AboutInfoField(
            label = stringResource(R.string.settings_about_software),
            value = info.software,
        )
        AboutInfoField(
            label = stringResource(R.string.settings_about_total_storage),
            value = info.totalStorage,
        )
        AboutInfoField(
            label = stringResource(R.string.settings_about_available_storage),
            value = info.availableStorage,
        )

        SettingsSpacer(height = 20)

        AboutInfoField(
            label = stringResource(R.string.settings_about_os_version),
            value = info.osVersion,
        )
        AboutInfoField(
            label = stringResource(R.string.settings_about_firmware),
            value = info.firmwareRevision,
        )
        AboutInfoField(
            label = stringResource(R.string.settings_about_hardware),
            value = info.hardwareRevision,
        )
        AboutInfoField(
            label = stringResource(R.string.settings_about_radio),
            value = info.radioSoftware,
        )
        AboutInfoField(
            label = stringResource(R.string.settings_about_bootloader),
            value = info.bootloader,
        )
        AboutInfoField(
            label = stringResource(R.string.settings_about_chip_soc),
            value = info.chipSoc,
        )
        AboutInfoField(
            label = stringResource(R.string.settings_about_build_id),
            value = info.buildId,
        )
        AboutInfoField(label = stringResource(R.string.settings_about_board), value = info.board)
        AboutInfoField(label = stringResource(R.string.settings_about_abi), value = info.abi)
    }
}

@Composable
internal fun AboutInfoField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 10.dp,
            ),
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
        )
        MetroText(
            text = value,
            style = MetroTextStyle.Body,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
