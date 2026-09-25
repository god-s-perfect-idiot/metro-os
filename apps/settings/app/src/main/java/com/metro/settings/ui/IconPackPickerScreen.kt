package com.metro.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.metro.settings.R
import com.metro.system.MetroIconPacks
import com.metro.ui.MetroAppPickerEntry
import com.metro.ui.MetroIconPackPickerScreen

@Composable
fun IconPackPickerScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val packs = remember {
        MetroIconPacks.listInstalled(context).map {
            MetroAppPickerEntry(packageName = it.packageName, label = it.label)
        }
    }

    MetroIconPackPickerScreen(
        packs = packs,
        selectedPackageName = state.iconPackPackage,
        onSelected = { packageName ->
            state.applyIconPackPackage(packageName)
            state.goBack()
        },
        modifier = modifier,
        headerTitle = stringResource(R.string.settings_icon_pack_choose),
        onBack = state::goBack,
    )
}
