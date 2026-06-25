package com.metro.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.metro.ui.R

/**
 * Noto Sans (Segoe WP stand-in) typography roles from scope.md §1.
 */
val MetroFontFamily = FontFamily(
    Font(R.font.noto_sans, FontWeight.Light),
    Font(R.font.noto_sans, FontWeight.Normal),
    Font(R.font.noto_sans, FontWeight.Medium),
    Font(R.font.noto_sans, FontWeight.SemiBold),
    Font(R.font.noto_sans, FontWeight.Bold),
    Font(R.font.noto_sans, FontWeight.Black),
)

enum class MetroTextStyle {
    PageTitle,
    /** Panorama / pivot hub titles — thinner than [PageTitle]. */
    HubTitle,
    /** Pivot tab headers — light weight, slightly smaller than [HubTitle]. */
    PivotTab,
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
