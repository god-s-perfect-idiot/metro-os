package com.metro.launcher.ui

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.database.ContentObserver
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.metro.launcher.BuildConfig
import com.metro.launcher.data.AppLauncherOption
import com.metro.launcher.data.CustomTileBranding
import com.metro.launcher.data.DisplayTile
import com.metro.launcher.data.GalleryLiveTileStore
import com.metro.launcher.data.LauncherRepository
import com.metro.launcher.data.MusicNowPlayingStore
import com.metro.launcher.data.PinnedTileEntry
import com.metro.launcher.data.PinnedTileSize
import com.metro.launcher.data.TileAppWidgetController
import com.metro.launcher.data.TileBackgroundMode
import com.metro.launcher.data.TileNotificationAccess
import com.metro.launcher.data.TileSizeCycle
import com.metro.launcher.data.adaptTilesToColumnCount
import com.metro.launcher.data.applyTileResize
import com.metro.launcher.data.compactEmptyRows
import com.metro.launcher.data.ensureGridPositions
import com.metro.launcher.data.mergePinnedDisplayTiles
import com.metro.launcher.data.supportsCustomWidget
import com.metro.launcher.data.tileGridColumnCount
import com.metro.system.MetroAccentPalette
import com.metro.system.MetroAppBranding
import com.metro.system.MetroAppInfo
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroIntents
import com.metro.system.MetroPreferenceKeys
import com.metro.system.MetroPreferences
import com.metro.system.MetroStartBackground
import com.metro.system.MetroThemeMode
import com.metro.system.MetroTypeface
import com.metro.system.MetroTileContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Start-owned open animation request — splash pivots in, then the activity starts underneath. */
data class AppOpenSplashRequest(
    val packageName: String,
    val deepLinkUri: String?,
    val backgroundColor: Color,
    val iconBitmap: ImageBitmap?,
    @DrawableRes val glyphResId: Int?,
    val shortcut: AppLauncherOption? = null,
    val launched: Boolean = false,
)

class LauncherState(context: Context) {
    private val appContext = context.applicationContext
    private val hostContext: Context = context
    private val repository = LauncherRepository(appContext)
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val persistMutex = Mutex()
    @Volatile
    private var persistJob: Job? = null
    private var persistGeneration = 0
    private val metroPrefs = MetroPreferences(appContext)
    private val launcherPrefs =
        appContext.getSharedPreferences(PREFS_LAUNCHER, Context.MODE_PRIVATE)

    var darkTheme by mutableStateOf(metroPrefs.peekCachedIsDark() ?: metroPrefs.isDark)
    var accent by mutableStateOf(
        metroPrefs.peekCachedAccentColorHex()?.let { MetroPreferences.parseAccentHex(it) }
            ?: metroPrefs.accentColor,
    )
    /** Decoded Start background for viewport-window tiles; null when unset. */
    var startBackgroundBitmap by mutableStateOf<android.graphics.Bitmap?>(null)
        private set
    /** 4 (default) or 6 when Settings → show more columns is on. */
    var gridColumns by mutableIntStateOf(tileGridColumnCount(metroPrefs.showMoreColumns))
        private set
    var currentPage by mutableIntStateOf(0)
    /** Package+tile to bring into view after pin-to-Start; consumed by Start. */
    var pendingPinReveal by mutableStateOf<TileKey?>(null)
        private set
    var searchActive by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var editingTile by mutableStateOf<DisplayTile?>(null)
    /**
     * Bumped by [onHomeRequested] when leaving edit/customize via Start/Home so the shell
     * can remount Start and replay the tile enter wave.
     */
    var homeEnterRequestId by mutableIntStateOf(0)
        private set
    /** Non-null while the tile customize page is open (brush corner). */
    var customizingTile by mutableStateOf<DisplayTile?>(null)
        private set
    var tileCustomizeDraft by mutableStateOf<TileCustomizeDraft?>(null)
        private set
    /** True while the customize page is playing its pivot exit. */
    var tileCustomizeExiting by mutableStateOf(false)
    /**
     * Bumped on every open so [MetroPagePivotLoad] remounts and replays enter, and the
     * app bar can key its creep-in to the same session.
     */
    var tileCustomizeEpoch by mutableIntStateOf(0)
        private set
    /** False until the customize page pivot enter finishes — drives app bar creep-in. */
    var tileCustomizeAppBarVisible by mutableStateOf(false)
        private set
    /** Color-picker subpage stacked on customize (Settings accents pattern). */
    var tileCustomizeColorPickerOpen by mutableStateOf(false)
        private set
    var tileCustomizeColorPickerExiting by mutableStateOf(false)
    val widgetController = TileAppWidgetController(appContext)
    /**
     * Pending bind / configure activity after Save chooses a new widget. The activity
     * launches these intents and calls [onWidgetBindResult] / [onWidgetConfigureResult].
     */
    var pendingWidgetBindIntent by mutableStateOf<Intent?>(null)
        private set
    var pendingWidgetConfigureIntent by mutableStateOf<Intent?>(null)
        private set
    private var pendingWidgetBindEntryKey: TileKey? = null
    private var pendingWidgetBindId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    var showNotificationAccessPrompt by mutableStateOf(false)
    /** Non-null while Start is playing the system-wide splash open for a package. */
    var appOpenSplash by mutableStateOf<AppOpenSplashRequest?>(null)
        private set

    private var pinnedEntries by mutableStateOf(repository.loadPinnedTiles(gridColumns))
    /**
     * Bumped on every pin/unpin/reorder mutation. [refreshAllAsync] discards results started
     * before the latest bump so an in-flight reload cannot wipe a just-pinned contact tile.
     */
    private var layoutEpoch = 0
    /** Static Start chrome first; [refreshAllAsync] fills live tile payloads off the critical path. */
    var displayTiles by mutableStateOf(
        repository.resolveDisplayTiles(pinnedEntries, liveContent = false),
    )
    var apps by mutableStateOf(repository.discoverApps(pinnedEntries))

    /**
     * True while [refreshAllAsync] is resolving live tile providers (contacts, photos, …).
     * Live refresh updates tiles in place; it does not drive the splash loader.
     */
    var isRefreshingContent by mutableStateOf(false)
        private set

    /**
     * False until Start can paint its shell (pinned layout + static tile chrome).
     * Cold start keeps the splash loader up only until this flips and Start has drawn —
     * not until live providers finish.
     */
    var hasCompletedInitialLoad by mutableStateOf(false)
        private set

    val filteredApps: List<MetroAppInfo>
        get() = repository.filterApps(apps, searchQuery)

    val wideTilesEnabled: Boolean
        get() = BuildConfig.WIDE_TILES

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            MetroPreferenceKeys.THEME_MODE -> darkTheme = metroPrefs.isDark
            MetroPreferenceKeys.ACCENT_COLOR -> {
                accent = metroPrefs.accentColor
                // System/Metro tiles follow accent — never resolve providers on this callback thread.
                clearAppListIconCache()
                refreshTilesLiveAsync(pinnedEntries)
            }
            MetroPreferenceKeys.SHOW_MORE_COLUMNS -> applyShowMoreColumns(metroPrefs.showMoreColumns)
            MetroPreferenceKeys.START_BACKGROUND_ENABLED -> {
                scheduleStartBackgroundReload()
                refreshTilesLiveAsync(pinnedEntries)
            }
            MetroPreferenceKeys.CONNECTED_GALLERY_APPS,
            MetroPreferenceKeys.CONNECTED_MUSIC_APPS,
            -> {
                GalleryLiveTileStore.clearCache()
                refreshTilesLiveAsync(pinnedEntries)
            }
        }
    }

    private val tileUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val packageName = intent?.getStringExtra(MetroBroadcasts.EXTRA_TILE_PACKAGE) ?: return
            refreshTile(packageName)
        }
    }

    private val themeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action != MetroBroadcasts.ACTION_THEME_CHANGED) return
            val modeExtra = intent.getStringExtra(MetroBroadcasts.EXTRA_THEME_MODE)
            val accentExtra = intent.getStringExtra(MetroBroadcasts.EXTRA_ACCENT_COLOR)
            metroPrefs.cacheThemeSnapshot(
                themeMode = modeExtra?.let { MetroThemeMode.fromStorage(it) },
                accentColorHex = accentExtra,
                typeface = intent.getStringExtra(MetroBroadcasts.EXTRA_FONT_FAMILY)
                    ?.let { MetroTypeface.fromStorage(it) },
            )
            modeExtra?.let { mode ->
                darkTheme = MetroThemeMode.fromStorage(mode) == MetroThemeMode.Dark
            }
            accentExtra?.let { hex ->
                accent = MetroPreferences.parseAccentHex(hex)
                clearAppListIconCache()
            }
            scheduleStartBackgroundReload()
            refreshTilesLiveAsync(pinnedEntries)
        }
    }

    private var prefsObserver: ContentObserver? = null
    /** Coalesces Settings ContentObserver storms (common right after unlock). */
    private var prefsDrivenRefreshJob: Job? = null

    init {
        appContext.getSharedPreferences(MetroPreferenceKeys.PREFS_NAME, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    fun registerReceivers(context: Context) {
        val tileFilter = IntentFilter(MetroBroadcasts.ACTION_TILE_UPDATE)
        val themeFilter = IntentFilter(MetroBroadcasts.ACTION_THEME_CHANGED)
        context.registerReceiver(tileUpdateReceiver, tileFilter, Context.RECEIVER_EXPORTED)
        // Settings is a different package — must be exported to receive THEME_CHANGED.
        context.registerReceiver(themeReceiver, themeFilter, Context.RECEIVER_EXPORTED)
        prefsObserver = metroPrefs.registerObserver {
            // ContentObserver.onChange runs on the main thread. Theme fields are cheap; live
            // tile ContentProvider queries must not run here (ANR on unlock / theme pull).
            darkTheme = metroPrefs.isDark
            accent = metroPrefs.accentColor
            applyShowMoreColumns(metroPrefs.showMoreColumns)
            clearAppListIconCache()
            schedulePrefsDrivenRefresh()
        }
    }

    fun unregisterReceivers(context: Context) {
        context.unregisterReceiver(tileUpdateReceiver)
        context.unregisterReceiver(themeReceiver)
        metroPrefs.unregisterObserver(prefsObserver)
        prefsObserver = null
        appContext.getSharedPreferences(MetroPreferenceKeys.PREFS_NAME, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    fun refreshAll() {
        // Keep API sync-looking for callers, but never resolve live providers on the calling
        // thread (permission callbacks / tests may be on main).
        persistScope.launch {
            withContext(Dispatchers.Main) { refreshAllAsync() }
        }
    }

    /**
     * Loads pinned layout on the caller thread, then resolves live tile ContentProviders on
     * [Dispatchers.IO] so Start can paint before SMS/contacts/media queries finish.
     */
    suspend fun refreshAllAsync() {
        isRefreshingContent = true
        try {
            // Flush in-flight pin/unpin writes so a resume reload cannot clobber them.
            persistJob?.join()
            val epochAtStart = layoutEpoch
            applyShowMoreColumns(metroPrefs.showMoreColumns)
            val columns = gridColumns
            val pinned = withContext(Dispatchers.IO) { repository.loadPinnedTiles(columns) }
            if (epochAtStart != layoutEpoch) return
            pinnedEntries = pinned
            apps = withContext(Dispatchers.IO) { repository.discoverApps(pinned) }
            if (epochAtStart != layoutEpoch) return
            // Settings provider is often unreachable on the first frame after a cold start;
            // pull with retries so system/Metro tiles get the real accent before paint.
            withContext(Dispatchers.IO) {
                repeat(8) { attempt ->
                    if (metroPrefs.pullThemeFromProvider()) return@withContext
                    if (attempt < 7) delay(40L * (attempt + 1))
                }
            }
            darkTheme = metroPrefs.isDark
            accent = metroPrefs.accentColor
            refreshNotificationAccessPrompt()
            val background = withContext(Dispatchers.IO) {
                if (metroPrefs.startBackgroundEnabled) {
                    MetroStartBackground.decode(appContext)
                } else {
                    null
                }
            }
            startBackgroundBitmap = background
            if (!hasCompletedInitialLoad) {
                // Cold start: paint static chrome so splash can lift; live providers fill in.
                displayTiles = withContext(Dispatchers.IO) {
                    repository.resolveDisplayTiles(pinned, liveContent = false)
                }
                if (epochAtStart != layoutEpoch) return
                hasCompletedInitialLoad = true
            } else {
                // Resume / unlock: keep already-painted live faces. Never flash icon+title
                // placeholders (calendar, photos, …) while ContentProviders re-resolve.
                val existing = displayTiles
                displayTiles = withContext(Dispatchers.IO) {
                    mergePinnedDisplayTiles(
                        pinned = pinned,
                        existing = existing,
                        resolveMissing = { missing ->
                            repository.resolveDisplayTiles(missing, liveContent = false)
                        },
                    )
                }
                if (epochAtStart != layoutEpoch) return
            }
            val liveTiles = withContext(Dispatchers.IO) {
                repository.resolveDisplayTiles(pinned, liveContent = true)
            }
            if (epochAtStart != layoutEpoch) return
            displayTiles = liveTiles
        } finally {
            isRefreshingContent = false
            hasCompletedInitialLoad = true
        }
    }

    fun refreshNotificationAccessPrompt() {
        val dismissed = launcherPrefs.getBoolean(KEY_NOTIF_PROMPT_DISMISSED, false)
        showNotificationAccessPrompt =
            !dismissed && !TileNotificationAccess.isEnabled(appContext)
    }

    fun openNotificationAccessSettings() {
        TileNotificationAccess.openSettings(appContext)
    }

    fun dismissNotificationAccessPrompt() {
        launcherPrefs.edit().putBoolean(KEY_NOTIF_PROMPT_DISMISSED, true).apply()
        showNotificationAccessPrompt = false
    }

    /** Loads or clears the cropped Start background JPEG from Settings. */
    fun reloadStartBackground() {
        scheduleStartBackgroundReload()
    }

    fun refreshTile(packageName: String) {
        refreshTilesLiveAsync(pinnedEntries.filter { it.packageName == packageName })
    }

    fun onTileClick(tile: DisplayTile) {
        // 1×1 music now-playing face is transport-only (play/pause), matching Xbox Music small tile.
        val music = tile.musicNowPlaying
        if (music != null && tile.entry.size == PinnedTileSize.OneByOne) {
            MusicNowPlayingStore.togglePlayPause(music.packageName)
            return
        }
        // Custom Start widget faces: dispatch tapAction (or swallow for display-only clocks).
        if (tile.handlesStartTap) {
            val action = tile.tapAction
            if (!action.isNullOrBlank()) {
                MetroIntents.dispatchTileTap(
                    context = appContext,
                    packageName = tile.entry.packageName,
                    tileId = tile.entry.tileId,
                    action = action,
                )
            }
            return
        }
        beginAppOpen(
            packageName = tile.entry.packageName,
            deepLinkUri = tile.deepLinkUri,
            backgroundColor = tile.backgroundColor,
        )
    }

    fun launchApp(app: MetroAppInfo) {
        // App list always opens the package. Do not route through onTileClick — pinned
        // widget faces swallow taps / dispatch tile actions (clocks, torch, lock, …).
        val pinnedTile = displayTiles.firstOrNull { it.entry.packageName == app.packageName }
        beginAppOpen(
            packageName = app.packageName,
            deepLinkUri = null,
            backgroundColor = pinnedTile?.backgroundColor,
        )
    }

    /**
     * Shows [MetroAppOpenSplash] for [packageName], then starts the activity when the
     * pivot enter completes. Covers suite and third-party packages alike.
     */
    fun beginAppOpen(
        packageName: String,
        deepLinkUri: String?,
        backgroundColor: Color? = null,
        shortcut: AppLauncherOption? = null,
    ) {
        if (appOpenSplash != null) return
        val glyphResId = CustomTileBranding.glyphResId(packageName)
        val bg = backgroundColor
            ?: CustomTileBranding.resolveBackgroundColor(appContext, packageName)
            ?: MetroAppBranding.resolveTileBackgroundColor(appContext, packageName)
        val iconBitmap = if (glyphResId == null) {
            val px = (OPEN_SPLASH_ICON_DP * appContext.resources.displayMetrics.density)
                .toInt()
                .coerceAtLeast(1)
            MetroAppBranding.loadAppIcon(appContext, packageName)
                ?.toBitmap(px, px)
                ?.asImageBitmap()
        } else {
            null
        }
        appOpenSplash = AppOpenSplashRequest(
            packageName = packageName,
            deepLinkUri = deepLinkUri,
            backgroundColor = bg,
            iconBitmap = iconBitmap,
            glyphResId = glyphResId,
            shortcut = shortcut,
        )
    }

    /** Pivot enter finished — start the target under the splash, then clear on pause. */
    fun onAppOpenSplashEnterComplete() {
        val req = appOpenSplash ?: return
        if (req.launched) return
        if (req.shortcut != null) {
            repository.launchAppOption(req.shortcut, hostContext)
        } else {
            repository.launchApp(req.packageName, req.deepLinkUri, hostContext)
        }
        appOpenSplash = req.copy(launched = true)
    }

    fun clearAppOpenSplash() {
        appOpenSplash = null
    }

    fun onTileLongPress(tile: DisplayTile) {
        editingTile = tile
    }

    fun dismissEdit() {
        editingTile = null
    }

    /**
     * Navbar Start / system Home while edit or tile-customize is open: snap back to Start
     * and ask the shell to replay the tile enter wave. Leaves [editingTile] for the shell
     * to clear after arming snap-exit so the edit outro does not fight the enter wave.
     *
     * @return true when an overlay was active and the shell should finish the home return.
     */
    fun onHomeRequested(): Boolean {
        val wasCustomizing = customizingTile != null
        val wasEditing = editingTile != null
        if (!wasCustomizing && !wasEditing) return false
        if (wasCustomizing) {
            // Skip the customize pivot exit — Home should land on Start immediately.
            finishCloseTileCustomize()
        }
        currentPage = 0
        dismissSearch()
        homeEnterRequestId++
        return true
    }

    fun resizeEditingTile() {
        val current = editingTile ?: return
        val newSize = TileSizeCycle.nextSize(current.entry.size)
        updateTileSize(current.entry, newSize)
        editingTile = displayTiles.firstOrNull {
            it.entry.packageName == current.entry.packageName &&
                it.entry.tileId == current.entry.tileId
        }
    }

    fun unpinEditingTile() {
        val current = editingTile ?: return
        deleteWidgetIfNeeded(current.entry)
        unpinTile(current.entry)
        editingTile = null
    }

    fun openTileCustomize() {
        val current = editingTile ?: return
        // Keep editingTile while customize covers Start — clearing it ran the edit-exit tween
        // across every tile and stalled the page pivot. Motion freezes via suspendEditMotion.
        tileCustomizeEpoch++
        customizingTile = current
        // Never call accentColorHex here — it hits the Settings ContentProvider on the main
        // thread and can stall open for seconds when the provider process is cold.
        tileCustomizeDraft = TileCustomizeDraft(
            backgroundMode = current.entry.backgroundMode,
            customBackgroundHex = current.entry.customBackgroundHex
                ?: MetroAccentPalette.normalizeHex(
                    metroPrefs.peekCachedAccentColorHex()
                        ?: MetroPreferences.DEFAULT_ACCENT_HEX,
                ),
            useCustomWidget = current.entry.useCustomWidget,
            widgetProvider = current.entry.widgetProvider,
        )
        tileCustomizeExiting = false
        tileCustomizeAppBarVisible = false
        tileCustomizeColorPickerOpen = false
        tileCustomizeColorPickerExiting = false
    }

    fun onTileCustomizeEnterComplete() {
        // App bar is already visible on open; keep this for any future enter-gated chrome.
        if (customizingTile == null || tileCustomizeExiting) return
        if (tileCustomizeColorPickerOpen || tileCustomizeColorPickerExiting) return
        tileCustomizeAppBarVisible = true
    }

    fun openTileColorPicker() {
        if (customizingTile == null || tileCustomizeExiting) return
        tileCustomizeAppBarVisible = false
        tileCustomizeColorPickerOpen = true
        tileCustomizeColorPickerExiting = false
    }

    fun beginCloseTileColorPicker() {
        if (!tileCustomizeColorPickerOpen || tileCustomizeColorPickerExiting) return
        tileCustomizeColorPickerExiting = true
    }

    fun finishCloseTileColorPicker() {
        tileCustomizeColorPickerOpen = false
        tileCustomizeColorPickerExiting = false
        if (customizingTile != null && !tileCustomizeExiting) {
            tileCustomizeAppBarVisible = true
        }
    }

    fun selectTileCustomColor(hex: String) {
        val draft = tileCustomizeDraft ?: return
        tileCustomizeDraft = draft.copy(
            customBackgroundHex = MetroAccentPalette.normalizeHex(hex) ?: hex,
            backgroundMode = TileBackgroundMode.Custom,
        )
        beginCloseTileColorPicker()
    }

    fun beginCloseTileCustomize() {
        if (customizingTile == null || tileCustomizeExiting) return
        if (tileCustomizeColorPickerOpen) {
            beginCloseTileColorPicker()
            return
        }
        tileCustomizeAppBarVisible = false
        tileCustomizeExiting = true
        // Snap-clear edit under the still-opaque page (suspendEditMotion → snapExit).
        editingTile = null
    }

    fun finishCloseTileCustomize() {
        customizingTile = null
        tileCustomizeDraft = null
        tileCustomizeExiting = false
        tileCustomizeAppBarVisible = false
        tileCustomizeColorPickerOpen = false
        tileCustomizeColorPickerExiting = false
    }

    fun updateTileCustomizeDraft(draft: TileCustomizeDraft) {
        tileCustomizeDraft = draft
    }

    /**
     * Persists draft customize settings. Widget binding may defer completion until the
     * activity returns from ACTION_APPWIDGET_BIND / configure.
     */
    fun saveTileCustomize() {
        val tile = customizingTile ?: return
        val draft = tileCustomizeDraft ?: return
        val key = TileKey(tile.entry.packageName, tile.entry.tileId)
        val previous = tile.entry
        val bgHex = when (draft.backgroundMode) {
            TileBackgroundMode.Custom ->
                MetroAccentPalette.normalizeHex(draft.customBackgroundHex.orEmpty())
            else -> null
        }
        val wantsWidget = draft.useCustomWidget &&
            previous.supportsCustomWidget() &&
            !draft.widgetProvider.isNullOrBlank()
        val providerChanged = wantsWidget &&
            (draft.widgetProvider != previous.widgetProvider ||
                previous.appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)

        if (!wantsWidget) {
            deleteWidgetIfNeeded(previous)
            applyTileCustomize(
                key = key,
                backgroundMode = draft.backgroundMode,
                customBackgroundHex = bgHex,
                useCustomWidget = false,
                widgetProvider = null,
                appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            beginCloseTileCustomize()
            return
        }

        if (!providerChanged) {
            applyTileCustomize(
                key = key,
                backgroundMode = draft.backgroundMode,
                customBackgroundHex = bgHex,
                useCustomWidget = true,
                widgetProvider = draft.widgetProvider,
                appWidgetId = previous.appWidgetId,
            )
            beginCloseTileCustomize()
            return
        }

        val component = ComponentName.unflattenFromString(draft.widgetProvider!!) ?: run {
            beginCloseTileCustomize()
            return
        }
        deleteWidgetIfNeeded(previous)
        pendingWidgetBindEntryKey = key
        tileCustomizeDraft = draft.copy(customBackgroundHex = bgHex)
        val boundId = widgetController.allocateAndBind(component) { intent ->
            pendingWidgetBindId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            pendingWidgetBindIntent = intent
        }
        if (boundId != null) {
            finishWidgetBind(boundId)
        }
    }

    fun onWidgetBindResult(granted: Boolean, appWidgetId: Int) {
        pendingWidgetBindIntent = null
        val resolvedId = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetId
        } else {
            pendingWidgetBindId
        }
        if (!granted) {
            if (resolvedId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                widgetController.deleteAppWidgetId(resolvedId)
            }
            pendingWidgetBindEntryKey = null
            pendingWidgetBindId = AppWidgetManager.INVALID_APPWIDGET_ID
            beginCloseTileCustomize()
            return
        }
        finishWidgetBind(resolvedId)
    }

    fun onWidgetConfigureResult(ok: Boolean, appWidgetId: Int) {
        pendingWidgetConfigureIntent = null
        val key = pendingWidgetBindEntryKey
        val draft = tileCustomizeDraft
        val resolvedId = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetId
        } else {
            pendingWidgetBindId
        }
        pendingWidgetBindEntryKey = null
        pendingWidgetBindId = AppWidgetManager.INVALID_APPWIDGET_ID
        if (!ok || key == null || draft == null) {
            if (resolvedId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                widgetController.deleteAppWidgetId(resolvedId)
            }
            beginCloseTileCustomize()
            return
        }
        applyTileCustomize(
            key = key,
            backgroundMode = draft.backgroundMode,
            customBackgroundHex = draft.customBackgroundHex,
            useCustomWidget = true,
            widgetProvider = draft.widgetProvider,
            appWidgetId = resolvedId,
        )
        beginCloseTileCustomize()
    }

    private fun finishWidgetBind(appWidgetId: Int) {
        pendingWidgetBindId = appWidgetId
        val draft = tileCustomizeDraft ?: return
        val provider = draft.widgetProvider ?: return
        val info = widgetController.providerInfo(provider)
        val configure = info?.let { widgetController.configurationIntent(appWidgetId, it) }
        if (configure != null) {
            pendingWidgetConfigureIntent = configure
            return
        }
        val key = pendingWidgetBindEntryKey ?: return
        pendingWidgetBindEntryKey = null
        pendingWidgetBindId = AppWidgetManager.INVALID_APPWIDGET_ID
        applyTileCustomize(
            key = key,
            backgroundMode = draft.backgroundMode,
            customBackgroundHex = draft.customBackgroundHex,
            useCustomWidget = true,
            widgetProvider = provider,
            appWidgetId = appWidgetId,
        )
        beginCloseTileCustomize()
    }

    private fun applyTileCustomize(
        key: TileKey,
        backgroundMode: TileBackgroundMode,
        customBackgroundHex: String?,
        useCustomWidget: Boolean,
        widgetProvider: String?,
        appWidgetId: Int,
    ) {
        layoutEpoch++
        pinnedEntries = pinnedEntries.map { entry ->
            if (entry.packageName == key.packageName && entry.tileId == key.tileId) {
                entry.copy(
                    backgroundMode = backgroundMode,
                    customBackgroundHex = customBackgroundHex,
                    useCustomWidget = useCustomWidget,
                    widgetProvider = widgetProvider,
                    appWidgetId = appWidgetId,
                )
            } else {
                entry
            }
        }
        displayTiles = applyPinnedLayoutToDisplayTiles()
        editingTile = displayTiles.firstOrNull {
            it.entry.packageName == key.packageName && it.entry.tileId == key.tileId
        }
        customizingTile = editingTile
        // Keep app-bar / pivot session stable through save; visibility is owned by close.
        persistPinnedEntriesAsync()
        refreshTilesLiveAsync(
            pinnedEntries.filter {
                it.packageName == key.packageName && it.tileId == key.tileId
            },
        )
    }

    private fun deleteWidgetIfNeeded(entry: PinnedTileEntry) {
        if (entry.appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            widgetController.deleteAppWidgetId(entry.appWidgetId)
        }
    }

    /**
     * Live magnet preview while dragging in edit mode. Updates in-memory grid positions only;
     * call [commitTileOrder] on drag end to persist.
     */
    fun applyDragLayout(placements: List<PlacedTile>) {
        displayTiles = displayTiles.map { tile ->
            val placement = placements.firstOrNull { sameTile(it.tile, tile) }
            if (placement != null) {
                tile.copy(
                    entry = tile.entry.copy(
                        gridCol = placement.col,
                        gridRow = placement.row,
                    ),
                )
            } else {
                tile
            }
        }
        val editing = editingTile ?: return
        editingTile = displayTiles.firstOrNull {
            it.entry.packageName == editing.entry.packageName &&
                it.entry.tileId == editing.entry.tileId
        }
    }

    fun commitTileOrder() {
        layoutEpoch++
        pinnedEntries = compactEmptyRows(
            pinnedEntries.map { entry ->
                val display = displayTiles.firstOrNull {
                    it.entry.packageName == entry.packageName && it.entry.tileId == entry.tileId
                }
                if (display != null) {
                    entry.copy(
                        gridCol = display.entry.gridCol,
                        gridRow = display.entry.gridRow,
                    )
                } else {
                    entry
                }
            },
        )
        displayTiles = applyPinnedLayoutToDisplayTiles()
        editingTile = editingTile?.let { editing ->
            displayTiles.firstOrNull {
                it.entry.packageName == editing.entry.packageName &&
                    it.entry.tileId == editing.entry.tileId
            }
        }
        persistPinnedEntriesAsync()
    }

    fun updateTileSize(entry: PinnedTileEntry, size: PinnedTileSize) {
        layoutEpoch++
        val resized = applyTileResize(
            entries = pinnedEntries,
            packageName = entry.packageName,
            tileId = entry.tileId,
            newSize = size,
            columns = gridColumns,
        )
        pinnedEntries = resized.map { next ->
            if (next.packageName == entry.packageName && next.tileId == entry.tileId) {
                if (next.supportsCustomWidget()) {
                    next
                } else {
                    if (next.appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        widgetController.deleteAppWidgetId(next.appWidgetId)
                    }
                    next.copy(
                        useCustomWidget = false,
                        widgetProvider = null,
                        appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID,
                    )
                }
            } else {
                next
            }
        }
        displayTiles = applyPinnedLayoutToDisplayTiles()
        persistPinnedEntriesAsync()
    }

    fun unpinTile(entry: PinnedTileEntry) {
        deleteWidgetIfNeeded(entry)
        pinnedEntries = pinnedEntries.filterNot {
            it.packageName == entry.packageName && it.tileId == entry.tileId
        }
        persistLayoutAndPaint()
        syncAppPinnedFlags()
    }

    fun pinApp(app: MetroAppInfo) {
        val existing = pinnedEntries.firstOrNull { it.packageName == app.packageName }
        if (existing != null) {
            revealPinnedTile(existing.packageName, existing.tileId)
            return
        }
        pinNewEntry(
            PinnedTileEntry(
                packageName = app.packageName,
                size = PinnedTileSize.OneByOne,
            ),
        )
    }

    /**
     * Pin a primary or secondary tile (e.g. People contact shortcut).
     * Reveals Start when the same package+tileId is already pinned.
     */
    fun pinTile(
        packageName: String,
        tileId: String,
        size: PinnedTileSize = PinnedTileSize.TwoByTwo,
    ) {
        val existing = pinnedEntries.firstOrNull {
            it.packageName == packageName && it.tileId == tileId
        }
        if (existing != null) {
            revealPinnedTile(existing.packageName, existing.tileId)
            return
        }
        pinNewEntry(
            PinnedTileEntry(
                packageName = packageName,
                tileId = tileId,
                size = size,
            ),
        )
    }

    fun consumePinReveal() {
        pendingPinReveal = null
    }

    fun handlePinTileIntent(intent: Intent?) {
        if (intent?.action != MetroIntents.ACTION_PIN_TILE) return
        val packageName = intent.getStringExtra(MetroIntents.EXTRA_PACKAGE)?.trim().orEmpty()
        if (packageName.isEmpty()) return
        val tileId = intent.getStringExtra(MetroIntents.EXTRA_TILE_ID)
            ?.takeIf { it.isNotBlank() }
            ?: MetroTileContract.DEFAULT_TILE_ID
        val size = intent.getStringExtra(MetroIntents.EXTRA_TILE_SIZE)
            ?.takeIf { it.isNotBlank() }
            ?.let { PinnedTileSize.fromStorage(it) }
            ?: PinnedTileSize.TwoByTwo
        pinTile(packageName = packageName, tileId = tileId, size = size)
    }

    fun uninstallApp(app: MetroAppInfo) {
        if (app.isSystemApp) return
        pinnedEntries
            .filter { it.packageName == app.packageName }
            .forEach { unpinTile(it) }
        repository.requestUninstall(hostContext, app.packageName)
    }

    suspend fun queryAppOptions(packageName: String): List<AppLauncherOption> =
        withContext(Dispatchers.IO) {
            repository.queryAppOptions(packageName)
        }

    fun launchAppOption(option: AppLauncherOption) {
        beginAppOpen(
            packageName = option.packageName,
            deepLinkUri = null,
            shortcut = option,
        )
    }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
    }

    fun onSearchActiveChange(active: Boolean) {
        searchActive = active
        if (!active) searchQuery = ""
    }

    fun dismissSearch() {
        onSearchActiveChange(false)
    }

    /**
     * Keeps tile chrome (photos, contacts, music, …) while applying grid layout changes.
     * Avoids re-querying every live ContentProvider on resize/reorder.
     */
    private fun applyPinnedLayoutToDisplayTiles(): List<DisplayTile> {
        val existingByKey = displayTiles.associateBy {
            it.entry.packageName to it.entry.tileId
        }
        return pinnedEntries.mapNotNull { entry ->
            existingByKey[entry.packageName to entry.tileId]?.copy(entry = entry)
        }
    }

    private fun persistPinnedEntriesAsync() {
        val snapshot = pinnedEntries.toList()
        val generation = ++persistGeneration
        persistJob = persistScope.launch {
            persistMutex.withLock {
                if (generation != persistGeneration) return@withLock
                repository.savePinnedTiles(snapshot)
            }
        }
    }

    /**
     * Immediate Start paint: keep existing live chrome, resolve only newly pinned
     * tiles as static faces, persist off the main thread. Live payloads fill in after.
     */
    private fun persistLayoutAndPaint() {
        layoutEpoch++
        pinnedEntries = compactEmptyRows(pinnedEntries)
        val previousKeys = displayTiles.map { it.entry.packageName to it.entry.tileId }.toSet()
        displayTiles = mergePinnedDisplayTiles(
            pinned = pinnedEntries,
            existing = displayTiles,
            resolveMissing = { missing ->
                repository.resolveDisplayTiles(missing, liveContent = false)
            },
        )
        persistPinnedEntriesAsync()
        val added = pinnedEntries.filter { entry ->
            (entry.packageName to entry.tileId) !in previousKeys
        }
        refreshTilesLiveAsync(added)
    }

    private fun pinNewEntry(entry: PinnedTileEntry) {
        pinnedEntries = ensureGridPositions(
            pinnedEntries + entry,
            columns = gridColumns,
        )
        persistLayoutAndPaint()
        syncAppPinnedFlags()
        revealPinnedTile(entry.packageName, entry.tileId)
    }

    private fun revealPinnedTile(packageName: String, tileId: String) {
        pendingPinReveal = TileKey(packageName, tileId)
        currentPage = 0
    }

    private fun syncAppPinnedFlags() {
        val pinnedPackages = pinnedEntries.map { it.packageName }.toSet()
        apps = apps.map { app ->
            val pinned = app.packageName in pinnedPackages
            if (app.isPinned == pinned) app else app.copy(isPinned = pinned)
        }
    }

    private fun refreshTilesLiveAsync(entries: List<PinnedTileEntry>) {
        if (entries.isEmpty()) return
        val epoch = layoutEpoch
        persistScope.launch {
            val live = repository.resolveDisplayTiles(entries, liveContent = true)
            withContext(Dispatchers.Main) {
                if (epoch != layoutEpoch) return@withContext
                val liveByKey = live.associateBy { it.entry.packageName to it.entry.tileId }
                displayTiles = displayTiles.map { tile ->
                    liveByKey[tile.entry.packageName to tile.entry.tileId] ?: tile
                }
            }
        }
    }

    /**
     * Settings ContentObserver can fire in a burst on unlock. Coalesce background decode +
     * live tile resolve onto IO so the main thread can keep a focused window.
     */
    private fun schedulePrefsDrivenRefresh() {
        prefsDrivenRefreshJob?.cancel()
        prefsDrivenRefreshJob = persistScope.launch {
            delay(48L)
            val epoch = layoutEpoch
            val pinned = pinnedEntries
            val bmp = if (metroPrefs.startBackgroundEnabled) {
                MetroStartBackground.decode(appContext)
            } else {
                null
            }
            val live = repository.resolveDisplayTiles(pinned, liveContent = true)
            withContext(Dispatchers.Main) {
                if (epoch != layoutEpoch) return@withContext
                startBackgroundBitmap = bmp
                displayTiles = live
            }
        }
    }

    private fun scheduleStartBackgroundReload() {
        persistScope.launch {
            val bmp = if (metroPrefs.startBackgroundEnabled) {
                MetroStartBackground.decode(appContext)
            } else {
                null
            }
            withContext(Dispatchers.Main) {
                startBackgroundBitmap = bmp
            }
        }
    }

    /** Applies Settings → show more columns; reflows when the column count changes. */
    private fun applyShowMoreColumns(enabled: Boolean) {
        val columns = tileGridColumnCount(enabled)
        if (columns == gridColumns) return
        gridColumns = columns
        pinnedEntries = adaptTilesToColumnCount(pinnedEntries, columns)
        persistLayoutAndPaint()
    }

    companion object {
        private const val PREFS_LAUNCHER = "metro_launcher"
        private const val KEY_NOTIF_PROMPT_DISMISSED = "notification_access_prompt_dismissed"
        /** Matches toolkit splash glyph box (288dp). */
        private const val OPEN_SPLASH_ICON_DP = 288f
    }
}
