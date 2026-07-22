package com.metro.ui

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.metro.ui.R

/**
 * Noto Sans variable font instance for [weight].
 *
 * The bundled `noto_sans.ttf` is a single VF (wght 100–900). Without
 * [FontVariation.Settings], Compose falls back to the Regular instance for every
 * [FontWeight], so page titles looked wrong instead of the intended weight.
 */
@OptIn(ExperimentalTextApi::class)
private fun metroFont(weight: FontWeight) = Font(
    resId = R.font.noto_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
    ),
)

/**
 * Noto Sans (Segoe WP stand-in) typography roles from scope.md §1.
 */
val MetroFontFamily = FontFamily(
    metroFont(FontWeight.ExtraLight),
    metroFont(FontWeight.Light),
    metroFont(FontWeight.Normal),
    metroFont(FontWeight.Medium),
    metroFont(FontWeight.SemiBold),
    metroFont(FontWeight.Bold),
    metroFont(FontWeight.Black),
)

enum class MetroTextStyle {
    PageTitle,
    /** Panorama / pivot hub titles — thinner than [PageTitle]. */
    HubTitle,
    /** Pivot tab headers — light weight, slightly smaller than [HubTitle]. */
    PivotTab,
    /** App-name overline above a page/hub title (`MetroAppTitle`). */
    AppTitle,
    SectionHeader,
    ListItemTitle,
    ListItemSubtitle,
    Body,
    DialogTitle,
    DialogBody,
    ;

    fun toTextStyle(): TextStyle = when (this) {
        PageTitle -> TextStyle(
            fontFamily = MetroFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 64.sp,
            lineHeight = 72.sp,
        )
        HubTitle -> TextStyle(
            fontFamily = MetroFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 56.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.5).sp,
        )
        PivotTab -> TextStyle(
            fontFamily = MetroFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 48.sp,
            lineHeight = 56.sp,
            letterSpacing = (-0.5).sp,
        )
        AppTitle -> TextStyle(
            fontFamily = MetroFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        )
        SectionHeader -> TextStyle(
            fontFamily = MetroFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 24.sp,
        )
        ListItemTitle -> TextStyle(
            fontFamily = MetroFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 28.sp,
        )
        ListItemSubtitle -> TextStyle(
            fontFamily = MetroFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        )
        Body -> TextStyle(
            fontFamily = MetroFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
        DialogTitle -> TextStyle(
            fontFamily = MetroFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 28.sp,
        )
        DialogBody -> TextStyle(
            fontFamily = MetroFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )
    }
}
