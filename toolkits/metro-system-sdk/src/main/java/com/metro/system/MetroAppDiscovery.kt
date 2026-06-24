package com.metro.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object MetroAppDiscovery {
    private const val LAUNCHER_PACKAGE = "com.metro.launcher"

    fun discoverInstalledApps(
        context: Context,
        pinnedPackages: Set<String> = emptySet(),
    ): List<MetroAppInfo> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val activities = packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)

        val launchablePackages = activities
            .map { it.activityInfo.packageName }
            .filter { it != LAUNCHER_PACKAGE }
            .toSet()

        val packages = (launchablePackages + pinnedPackages)
            .distinct()

        return packages
            .map { packageName ->
                MetroAppInfo(
                    packageName = packageName,
                    label = resolveAppLabel(packageManager, packageName),
                    isPinned = packageName in pinnedPackages,
                    isSystemApp = isSystemApp(packageManager, packageName),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun isSystemApp(packageManager: PackageManager, packageName: String): Boolean =
        try {
            val flags = packageManager.getApplicationInfo(packageName, 0).flags
            (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } catch (_: PackageManager.NameNotFoundException) {
            MetroAppRegistry.isKnown(packageName)
        }

    private fun resolveAppLabel(packageManager: PackageManager, packageName: String): String =
        try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0),
            ).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            MetroAppRegistry.label(packageName) ?: packageName.substringAfterLast('.')
        }

    fun filterApps(apps: List<MetroAppInfo>, query: String): List<MetroAppInfo> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return apps
        val needle = trimmed.lowercase()
        return apps.filter { it.label.lowercase().contains(needle) }
    }
}
