package com.metro.launcher.data

/**
 * Aggregated Android notification state for one package, mapped onto a WP8.1 live-tile face.
 *
 * [count] drives the naked numeral badge (center-right on 1×1/2×2, bottom-right on 4×2).
 * [peekTitle] / [peekSubtitle] / [peekBody] become the flip (back) face when the app has no
 * richer tile provider face (agenda / photo grid).
 *
 * Mail / Gmail peeks use all three lines: sender → subject → content preview.
 */
data class TileNotificationInfo(
    val packageName: String,
    val count: Int,
    val peekTitle: String?,
    val peekBody: String?,
    val updatedAtMs: Long,
    /** Middle peek line (e.g. email subject). Null for simple two-line peeks. */
    val peekSubtitle: String? = null,
) {
    val hasPeek: Boolean
        get() = !peekTitle.isNullOrBlank() ||
            !peekSubtitle.isNullOrBlank() ||
            !peekBody.isNullOrBlank()

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
