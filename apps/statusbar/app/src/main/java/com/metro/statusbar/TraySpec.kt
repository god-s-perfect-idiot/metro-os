package com.metro.statusbar

import com.metro.system.MetroStatusBar

object TraySpec {
    const val TRAY_HEIGHT_DP = MetroStatusBar.HEIGHT_DP
    const val START_PADDING_DP = 10
    const val END_PADDING_DP = 2
    const val PRIVACY_INDICATOR_GAP_DP = 2
    /** Gap between cellular signal bars and the data connection label (4G, 5G, …). */
    const val CELLULAR_DATA_LABEL_GAP_DP = 2
    const val EXPAND_ANIMATION_MS = 200L
    const val COLLAPSE_ANIMATION_MS = 200L
    const val AUTO_COLLAPSE_MS = MetroStatusBar.AUTO_COLLAPSE_MS
}

enum class TrayVisibilityMode {
    Opaque,
    Translucent,
    Hidden,
    ;

    companion object {
        /**
         * Maps a [MetroStatusBar] `MODE_*` contract string to a tray mode, defaulting to
         * [Opaque] for unknown values so a malformed request never hides the clock.
         */
        fun fromContract(mode: String?): TrayVisibilityMode = when (mode) {
            MetroStatusBar.MODE_TRANSLUCENT -> Translucent
            MetroStatusBar.MODE_HIDDEN -> Hidden
            else -> Opaque
        }
    }
}

/**
 * Decoupled battery snapshot so the static-v1 / dynamic sources can swap without touching
 * rendering (README § Data and state model).
 *
 * @param fraction charge level in `0f..1f`
 * @param charging whether the device is plugged in / charging
 * @param present whether a battery is reported at all (emulators may report none)
 */
data class BatteryStatus(
    val fraction: Float,
    val charging: Boolean,
    val present: Boolean = true,
) {
    /** Whole-number battery percentage, `0..100`. */
    val percent: Int get() = (fraction.coerceIn(0f, 1f) * 100f).toInt()

    companion object {
        /** Neutral fallback used before the first battery broadcast arrives. */
        val Unknown = BatteryStatus(fraction = 1f, charging = false, present = true)

        /** Builds a clamped status from a raw level/scale pair (e.g. `BatteryManager` extras). */
        fun fromLevel(level: Int, scale: Int, charging: Boolean): BatteryStatus {
            if (scale <= 0 || level < 0) return Unknown.copy(charging = charging)
            return BatteryStatus(
                fraction = (level.toFloat() / scale.toFloat()).coerceIn(0f, 1f),
                charging = charging,
            )
        }
    }
}

/**
 * WP8.1 system tray indicators (left → right), per
 * `references/images/image.png`. [Battery] is rendered on the right next to the clock; the rest
 * form the left indicator row.
 */
enum class TrayIndicator {
    Cellular,
    DataConnection,
    CallForwarding,
    Roaming,
    Wifi,
    Bluetooth,
    QuietHours,
    DrivingMode,
    Ringer,
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
    /** WP8.1 data connection label (4G, LTE, 5G, 3G, 2G, G) shown after cellular bars. */
    val dataConnectionLabel: String?,
    val battery: BatteryStatus,
    val theme: TrayThemeSnapshot,
)

object TrayIndicatorOrder {
    /** Left-side icons shown in the resting (collapsed) tray — common connection indicators. */
    val collapsed: List<TrayIndicator> = listOf(
        TrayIndicator.Cellular,
        TrayIndicator.DataConnection,
        TrayIndicator.Wifi,
    )

    /**
     * Full WP8.1 left-side indicator row revealed on tap, in
     * `references/images/image.png` order (battery/clock live on the right, not here).
     */
    val expanded: List<TrayIndicator> = listOf(
        TrayIndicator.Cellular,
        TrayIndicator.DataConnection,
        TrayIndicator.CallForwarding,
        TrayIndicator.Roaming,
        TrayIndicator.Wifi,
        TrayIndicator.Bluetooth,
        TrayIndicator.QuietHours,
        TrayIndicator.DrivingMode,
        TrayIndicator.Ringer,
        TrayIndicator.Location,
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
