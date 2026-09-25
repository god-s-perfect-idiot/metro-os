package com.metro.launcher.ui

import androidx.compose.runtime.compositionLocalOf

/**
 * Active [com.metro.system.MetroPreferences.iconPackPackage] for Start / app-list icon
 * invalidation. Null = system icons. Provided by [LauncherShell] from [LauncherState].
 */
val LocalIconPackPackage = compositionLocalOf<String?> { null }
