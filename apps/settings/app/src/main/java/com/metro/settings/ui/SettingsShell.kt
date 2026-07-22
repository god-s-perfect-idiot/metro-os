package com.metro.settings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

@Composable
fun SettingsShell(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.route != SettingsRoute.Root) {
        state.goBack()
    }

    val contentModifier = modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .metroNavBarPadding()
        .background(MetroTheme.colors.background)

    when (state.route) {
        SettingsRoute.Root -> SettingsRootScreen(state = state, modifier = contentModifier)
        SettingsRoute.StartTheme -> StartThemeScreen(state = state, modifier = contentModifier)
        SettingsRoute.AccentPicker -> AccentPickerScreen(state = state, modifier = contentModifier)
        SettingsRoute.EaseOfAccess -> EaseOfAccessScreen(state = state, modifier = contentModifier)
    }
}
