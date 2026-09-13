package com.metro.launcher.ui

/**
 * App-list long-press menu — WP8.1 greys out pin to start when the app already
 * has a live tile on Start.
 */
internal object AppListContextMenuLogic {
    fun pinToStartEnabled(isPinned: Boolean): Boolean = !isPinned
}
