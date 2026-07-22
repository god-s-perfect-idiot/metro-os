package com.metro.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.metro.system.MetroAccentPalette

/**
 * WP8.1 color tokens from scope.md §2, with the full WP8 20-color accent set.
 */
object MetroColors {
    val TileContentOnAccent = Color(0xFFFFFFFF)
    val DarkBackground = Color(0xFF000000)
    val DarkSecondarySurface = Color(0xFF1F1F1F)
    /** Inactive find-by-letter tile fill (WP8.1 jump list gray). */
    val JumpListInactive = Color(0xFF2B2B2B)
    val LightBackground = Color(0xFFFFFFFF)
    val LightSecondarySurface = Color(0xFFF2F2F2)

    val DarkPrimaryText = Color(0xFFFFFFFF)
    val DarkSecondaryText = Color(0xFF999999)
    val LightPrimaryText = Color(0xFF000000)
    val LightSecondaryText = Color(0xFF666666)

    val AccentBlue = Color(0xFF1BA1E2)
    val AccentCyan = AccentBlue
    val AccentLime = Color(0xFFA4C400)
    val AccentGreen = Color(0xFF60A917)
    val AccentEmerald = Color(0xFF008A00)
    val AccentTeal = Color(0xFF00ABA9)
    val AccentCobalt = Color(0xFF0050EF)
    val AccentIndigo = Color(0xFF6A00FF)
    val AccentViolet = Color(0xFFAA00FF)
    val AccentPink = Color(0xFFF472D0)
    val AccentMagenta = Color(0xFFD80073)
    val AccentCrimson = Color(0xFFA20025)
    val AccentRed = Color(0xFFE51400)
    val AccentOrange = Color(0xFFFA6800)
    val AccentAmber = Color(0xFFF0A30A)
    val AccentYellow = Color(0xFFE3C800)
    val AccentBrown = Color(0xFF825A2C)
    val AccentOlive = Color(0xFF6D8764)
    val AccentSteel = Color(0xFF647687)
    val AccentMauve = Color(0xFF76608A)
    val AccentTaupe = Color(0xFF87794E)

    /** Legacy WP7-era aliases kept for existing callers. */
    val AccentPurple = AccentViolet

    /** Official WP8.1 20-color accent grid order. */
    val AccentPalette: List<Color> = MetroAccentPalette.all.map { Color(it.colorArgb) }

    fun background(dark: Boolean): Color = if (dark) DarkBackground else LightBackground

    fun secondarySurface(dark: Boolean): Color = if (dark) DarkSecondarySurface else LightSecondarySurface

    fun primaryText(dark: Boolean): Color = if (dark) DarkPrimaryText else LightPrimaryText

    fun secondaryText(dark: Boolean): Color = if (dark) DarkSecondaryText else LightSecondaryText

    /** App bar / chrome overlay at 80% opacity over theme background. */
    fun chromeBackground(dark: Boolean): Color = background(dark).copy(alpha = 0.8f)

    /** Title/counter on tile backgrounds — black on light adaptive icon colors, white otherwise. */
    fun tileContentColor(backgroundColor: Color): Color =
        if (backgroundColor.luminance() > TileLightBackgroundLuminance) {
            LightPrimaryText
        } else {
            TileContentOnAccent
        }

    private const val TileLightBackgroundLuminance = 0.5f
}
