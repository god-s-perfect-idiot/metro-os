package com.metro.ui

import androidx.compose.ui.graphics.Brush

/**
 * Named horizontal gradients for panoramic hub brand titles ([MetroPanoramaBrandTitle]).
 * Each suite hub gets a distinct palette so brands stay recognizable at a glance.
 */
object MetroPanoramaBrandGradients {
    /** Music — warm sunset (orange → magenta → violet), matches album-art fallback. */
    val Music: Brush = Brush.horizontalGradient(
        listOf(
            MetroColors.AccentOrange,
            MetroColors.AccentMagenta,
            MetroColors.AccentViolet,
        ),
    )

    /** Hub — cool suite catalogue (cyan → cobalt → indigo). */
    val Hub: Brush = Brush.horizontalGradient(
        listOf(
            MetroColors.AccentCyan,
            MetroColors.AccentCobalt,
            MetroColors.AccentIndigo,
        ),
    )

    /** Metron — fresh explore / library (lime → emerald → teal). */
    val Metron: Brush = Brush.horizontalGradient(
        listOf(
            MetroColors.AccentLime,
            MetroColors.AccentEmerald,
            MetroColors.AccentTeal,
        ),
    )

    /** News — editorial urgency (crimson → orange → amber). */
    val News: Brush = Brush.horizontalGradient(
        listOf(
            MetroColors.AccentCrimson,
            MetroColors.AccentOrange,
            MetroColors.AccentAmber,
        ),
    )
}
