package com.metro.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography roles from scope.md §1.
 *
 * Face comes from [fontFamily] (suite preference via [LocalMetroFontFamily] / [MetroTheme.fontFamily]).
 */
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
    /** App-bar / expanded action icon caption (WP8.1: 15sp Regular, lowercase). */
    AppBarIconHint,
    ;

    /**
     * Page / hub / pivot titles stay on one line and overflow the screen edge
     * (WP8.1). [MetroText] enforces this for these roles.
     */
    fun overflowsAtScreenEdge(): Boolean = when (this) {
        PageTitle, HubTitle, PivotTab -> true
        else -> false
    }

    /**
     * @param fontFamily Suite chrome face; defaults to Noto for non-composable / test callers.
     * Prefer resolving via [LocalMetroFontFamily] in UI.
     */
    fun toTextStyle(fontFamily: FontFamily = MetroFontFamily): TextStyle = when (this) {
        PageTitle -> TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 64.sp,
            lineHeight = 72.sp,
        )
        HubTitle -> TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 56.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.5).sp,
        )
        PivotTab -> TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 48.sp,
            lineHeight = 56.sp,
            letterSpacing = (-0.5).sp,
        )
        AppTitle -> TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        )
        HubLink -> TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 38.sp,
            lineHeight = 44.sp,
        )
        SectionHeader -> TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 24.sp,
        )
        ListItemTitle -> TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 28.sp,
        )
        ListItemSubtitle -> TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 22.sp,
        )
        Body -> TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 24.sp,
        )
        DialogTitle -> TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 28.sp,
        )
        DialogBody -> TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )
        AppBarIconHint -> TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 18.sp,
        )
    }
}
