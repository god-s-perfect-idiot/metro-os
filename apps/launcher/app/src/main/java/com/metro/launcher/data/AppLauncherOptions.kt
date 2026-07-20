package com.metro.launcher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process

object AppLauncherOptions {
    fun query(context: Context, packageName: String): List<AppLauncherOption> {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return emptyList()
        return try {
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                )
            }
            launcherApps.getShortcuts(query, Process.myUserHandle())
                ?.mapNotNull(::toOption)
                ?.let(::dedupeByShortcutId)
                ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: IllegalStateException) {
            emptyList()
        }
    }

    fun launch(context: Context, option: AppLauncherOption) {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return
        try {
            launcherApps.startShortcut(
                option.packageName,
                option.shortcutId,
                null,
                null,
                Process.myUserHandle(),
            )
        } catch (_: SecurityException) {
            // Launcher may not be default home yet, or shortcut was removed.
        }
    }

    internal fun toOption(shortcut: android.content.pm.ShortcutInfo): AppLauncherOption? {
        if (!shortcut.isEnabled) return null
        val label = shortcut.shortLabel?.toString()?.takeIf { it.isNotBlank() }
            ?: shortcut.longLabel?.toString()?.takeIf { it.isNotBlank() }
            ?: return null
        return AppLauncherOption(
            packageName = shortcut.`package`,
            shortcutId = shortcut.id,
            label = label,
        )
    }

    internal fun dedupeByShortcutId(options: List<AppLauncherOption>): List<AppLauncherOption> =
        options.distinctBy { it.shortcutId }
}
