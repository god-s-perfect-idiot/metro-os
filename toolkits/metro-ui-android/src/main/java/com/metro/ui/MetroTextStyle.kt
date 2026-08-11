package com.metro.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.metro.ui.R

/**
 * Noto Sans (Segoe WP stand-in) typography roles from scope.md §1.
 *
 * Static faces from DiscoLauncher NotoCustom (OFL), not the Google Fonts VF.
 */
val MetroFontFamily = FontFamily(
    Font(R.font.noto_sans_thin, FontWeight.Thin),
    Font(R.font.noto_sans_extralight, FontWeight.ExtraLight),
    Font(R.font.noto_sans_light, FontWeight.Light),
    Font(R.font.noto_sans_regular, FontWeight.Normal),
    Font(R.font.noto_sans_medium, FontWeight.Medium),
    Font(R.font.noto_sans_semibold, FontWeight.SemiBold),
    Font(R.font.noto_sans_bold, FontWeight.Bold),
    Font(R.font.noto_sans_extrabold, FontWeight.ExtraBold),
    Font(R.font.noto_sans_black, FontWeight.Black),
)

enum class MetroTextStyle {
    PageTitle,
    /** Panorama / pivot hub titles — thinner than [PageTitle]. */
    HubTitle,
    /** Pivot tab headers — light weight, slightly smaller than [HubTitle]. */
    PivotTab,
    /** App-name overline above a page/hub title (`MetroAppTitle`). */
    AppTitle,
    /** Hub pane link lists (WP8.1 `PhoneTextExtraLargeStyle`) — e.g. music collection links. */
    HubLink,
    SectionHeader,
    ListItemTitle,
    ListItemSubtitle,
    Body,
    DialogTitle,
    DialogBody,
    ;

    /**
     * Page / hub / pivot titles stay on one line and overflow the screen edge
     * (WP8.1). [MetroText] enforces this for these roles.
     */
    fun overflowsAtScreenEdge(): Boolean = when (this) {
        PageTitle, HubTitle, PivotTab -> true
        else -> false
    }

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
        HubLink -> TextStyle(
            fontFamily = MetroFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 34.sp,
            lineHeight = 40.sp,
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
            fontSize = 18.sp,
            lineHeight = 22.sp,
        )
        Body -> TextStyle(
            fontFamily = MetroFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 24.sp,
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
