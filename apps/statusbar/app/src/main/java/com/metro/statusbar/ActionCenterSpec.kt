package com.metro.statusbar

/**
 * WP8.1 Action Center layout and motion tokens.
 *
 * References: `references/images/action_center_dark_cyan.png`,
 * `references/images/expanded_dark.png`, `references/guides/blueprint.md` § Action Center.
 */
object ActionCenterSpec {
    /** Slide open / close duration for the shade. */
    const val OPEN_MS = 280
    const val CLOSE_MS = 240

    /** Vertical drag (px density-independent) past which a fling/release snaps open. */
    const val OPEN_THRESHOLD_DP = 72

    /** Quick-action tile row. */
    const val TILE_GAP_DP = 4
    const val TILE_HEIGHT_DP = 86
    const val TILE_ICON_SIZE_DP = 28
    const val TILE_HORIZONTAL_PADDING_DP = 12
    const val TILE_TOP_PADDING_DP = 8

    /** Inactive quick-action fill (dark theme). */
    const val INACTIVE_TILE_DARK = 0xFF1F1F1F.toInt()
    /** Inactive quick-action fill (light theme). */
    const val INACTIVE_TILE_LIGHT = 0xFFE6E6E6.toInt()

    /** Clear All / All Settings row. */
    const val ACTIONS_ROW_TOP_DP = 16
    const val ACTIONS_ROW_BOTTOM_DP = 20
    const val ACTIONS_ROW_ICON_GAP_DP = 8

    /** Notification group header. */
    const val GROUP_ICON_DP = 28
    const val GROUP_TITLE_GAP_DP = 12
    const val GROUP_SPACING_DP = 28
    const val ITEM_VERTICAL_GAP_DP = 14

    /** Accent drag handle at the bottom of the shade. */
    const val HANDLE_BAR_HEIGHT_DP = 24
    const val HANDLE_GRIP_WIDTH_DP = 36
    const val HANDLE_GRIP_HEIGHT_DP = 3

    /** Horizontal content inset matching WP list margins. */
    const val CONTENT_HORIZONTAL_DP = 12
}

/** Default WP8.1 quick-action slots (customizable later via Settings). */
enum class QuickActionType {
    Wifi,
    Bluetooth,
    Airplane,
    InternetSharing,
}

data class QuickActionSlot(
    val type: QuickActionType,
    val enabled: Boolean,
    /** Secondary label: SSID, paired device, or the action name when off / unknown. */
    val label: String,
)

data class ActionNotificationItem(
    val key: String,
    val packageName: String,
    val title: String,
    val body: String?,
    val timeText: String,
    val postedAtMs: Long,
)

data class ActionNotificationGroup(
    val packageName: String,
    val appLabel: String,
    val items: List<ActionNotificationItem>,
)
