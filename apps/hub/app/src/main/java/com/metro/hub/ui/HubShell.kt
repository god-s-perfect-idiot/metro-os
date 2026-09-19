package com.metro.hub.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.metro.hub.data.ApkInstaller
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarTextButton
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HubShell(
    state: HubState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observe = generation

    // Panorama intro once per process — not when returning to hub in-app.
    var panoramaIntroPlayed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        state.ensureReleaseLoaded()
    }

    LaunchedEffect(state.pendingInstallFile) {
        val file = state.pendingInstallFile ?: return@LaunchedEffect
        if (!ApkInstaller.canRequestPackageInstalls(context)) {
            context.startActivity(ApkInstaller.installPermissionSettingsIntent(context))
            return@LaunchedEffect
        }
        val apk = state.consumePendingInstall() ?: return@LaunchedEffect
        context.startActivity(ApkInstaller.installIntent(context, apk))
    }

    MetroSubpageHost(
        route = state.route,
        isRoot = { it == HubRoute.Hub },
        parentOf = { it.parentRoute(state) },
        loadKeyOf = { subpageLoadKey(it, state) },
        onGoBack = state::goBack,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding()
            .background(MetroTheme.colors.background),
        rootContent = {
            Box(modifier = Modifier.fillMaxSize()) {
                val pagerState = rememberPagerState(
                    initialPage = state.hubPage,
                    pageCount = { 3 },
                )
                LaunchedEffect(pagerState.currentPage) {
                    state.hubPage = pagerState.currentPage
                }
                LaunchedEffect(state.hubPage) {
                    if (pagerState.currentPage != state.hubPage) {
                        pagerState.scrollToPage(state.hubPage)
                    }
                }
                HubPanorama(
                    state = state,
                    pagerState = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    skipIntro = panoramaIntroPlayed,
                    onIntroPlayed = { panoramaIntroPlayed = true },
                )
                MetroAppBar(
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Search,
                            label = "search",
                            onClick = state::openSearch,
                        ),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        },
        subpageContent = { route ->
            HubSubpageContent(route = route, state = state)
        },
    )
}

@Composable
private fun HubSubpageContent(
    route: HubRoute,
    state: HubState,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (route) {
            HubRoute.AppList -> {
                AppListScreen(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                )
                MetroAppBar(
                    minimized = false,
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Refresh,
                            label = "refresh",
                            onClick = state::refreshRelease,
                        ),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            HubRoute.AppDetail -> {
                val asset = state.selectedAsset
                val downloading = asset != null && state.downloadingAssetName == asset.name
                AppDetailScreen(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                )
                MetroAppBar(
                    minimized = false,
                    textButtons = listOf(
                        MetroAppBarTextButton(
                            text = "download",
                            enabled = asset != null && !downloading && asset.downloadUrl.isNotBlank(),
                            onClick = {
                                val selected = state.selectedAsset ?: return@MetroAppBarTextButton
                                if (state.downloadingAssetName == null) {
                                    state.downloadAndInstall(selected)
                                }
                            },
                        ),
                        MetroAppBarTextButton(
                            text = "share",
                            enabled = asset != null,
                            onClick = {
                                val selected = state.selectedAsset ?: return@MetroAppBarTextButton
                                state.shareApp(selected)
                            },
                        ),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            HubRoute.ExtrasInfo -> {
                ExtrasInfoScreen(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            HubRoute.Search -> {
                SearchScreen(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            HubRoute.Hub -> Unit
        }
    }
}

private fun HubRoute.parentRoute(state: HubState): HubRoute = when (this) {
    HubRoute.AppDetail -> state.appDetailParent
    HubRoute.AppList -> HubRoute.Hub
    HubRoute.Search -> HubRoute.Hub
    HubRoute.ExtrasInfo -> HubRoute.Hub
    HubRoute.Hub -> HubRoute.Hub
}

private fun subpageLoadKey(route: HubRoute, state: HubState): Any = when (route) {
    HubRoute.AppList -> "AppList:${state.listFilter?.name ?: "all"}"
    HubRoute.AppDetail -> "AppDetail:${state.selectedAssetName.orEmpty()}"
    HubRoute.Search -> "Search"
    HubRoute.ExtrasInfo -> "ExtrasInfo"
    HubRoute.Hub -> "Hub"
}
