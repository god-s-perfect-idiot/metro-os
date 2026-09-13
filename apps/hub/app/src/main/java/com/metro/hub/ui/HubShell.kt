package com.metro.hub.ui

import androidx.activity.compose.BackHandler
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
import com.metro.ui.MetroPagePivotLoad
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

    var exitingAppList by remember { mutableStateOf(false) }
    val showingAppList = state.route == HubRoute.AppList || exitingAppList

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

    BackHandler(enabled = state.route == HubRoute.AppList && !exitingAppList) {
        exitingAppList = true
    }

    BackHandler(enabled = exitingAppList) {
        // Hold the stack until the flip-out finishes.
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding()
            .background(MetroTheme.colors.background),
    ) {
        when {
            showingAppList -> {
                HubAppListPage(
                    state = state,
                    loadKey = appListLoadKey(state),
                    exiting = exitingAppList,
                    onExitComplete = {
                        state.closeAppList()
                        exitingAppList = false
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                val pagerState = rememberPagerState(
                    initialPage = state.hubPage,
                    pageCount = { 2 },
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
                )
                MetroAppBar(
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Search,
                            label = "search",
                            onClick = { /* wired later */ },
                        ),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun HubAppListPage(
    state: HubState,
    loadKey: Any,
    exiting: Boolean,
    onExitComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MetroPagePivotLoad(
        modifier = modifier.background(MetroTheme.colors.background),
        loadKey = loadKey,
        exiting = exiting,
        onExitComplete = onExitComplete,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
    }
}

private fun appListLoadKey(state: HubState): Any =
    "AppList:${state.listFilter?.name ?: "all"}"
