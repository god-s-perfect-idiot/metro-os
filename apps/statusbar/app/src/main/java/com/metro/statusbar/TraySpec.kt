package com.metro.statusbar

object TraySpec {
    const val TRAY_HEIGHT_DP = 32
    const val EXPAND_ANIMATION_MS = 200L
    const val COLLAPSE_ANIMATION_MS = 200L
    const val AUTO_COLLAPSE_MS = 8000L
}

enum class TrayVisibilityMode {
    Opaque,
    Translucent,
    Hidden,
}

enum class TrayIndicator {
    Cellular,
    Wifi,
    Bluetooth,
    Alarm,
    Location,
    Battery,
}

data class TrayThemeSnapshot(
    val backgroundColor: androidx.compose.ui.graphics.Color,
    val foregroundColor: androidx.compose.ui.graphics.Color,
    val accentColor: androidx.compose.ui.graphics.Color,
    val darkTheme: Boolean,
    val visibilityMode: TrayVisibilityMode,
)

data class TraySnapshot(
    val clockText: String,
    val expanded: Boolean,
    val showProgress: Boolean,
    val indicators: List<TrayIndicator>,
    val theme: TrayThemeSnapshot,
)

object TrayIndicatorOrder {
    val default: List<TrayIndicator> = listOf(
        TrayIndicator.Cellular,
        TrayIndicator.Wifi,
        TrayIndicator.Bluetooth,
        TrayIndicator.Alarm,
        TrayIndicator.Location,
        TrayIndicator.Battery,
    )
}

object TrayCollapseScheduler {
    fun shouldAutoCollapse(
        expanded: Boolean,
        lastExpandedAtMs: Long,
        nowMs: Long,
        timeoutMs: Long = TraySpec.AUTO_COLLAPSE_MS,
    ): Boolean = expanded && nowMs - lastExpandedAtMs >= timeoutMs
}
