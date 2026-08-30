package com.metro.launcher.ui

import android.content.BroadcastReceiver
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
import com.metro.launcher.data.adaptTilesToColumnCount
import com.metro.launcher.data.applyTileResize
import com.metro.launcher.data.compactEmptyRows
import com.metro.launcher.data.ensureGridPositions
import com.metro.launcher.data.PinnedTileSize
import com.metro.launcher.data.TileNotificationAccess
import com.metro.launcher.data.TileSizeCycle
import com.metro.launcher.data.tileGridColumnCount
import com.metro.system.MetroAppBranding
import com.metro.system.MetroAppInfo
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroIntents
import com.metro.system.MetroPreferenceKeys
import com.metro.system.MetroPreferences
import com.metro.system.MetroStartBackground
import com.metro.system.MetroThemeMode
import com.metro.system.MetroTileContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    var searchActive by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var editingTile by mutableStateOf<DisplayTile?>(null)
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
                // System/Metro tiles follow accent; re-resolve fills immediately.
                displayTiles = repository.resolveDisplayTiles(pinnedEntries, liveContent = true)
                clearAppListIconCache()
            }
            MetroPreferenceKeys.SHOW_MORE_COLUMNS -> applyShowMoreColumns(metroPrefs.showMoreColumns)
            MetroPreferenceKeys.START_BACKGROUND_ENABLED -> {
                reloadStartBackground()
                displayTiles = repository.resolveDisplayTiles(pinnedEntries, liveContent = true)
            }
            MetroPreferenceKeys.CONNECTED_GALLERY_APPS,
            MetroPreferenceKeys.CONNECTED_MUSIC_APPS,
            -> {
                GalleryLiveTileStore.clearCache()
                displayTiles = repository.resolveDisplayTiles(pinnedEntries, liveContent = true)
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
            )
            modeExtra?.let { mode ->
                darkTheme = MetroThemeMode.fromStorage(mode) == MetroThemeMode.Dark
            }
            accentExtra?.let { hex ->
                accent = MetroPreferences.parseAccentHex(hex)
                clearAppListIconCache()
            }
            reloadStartBackground()
            displayTiles = repository.resolveDisplayTiles(pinnedEntries, liveContent = true)
        }
    }

    private var prefsObserver: ContentObserver? = null

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
            darkTheme = metroPrefs.isDark
            accent = metroPrefs.accentColor
            applyShowMoreColumns(metroPrefs.showMoreColumns)
            reloadStartBackground()
            displayTiles = repository.resolveDisplayTiles(pinnedEntries, liveContent = true)
            clearAppListIconCache()
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
        applyShowMoreColumns(metroPrefs.showMoreColumns)
        pinnedEntries = repository.loadPinnedTiles(gridColumns)
        displayTiles = repository.resolveDisplayTiles(pinnedEntries, liveContent = true)
        apps = repository.discoverApps(pinnedEntries)
        darkTheme = metroPrefs.isDark
        accent = metroPrefs.accentColor
        reloadStartBackground()
        refreshNotificationAccessPrompt()
    }

    /**
     * Loads pinned layout on the caller thread, then resolves live tile ContentProviders on
     * [Dispatchers.IO] so Start can paint before SMS/contacts/media queries finish.
     */
    suspend fun refreshAllAsync() {
        isRefreshingContent = true
        try {
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
            withContext(Dispatchers.IO) { reloadStartBackground() }
            // Paint static chrome first so cold-start splash can lift; live providers fill in.
            displayTiles = withContext(Dispatchers.IO) {
                repository.resolveDisplayTiles(pinned, liveContent = false)
            }
            if (epochAtStart != layoutEpoch) return
            hasCompletedInitialLoad = true
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
        startBackgroundBitmap = if (metroPrefs.startBackgroundEnabled) {
            MetroStartBackground.decode(appContext)
        } else {
            null
        }
    }

    fun refreshTile(packageName: String) {
        displayTiles = displayTiles.map { tile ->
            if (tile.entry.packageName == packageName) {
                repository.resolveDisplayTiles(listOf(tile.entry)).first()
            } else {
                tile
            }
        }
    }

    fun onTileClick(tile: DisplayTile) {
        // 1×1 music now-playing face is transport-only (play/pause), matching Xbox Music small tile.
        val music = tile.musicNowPlaying
        if (music != null && tile.entry.size == PinnedTileSize.OneByOne) {
            MusicNowPlayingStore.togglePlayPause(music.packageName)
            return
        }
        beginAppOpen(
            packageName = tile.entry.packageName,
            deepLinkUri = tile.deepLinkUri,
            backgroundColor = tile.backgroundColor,
        )
    }

    fun launchApp(app: MetroAppInfo) {
        val pinnedTile = displayTiles.firstOrNull { it.entry.packageName == app.packageName }
        if (pinnedTile != null) {
            onTileClick(pinnedTile)
        } else {
            beginAppOpen(packageName = app.packageName, deepLinkUri = null)
        }
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
        unpinTile(current.entry)
        editingTile = null
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
        displayTiles = displayTiles.map { tile ->
            val entry = pinnedEntries.firstOrNull {
                it.packageName == tile.entry.packageName && it.tileId == tile.entry.tileId
            }
            if (entry != null) {
                tile.copy(
                    entry = tile.entry.copy(
                        gridCol = entry.gridCol,
                        gridRow = entry.gridRow,
                    ),
                )
            } else {
                tile
            }
        }
        editingTile = editingTile?.let { editing ->
            displayTiles.firstOrNull {
                it.entry.packageName == editing.entry.packageName &&
                    it.entry.tileId == editing.entry.tileId
            }
        }
        repository.savePinnedTiles(pinnedEntries)
    }

    fun updateTileSize(entry: PinnedTileEntry, size: PinnedTileSize) {
        pinnedEntries = applyTileResize(
            entries = pinnedEntries,
            packageName = entry.packageName,
            tileId = entry.tileId,
            newSize = size,
            columns = gridColumns,
        )
        persistAndRefresh()
    }

    fun unpinTile(entry: PinnedTileEntry) {
        pinnedEntries = pinnedEntries.filterNot {
            it.packageName == entry.packageName && it.tileId == entry.tileId
        }
        persistAndRefresh()
    }

    fun pinApp(app: MetroAppInfo) {
        if (pinnedEntries.any { it.packageName == app.packageName }) return
        pinnedEntries = ensureGridPositions(
            pinnedEntries + PinnedTileEntry(
                packageName = app.packageName,
                size = PinnedTileSize.OneByOne,
            ),
            columns = gridColumns,
        )
        persistAndRefresh()
        currentPage = 0
    }

    /**
     * Pin a primary or secondary tile (e.g. People contact shortcut).
     * No-ops when the same package+tileId is already pinned.
     */
    fun pinTile(
        packageName: String,
        tileId: String,
        size: PinnedTileSize = PinnedTileSize.TwoByTwo,
    ) {
        if (pinnedEntries.any { it.packageName == packageName && it.tileId == tileId }) {
            currentPage = 0
            return
        }
        pinnedEntries = ensureGridPositions(
            pinnedEntries + PinnedTileEntry(
                packageName = packageName,
                tileId = tileId,
                size = size,
            ),
            columns = gridColumns,
        )
        persistAndRefresh()
        currentPage = 0
    }

    fun handlePinTileIntent(intent: Intent?) {
        if (intent?.action != MetroIntents.ACTION_PIN_TILE) return
        val packageName = intent.getStringExtra(MetroIntents.EXTRA_PACKAGE)?.trim().orEmpty()
        if (packageName.isEmpty()) return
        val tileId = intent.getStringExtra(MetroIntents.EXTRA_TILE_ID)
            ?.takeIf { it.isNotBlank() }
            ?: MetroTileContract.DEFAULT_TILE_ID
        pinTile(packageName = packageName, tileId = tileId)
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

    private fun persistAndRefresh() {
        layoutEpoch++
        pinnedEntries = compactEmptyRows(pinnedEntries)
        repository.savePinnedTiles(pinnedEntries)
        displayTiles = repository.resolveDisplayTiles(pinnedEntries, liveContent = true)
        apps = repository.discoverApps(pinnedEntries)
    }

    /** Applies Settings → show more columns; reflows when the column count changes. */
    private fun applyShowMoreColumns(enabled: Boolean) {
        val columns = tileGridColumnCount(enabled)
        if (columns == gridColumns) return
        gridColumns = columns
        pinnedEntries = adaptTilesToColumnCount(pinnedEntries, columns)
        persistAndRefresh()
    }

    companion object {
        private const val PREFS_LAUNCHER = "metro_launcher"
        private const val KEY_NOTIF_PROMPT_DISMISSED = "notification_access_prompt_dismissed"
        /** Matches toolkit splash glyph box (288dp). */
        private const val OPEN_SPLASH_ICON_DP = 288f
    }
}
