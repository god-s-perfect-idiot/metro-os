package com.metro.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

@Composable
fun SettingsShell(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    MetroSubpageHost(
        route = state.route,
        isRoot = { it == SettingsRoute.Root },
        parentOf = { it.parentRoute() },
        loadKeyOf = { subpageLoadKey(it, state) },
        onGoBack = state::goBack,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .metroNavBarPadding()
            .background(MetroTheme.colors.background),
        rootContent = {
            SettingsRootScreen(state = state, modifier = Modifier.fillMaxSize())
        },
        subpageContent = { route ->
            SettingsSubpageContent(route = route, state = state, modifier = Modifier.fillMaxSize())
        },
    )
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
        SettingsRoute.IconPackPicker -> IconPackPickerScreen(state = state, modifier = modifier)
        SettingsRoute.EaseOfAccess -> EaseOfAccessScreen(state = state, modifier = modifier)
        SettingsRoute.Brightness -> BrightnessScreen(state = state, modifier = modifier)
        SettingsRoute.StorageSense -> StorageSenseScreen(state = state, modifier = modifier)
        SettingsRoute.About -> AboutScreen(state = state, modifier = modifier)
        SettingsRoute.AppDetail -> AppDetailScreen(state = state, modifier = modifier)
        SettingsRoute.ConnectedApps -> ConnectedAppsScreen(state = state, modifier = modifier)
        SettingsRoute.GalleryApps -> ConnectedAppListScreen(
            state = state,
            kind = ConnectedAppKind.Gallery,
            modifier = modifier,
        )
        SettingsRoute.MusicApps -> ConnectedAppListScreen(
            state = state,
            kind = ConnectedAppKind.Music,
            modifier = modifier,
        )
        SettingsRoute.GalleryAppPicker -> ConnectedAppPickerScreen(
            state = state,
            kind = ConnectedAppKind.Gallery,
            modifier = modifier,
        )
        SettingsRoute.MusicAppPicker -> ConnectedAppPickerScreen(
            state = state,
            kind = ConnectedAppKind.Music,
            modifier = modifier,
        )
        SettingsRoute.Root -> Unit
    }
}

private fun SettingsRoute.parentRoute(): SettingsRoute = when (this) {
    SettingsRoute.AccentPicker,
    SettingsRoute.StartBackgroundCrop,
    SettingsRoute.IconPackPicker,
    -> SettingsRoute.StartTheme
    SettingsRoute.GalleryApps,
    SettingsRoute.MusicApps,
    -> SettingsRoute.ConnectedApps
    SettingsRoute.GalleryAppPicker -> SettingsRoute.GalleryApps
    SettingsRoute.MusicAppPicker -> SettingsRoute.MusicApps
    else -> SettingsRoute.Root
}

private fun subpageLoadKey(route: SettingsRoute, state: SettingsState): Any = when (route) {
    SettingsRoute.AppDetail -> "AppDetail:${state.selectedApp?.packageName.orEmpty()}"
    SettingsRoute.GalleryApps -> "GalleryApps"
    SettingsRoute.MusicApps -> "MusicApps"
    SettingsRoute.GalleryAppPicker -> "GalleryAppPicker"
    SettingsRoute.MusicAppPicker -> "MusicAppPicker"
    else -> route
}
