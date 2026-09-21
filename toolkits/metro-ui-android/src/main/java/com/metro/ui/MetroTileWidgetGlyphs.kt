package com.metro.ui

import androidx.annotation.DrawableRes
import com.metro.system.MetroTileWidgetGlyph

/**
 * Drawables for [com.metro.system.MetroTileWidgetFace] glyph keys.
 * Launcher Start widget faces and the Widgets catalog both resolve through here.
 */
object MetroTileWidgetGlyphs {
    @DrawableRes
    fun resId(glyphKey: String?): Int? = when (glyphKey) {
        MetroTileWidgetGlyph.TORCH -> R.drawable.metro_tile_glyph_torch
        MetroTileWidgetGlyph.LOCK -> R.drawable.metro_app_lockscreen
        MetroTileWidgetGlyph.BATTERY_SAVER -> R.drawable.metro_tile_glyph_battery_saver
        else -> null
    }
}
