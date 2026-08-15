package com.metro.statusbar

import com.metro.system.MetroStatusBar

object TraySpec {
    const val TRAY_HEIGHT_DP = MetroStatusBar.HEIGHT_DP

    /** Vertical drag past this distance (dp) opens the Android notification shade. */
    const val SHADE_OPEN_DRAG_DP = 28
    const val START_PADDING_DP = 10
    const val END_PADDING_DP = 2
    /**
     * Extra horizontal clearance when the user places the notch on the left or right.
     * Sized for a typical corner punch-hole (diameter + margin from the screen edge);
     * Center keeps the base paddings only (plus any system cutout / corner / privacy insets).
     */
    const val NOTCH_SIDE_CLEARANCE_DP = 56
    const val PRIVACY_INDICATOR_GAP_DP = 2
    /** Gap between cellular signal bars and the data connection label (4G, 5G, …). */
    const val CELLULAR_DATA_LABEL_GAP_DP = 2
    /** Extra leading space before Wi-Fi so it sits clearly apart from the network group. */
    const val WIFI_LEADING_PADDING_DP = 8
    /** Per-icon slide duration when dropping in or exiting upward. */
    const val EXPAND_ANIMATION_MS = 200L
    const val COLLAPSE_ANIMATION_MS = 200L
    /** Delay between successive icons (right → left) on enter and exit. */
    const val ICON_STAGGER_MS = 90L
    /** WP8.1 default hold after the last enter finishes; setup can choose 3s / 5s / 10s. */
    const val AUTO_COLLAPSE_MS = MetroStatusBar.AUTO_COLLAPSE_MS

    /** Total time for a staggered enter (or exit) of [iconCount] icons. */
    fun staggerSequenceMs(iconCount: Int, perIconMs: Long = EXPAND_ANIMATION_MS): Long {
        if (iconCount <= 0) return 0L
        return perIconMs + (iconCount - 1) * ICON_STAGGER_MS
    }

    /**
     * Horizontal content insets for the tray row (physical left/right — not RTL).
     * [NotchPosition.Center] keeps the base WP paddings; left/right add
     * [NOTCH_SIDE_CLEARANCE_DP] on that edge. System insets still win when larger.
     */
    fun horizontalPaddingDp(
        notchPosition: NotchPosition,
        systemLeftDp: Int = 0,
        systemRightDp: Int = 0,
    ): HorizontalPaddingDp {
        var left = START_PADDING_DP
        var right = END_PADDING_DP
        when (notchPosition) {
            NotchPosition.Center -> Unit
            NotchPosition.Left -> left += NOTCH_SIDE_CLEARANCE_DP
            NotchPosition.Right -> right += NOTCH_SIDE_CLEARANCE_DP
        }
        return HorizontalPaddingDp(
            left = maxOf(left, systemLeftDp),
            right = maxOf(right, systemRightDp),
        )
    }
}

/** Where the display cutout / punch-hole sits — setup ListPicker drives tray side padding. */
enum class NotchPosition {
    Center,
    Left,
    Right,
    ;

    fun toStorage(): String = when (this) {
        Center -> STORAGE_CENTER
        Left -> STORAGE_LEFT
        Right -> STORAGE_RIGHT
    }

    companion object {
        const val STORAGE_CENTER = "center"
        const val STORAGE_LEFT = "left"
        const val STORAGE_RIGHT = "right"

        fun fromStorage(value: String?): NotchPosition = when (value) {
            STORAGE_LEFT -> Left
            STORAGE_RIGHT -> Right
            else -> Center
        }
    }
}

data class HorizontalPaddingDp(
    val left: Int,
    val right: Int,
)

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

    /**
     * WP tray paints the charge bar red at or below 20%; above that the bar matches the
     * foreground (white on dark / black on light).
     */
    val isLow: Boolean get() = percent <= LOW_PERCENT_THRESHOLD

    companion object {
        /** Charge level at which the tray switches the fill to the low-battery red. */
        const val LOW_PERCENT_THRESHOLD = 20

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
    /**
     * True while the Android notification shade is open. The accessibility overlay must not draw
     * over SystemUI's panel; [StatusTray] returns without content when this is set.
     */
    val notificationShadeOpen: Boolean = false,
)

object TrayIndicatorOrder {
    /** Collapsed resting tray shows clock only — no left-side indicators. */
    val collapsed: List<TrayIndicator> = emptyList()

    /**
     * Left-side indicators revealed on tap / home: network (cellular + data label) and Wi-Fi.
     * Battery and clock live on the right.
     */
    val expanded: List<TrayIndicator> = listOf(
        TrayIndicator.Cellular,
        TrayIndicator.DataConnection,
        TrayIndicator.Wifi,
    )

    /**
     * Left-row glyphs that actually draw for [dataConnectionLabel]. Skips [TrayIndicator.DataConnection]
     * when there is no label so stagger timing matches visible icons.
     */
    fun visibleLeft(dataConnectionLabel: String?): List<TrayIndicator> =
        expanded.filter { it != TrayIndicator.DataConnection || dataConnectionLabel != null }
}

object TrayCollapseScheduler {
    /**
     * Auto-collapse after the staggered enter finishes plus the hold timeout.
     * [animatingIconCount] includes left indicators and battery when present.
     */
    fun shouldAutoCollapse(
        expanded: Boolean,
        lastExpandedAtMs: Long,
        nowMs: Long,
        animatingIconCount: Int,
        holdMs: Long = TraySpec.AUTO_COLLAPSE_MS,
    ): Boolean {
        if (!expanded) return false
        val enterMs = TraySpec.staggerSequenceMs(animatingIconCount)
        return nowMs - lastExpandedAtMs >= enterMs + holdMs
    }
}
