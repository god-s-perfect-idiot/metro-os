package com.metro.system

/**
 * Installed Metro app surfaced in the launcher app list.
 */
data class MetroAppInfo(
    val packageName: String,
    val label: String,
    val isPinned: Boolean = false,
    val isSystemApp: Boolean = false,
)
