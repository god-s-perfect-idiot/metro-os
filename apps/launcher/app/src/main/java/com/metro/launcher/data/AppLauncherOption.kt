package com.metro.launcher.data

/**
 * Launchable shortcut surfaced in the app-list long-press menu (Android app shortcuts).
 */
data class AppLauncherOption(
    val packageName: String,
    val shortcutId: String,
    val label: String,
)
