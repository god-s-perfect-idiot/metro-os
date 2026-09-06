package com.metro.launcher.data

/**
 * Aggregated Android notification state for one package, mapped onto a WP8.1 live-tile face.
 *
 * [count] drives the naked numeral badge (centered beside the icon on 1×1, center-right on
 * 2×2 front faces, bottom-right on 4×2 and on any size when the notification peek face is
 * showing).
 * [peeks] is the newest-first queue of flip faces; the Start tile cycles through them one
 * by one. [peekTitle] / [peekSubtitle] / [peekBody] mirror the first peek for callers that
 * only need the lead face.
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
    /**
     * All active notification peeks for this package (newest first, distinct). Empty when
     * only the legacy single-peek fields are set (tests / fallback).
     */
    val peeks: List<TilePeekLines> = emptyList(),
) {
    /** Resolved flip queue: [peeks] when present, else a single face from the legacy fields. */
    val peekQueue: List<TilePeekLines>
        get() {
            if (peeks.isNotEmpty()) return peeks
            val single = TilePeekLines(peekTitle, peekSubtitle, peekBody).normalizedForFlip()
            return if (single.hasContent) listOf(single) else emptyList()
        }

    val hasPeek: Boolean
        get() = peekQueue.isNotEmpty()

    val hasProgress: Boolean
        get() = progress != null

    /** Stacked back-face copy for medium and wide tiles (lead peek only). */
    fun backFaceLines(wide: Boolean): List<String> {
        val lead = peekQueue.firstOrNull() ?: return emptyList()
        val lines = listOfNotNull(
            lead.title?.trim()?.takeIf { it.isNotEmpty() },
            lead.subtitle?.trim()?.takeIf { it.isNotEmpty() },
            lead.body?.trim()?.takeIf { it.isNotEmpty() },
        )
        return when {
            lines.isEmpty() -> emptyList()
            wide -> lines.take(3)
            lines.size <= 3 -> lines
            else -> listOf(lines.first(), lines.last())
        }
    }
}
