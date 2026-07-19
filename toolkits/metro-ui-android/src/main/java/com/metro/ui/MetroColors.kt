package com.metro.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * WP8.1 color tokens from scope.md §2.
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
    val AccentRed = Color(0xFFE51400)
    val AccentGreen = Color(0xFF339933)
    val AccentOrange = Color(0xFFF09609)
    val AccentPurple = Color(0xFFA200FF)
    val AccentTeal = Color(0xFF00ABA9)
    val AccentLime = Color(0xFF8CBF26)
    val AccentBrown = Color(0xFF996600)
    val AccentPink = Color(0xFFFF0097)

    val AccentPalette = listOf(
        AccentBlue,
        AccentRed,
        AccentGreen,
        AccentOrange,
        AccentPurple,
        AccentTeal,
        AccentLime,
        AccentBrown,
        AccentPink,
    )

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
