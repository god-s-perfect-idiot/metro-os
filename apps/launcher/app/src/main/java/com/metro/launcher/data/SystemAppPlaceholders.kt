package com.metro.launcher.data

import androidx.annotation.DrawableRes
import com.metro.system.MetroAppRegistry
import com.metro.ui.MetroAppGlyphs

/**
 * Labels (and legacy glyph assets) for known Metro suite packages.
 *
 * Start tiles and the app list render each installed suite app's own launcher icon via
 * [com.metro.system.MetroAppBranding] — [MetroAppGlyphs] is the shared suite icon set used
 * for placeholders and any surface that needs a known package glyph without PackageManager.
 */
object SystemAppPlaceholders {
    @DrawableRes
    fun iconResId(packageName: String): Int? = MetroAppGlyphs.forPackage(packageName)

    fun label(packageName: String): String? = MetroAppRegistry.label(packageName)

    fun hasPlaceholder(packageName: String): Boolean = MetroAppGlyphs.forPackage(packageName) != null
}
