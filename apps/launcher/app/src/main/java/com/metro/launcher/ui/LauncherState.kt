package com.metro.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.launcher.BuildConfig
import com.metro.launcher.data.DisplayTile
import com.metro.launcher.data.LauncherRepository
import com.metro.launcher.data.PinnedTileEntry
import com.metro.launcher.data.PinnedTileSize
import com.metro.launcher.data.TileNotificationAccess
import com.metro.launcher.data.TileSizeCycle
import com.metro.system.MetroAppInfo
import com.metro.system.MetroBroadcasts
import com.metro.system.MetroPreferenceKeys
import com.metro.system.MetroPreferences
import com.metro.system.MetroThemeMode
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
            MetroPreferenceKeys.ACCENT_COLOR -> accent = metroPrefs.accentColor
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
            intent?.getStringExtra(MetroBroadcasts.EXTRA_THEME_MODE)?.let { mode ->
                darkTheme = MetroThemeMode.fromStorage(mode) == MetroThemeMode.Dark
            }
            intent?.getStringExtra(MetroBroadcasts.EXTRA_ACCENT_COLOR)?.let { hex ->
                accent = MetroPreferences.parseAccentHex(hex)
            }
        }
    }

    init {
        metroPrefs.let { prefs ->
            appContext.getSharedPreferences(MetroPreferenceKeys.PREFS_NAME, Context.MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(preferenceListener)
        }
    }

    fun registerReceivers(context: Context) {
        val tileFilter = IntentFilter(MetroBroadcasts.ACTION_TILE_UPDATE)
        val themeFilter = IntentFilter(MetroBroadcasts.ACTION_THEME_CHANGED)
        context.registerReceiver(tileUpdateReceiver, tileFilter, Context.RECEIVER_EXPORTED)
        context.registerReceiver(themeReceiver, themeFilter, Context.RECEIVER_NOT_EXPORTED)
    }

    fun unregisterReceivers(context: Context) {
        context.unregisterReceiver(tileUpdateReceiver)
        context.unregisterReceiver(themeReceiver)
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
        val pinned = withContext(Dispatchers.IO) { repository.loadPinnedTiles() }
        pinnedEntries = pinned
        apps = withContext(Dispatchers.IO) { repository.discoverApps(pinned) }
        darkTheme = metroPrefs.isDark
        accent = metroPrefs.accentColor
        refreshNotificationAccessPrompt()
        val liveTiles = withContext(Dispatchers.IO) {
            repository.resolveDisplayTiles(pinned, liveContent = true)
        }
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
        editingTile = displayTiles.firstOrNull { it.entry.packageName == current.entry.packageName }
    }

    fun unpinEditingTile() {
        val current = editingTile ?: return
        unpinTile(current.entry)
        editingTile = null
    }

    fun updateTileSize(entry: PinnedTileEntry, size: PinnedTileSize) {
        pinnedEntries = pinnedEntries.map {
            if (it.packageName == entry.packageName && it.tileId == entry.tileId) it.copy(size = size) else it
        }
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
        pinnedEntries = pinnedEntries + PinnedTileEntry(app.packageName)
        persistAndRefresh()
        currentPage = 0
    }

    fun uninstallApp(app: MetroAppInfo) {
        if (app.isSystemApp) return
        pinnedEntries
            .filter { it.packageName == app.packageName }
            .forEach { unpinTile(it) }
        repository.requestUninstall(hostContext, app.packageName)
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
        repository.savePinnedTiles(pinnedEntries)
        displayTiles = repository.resolveDisplayTiles(pinnedEntries, liveContent = true)
        apps = repository.discoverApps(pinnedEntries)
    }

    companion object {
        private const val PREFS_LAUNCHER = "metro_launcher"
        private const val KEY_NOTIF_PROMPT_DISMISSED = "notification_access_prompt_dismissed"
    }
}
