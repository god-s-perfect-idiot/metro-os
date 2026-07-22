package com.metro.settings.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.settings.R
import com.metro.system.MetroFontScale
import com.metro.ui.MetroListItem
import com.metro.ui.MetroSettingsHeader

/** WP8.1 system settings rows stay at single-line height even with a value subtitle. */
private val SettingsRowHeight = 76.dp

@Composable
fun SettingsRootScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            MetroSettingsHeader(pageTitle = stringResource(R.string.settings_system_title))
        }
        item {
            MetroListItem(
                title = stringResource(R.string.settings_start_theme),
                subtitle = state.accentDisplayName,
                modifier = Modifier.height(SettingsRowHeight),
                onClick = { state.open(SettingsRoute.StartTheme) },
            )
        }
        item {
            MetroListItem(
                title = stringResource(R.string.settings_ease_of_access),
                subtitle = "text ${state.fontScaleIndex + 1}/${MetroFontScale.STEP_COUNT}",
                modifier = Modifier.height(SettingsRowHeight),
                onClick = { state.open(SettingsRoute.EaseOfAccess) },
            )
        }
    }
}
