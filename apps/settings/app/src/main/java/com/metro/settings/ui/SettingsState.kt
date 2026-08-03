package com.metro.settings.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.metro.settings.data.ApplicationsBridge
import com.metro.settings.data.InstalledAppEntry
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
    AppDetail,
}

class SettingsState(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val prefs = MetroPreferences(appContext)
    val system = SystemSettingsBridge(appContext)
    val applications = ApplicationsBridge(appContext)

    companion object {
        /** Suite keyboard settings (`com.metro.keyboard`), not Android Settings. */
        const val KEYBOARD_PACKAGE = "com.metro.keyboard"

        /** Suite Files app (`com.metro.files`), not Android DocumentsUI / Settings. */
        const val FILES_PACKAGE = "com.metro.files"

        /** Suite navigation bar setup (`com.metro.navbar`), not Android Settings. */
        const val NAVBAR_PACKAGE = "com.metro.navbar"

        /** Suite status bar / system tray setup (`com.metro.statusbar`), not Android Settings. */
        const val STATUSBAR_PACKAGE = "com.metro.statusbar"

        /** Suite volume HUD setup (`com.metro.volume`), not Android Settings. */
        const val VOLUME_PACKAGE = "com.metro.volume"

        const val PIVOT_SYSTEM = 0
        const val PIVOT_APPLICATIONS = 1
    }

    var route by mutableStateOf(SettingsRoute.Root)
        private set

    /** Root pivot: 0 = system, 1 = applications. */
    var rootPivot by mutableIntStateOf(PIVOT_SYSTEM)
        private set

    var accentHex by mutableStateOf(prefs.accentColorHex)
        private set

    var fontScale by mutableFloatStateOf(prefs.fontScale)
        private set

    var brightness by mutableFloatStateOf(system.brightnessFraction())
        private set

    var applicationEntries by mutableStateOf(applications.listInstalledApps())
        private set

    var selectedApp by mutableStateOf<InstalledAppEntry?>(null)
        private set

    var appBackgroundAllowed by mutableStateOf(true)
        private set

    var appNotificationsAllowed by mutableStateOf(true)
        private set

    var showUninstallConfirm by mutableStateOf(false)
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

    fun selectRootPivot(index: Int) {
        rootPivot = index.coerceIn(PIVOT_SYSTEM, PIVOT_APPLICATIONS)
    }

    fun goBack() {
        when (route) {
            SettingsRoute.Root -> Unit
            SettingsRoute.StartTheme -> route = SettingsRoute.Root
            SettingsRoute.AccentPicker -> route = SettingsRoute.StartTheme
            SettingsRoute.AppDetail -> {
                showUninstallConfirm = false
                selectedApp = null
                rootPivot = PIVOT_APPLICATIONS
                refreshSystemReads()
                route = SettingsRoute.Root
            }
            SettingsRoute.EaseOfAccess,
            SettingsRoute.Brightness,
            SettingsRoute.StorageSense,
            SettingsRoute.About,
            -> route = SettingsRoute.Root
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
        launchPackage(KEYBOARD_PACKAGE)
    }

    /** Opens the metro-os navigation bar setup app (soft keys / overlay grants). */
    fun openNavbarSettings() {
        launchPackage(NAVBAR_PACKAGE)
    }

    /** Opens the metro-os status bar setup app (system tray / overlay grants). */
    fun openStatusbarSettings() {
        launchPackage(STATUSBAR_PACKAGE)
    }

    /** Opens the metro-os volume HUD setup app (rocker HUD / overlay grants). */
    fun openVolumeSettings() {
        launchPackage(VOLUME_PACKAGE)
    }

    /** Opens the metro-os Files app from Storage Sense. */
    fun openFiles() {
        launchPackage(FILES_PACKAGE)
    }

    /** Opens in-Settings app detail (does not launch the target app). */
    fun openApplicationSettings(entry: InstalledAppEntry) {
        val detail = applications.loadApp(entry.packageName) ?: entry
        selectedApp = detail
        appBackgroundAllowed = applications.policy.backgroundAllowed(detail.packageName)
        appNotificationsAllowed = applications.policy.notificationsAllowed(detail.packageName)
        showUninstallConfirm = false
        route = SettingsRoute.AppDetail
    }

    fun applyAppBackgroundAllowed(allowed: Boolean) {
        val pkg = selectedApp?.packageName ?: return
        appBackgroundAllowed = allowed
        applications.policy.setBackgroundAllowed(pkg, allowed)
    }

    fun applyAppNotificationsAllowed(allowed: Boolean) {
        val pkg = selectedApp?.packageName ?: return
        appNotificationsAllowed = allowed
        applications.policy.setNotificationsAllowed(pkg, allowed)
    }

    fun openSelectedApp() {
        val pkg = selectedApp?.packageName ?: return
        applications.launchApp(pkg)
    }

    fun requestUninstallSelectedApp() {
        if (selectedApp?.canUninstall != true) return
        showUninstallConfirm = true
    }

    fun dismissUninstallConfirm() {
        showUninstallConfirm = false
    }

    fun confirmUninstallSelectedApp() {
        val pkg = selectedApp?.packageName ?: return
        showUninstallConfirm = false
        applications.requestUninstall(pkg)
    }

    fun refreshSystemReads() {
        brightness = system.brightnessFraction()
        accentHex = prefs.accentColorHex
        fontScale = prefs.fontScale
        applicationEntries = applications.listInstalledApps()
        selectedApp?.packageName?.let { pkg ->
            selectedApp = applications.loadApp(pkg)
            if (selectedApp == null && route == SettingsRoute.AppDetail) {
                showUninstallConfirm = false
                rootPivot = PIVOT_APPLICATIONS
                route = SettingsRoute.Root
            } else {
                selectedApp?.let { app ->
                    appBackgroundAllowed = applications.policy.backgroundAllowed(app.packageName)
                    appNotificationsAllowed = applications.policy.notificationsAllowed(app.packageName)
                }
            }
        }
    }

    private fun launchPackage(packageName: String) {
        val launch = appContext.packageManager.getLaunchIntentForPackage(packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(launch)
    }
}
