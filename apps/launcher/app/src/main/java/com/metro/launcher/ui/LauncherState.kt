package com.metro.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.database.ContentObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.launcher.BuildConfig
import com.metro.launcher.data.AppLauncherOption
import com.metro.launcher.data.DisplayTile
import com.metro.launcher.data.LauncherRepository
import com.metro.launcher.data.PinnedTileEntry
import com.metro.launcher.data.applyTileResize
import com.metro.launcher.data.compactEmptyRows
import com.metro.launcher.data.ensureGridPositions
import com.metro.launcher.data.PinnedTileSize
import com.metro.launcher.data.TileNotificationAccess
import com.metro.launcher.data.TileSizeCycle
import com.metro.system.MetroAppInfo
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroIntents
import com.metro.system.MetroPreferenceKeys
import com.metro.system.MetroPreferences
import com.metro.system.MetroThemeMode
import com.metro.system.MetroTileContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LauncherState(context: Context) {
    private val appContext = context.applicationContext
    private val hostContext: Context = context
    private val repository = LauncherRepository(appContext)
    private val metroPrefs = MetroPreferences(appContext)
    private val launcherPrefs =
        appContext.getSharedPreferences(PREFS_LAUNCHER, Context.MODE_PRIVATE)

    var darkTheme by mutableStateOf(metroPrefs.isDark)
    var accent by mutableStateOf(metroPrefs.accentColor)
    var currentPage by mutableIntStateOf(0)
    var searchActive by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var editingTile by mutableStateOf<DisplayTile?>(null)
    var showNotificationAccessPrompt by mutableStateOf(false)

    private var pinnedEntries by mutableStateOf(repository.loadPinnedTiles())
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
                displayTiles = repository.resolveDisplayTiles(pinnedEntries, liveContent = true)
                clearAppListIconCache()
            }
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
        pinnedEntries = repository.loadPinnedTiles()
        displayTiles = repository.resolveDisplayTiles(pinnedEntries, liveContent = true)
        apps = repository.discoverApps(pinnedEntries)
        darkTheme = metroPrefs.isDark
        accent = metroPrefs.accentColor
        refreshNotificationAccessPrompt()
    }

    /**
     * Loads pinned layout on the caller thread, then resolves live tile ContentProviders on
     * [Dispatchers.IO] so Start can paint before SMS/contacts/media queries finish.
     */
    suspend fun refreshAllAsync() {
        val epochAtStart = layoutEpoch
        val pinned = withContext(Dispatchers.IO) { repository.loadPinnedTiles() }
        if (epochAtStart != layoutEpoch) return
        pinnedEntries = pinned
        apps = withContext(Dispatchers.IO) { repository.discoverApps(pinned) }
        if (epochAtStart != layoutEpoch) return
        darkTheme = metroPrefs.isDark
        accent = metroPrefs.accentColor
        refreshNotificationAccessPrompt()
        val liveTiles = withContext(Dispatchers.IO) {
            repository.resolveDisplayTiles(pinned, liveContent = true)
        }
        if (epochAtStart != layoutEpoch) return
        displayTiles = liveTiles
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
        repository.launchApp(tile.entry.packageName, tile.deepLinkUri)
    }

    fun launchApp(app: MetroAppInfo) {
        val pinnedTile = displayTiles.firstOrNull { it.entry.packageName == app.packageName }
        if (pinnedTile != null) {
            onTileClick(pinnedTile)
        } else {
            repository.launchApp(app.packageName, null)
        }
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
        repository.launchAppOption(option)
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

    companion object {
        private const val PREFS_LAUNCHER = "metro_launcher"
        private const val KEY_NOTIF_PROMPT_DISMISSED = "notification_access_prompt_dismissed"
    }
}
