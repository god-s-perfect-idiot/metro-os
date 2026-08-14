package com.metro.launcher.data

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.metro.system.MetroPreferences
import com.metro.ui.MetroAppGlyphs

/**
 * WP-style custom Start / app-list tiles for selected third-party packages.
 *
 * Glyphs live in the shared suite set ([MetroAppGlyphs]). Default look: white monochrome
 * glyph on the system accent. Set [MetroAppGlyphs.TileOverride.backgroundHex] only when a
 * fixed brand fill is required.
 *
 * Fully custom composed faces (e.g. Chrome wedges) live in `CustomTileFaces.kt`.
 */
object CustomTileBranding {
    data class Entry(
        @DrawableRes val glyphResId: Int,
        val backgroundHex: String? = null,
    )

    fun entry(packageName: String): Entry? =
        MetroAppGlyphs.tileOverrideEntry(packageName)?.let {
            Entry(glyphResId = it.glyphResId, backgroundHex = it.backgroundHex)
        }

    fun hasCustomTile(packageName: String): Boolean = MetroAppGlyphs.tileOverride(packageName) != null

    @DrawableRes
    fun glyphResId(packageName: String): Int? = MetroAppGlyphs.tileOverride(packageName)

    fun resolveBackgroundColor(context: Context, packageName: String): Color? {
        val entry = entry(packageName) ?: return null
        val prefs = MetroPreferences(context)
        return entry.backgroundHex?.let { MetroPreferences.parseAccentHex(it) } ?: prefs.accentColor
    }
}
