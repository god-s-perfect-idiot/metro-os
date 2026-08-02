package com.metro.settings.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import com.metro.system.MetroAppDiscovery
import java.io.File

/**
 * Launchable app row for Settings → applications (system + user).
 */
data class InstalledAppEntry(
    val packageName: String,
    val title: String,
    val isSystemApp: Boolean,
    val versionName: String,
    val sizeBytes: Long,
    val canUninstall: Boolean,
) {
    val sizeLabel: String
        get() = SettingsLogic.formatBytes(sizeBytes)

    val typeLabel: String
        get() = if (isSystemApp) "system" else "app"

    val listSubtitle: String
        get() = if (isSystemApp) "system" else sizeLabel
}

/**
 * Per-app policy toggles owned by Settings (Battery Saver–style controls).
 * Suite apps may honor these later via shared prefs; values persist here.
 */
class AppPolicyStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun backgroundAllowed(packageName: String): Boolean =
        prefs.getBoolean(bgKey(packageName), true)

    fun setBackgroundAllowed(packageName: String, allowed: Boolean) {
        prefs.edit().putBoolean(bgKey(packageName), allowed).apply()
    }

    fun notificationsAllowed(packageName: String): Boolean =
        prefs.getBoolean(notifyKey(packageName), true)

    fun setNotificationsAllowed(packageName: String, allowed: Boolean) {
        prefs.edit().putBoolean(notifyKey(packageName), allowed).apply()
    }

    companion object {
        private const val PREFS = "metro_app_policy"
        private fun bgKey(packageName: String) = "bg:$packageName"
        private fun notifyKey(packageName: String) = "notify:$packageName"
    }
}

/**
 * Discovers launchable apps and resolves package metadata for the applications pivot.
 */
class ApplicationsBridge(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    val policy = AppPolicyStore(appContext)

    fun listInstalledApps(selfPackageName: String = appContext.packageName): List<InstalledAppEntry> {
        val catalogTitles = SettingsLogic.APPLICATION_SETTINGS_CATALOG
            .associate { it.packageName to it.title }
        return MetroAppDiscovery.discoverInstalledApps(appContext)
            .mapNotNull { info ->
                val title = catalogTitles[info.packageName] ?: info.label
                toEntry(info.packageName, title, info.isSystemApp, selfPackageName)
            }
            .sortedBy { it.title.lowercase() }
    }

    fun loadApp(packageName: String, selfPackageName: String = appContext.packageName): InstalledAppEntry? {
        if (packageName.isBlank()) return null
        val isSystem = MetroAppDiscovery.isSystemApp(packageManager, packageName)
        val catalogTitle = SettingsLogic.APPLICATION_SETTINGS_CATALOG
            .firstOrNull { it.packageName == packageName }
            ?.title
        val label = catalogTitle ?: try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0),
            ).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        return toEntry(packageName, label, isSystem, selfPackageName)
    }

    fun requestUninstall(packageName: String) {
        if (packageName == appContext.packageName) return
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
    }

    fun launchApp(packageName: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(launch)
    }

    private fun toEntry(
        packageName: String,
        label: String,
        isSystemApp: Boolean,
        selfPackageName: String,
    ): InstalledAppEntry? {
        val packageInfo = packageInfoOrNull(packageName) ?: return null
        val size = packageSizeBytes(packageInfo)
        return InstalledAppEntry(
            packageName = packageName,
            title = label,
            isSystemApp = isSystemApp,
            versionName = SettingsLogic.displayOrDash(packageInfo.versionName),
            sizeBytes = size,
            canUninstall = SettingsLogic.canUninstallApp(
                packageName = packageName,
                isSystemApp = isSystemApp,
                selfPackageName = selfPackageName,
            ),
        )
    }

    private fun packageInfoOrNull(packageName: String): PackageInfo? =
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    private fun packageSizeBytes(packageInfo: PackageInfo): Long {
        val paths = buildList {
            packageInfo.applicationInfo?.sourceDir?.let { add(it) }
            packageInfo.applicationInfo?.splitSourceDirs?.forEach { add(it) }
        }
        return paths.sumOf { path -> File(path).takeIf { it.exists() }?.length() ?: 0L }
    }
}
