package com.metro.settings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.metro.ui.MetroPagePivotLoad
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

@Composable
fun SettingsShell(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    var exitingRoute by remember { mutableStateOf<SettingsRoute?>(null) }
    var suppressEnterFor by remember { mutableStateOf<SettingsRoute?>(null) }
    val isExiting = exitingRoute != null

    BackHandler(enabled = state.route != SettingsRoute.Root && !isExiting) {
        exitingRoute = state.route
    }

    BackHandler(enabled = isExiting) {
        // Hold the stack until the flip-out finishes.
    }

    val contentModifier = modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .metroNavBarPadding()
        .background(MetroTheme.colors.background)

    Box(modifier = contentModifier) {
        when {
            isExiting -> {
                SettingsSubpage(
                    route = exitingRoute!!,
                    state = state,
                    loadKey = subpageLoadKey(exitingRoute!!, state),
                    exiting = true,
                    onExitComplete = {
                        suppressEnterFor = exitingRoute!!.parentRoute()
                        state.goBack()
                        exitingRoute = null
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            state.route == SettingsRoute.Root -> {
                SettingsRootScreen(state = state, modifier = Modifier.fillMaxSize())
            }
            else -> {
                val route = state.route
                if (route == suppressEnterFor) {
                    LaunchedEffect(route) { suppressEnterFor = null }
                    SettingsSubpageContent(
                        route = route,
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    SettingsSubpage(
                        route = route,
                        state = state,
                        loadKey = subpageLoadKey(route, state),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSubpage(
    route: SettingsRoute,
    state: SettingsState,
    loadKey: Any,
    modifier: Modifier = Modifier,
    exiting: Boolean = false,
    onExitComplete: () -> Unit = {},
) {
    MetroPagePivotLoad(
        modifier = modifier.background(MetroTheme.colors.background),
        loadKey = loadKey,
        exiting = exiting,
        onExitComplete = onExitComplete,
    ) {
        SettingsSubpageContent(route = route, state = state, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun SettingsSubpageContent(
    route: SettingsRoute,
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    when (route) {
        SettingsRoute.StartTheme -> StartThemeScreen(state = state, modifier = modifier)
        SettingsRoute.AccentPicker -> AccentPickerScreen(state = state, modifier = modifier)
        SettingsRoute.StartBackgroundCrop -> StartBackgroundCropScreen(state = state, modifier = modifier)
        SettingsRoute.EaseOfAccess -> EaseOfAccessScreen(state = state, modifier = modifier)
        SettingsRoute.Brightness -> BrightnessScreen(state = state, modifier = modifier)
        SettingsRoute.StorageSense -> StorageSenseScreen(state = state, modifier = modifier)
        SettingsRoute.About -> AboutScreen(state = state, modifier = modifier)
        SettingsRoute.AppDetail -> AppDetailScreen(state = state, modifier = modifier)
        SettingsRoute.Root -> Unit
    }
}

private fun SettingsRoute.parentRoute(): SettingsRoute = when (this) {
    SettingsRoute.AccentPicker,
    SettingsRoute.StartBackgroundCrop,
    -> SettingsRoute.StartTheme
    else -> SettingsRoute.Root
}

private fun subpageLoadKey(route: SettingsRoute, state: SettingsState): Any = when (route) {
    SettingsRoute.AppDetail -> "AppDetail:${state.selectedApp?.packageName.orEmpty()}"
    else -> route
}
