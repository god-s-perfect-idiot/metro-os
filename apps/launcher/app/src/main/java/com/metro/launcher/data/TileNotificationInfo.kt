package com.metro.launcher.data

/**
 * Aggregated Android notification state for one package, mapped onto a WP8.1 live-tile face.
 *
 * [count] drives the top-right badge. [peekTitle] / [peekBody] become the flip (back) face when the
 * app has no richer tile provider face (agenda / photo grid).
 */
data class TileNotificationInfo(
    val packageName: String,
    val count: Int,
    val peekTitle: String?,
    val peekBody: String?,
    val updatedAtMs: Long,
) {
    val hasPeek: Boolean
        get() = !peekTitle.isNullOrBlank() || !peekBody.isNullOrBlank()

    /** Single-line / stacked back-face copy for medium and wide tiles. */
    fun backFaceLines(wide: Boolean): List<String> {
        val title = peekTitle?.trim().orEmpty()
        val body = peekBody?.trim().orEmpty()
        return when {
            title.isEmpty() && body.isEmpty() -> emptyList()
            title.isEmpty() -> listOf(body)
            body.isEmpty() -> listOf(title)
            wide -> listOf(title, body)
            else -> listOf(title, body)
        }
    }
}
