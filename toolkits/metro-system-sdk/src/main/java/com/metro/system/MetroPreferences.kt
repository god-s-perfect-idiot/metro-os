package com.metro.system

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

class MetroPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val localPrefs: SharedPreferences =
        appContext.getSharedPreferences(
            MetroPreferenceKeys.PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    var themeMode: MetroThemeMode
        get() = MetroThemeMode.fromStorage(
            readString(MetroPreferenceKeys.THEME_MODE, MetroThemeMode.Dark.storageValue),
        )
        set(value) = writeString(MetroPreferenceKeys.THEME_MODE, value.storageValue)

    val isDark: Boolean
        get() = themeMode == MetroThemeMode.Dark

    var accentColorHex: String
        get() = readString(MetroPreferenceKeys.ACCENT_COLOR, DEFAULT_ACCENT_HEX) ?: DEFAULT_ACCENT_HEX
        set(value) {
            val normalized = MetroAccentPalette.normalizeHex(value) ?: DEFAULT_ACCENT_HEX
            writeString(MetroPreferenceKeys.ACCENT_COLOR, normalized)
        }

    val accentColor: Color
        get() = parseAccentHex(accentColorHex)

    /**
     * Local mirror of the suite accent only — no ContentProvider round-trip.
     * Used to paint cold starts before Settings wakes; null when this process has never
     * successfully cached a suite accent.
     */
    fun peekCachedAccentColorHex(): String? =
        localPrefs.getString(MetroPreferenceKeys.ACCENT_COLOR, null)

    /** Local mirror of dark/light; null when unset in this process's cache. */
    fun peekCachedIsDark(): Boolean? =
        localPrefs.getString(MetroPreferenceKeys.THEME_MODE, null)?.let {
            MetroThemeMode.fromStorage(it) == MetroThemeMode.Dark
        }

    /**
     * Forces a ContentProvider read for theme keys and mirrors any values into the local
     * cache with [SharedPreferences.Editor.commit]. Returns true when the Settings provider
     * answered (even if the accent is still the suite default).
     */
    fun pullThemeFromProvider(): Boolean {
        val accentRemote = queryProvider(MetroPreferenceKeys.ACCENT_COLOR)
        val themeRemote = queryProvider(MetroPreferenceKeys.THEME_MODE)
        val fontRemote = queryProvider(MetroPreferenceKeys.FONT_SCALE)
        var reached = false
        accentRemote?.let {
            reached = true
            cacheStringLocally(MetroPreferenceKeys.ACCENT_COLOR, it, durable = true)
        }
        themeRemote?.let {
            reached = true
            cacheStringLocally(MetroPreferenceKeys.THEME_MODE, it, durable = true)
        }
        fontRemote?.toFloatOrNull()?.let {
            reached = true
            cacheFloatLocally(MetroPreferenceKeys.FONT_SCALE, MetroFontScale.coerceToStep(it), durable = true)
        }
        return reached
    }

    var fontScale: Float
        get() = MetroFontScale.coerceToStep(
            readFloat(MetroPreferenceKeys.FONT_SCALE, MetroFontScale.DEFAULT),
        )
        set(value) = writeFloat(MetroPreferenceKeys.FONT_SCALE, MetroFontScale.coerceToStep(value))

    var navBarColorHex: String?
        get() = readString(MetroPreferenceKeys.NAV_BAR_COLOR, null)
        set(value) {
            if (value == null) {
                remove(MetroPreferenceKeys.NAV_BAR_COLOR)
            } else {
                writeString(MetroPreferenceKeys.NAV_BAR_COLOR, value)
            }
        }

    /**
     * Whether the Metro navigation bar overlay is currently active. The navbar app owns this flag;
     * other apps read it (and observe [MetroBroadcasts.ACTION_NAVBAR_CHANGED]) to reserve bottom
     * space for the overlay. Defaults to `false` until the navbar reports its state.
     */
    var navBarEnabled: Boolean
        get() = readBoolean(MetroPreferenceKeys.NAV_BAR_ENABLED, false)
        set(value) = writeBoolean(MetroPreferenceKeys.NAV_BAR_ENABLED, value)

    /**
     * Start screen density: off = 4-column grid (2 medium tiles across), on = 6-column
     * (3 medium tiles across). Settings owns the write; launcher observes and reflows.
     */
    var showMoreColumns: Boolean
        get() = readBoolean(MetroPreferenceKeys.SHOW_MORE_COLUMNS, false)
        set(value) = writeBoolean(MetroPreferenceKeys.SHOW_MORE_COLUMNS, value)

    /**
     * Whether a cropped Start background photo is active (WP8.1 Start background).
     * Image bytes: [MetroStartBackground.CONTENT_URI]. Settings owns writes.
     */
    var startBackgroundEnabled: Boolean
        get() = readBoolean(MetroPreferenceKeys.START_BACKGROUND_ENABLED, false)
        set(value) = writeBoolean(MetroPreferenceKeys.START_BACKGROUND_ENABLED, value)

    /** Writes theme/accent/font and broadcasts [MetroBroadcasts.ACTION_THEME_CHANGED]. */
    fun applyThemeChange(
        themeMode: MetroThemeMode? = null,
        accentColorHex: String? = null,
        fontScale: Float? = null,
        broadcast: Boolean = true,
    ) {
        themeMode?.let { this.themeMode = it }
        accentColorHex?.let { this.accentColorHex = it }
        fontScale?.let { this.fontScale = it }
        if (broadcast) {
            broadcastThemeChanged()
        }
    }

    /**
     * Mirrors a theme snapshot into this app's local cache only (no provider write, no broadcast).
     * Used when a [MetroBroadcasts.ACTION_THEME_CHANGED] arrives so cold starts still see the
     * last-known suite theme even if the Settings ContentProvider is briefly unreachable.
     *
     * Uses [SharedPreferences.Editor.commit] so the mirror survives immediate process death
     * after a theme change (apply() can lose the write under memory pressure).
     */
    fun cacheThemeSnapshot(
        themeMode: MetroThemeMode? = null,
        accentColorHex: String? = null,
        fontScale: Float? = null,
    ) {
        val editor = localPrefs.edit()
        themeMode?.let { editor.putString(MetroPreferenceKeys.THEME_MODE, it.storageValue) }
        accentColorHex?.let { hex ->
            val normalized = MetroAccentPalette.normalizeHex(hex) ?: DEFAULT_ACCENT_HEX
            editor.putString(MetroPreferenceKeys.ACCENT_COLOR, normalized)
        }
        fontScale?.let {
            editor.putFloat(MetroPreferenceKeys.FONT_SCALE, MetroFontScale.coerceToStep(it))
        }
        editor.commit()
    }

    fun broadcastThemeChanged() {
        val intent = Intent(MetroBroadcasts.ACTION_THEME_CHANGED).apply {
            putExtra(MetroBroadcasts.EXTRA_THEME_MODE, themeMode.storageValue)
            putExtra(MetroBroadcasts.EXTRA_ACCENT_COLOR, this@MetroPreferences.accentColorHex)
            putExtra(MetroBroadcasts.EXTRA_FONT_SCALE, fontScale)
            putExtra(
                MetroBroadcasts.EXTRA_START_BACKGROUND_ENABLED,
                this@MetroPreferences.startBackgroundEnabled,
            )
            navBarColorHex?.let { putExtra(MetroBroadcasts.EXTRA_NAV_BAR_COLOR, it) }
            // Reach every metro package; receivers declare the action in their manifests / runtime.
            setPackage(null)
        }
        appContext.sendBroadcast(intent)
    }

    fun registerObserver(onChange: () -> Unit): ContentObserver? {
        return runCatching {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = onChange()
                override fun onChange(selfChange: Boolean, uri: Uri?) = onChange()
            }
            appContext.contentResolver.registerContentObserver(
                MetroSystemPreferencesProvider.PREFERENCES_URI,
                true,
                observer,
            )
            observer
        }.getOrNull()
    }

    fun unregisterObserver(observer: ContentObserver?) {
        if (observer != null) {
            appContext.contentResolver.unregisterContentObserver(observer)
        }
    }

    private fun readString(key: String, default: String?): String? {
        // Prefer a durable local mirror first for theme keys so cold opens do not flash the
        // suite default while the Settings provider process is still starting. Still refresh
        // from the provider when it answers so Settings remains source of truth.
        val local = localPrefs.getString(key, null)
        if (isThemeKey(key) && local != null) {
            queryProvider(key)?.let { value ->
                cacheStringLocally(key, value, durable = true)
                return value
            }
            return local
        }
        queryProvider(key)?.let { value ->
            cacheStringLocally(key, value, durable = isThemeKey(key))
            return value
        }
        return local ?: default
    }

    private fun readFloat(key: String, default: Float): Float {
        if (key == MetroPreferenceKeys.FONT_SCALE && localPrefs.contains(key)) {
            val local = localPrefs.getFloat(key, default)
            queryProvider(key)?.toFloatOrNull()?.let { value ->
                cacheFloatLocally(key, value, durable = true)
                return value
            }
            queryProviderRaw(key)?.let { raw ->
                rawAsFloat(raw)?.let { value ->
                    cacheFloatLocally(key, value, durable = true)
                    return value
                }
            }
            return local
        }
        queryProvider(key)?.toFloatOrNull()?.let { value ->
            cacheFloatLocally(key, value, durable = key == MetroPreferenceKeys.FONT_SCALE)
            return value
        }
        queryProviderRaw(key)?.let { raw ->
            rawAsFloat(raw)?.let { value ->
                cacheFloatLocally(key, value, durable = key == MetroPreferenceKeys.FONT_SCALE)
                return value
            }
        }
        return localPrefs.getFloat(key, default)
    }

    private fun readBoolean(key: String, default: Boolean): Boolean {
        queryProviderRaw(key)?.let { raw ->
            val value = when (raw) {
                is Boolean -> raw
                is Number -> raw.toInt() != 0
                is String -> raw == "1" || raw.equals("true", ignoreCase = true)
                else -> null
            }
            if (value != null) {
                cacheBooleanLocally(key, value)
                return value
            }
        }
        return localPrefs.getBoolean(key, default)
    }

    private fun writeString(key: String, value: String) {
        val editor = localPrefs.edit().putString(key, value)
        // Theme keys must hit disk before Settings can be killed; apply() races with process death.
        if (isThemeKey(key)) editor.commit() else editor.apply()
        propagateWrite { updateProvider(key, value) }
    }

    private fun writeFloat(key: String, value: Float) {
        val editor = localPrefs.edit().putFloat(key, value)
        if (key == MetroPreferenceKeys.FONT_SCALE) editor.commit() else editor.apply()
        propagateWrite { updateProvider(key, value) }
    }

    private fun writeBoolean(key: String, value: Boolean) {
        localPrefs.edit().putBoolean(key, value).apply()
        propagateWrite { updateProvider(key, if (value) 1 else 0) }
    }

    private fun remove(key: String) {
        localPrefs.edit().remove(key).apply()
        propagateWrite {
            val values = ContentValues().apply { putNull(key) }
            appContext.contentResolver.update(
                MetroSystemPreferencesProvider.PREFERENCES_URI,
                values,
                null,
                null,
            )
        }
    }

    private fun isThemeKey(key: String): Boolean =
        key == MetroPreferenceKeys.THEME_MODE ||
            key == MetroPreferenceKeys.ACCENT_COLOR ||
            key == MetroPreferenceKeys.FONT_SCALE

    private fun rawAsFloat(raw: Any?): Float? = when (raw) {
        is Float -> raw
        is Double -> raw.toFloat()
        is Number -> raw.toFloat()
        is String -> raw.toFloatOrNull()
        else -> null
    }

    /**
     * Host (Settings) owns the SharedPreferences backing the provider — notify observers.
     * Clients always attempt a ContentResolver update; do not gate on
     * [android.content.pm.PackageManager.resolveContentProvider], which fails under Android 11+
     * package visibility even when URI access still works.
     */
    private fun propagateWrite(clientUpdate: () -> Unit) {
        if (isProviderHost()) {
            runCatching {
                appContext.contentResolver.notifyChange(
                    MetroSystemPreferencesProvider.PREFERENCES_URI,
                    null,
                )
            }
        } else {
            runCatching(clientUpdate)
        }
    }

    private fun isProviderHost(): Boolean {
        if (appContext.packageName == MetroContentProviderContract.HOST_PACKAGE) {
            return true
        }
        val provider = appContext.packageManager.resolveContentProvider(
            MetroContentProviderContract.AUTHORITY,
            0,
        ) ?: return false
        return provider.packageName == appContext.packageName
    }

    private fun cacheStringLocally(key: String, value: String, durable: Boolean = false) {
        if (localPrefs.getString(key, null) != value) {
            val editor = localPrefs.edit().putString(key, value)
            if (durable) editor.commit() else editor.apply()
        }
    }

    private fun cacheFloatLocally(key: String, value: Float, durable: Boolean = false) {
        if (!localPrefs.contains(key) || localPrefs.getFloat(key, 0f) != value) {
            val editor = localPrefs.edit().putFloat(key, value)
            if (durable) editor.commit() else editor.apply()
        }
    }

    private fun cacheBooleanLocally(key: String, value: Boolean) {
        if (!localPrefs.contains(key) || localPrefs.getBoolean(key, !value) != value) {
            localPrefs.edit().putBoolean(key, value).apply()
        }
    }

    private fun queryProvider(key: String): String? {
        return runCatching {
            appContext.contentResolver.query(
                MetroSystemPreferencesProvider.keyUri(key),
                arrayOf(MetroSystemPreferencesProvider.COLUMN_VALUE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                cursor.getString(0)
            }
        }.getOrNull()
    }

    private fun queryProviderRaw(key: String): Any? {
        return runCatching {
            appContext.contentResolver.query(
                MetroSystemPreferencesProvider.keyUri(key),
                arrayOf(MetroSystemPreferencesProvider.COLUMN_VALUE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                when (cursor.getType(0)) {
                    android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getFloat(0)
                    android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getInt(0)
                    android.database.Cursor.FIELD_TYPE_STRING -> cursor.getString(0)
                    else -> cursor.getString(0)
                }
            }
        }.getOrNull()
    }

    private fun updateProvider(key: String, value: Any) {
        val values = ContentValues().apply {
            when (value) {
                is String -> put(key, value)
                is Float -> put(key, value)
                is Int -> put(key, value)
                is Boolean -> put(key, value)
                else -> put(key, value.toString())
            }
        }
        appContext.contentResolver.update(
            MetroSystemPreferencesProvider.PREFERENCES_URI,
            values,
            null,
            null,
        )
    }

    companion object {
        const val DEFAULT_ACCENT_HEX = "#1BA1E2"

        fun parseAccentHex(hex: String): Color {
            val normalized = hex.removePrefix("#")
            val argb = when (normalized.length) {
                6 -> "FF$normalized"
                8 -> normalized
                else -> DEFAULT_ACCENT_HEX.removePrefix("#").let { "FF$it" }
            }
            return Color(argb.toLong(16))
        }

        fun Color.toHexString(): String {
            val rgb = toArgb() and 0xFFFFFF
            return "#%06X".format(rgb)
        }
    }
}
