package com.metro.settings.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.metro.settings.data.SystemSettingsBridge
import com.metro.system.MetroAccentPalette
import com.metro.system.MetroFontScale
import com.metro.system.MetroPreferences

enum class SettingsRoute {
    Root,
    StartTheme,
    AccentPicker,
    EaseOfAccess,
    Brightness,
    StorageSense,
    About,
}

class SettingsState(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val prefs = MetroPreferences(appContext)
    val system = SystemSettingsBridge(appContext)

    companion object {
        /** Suite keyboard settings (`com.metro.keyboard`), not Android Settings. */
        const val KEYBOARD_PACKAGE = "com.metro.keyboard"
    }

    var route by mutableStateOf(SettingsRoute.Root)
        private set

    var accentHex by mutableStateOf(prefs.accentColorHex)
        private set

    var fontScale by mutableFloatStateOf(prefs.fontScale)
        private set

    var brightness by mutableFloatStateOf(system.brightnessFraction())
        private set

    val accentColor: Color
        get() = MetroPreferences.parseAccentHex(accentHex)

    val accentDisplayName: String
        get() = MetroAccentPalette.displayName(accentHex)

    val fontScaleIndex: Int
        get() = MetroFontScale.indexOf(fontScale)

    fun open(route: SettingsRoute) {
        refreshSystemReads()
        this.route = route
    }

    fun goBack() {
        route = when (route) {
            SettingsRoute.Root -> SettingsRoute.Root
            SettingsRoute.StartTheme -> SettingsRoute.Root
            SettingsRoute.AccentPicker -> SettingsRoute.StartTheme
            SettingsRoute.EaseOfAccess,
            SettingsRoute.Brightness,
            SettingsRoute.StorageSense,
            SettingsRoute.About,
            -> SettingsRoute.Root
        }
    }

    fun applyAccentHex(hex: String) {
        accentHex = MetroAccentPalette.normalizeHex(hex) ?: MetroPreferences.DEFAULT_ACCENT_HEX
        prefs.applyThemeChange(accentColorHex = accentHex)
    }

    fun applyFontScaleIndex(index: Int) {
        fontScale = MetroFontScale.fromIndex(index)
        prefs.applyThemeChange(fontScale = fontScale)
    }

    fun applyBrightness(fraction: Float) {
        brightness = fraction.coerceIn(0f, 1f)
        system.setBrightnessFraction(brightness)
        brightness = system.brightnessFraction()
    }

    /** Opens the metro-os keyboard settings app (WP8.1 Settings → keyboard). */
    fun openKeyboardSettings() {
        val launch = appContext.packageManager.getLaunchIntentForPackage(KEYBOARD_PACKAGE) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(launch)
    }

    fun refreshSystemReads() {
        brightness = system.brightnessFraction()
        accentHex = prefs.accentColorHex
        fontScale = prefs.fontScale
    }
}
