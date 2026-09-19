package com.metro.ui

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.metro.system.MetroTypeface

/**
 * Noto Sans (Segoe WP stand-in) — default suite chrome face.
 * Static faces from DiscoLauncher NotoCustom (OFL), not the Google Fonts VF.
 */
val MetroNotoFontFamily = FontFamily(
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

/**
 * Default / fallback typeface when [LocalMetroFontFamily] is unavailable.
 * Prefer [MetroTheme.fontFamily] inside composables so Settings → start+theme applies.
 */
val MetroFontFamily: FontFamily = MetroNotoFontFamily

/** Adobe Source Sans 3 variable font (OFL) — weight axis covers Thin–Black. */
@OptIn(ExperimentalTextApi::class)
val MetroSourceSans3FontFamily: FontFamily = FontFamily(
    variableWeightFace(R.font.source_sans_3, FontWeight.Thin),
    variableWeightFace(R.font.source_sans_3, FontWeight.ExtraLight),
    variableWeightFace(R.font.source_sans_3, FontWeight.Light),
    variableWeightFace(R.font.source_sans_3, FontWeight.Normal),
    variableWeightFace(R.font.source_sans_3, FontWeight.Medium),
    variableWeightFace(R.font.source_sans_3, FontWeight.SemiBold),
    variableWeightFace(R.font.source_sans_3, FontWeight.Bold),
    variableWeightFace(R.font.source_sans_3, FontWeight.ExtraBold),
    variableWeightFace(R.font.source_sans_3, FontWeight.Black),
)

/**
 * Alegreya Sans static romans (OFL). No dedicated SemiBold — [FontWeight.SemiBold] falls
 * through to the nearest bundled face (Medium / Bold).
 */
val MetroAlegreyaSansFontFamily = FontFamily(
    Font(R.font.alegreya_sans_thin, FontWeight.Thin),
    Font(R.font.alegreya_sans_thin, FontWeight.ExtraLight),
    Font(R.font.alegreya_sans_light, FontWeight.Light),
    Font(R.font.alegreya_sans_regular, FontWeight.Normal),
    Font(R.font.alegreya_sans_medium, FontWeight.Medium),
    Font(R.font.alegreya_sans_medium, FontWeight.SemiBold),
    Font(R.font.alegreya_sans_bold, FontWeight.Bold),
    Font(R.font.alegreya_sans_extrabold, FontWeight.ExtraBold),
    Font(R.font.alegreya_sans_black, FontWeight.Black),
)

/** Resolves the Compose [FontFamily] for a suite [MetroTypeface] preference. */
fun metroFontFamilyFor(typeface: MetroTypeface): FontFamily = when (typeface) {
    MetroTypeface.MetroNoto -> MetroNotoFontFamily
    MetroTypeface.SourceSans3 -> MetroSourceSans3FontFamily
    MetroTypeface.AlegreyaSans -> MetroAlegreyaSansFontFamily
}

@OptIn(ExperimentalTextApi::class)
private fun variableWeightFace(resId: Int, weight: FontWeight): Font = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)
