package com.metro.launcher.data

/**
 * Aggregated Android notification state for one package, mapped onto a WP8.1 live-tile face.
 *
 * [count] drives the naked numeral badge (centered beside the icon on 1×1, center-right on
 * 2×2 front faces, bottom-right on 4×2 and on any size when the notification peek face is
 * showing).
 * [peekTitle] / [peekSubtitle] / [peekBody] become the flip (back) face when the app has no
 * richer tile provider face (agenda / photo grid).
 *
 * Mail / Gmail peeks use two lines: sender (From) → content preview.
 */
data class TileNotificationInfo(
    val packageName: String,
    val count: Int,
    val peekTitle: String?,
    val peekBody: String?,
    val updatedAtMs: Long,
    /** Middle peek line (e.g. email subject). Null for simple two-line peeks. */
    val peekSubtitle: String? = null,
    /** Progress-bar notification mapped onto the front of the tile (charging, downloads). */
    val progress: TileProgressInfo? = null,
) {
    val hasPeek: Boolean
        get() = !peekTitle.isNullOrBlank() ||
            !peekSubtitle.isNullOrBlank() ||
            !peekBody.isNullOrBlank()

    val hasProgress: Boolean
        get() = progress != null

    /** Stacked back-face copy for medium and wide tiles. */
    fun backFaceLines(wide: Boolean): List<String> {
        val lines = listOfNotNull(
            peekTitle?.trim()?.takeIf { it.isNotEmpty() },
            peekSubtitle?.trim()?.takeIf { it.isNotEmpty() },
            peekBody?.trim()?.takeIf { it.isNotEmpty() },
        )
        return when {
            lines.isEmpty() -> emptyList()
            wide -> lines.take(3)
            lines.size <= 3 -> lines
            else -> listOf(lines.first(), lines.last())
        }
    }
}
