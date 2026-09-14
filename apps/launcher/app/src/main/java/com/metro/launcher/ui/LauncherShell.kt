package com.metro.launcher.ui

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.metro.launcher.R
import com.metro.launcher.data.AppLauncherOption
import com.metro.launcher.data.DisplayTile
import com.metro.system.MetroAppInfo
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppGlyphs
import com.metro.ui.MetroAppOpenSplash
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroPagePivotLoad
import com.metro.ui.MetroSplashLoadingScreen
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.ui.metroNavBarPadding
import com.metro.ui.metroPagePivotCameraDistance
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** First dots need ~150–600ms delay + travel before they read as “dancing”. */
private const val MIN_SPLASH_DOTS_VISIBLE_MS = 700L

private const val CUSTOMIZE_DEBUG_TAG = "MetroCustomize"

internal fun customizeDebugLog(event: String) {
    Log.i(CUSTOMIZE_DEBUG_TAG, "t=${SystemClock.elapsedRealtime()} $event")
}

/**
 * Two-page shell: Start tiles (page 0) and app menu (page 1).
 * Reference: references/guides/blueprint.md
 *
 * Customize is a sibling composition scope that does **not** share state reads with the
 * Start pager — opening the cube must not rebuild the tile grid on the pivot frame.
 */
@OptIn(ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun LauncherShell(
    state: LauncherState,
    modifier: Modifier = Modifier,
    onComposeSplashReady: () -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    // Written from the customize overlay / cube tap — Start reads only inside the float-clock
    // loop so flipping it does not recompose the tile grid.
    val customizeSuspendStart = remember { mutableStateOf(false) }
    // Drop the Start pager after cube tap so its layout/draw cannot starve the page pivot.
    // Read only by [LauncherPagerHost] — not by this shell — so overlay open stays cheap.
    val startCoveredByCustomize = remember { mutableStateOf(false) }
    // Cold start waits for the splash loader to lift before playing the enter wave.
    var enterWaveKey by remember { mutableIntStateOf(0) }
    // Survives Start dispose when the pager drops page 0 — returning from the app list
    // remounts tiles without replaying a wave that already ran (or was left mid-flight).
    var consumedEnterWaveKey by remember { mutableIntStateOf(0) }
    // Debounce enter-wave bumps when onNewIntent + ON_RESUME both fire for Home.
    var lastEnterWaveBumpElapsed by remember { mutableStateOf(0L) }
    // Screen-off / keyguard — unlock must not replay the enter wave (ANR + black Start).
    var stoppedBehindLock by remember { mutableStateOf(false) }
    fun bumpEnterWave() {
        if (stoppedBehindLock) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastEnterWaveBumpElapsed < 400L) return
        lastEnterWaveBumpElapsed = now
        enterWaveKey++
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val keyguardManager = remember(context) {
        context.getSystemService(KeyguardManager::class.java)
    }
    // Keep splash until Start has actually drawn — dismissing on shell-ready alone leaves a
    // brief black gap while the tile grid mounts. Live-tile refreshes update in place.
    var startDrawn by remember { mutableStateOf(false) }
    // Cold-start only: do not re-cover Start on resume / live refresh.
    var coldSplashActive by remember { mutableStateOf(true) }

    // Stable identities — unstable lambdas / modifiers force a full Start+AppList rebuild.
    val startPageModifier = remember { Modifier.testTag("metro_page_start") }
    val appListPageModifier = remember { Modifier.testTag("metro_page_app_list") }
    val noopTileClick = remember<(DisplayTile) -> Unit> { {} }
    val openAppList = remember(state) { { state.currentPage = 1 } }
    val onTileClick = remember(state) { state::onTileClick }
    val onTileLongPress = remember(state) { state::onTileLongPress }
    val onDismissEdit = remember(state) { state::dismissEdit }
    val onResize = remember(state) { state::resizeEditingTile }
    val onUnpin = remember(state) { state::unpinEditingTile }
    val onCustomize = remember(state, customizeSuspendStart, startCoveredByCustomize) {
        {
            customizeDebugLog("cube_tap")
            // Freeze jiggle, then remove Start from composition before the overlay mounts.
            // Keeping Start under the page was starving frames (~800ms+/frame) so the 200ms
            // pivot took ~2.5s and looked like a pop-in.
            customizeSuspendStart.value = true
            startCoveredByCustomize.value = true
            consumedEnterWaveKey = enterWaveKey
            state.openTileCustomize()
            customizeDebugLog("openTileCustomize_returned")
        }
    }
    val onDragLayout = remember(state) { state::applyDragLayout }
    val onReorderCommit = remember(state) { state::commitTileOrder }
    val onPinRevealConsumed = remember(state) { state::consumePinReveal }
    val onSearchActiveChange = remember(state) { state::onSearchActiveChange }
    val onSearchQueryChange = remember(state) { state::onSearchQueryChange }
    val onAppClick = remember(state) { state::launchApp }
    val onPinToStart = remember(state) { state::pinApp }
    val onUninstall = remember(state) { state::uninstallApp }
    val queryAppOptions = remember(state) { state::queryAppOptions }
    val onLaunchAppOption = remember(state) { state::launchAppOption }
    val onGrantNotificationAccess = remember(state) { state::openNotificationAccessSettings }
    val onDismissNotificationAccess = remember(state) { state::dismissNotificationAccessPrompt }
    // Latest bump for overlay dispose — DisposableEffect(Unit) keeps the first onClosed.
    val bumpEnterWaveUpdated = rememberUpdatedState(newValue = { bumpEnterWave() })
    val onUnfreezeStart = remember(customizeSuspendStart, startCoveredByCustomize) {
        {
            startCoveredByCustomize.value = false
            customizeSuspendStart.value = false
            // Back/Save/Close remounts Start after the page pivot — replay the tile enter wave
            // (Home also bumps via homeEnterRequestId; debounce collapses the double fire).
            bumpEnterWaveUpdated.value.invoke()
        }
    }

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

    // Lift the platform splash on the first cold-start frame so dancing dots can tick
    // (View/ObjectAnimator and Compose animations stall under the SplashScreen overlay).
    // Keep the Compose loader until Start has painted and dots had a visible beat.
    LaunchedEffect(coldSplashActive) {
        if (!coldSplashActive) return@LaunchedEffect
        onComposeSplashReady()
        withFrameNanos { }
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
    // Customize BackHandlers live in [TileCustomizeOverlay] (child wins while open).
    BackHandler {
        when {
            state.customizingTile != null -> state.beginCloseTileCustomize()
            state.editingTile != null -> state.dismissEdit()
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
            // WP8.1 Start ↔ app list: 300ms horizontal pan (Back, → arrow, pin-to-Start).
            pagerState.animateScrollToPage(
                page = state.currentPage,
                animationSpec = MetroTransitions.pageTween(),
            )
        }
        if (state.currentPage != 1) {
            state.dismissSearch()
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    stoppedBehindLock = true
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
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
                    val stillLocked = keyguardManager?.isKeyguardLocked == true
                    if (stillLocked) {
                        stoppedBehindLock = true
                    }
                    val fromLock = stoppedBehindLock || stillLocked
                    // Edit / tile-customize: Home returns to Start with the enter wave, not
                    // the in-place edit chrome. Unlock skips the wave so tiles stay painted.
                    if (state.onHomeRequested()) {
                        // Leave stoppedBehindLock set so homeEnterRequestId does not bump wave.
                        if (fromLock && !stillLocked) {
                            // Cleared in homeEnter LaunchedEffect after it skips the wave.
                        } else if (!fromLock) {
                            // normal home-from-edit — wave plays via homeEnterRequestId
                        }
                        return@LifecycleEventObserver
                    }
                    if (state.currentPage != 0) {
                        if (fromLock && !stillLocked) stoppedBehindLock = false
                        return@LifecycleEventObserver
                    }
                    if (fromLock) {
                        if (!stillLocked) stoppedBehindLock = false
                        return@LifecycleEventObserver
                    }
                    bumpEnterWave()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Home while edit/customize was open — remount Start (if covered) and play enter wave.
    LaunchedEffect(state.homeEnterRequestId) {
        if (state.homeEnterRequestId == 0) return@LaunchedEffect
        // Arm snapExit before clearing edit so tiles do not play the edit outro.
        customizeSuspendStart.value = true
        startCoveredByCustomize.value = false
        state.dismissEdit()
        withFrameNanos { }
        customizeSuspendStart.value = false
        if (stoppedBehindLock || keyguardManager?.isKeyguardLocked == true) {
            if (keyguardManager?.isKeyguardLocked != true) {
                stoppedBehindLock = false
            }
            return@LaunchedEffect
        }
        bumpEnterWave()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        // Pager host must not read customizingTile — otherwise every cube tap rebuilds Start.
        if (state.hasCompletedInitialLoad) {
            LauncherPagerHost(
                state = state,
                pagerState = pagerState,
                showSplashLoader = showSplashLoader,
                enterWaveKey = enterWaveKey,
                consumedEnterWaveKey = consumedEnterWaveKey,
                suspendEditMotion = customizeSuspendStart,
                coveredByCustomize = startCoveredByCustomize,
                startPageModifier = startPageModifier,
                appListPageModifier = appListPageModifier,
                noopTileClick = noopTileClick,
                openAppList = openAppList,
                onTileClick = onTileClick,
                onTileLongPress = onTileLongPress,
                onDismissEdit = onDismissEdit,
                onResize = onResize,
                onUnpin = onUnpin,
                onCustomize = onCustomize,
                onDragLayout = onDragLayout,
                onReorderCommit = onReorderCommit,
                onPinRevealConsumed = onPinRevealConsumed,
                onSearchActiveChange = onSearchActiveChange,
                onSearchQueryChange = onSearchQueryChange,
                onAppClick = onAppClick,
                onPinToStart = onPinToStart,
                onUninstall = onUninstall,
                queryAppOptions = queryAppOptions,
                onLaunchAppOption = onLaunchAppOption,
                onGrantNotificationAccess = onGrantNotificationAccess,
                onDismissNotificationAccess = onDismissNotificationAccess,
            )
        }

        // Sibling scope — only this subtree invalidates when customize opens/closes.
        TileCustomizeOverlay(
            state = state,
            onClosed = onUnfreezeStart,
        )

        if (showSplashLoader) {
            // Use the suite vector — ic_launcher_foreground is a layer-list wrapper that
            // Compose painterResource cannot load (crashes cold start).
            MetroSplashLoadingScreen(
                icon = painterResource(id = MetroAppGlyphs.Launcher),
                backgroundColor = state.accent,
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

/**
 * Start + app list pager. Intentionally avoids reading [LauncherState.customizingTile] so a
 * cube tap does not rebuild the tile grid on the same frame as the customize pivot.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LauncherPagerHost(
    state: LauncherState,
    pagerState: PagerState,
    showSplashLoader: Boolean,
    enterWaveKey: Int,
    consumedEnterWaveKey: Int,
    suspendEditMotion: State<Boolean>,
    coveredByCustomize: State<Boolean>,
    startPageModifier: Modifier,
    appListPageModifier: Modifier,
    noopTileClick: (DisplayTile) -> Unit,
    openAppList: () -> Unit,
    onTileClick: (DisplayTile) -> Unit,
    onTileLongPress: (DisplayTile) -> Unit,
    onDismissEdit: () -> Unit,
    onResize: () -> Unit,
    onUnpin: () -> Unit,
    onCustomize: () -> Unit,
    onDragLayout: (List<PlacedTile>) -> Unit,
    onReorderCommit: () -> Unit,
    onPinRevealConsumed: () -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAppClick: (MetroAppInfo) -> Unit,
    onPinToStart: (MetroAppInfo) -> Unit,
    onUninstall: (MetroAppInfo) -> Unit,
    queryAppOptions: suspend (String) -> List<AppLauncherOption>,
    onLaunchAppOption: (AppLauncherOption) -> Unit,
    onGrantNotificationAccess: () -> Unit,
    onDismissNotificationAccess: () -> Unit,
) {
    // Placeholder while customize owns the screen — avoids Start layout/draw starving the pivot.
    // Overlay shows MetroLoadingScreen during warm; this is only the underlay once Start drops.
    if (coveredByCustomize.value) {
        SideEffect { customizeDebugLog("pager_covered_placeholder") }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        )
        return
    }

    val editing = state.editingTile != null
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
    // Cache so AppList can skip when this host recomposes for unrelated Start edits.
    val filteredApps = remember(state.apps, state.searchQuery) {
        state.filteredApps
    }
    CompositionLocalProvider(
        LocalStartBackgroundViewport provides startBackground,
        LocalTileAppWidgetController provides state.widgetController,
    ) {
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
                // Keep the neighbor composed so animateScrollToPage can pan (Back / →)
                // instead of snapping when the target page has no layout info.
                beyondViewportPageCount = 1,
                userScrollEnabled = !editing && !showSplashLoader && state.appOpenSplash == null,
            ) { page ->
                when (page) {
                    0 -> StartScreen(
                        tiles = state.displayTiles,
                        onTileClick = if (editing) noopTileClick else onTileClick,
                        onTileLongPress = onTileLongPress,
                        onOpenAppList = openAppList,
                        columns = state.gridColumns,
                        editMode = editing,
                        editingTile = state.editingTile,
                        onDismissEdit = onDismissEdit,
                        onResize = onResize,
                        onUnpin = onUnpin,
                        onCustomize = onCustomize,
                        onDragLayout = onDragLayout,
                        onReorderCommit = onReorderCommit,
                        enterWaveKey = enterWaveKey,
                        consumedEnterWaveKey = consumedEnterWaveKey,
                        pendingPinReveal = state.pendingPinReveal,
                        onPinRevealConsumed = onPinRevealConsumed,
                        suspendEditMotion = suspendEditMotion,
                        modifier = startPageModifier,
                    )
                    1 -> AppListScreen(
                        apps = filteredApps,
                        searchActive = state.searchActive,
                        searchQuery = state.searchQuery,
                        onSearchActiveChange = onSearchActiveChange,
                        onSearchQueryChange = onSearchQueryChange,
                        onAppClick = onAppClick,
                        onPinToStart = onPinToStart,
                        onUninstall = onUninstall,
                        queryAppOptions = queryAppOptions,
                        onLaunchAppOption = onLaunchAppOption,
                        modifier = appListPageModifier,
                    )
                }
            }

            if (state.showNotificationAccessPrompt &&
                state.currentPage == 0 &&
                !editing &&
                !showSplashLoader
            ) {
                NotificationAccessPrompt(
                    onGrant = onGrantNotificationAccess,
                    onDismiss = onDismissNotificationAccess,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/**
 * Customize page over Start. Owns all customize state reads so the Start pager stays
 * skippable while this mounts.
 *
 * Critical sequencing (from live traces on Pixel 9):
 * 1. Unmount Start on cube tap so the tile grid cannot draw under the page.
 * 2. Compose the form at alpha=0 and let it layout for a few frames (warm).
 * 3. Only then run the 200ms pivot + app-bar creep — mounting the form on the same
 *    frame as [MetroPagePivotLoad] was starving the animation (~1–1.5s for 200ms).
 *
 * While warming, cover with [MetroLoadingScreen] (Android dots) so the wait is not a
 * blank black frame.
 */
@Composable
private fun TileCustomizeOverlay(
    state: LauncherState,
    onClosed: () -> Unit,
) {
    val customizing = state.customizingTile ?: return
    val customizeDraft = state.tileCustomizeDraft ?: return

    DisposableEffect(Unit) {
        customizeDebugLog("overlay_enter")
        onDispose {
            customizeDebugLog("overlay_exit")
            onClosed()
        }
    }

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

    CompositionLocalProvider(
        LocalTileAppWidgetController provides state.widgetController,
        LocalStartBackgroundViewport provides startBackground,
    ) {
        val colorPickerOpen = state.tileCustomizeColorPickerOpen
        val colorPickerExiting = state.tileCustomizeColorPickerExiting
        val showingColorPicker = colorPickerOpen || colorPickerExiting
        val epoch = state.tileCustomizeEpoch
        val exiting = state.tileCustomizeExiting

        BackHandler(enabled = !exiting && !showingColorPicker) {
            state.beginCloseTileCustomize()
        }
        BackHandler(enabled = colorPickerOpen && !colorPickerExiting) {
            state.beginCloseTileColorPicker()
        }
        BackHandler(enabled = exiting || colorPickerExiting) { }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .metroNavBarPadding(),
            ) {
                key(epoch) {
                    var motionStarted by remember(epoch) { mutableStateOf(false) }
                    WarmThenPivotPage(
                        exiting = exiting,
                        onEnterComplete = {
                            customizeDebugLog("pivot_enter_complete")
                            state.onTileCustomizeEnterComplete()
                        },
                        onExitComplete = {
                            customizeDebugLog("pivot_exit_complete")
                            state.finishCloseTileCustomize()
                        },
                        onEnterMotionStarted = {
                            motionStarted = true
                            customizeDebugLog("enter_motion_started")
                        },
                    ) {
                        SideEffect { customizeDebugLog("form_compose") }
                        TileCustomizeScreen(
                            tile = customizing,
                            draft = customizeDraft,
                            onDraftChange = state::updateTileCustomizeDraft,
                            onOpenColorPicker = state::openTileColorPicker,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    val saveLabel = stringResource(R.string.tile_customize_save)
                    val closeLabel = stringResource(R.string.tile_customize_close)
                    val appBarIcons = remember(saveLabel, closeLabel, state) {
                        listOf(
                            MetroAppBarIcon(
                                type = MetroSystemIconType.Check,
                                label = saveLabel,
                                onClick = state::saveTileCustomize,
                            ),
                            MetroAppBarIcon(
                                type = MetroSystemIconType.Close,
                                label = closeLabel,
                                onClick = state::beginCloseTileCustomize,
                            ),
                        )
                    }
                    // Sibling of the pivot — must not live inside the rotating layer.
                    MetroAppBar(
                        visible = motionStarted && !showingColorPicker && !exiting,
                        enterKey = if (motionStarted) epoch else null,
                        icons = appBarIcons,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )

                    // Warm covers Start with an invisible form; show feedback until pivot starts.
                    if (!motionStarted && !exiting) {
                        MetroLoadingScreen(
                            useAndroidDots = true,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("metro_tile_customize_loading"),
                        )
                    }
                }

                if (showingColorPicker) {
                    MetroPagePivotLoad(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MetroTheme.colors.background),
                        loadKey = "colorPicker:$epoch",
                        exiting = colorPickerExiting,
                        onExitComplete = state::finishCloseTileColorPicker,
                    ) {
                        TileColorPickerScreen(
                            onColorSelected = state::selectTileCustomColor,
                            onClose = state::beginCloseTileColorPicker,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Page pivot that composes [content] at alpha=0 first (warm layout), then plays enter.
 * Avoids starting the 200ms tween on the same frame as first form composition.
 */
@Composable
private fun WarmThenPivotPage(
    exiting: Boolean,
    onEnterComplete: () -> Unit,
    onExitComplete: () -> Unit,
    onEnterMotionStarted: () -> Unit,
    content: @Composable () -> Unit,
) {
    val rotationY = remember {
        Animatable(MetroTransitions.PagePivotLoadStartDegrees)
    }
    val alpha = remember { Animatable(0f) }
    val translationXFraction = remember {
        Animatable(MetroTransitions.PagePivotLoadStartTranslationXFraction)
    }

    LaunchedEffect(exiting) {
        if (exiting) {
            rotationY.snapTo(0f)
            alpha.snapTo(1f)
            translationXFraction.snapTo(0f)
            onEnterMotionStarted()
            customizeDebugLog("pivot_exit_start")
            coroutineScope {
                launch { alpha.animateTo(0f, MetroTransitions.pagePivotExitTween()) }
                launch {
                    rotationY.animateTo(
                        MetroTransitions.PagePivotExitEndDegrees,
                        MetroTransitions.pagePivotExitTween(),
                    )
                }
                launch {
                    translationXFraction.animateTo(
                        MetroTransitions.PagePivotExitTranslationXFraction,
                        MetroTransitions.pagePivotExitTween(),
                    )
                }
            }
            customizeDebugLog("pivot_exit_end")
            onExitComplete()
            return@LaunchedEffect
        }

        // Warm: content is composed below at alpha 0 — pay layout cost before tweening.
        rotationY.snapTo(MetroTransitions.PagePivotLoadStartDegrees)
        alpha.snapTo(0f)
        translationXFraction.snapTo(MetroTransitions.PagePivotLoadStartTranslationXFraction)
        customizeDebugLog("warm_start")
        withFrameNanos { customizeDebugLog("warm_frame_1") }
        withFrameNanos { customizeDebugLog("warm_frame_2") }
        // Require one healthy gap after form layout before motion.
        var attempts = 0
        while (attempts < 8) {
            val t0 = withFrameNanos { it }
            val t1 = withFrameNanos { it }
            val gapMs = (t1 - t0) / 1_000_000L
            customizeDebugLog("warm_gap_ms=$gapMs attempt=$attempts")
            if (gapMs in 1L..40L) break
            attempts++
        }
        onEnterMotionStarted()
        customizeDebugLog("pivot_anim_start")
        val animStart = SystemClock.elapsedRealtime()
        coroutineScope {
            launch { alpha.animateTo(1f, MetroTransitions.pagePivotLoadTween()) }
            launch { rotationY.animateTo(0f, MetroTransitions.pagePivotLoadTween()) }
            launch { translationXFraction.animateTo(0f, MetroTransitions.pagePivotLoadTween()) }
        }
        customizeDebugLog(
            "pivot_anim_end wall_ms=${SystemClock.elapsedRealtime() - animStart}",
        )
        onEnterComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.rotationY = rotationY.value
                this.alpha = alpha.value
                val layerWidth = size.width.coerceAtLeast(1f)
                translationX = translationXFraction.value * layerWidth
                transformOrigin = if (exiting) {
                    TransformOrigin(
                        pivotFractionX = MetroTransitions.PagePivotExitOriginX,
                        pivotFractionY = 0.5f,
                    )
                } else {
                    TransformOrigin(
                        pivotFractionX = MetroTransitions.PagePivotLoadOriginX,
                        pivotFractionY = 0.5f,
                    )
                }
                clip = false
                cameraDistance = metroPagePivotCameraDistance(
                    widthPx = size.width,
                    widthFactor = if (exiting) {
                        MetroTransitions.PagePivotExitCameraWidthFactor
                    } else {
                        0.9f
                    },
                )
            }
            .background(MetroTheme.colors.background),
    ) {
        content()
    }
}
