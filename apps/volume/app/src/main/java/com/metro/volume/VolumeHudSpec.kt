package com.metro.volume

import androidx.compose.ui.graphics.Color

/** Timing and chrome tokens for the WP8.1 volume HUD. */
object VolumeHudSpec {
    const val DISMISS_MS = 2500L
    const val COLLAPSED_HEIGHT_DP = 48
    const val BOTTOM_ROW_HEIGHT_DP = 44
    const val HORIZONTAL_PADDING_DP = 12

    val PanelBackground = Color(0xFF252525)
    val SecondaryText = Color(0xFF9E9E9E)
    val PrimaryText = Color.White
}

enum class VolumeStreamKind {
    Ringer,
    Media,
    Call,
    ;

    val wpMax: Int
        get() = when (this) {
            Ringer, Call -> 10
            Media -> 30
        }

    val label: String
        get() = when (this) {
            Ringer -> "Ringer + Notifications"
            Media -> "Media + Apps"
            Call -> "Call volume"
        }
}

data class VolumeHudSnapshot(
    val visible: Boolean,
    val expanded: Boolean,
    val activeStream: VolumeStreamKind,
    val ringerLevel: Int,
    val mediaLevel: Int,
    val callLevel: Int,
    val vibrateOn: Boolean,
    val inCall: Boolean,
    val accentColor: androidx.compose.ui.graphics.Color,
) {
    val collapsedLevel: Int
        get() = when (activeStream) {
            VolumeStreamKind.Ringer -> ringerLevel
            VolumeStreamKind.Media -> mediaLevel
            VolumeStreamKind.Call -> callLevel
        }

    val collapsedMax: Int get() = activeStream.wpMax
    val collapsedLabel: String get() = activeStream.label

    companion object {
        val Hidden = VolumeHudSnapshot(
            visible = false,
            expanded = false,
            activeStream = VolumeStreamKind.Ringer,
            ringerLevel = 0,
            mediaLevel = 0,
            callLevel = 0,
            vibrateOn = true,
            inCall = false,
            accentColor = Color(0xFF1BA1E2),
        )
    }
}
