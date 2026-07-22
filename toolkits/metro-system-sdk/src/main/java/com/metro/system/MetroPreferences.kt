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

    private val providerAvailable: Boolean
        get() = appContext.packageManager.resolveContentProvider(
            MetroContentProviderContract.AUTHORITY,
            0,
        ) != null

    var themeMode: MetroThemeMode
        get() = MetroThemeMode.fromStorage(readString(MetroPreferenceKeys.THEME_MODE, MetroThemeMode.Dark.storageValue))
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

    fun broadcastThemeChanged() {
        val intent = Intent(MetroBroadcasts.ACTION_THEME_CHANGED).apply {
            putExtra(MetroBroadcasts.EXTRA_THEME_MODE, themeMode.storageValue)
            putExtra(MetroBroadcasts.EXTRA_ACCENT_COLOR, this@MetroPreferences.accentColorHex)
            putExtra(MetroBroadcasts.EXTRA_FONT_SCALE, fontScale)
            navBarColorHex?.let { putExtra(MetroBroadcasts.EXTRA_NAV_BAR_COLOR, it) }
            // Reach every metro package; receivers declare the action in their manifests / runtime.
            setPackage(null)
        }
        appContext.sendBroadcast(intent)
    }

    fun registerObserver(onChange: () -> Unit): ContentObserver? {
        if (!providerAvailable) return null
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = onChange()
            override fun onChange(selfChange: Boolean, uri: Uri?) = onChange()
        }
        appContext.contentResolver.registerContentObserver(
            MetroSystemPreferencesProvider.PREFERENCES_URI,
            true,
            observer,
        )
        return observer
    }

    fun unregisterObserver(observer: ContentObserver?) {
        if (observer != null) {
            appContext.contentResolver.unregisterContentObserver(observer)
        }
    }

    private fun readString(key: String, default: String?): String? {
        if (providerAvailable) {
            queryProvider(key)?.let { return it }
        }
        return localPrefs.getString(key, default)
    }

    private fun readFloat(key: String, default: Float): Float {
        if (providerAvailable) {
            queryProvider(key)?.toFloatOrNull()?.let { return it }
            // Provider may return float via cursor type
            queryProviderRaw(key)?.let { raw ->
                when (raw) {
                    is Float -> return raw
                    is Double -> return raw.toFloat()
                    is Number -> return raw.toFloat()
                    is String -> raw.toFloatOrNull()?.let { return it }
                }
            }
        }
        return localPrefs.getFloat(key, default)
    }

    private fun readBoolean(key: String, default: Boolean): Boolean {
        if (providerAvailable) {
            queryProviderRaw(key)?.let { raw ->
                return when (raw) {
                    is Boolean -> raw
                    is Number -> raw.toInt() != 0
                    is String -> raw == "1" || raw.equals("true", ignoreCase = true)
                    else -> default
                }
            }
        }
        return localPrefs.getBoolean(key, default)
    }

    private fun writeString(key: String, value: String) {
        localPrefs.edit().putString(key, value).apply()
        if (providerAvailable && !isProviderHost()) {
            updateProvider(key, value)
        } else if (providerAvailable && isProviderHost()) {
            appContext.contentResolver.notifyChange(
                MetroSystemPreferencesProvider.PREFERENCES_URI,
                null,
            )
        }
    }

    private fun writeFloat(key: String, value: Float) {
        localPrefs.edit().putFloat(key, value).apply()
        if (providerAvailable && !isProviderHost()) {
            updateProvider(key, value)
        } else if (providerAvailable && isProviderHost()) {
            appContext.contentResolver.notifyChange(
                MetroSystemPreferencesProvider.PREFERENCES_URI,
                null,
            )
        }
    }

    private fun writeBoolean(key: String, value: Boolean) {
        localPrefs.edit().putBoolean(key, value).apply()
        if (providerAvailable && !isProviderHost()) {
            updateProvider(key, if (value) 1 else 0)
        } else if (providerAvailable && isProviderHost()) {
            appContext.contentResolver.notifyChange(
                MetroSystemPreferencesProvider.PREFERENCES_URI,
                null,
            )
        }
    }

    private fun remove(key: String) {
        localPrefs.edit().remove(key).apply()
        if (providerAvailable && !isProviderHost()) {
            val values = ContentValues().apply { putNull(key) }
            runCatching {
                appContext.contentResolver.update(
                    MetroSystemPreferencesProvider.PREFERENCES_URI,
                    values,
                    null,
                    null,
                )
            }
        } else if (providerAvailable && isProviderHost()) {
            appContext.contentResolver.notifyChange(
                MetroSystemPreferencesProvider.PREFERENCES_URI,
                null,
            )
        }
    }

    private fun isProviderHost(): Boolean {
        val provider = appContext.packageManager.resolveContentProvider(
            MetroContentProviderContract.AUTHORITY,
            0,
        ) ?: return false
        return provider.packageName == appContext.packageName
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
        runCatching {
            appContext.contentResolver.update(
                MetroSystemPreferencesProvider.PREFERENCES_URI,
                values,
                null,
                null,
            )
        }
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
