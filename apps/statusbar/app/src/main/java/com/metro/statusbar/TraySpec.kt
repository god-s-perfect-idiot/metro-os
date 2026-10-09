package com.metro.statusbar

import com.metro.system.MetroStatusBar

object TraySpec {
    const val TRAY_HEIGHT_DP = MetroStatusBar.HEIGHT_DP

    /** Vertical drag past this distance (dp) opens the Android notification shade. */
    const val SHADE_OPEN_DRAG_DP = 28
    const val START_PADDING_DP = 10
    /** Trailing inset — keep tight so the rightmost glyph (usually clock) sits near the edge. */
    const val END_PADDING_DP = 0
    /**
     * Extra horizontal clearance when the user places the notch on the left or right.
     * Sized for a typical corner punch-hole (diameter + margin from the screen edge);
     * Center keeps the base paddings only (plus any system cutout / corner / privacy insets).
     */
    const val NOTCH_SIDE_CLEARANCE_DP = 56
    /**
     * Extra end padding applied (and animated) while Android privacy dots sit near the clock.
     * Small on purpose — the system dots paint on top; this only slides the clock a touch left.
     */
    const val PRIVACY_CLOCK_NUDGE_DP = 14
    /** Gap between cellular signal bars and the data connection label (4G, 5G, …). */
    const val CELLULAR_DATA_LABEL_GAP_DP = 2
    /**
     * Conservative tray content width (after side insets) used to budget spacers when a
     * measured width is unavailable. Only the folding budget caps spacer count.
     */
    const val DEFAULT_CONTENT_WIDTH_DP = 340
    /** Per-icon slide duration when dropping in or exiting upward. */
    const val EXPAND_ANIMATION_MS = 200L
    const val COLLAPSE_ANIMATION_MS = 200L
    /** Whole-tray hide / reveal creep into / out of the top edge. */
    const val CREEP_MS = 200L
    /** Clock nudge when privacy dots appear / disappear. */
    const val PRIVACY_CLOCK_NUDGE_MS = 200L
    /** Delay between successive icons (left → right) on enter and exit. */
    const val ICON_STAGGER_MS = 90L
    /** WP8.1 default hold after the last enter finishes; setup can choose 3s / 5s / 10s / never. */
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

/** Setup ListPicker: how the tray fill is chosen for the foreground app. */
enum class StatusBarBackgroundMode {
    /** Solid black tray fill (default). */
    DefaultBlackBackground,
    /** Fill from the foreground app’s tile brand color (Metro suite stays black). */
    MatchAppBackground,
    /** Always use the system accent color. */
    ShowAccentColor,
    ;

    fun toStorage(): String = when (this) {
        DefaultBlackBackground -> STORAGE_DEFAULT_BLACK
        MatchAppBackground -> STORAGE_MATCH_APP
        ShowAccentColor -> STORAGE_SHOW_ACCENT
    }

    companion object {
        const val STORAGE_DEFAULT_BLACK = "default_black"
        const val STORAGE_MATCH_APP = "match_app"
        const val STORAGE_SHOW_ACCENT = "show_accent"

        fun fromStorage(value: String?): StatusBarBackgroundMode = when (value) {
            STORAGE_MATCH_APP -> MatchAppBackground
            STORAGE_SHOW_ACCENT -> ShowAccentColor
            STORAGE_DEFAULT_BLACK -> DefaultBlackBackground
            else -> DefaultBlackBackground
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
 * WP8.1 system tray indicators (layout order), per `references/images/image.png`.
 * [Battery] is a free-justified layout slot like the others; [Cellular]/[DataConnection]
 * form the Network group.
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
    /** Cycles active notification app icons when the icons-tab toggle is on. */
    NotificationApp,
    WifiHotspot,
    /** Bluetooth A2DP / SCO audio — headset or speaker glyph via [TraySnapshot.bluetoothAudio]. */
    BluetoothAudio,
    Location,
    Battery,
}

data class TrayThemeSnapshot(
    val backgroundColor: androidx.compose.ui.graphics.Color,
    val foregroundColor: androidx.compose.ui.graphics.Color,
    val accentColor: androidx.compose.ui.graphics.Color,
    val darkTheme: Boolean,
    val visibilityMode: TrayVisibilityMode,
    /**
     * Color used to carve glyph interiors (battery plug, moon). Matches [backgroundColor] unless
     * the tray is a transparent underlay over a shell overlay — then this is the logical fill.
     */
    val backdropColor: androidx.compose.ui.graphics.Color = backgroundColor,
)

data class TraySnapshot(
    val clockText: String,
    val expanded: Boolean,
    val showProgress: Boolean,
    val indicators: List<TrayIndicator>,
    /** WP8.1 data connection label (4G, LTE, 5G, 3G, 2G, G) shown after cellular bars. */
    val dataConnectionLabel: String?,
    /** Live cellular bar count and Wi-Fi arc count (null Wi-Fi = icon hidden). */
    val signalBars: SignalBarsStatus,
    /** True when ringer stream volume is 0 — shows the mute glyph after Wi-Fi. */
    val ringerMuted: Boolean = false,
    /** Soft-AP / Wi-Fi hotspot active. */
    val hotspotActive: Boolean = false,
    /** Connected Bluetooth audio device; null when none. */
    val bluetoothAudio: BluetoothAudioKind? = null,
    /**
     * Package currently shown for the notification-app glyph (cycled among active notifications).
     * Null when the toggle is off, access is missing, or there are no eligible notifications.
     */
    val notificationPackage: String? = null,
    /** Icons-tab allow-list; clock is always shown. */
    val iconFlags: TrayIconFlags = TrayIconFlags(),
    /**
     * Configure-page ordered slots (icons + spacers). Live tray and setup preview honor this
     * order; spacers reserve empty width for notch / privacy-mic regions.
     */
    val layout: List<TrayLayoutSlot> = TrayLayout.DEFAULT,
    val battery: BatteryStatus,
    val theme: TrayThemeSnapshot,
    /**
     * True while the Android notification shade is open. The accessibility overlay must not draw
     * over SystemUI's panel; [StatusTray] returns without content when this is set.
     */
    val notificationShadeOpen: Boolean = false,
    /**
     * True while Android system status bars are hidden (immersive fullscreen). The overlay must
     * not draw over edge-to-edge fullscreen content.
     */
    val systemStatusBarsHidden: Boolean = false,
    /**
     * Tray fill / glyph morph duration while a shell overlay (toast / volume) is tinting the strip.
     * Matches the overlay's enter/exit motion so the band does not snap separately.
     */
    val shellFillAnimationMs: Int = MetroStatusBar.SHELL_FILL_DURATION_MS_DEFAULT,
)

object TrayIndicatorOrder {
    /**
     * Collapsed resting tray keeps only the rightmost layout icon (often clock); everything
     * else is hidden.
     */
    val collapsed: List<TrayIndicator> = emptyList()

    /**
     * Default L→R indicator set used when a custom [TrayLayout] is not applied: network
     * (cellular + data), Wi-Fi, mute, notification app, hotspot, Bluetooth audio. Battery and
     * clock are separate layout slots that freely justify with the rest.
     */
    val expanded: List<TrayIndicator> = listOf(
        TrayIndicator.Cellular,
        TrayIndicator.DataConnection,
        TrayIndicator.Wifi,
        TrayIndicator.Ringer,
        TrayIndicator.NotificationApp,
        TrayIndicator.WifiHotspot,
        TrayIndicator.BluetoothAudio,
    )

    /**
     * Glyphs that actually draw given live conditions and [TrayIconFlags].
     * Skips data label / Wi-Fi / mute / notification / hotspot / Bluetooth when their toggle is
     * off or their live condition is false so stagger timing matches visible icons.
     */
    fun visibleLeft(
        dataConnectionLabel: String?,
        wifiConnected: Boolean,
        ringerMuted: Boolean = false,
        hotspotActive: Boolean = false,
        bluetoothAudio: BluetoothAudioKind? = null,
        notificationPackage: String? = null,
        iconFlags: TrayIconFlags = TrayIconFlags(),
    ): List<TrayIndicator> =
        expanded.filter {
            when (it) {
                TrayIndicator.Cellular,
                TrayIndicator.DataConnection,
                -> iconFlags.network &&
                    (it != TrayIndicator.DataConnection || dataConnectionLabel != null)
                TrayIndicator.Wifi -> iconFlags.wifi && wifiConnected
                TrayIndicator.Ringer -> iconFlags.mute && ringerMuted
                TrayIndicator.NotificationApp ->
                    iconFlags.notifications && !notificationPackage.isNullOrBlank()
                TrayIndicator.WifiHotspot -> iconFlags.hotspot && hotspotActive
                TrayIndicator.BluetoothAudio ->
                    iconFlags.bluetoothAudio && bluetoothAudio != null
                else -> true
            }
        }
}

object TrayCollapseScheduler {
    /**
     * Auto-collapse after the staggered enter finishes plus the hold timeout.
     * [animatingIconCount] is the number of icons that stagger in/out (all live icons except
     * the persistent rightmost).
     * Negative [holdMs] (e.g. [StatusTrayPreferences.TIMEOUT_NEVER_MS]) means never collapse.
     */
    fun shouldAutoCollapse(
        expanded: Boolean,
        lastExpandedAtMs: Long,
        nowMs: Long,
        animatingIconCount: Int,
        holdMs: Long = TraySpec.AUTO_COLLAPSE_MS,
    ): Boolean {
        if (!expanded) return false
        if (holdMs < 0L) return false
        val enterMs = TraySpec.staggerSequenceMs(animatingIconCount)
        return nowMs - lastExpandedAtMs >= enterMs + holdMs
    }
}
