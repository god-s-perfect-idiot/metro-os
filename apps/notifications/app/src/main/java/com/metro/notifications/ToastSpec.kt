package com.metro.notifications

/** WP8.1 toast banner tokens. Reference: `references/images/toast.png`. */
object ToastSpec {
    /** Accent strip min height: square icon + wrapping message. Clock lives in the status tray. */
    const val HEIGHT_DP = 52
    const val ICON_DP = 32
    const val HORIZONTAL_PADDING_DP = 12
    const val VERTICAL_PADDING_DP = 10
    const val ICON_TEXT_GAP_DP = 10
    const val DURATION_3S_MS = 3_000L
    const val DURATION_5S_MS = 5_000L
    const val DURATION_10S_MS = 10_000L
    /** WP8.1 default toast peek (~5s). Setup ListPicker may choose 3s or 10s. */
    const val DURATION_MS = DURATION_5S_MS
    val DURATION_OPTIONS_MS: List<Long> = listOf(DURATION_3S_MS, DURATION_5S_MS, DURATION_10S_MS)
    const val SWIPE_DISMISS_DP = 72
    /**
     * Extra overlay height below the accent bar so a perspective `rotationX` trapezoid is not
     * clipped by the WindowManager surface.
     */
    const val FLIP_PROJECTION_PAD_DP = 24

    fun coerceDurationMs(ms: Long): Long =
        if (ms in DURATION_OPTIONS_MS) ms else DURATION_MS

    /**
     * Skia/RenderNode camera distance is inches (×72 → px). Jump-list flips use 16×density
     * (~48″) so small tiles don't squash; that camera is so far from a full-width 52dp bar
     * that `rotationX` reads as a 2D scale. Size from banner width to keep WP PlaneProjection's
     * ~57° field of view (camera Z ≈ 0.9×width).
     */
    fun flipCameraInches(widthPx: Float): Float {
        if (widthPx <= 0f) return FLIP_CAMERA_DEFAULT_INCHES
        return (widthPx / SKIA_POINTS_PER_INCH) * FLIP_CAMERA_WIDTH_FACTOR
    }

    private const val SKIA_POINTS_PER_INCH = 72f
    private const val FLIP_CAMERA_WIDTH_FACTOR = 0.9f
    private const val FLIP_CAMERA_DEFAULT_INCHES = 8f

    val IgnoredPackages: Set<String> = setOf(
        "com.metro.notifications",
        "com.metro.statusbar",
        "com.metro.navbar",
        "com.metro.launcher",
        "com.metro.volume",
        "com.metro.keyboard",
    )
}
