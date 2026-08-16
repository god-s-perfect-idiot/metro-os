package com.metro.launcher.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.metro.launcher.R
import com.metro.ui.MetroAppOpenSplash
import com.metro.ui.MetroSplashLoadingScreen
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** First dots need ~150–600ms delay + travel before they read as “dancing”. */
private const val MIN_SPLASH_DOTS_VISIBLE_MS = 700L

/**
 * Two-page shell: Start tiles (page 0) and app menu (page 1).
 * Reference: references/guides/blueprint.md
 */
@OptIn(ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun LauncherShell(
    state: LauncherState,
    modifier: Modifier = Modifier,
    onComposeSplashReady: () -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val editing = state.editingTile != null
    // Cold start waits for the splash loader to lift before playing the enter wave.
    var enterWaveKey by remember { mutableIntStateOf(0) }
    // Survives Start dispose when the pager drops page 0 — returning from the app list
    // remounts tiles without replaying a wave that already ran (or was left mid-flight).
    var consumedEnterWaveKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // Keep splash until Start has actually drawn — dismissing on shell-ready alone leaves a
    // brief black gap while the tile grid mounts. Live-tile refreshes update in place.
    var startDrawn by remember { mutableStateOf(false) }
    // Cold-start only: do not re-cover Start on resume / live refresh.
    var coldSplashActive by remember { mutableStateOf(true) }
    var splashDotsStarted by remember { mutableStateOf(false) }

    LaunchedEffect(state.hasCompletedInitialLoad) {
        if (!state.hasCompletedInitialLoad) {
            startDrawn = false
            return@LaunchedEffect
        }
        // Start composes under the splash this frame; wait until it has been presented.
        withFrameNanos { }
        withFrameNanos { }
        startDrawn = true
    }

    // Hold system splash until Compose dots are actually running, then keep the Compose
    // loader up until Start has painted *and* dots have had a visible beat (fast loads
    // were lifting before the delayed off-screen dots entered the track).
    LaunchedEffect(splashDotsStarted) {
        if (!coldSplashActive || !splashDotsStarted) return@LaunchedEffect
        onComposeSplashReady()
        val visibleSince = SystemClock.elapsedRealtime()
        snapshotFlow { state.hasCompletedInitialLoad && startDrawn }
            .first { ready -> ready }
        val remaining = MIN_SPLASH_DOTS_VISIBLE_MS -
            (SystemClock.elapsedRealtime() - visibleSince)
        if (remaining > 0L) delay(remaining)
        coldSplashActive = false
        if (enterWaveKey == 0) {
            enterWaveKey = 1
        }
    }

    val showSplashLoader = coldSplashActive

    // Home must consume Back: the default finish/relaunch path resumes Start and replays
    // the enter wave (hang). App-list search keeps its own BackHandler (child wins).
    BackHandler {
        when {
            editing -> state.dismissEdit()
            state.currentPage == 1 -> state.currentPage = 0
            // Start: consume and stay put.
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            state.currentPage = page
            if (page != 0) {
                consumedEnterWaveKey = enterWaveKey
            }
        }
    }

    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
        if (state.currentPage != 1) {
            state.dismissSearch()
        }
    }

    DisposableEffect(lifecycleOwner) {
        var skipNextResume = true
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // Target is in front — drop the open splash so it is not stuck on return.
                    if (state.appOpenSplash?.launched == true) {
                        state.clearAppOpenSplash()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (skipNextResume) {
                        skipNextResume = false
                        return@LifecycleEventObserver
                    }
                    if (state.currentPage == 0 && state.editingTile == null) {
                        enterWaveKey++
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        // Mount Start as soon as live data is ready, but keep the splash on top until
        // Start has drawn (View-backed dots keep moving during that mount).
        if (state.hasCompletedInitialLoad) {
            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            val viewportWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
            val viewportHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
            val startBackground = remember(state.startBackgroundBitmap, viewportWidthPx, viewportHeightPx) {
                state.startBackgroundBitmap?.let { bmp ->
                    StartBackgroundViewport(
                        bitmap = bmp.asImageBitmap(),
                        viewportWidthPx = viewportWidthPx,
                        viewportHeightPx = viewportHeightPx,
                    )
                }
            }
            CompositionLocalProvider(LocalStartBackgroundViewport provides startBackground) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .metroNavBarPadding()
                    .background(Color.Black),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 0,
                    userScrollEnabled = !editing && !showSplashLoader && state.appOpenSplash == null,
                ) { page ->
                    when (page) {
                        0 -> StartScreen(
                            tiles = state.displayTiles,
                            onTileClick = if (editing) ({}) else state::onTileClick,
                            onTileLongPress = state::onTileLongPress,
                            onOpenAppList = { state.currentPage = 1 },
                            columns = state.gridColumns,
                            editMode = editing,
                            editingTile = state.editingTile,
                            onDismissEdit = state::dismissEdit,
                            onResize = state::resizeEditingTile,
                            onUnpin = state::unpinEditingTile,
                            onDragLayout = state::applyDragLayout,
                            onReorderCommit = state::commitTileOrder,
                            enterWaveKey = enterWaveKey,
                            consumedEnterWaveKey = consumedEnterWaveKey,
                            modifier = Modifier.testTag("metro_page_start"),
                        )
                        1 -> AppListScreen(
                            apps = state.filteredApps,
                            searchActive = state.searchActive,
                            searchQuery = state.searchQuery,
                            onSearchActiveChange = state::onSearchActiveChange,
                            onSearchQueryChange = state::onSearchQueryChange,
                            onAppClick = state::launchApp,
                            onPinToStart = state::pinApp,
                            onUninstall = state::uninstallApp,
                            queryAppOptions = state::queryAppOptions,
                            onLaunchAppOption = state::launchAppOption,
                            modifier = Modifier.testTag("metro_page_app_list"),
                        )
                    }
                }

                if (state.showNotificationAccessPrompt &&
                    state.currentPage == 0 &&
                    !editing &&
                    !showSplashLoader
                ) {
                    NotificationAccessPrompt(
                        onGrant = state::openNotificationAccessSettings,
                        onDismiss = state::dismissNotificationAccessPrompt,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            }
        }

        if (showSplashLoader) {
            MetroSplashLoadingScreen(
                icon = painterResource(id = R.drawable.ic_launcher_foreground),
                backgroundColor = state.accent,
                onDotsStarted = { splashDotsStarted = true },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("metro_start_splash_loader"),
            )
        }

        // System-wide app open: splash pivots in over Start, then the activity starts underneath.
        val openSplash = state.appOpenSplash
        if (openSplash != null) {
            val bitmapPainter = openSplash.iconBitmap?.let { bmp ->
                remember(bmp) { BitmapPainter(bmp) }
            }
            val iconPainter = when {
                openSplash.glyphResId != null -> painterResource(openSplash.glyphResId)
                else -> bitmapPainter
            }
            MetroAppOpenSplash(
                icon = iconPainter,
                backgroundColor = openSplash.backgroundColor,
                onEnterComplete = state::onAppOpenSplashEnterComplete,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("metro_app_open_splash"),
            )
            // Fallback if the target never brings us to pause (launch failure).
            LaunchedEffect(openSplash.launched, openSplash.packageName) {
                if (!openSplash.launched) return@LaunchedEffect
                delay(1_500L)
                state.clearAppOpenSplash()
            }
        }
    }
}
