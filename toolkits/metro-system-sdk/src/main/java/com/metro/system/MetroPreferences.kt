package com.metro.system

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

class MetroPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            MetroPreferenceKeys.PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    var themeMode: MetroThemeMode
        get() = MetroThemeMode.fromStorage(prefs.getString(MetroPreferenceKeys.THEME_MODE, null))
        set(value) = prefs.edit().putString(MetroPreferenceKeys.THEME_MODE, value.storageValue).apply()

    val isDark: Boolean
        get() = themeMode == MetroThemeMode.Dark

    var accentColorHex: String
        get() = prefs.getString(MetroPreferenceKeys.ACCENT_COLOR, DEFAULT_ACCENT_HEX) ?: DEFAULT_ACCENT_HEX
        set(value) = prefs.edit().putString(MetroPreferenceKeys.ACCENT_COLOR, value).apply()

    val accentColor: Color
        get() = parseAccentHex(accentColorHex)

    var fontScale: Float
        get() = prefs.getFloat(MetroPreferenceKeys.FONT_SCALE, 1.0f)
        set(value) = prefs.edit().putFloat(MetroPreferenceKeys.FONT_SCALE, value).apply()

    var navBarColorHex: String?
        get() = prefs.getString(MetroPreferenceKeys.NAV_BAR_COLOR, null)
        set(value) = prefs.edit().putString(MetroPreferenceKeys.NAV_BAR_COLOR, value).apply()

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
