package com.metro.widgets.data

/**
 * One live-tile notification peek face (flip back). Newest-first queue cycles one flip at a time
 * — same contract as launcher [TilePeekLines].
 */
data class NotifierPeekLines(
    val title: String?,
    val subtitle: String?,
    val body: String?,
    /** App display name shown as the Start-style footer on the peek face. */
    val appLabel: String,
    val packageName: String,
    val postTimeMs: Long,
) {
    val hasContent: Boolean
        get() = !title.isNullOrBlank() || !subtitle.isNullOrBlank() || !body.isNullOrBlank()

    fun normalizedForFlip(): NotifierPeekLines {
        val t = title?.trim()?.takeIf { it.isNotEmpty() }
        val s = subtitle?.trim()?.takeIf { it.isNotEmpty() }
        val b = body?.trim()?.takeIf { it.isNotEmpty() }
        return when {
            t != null -> copy(title = t, subtitle = s, body = b)
            s != null -> copy(title = s, subtitle = null, body = b)
            b != null -> copy(title = b, subtitle = null, body = null)
            else -> copy(title = null, subtitle = null, body = null)
        }
    }
}

data class NotifierTraySnapshot(
    val peeks: List<NotifierPeekLines> = emptyList(),
    val count: Int = 0,
) {
    val hasPeeks: Boolean get() = peeks.isNotEmpty()
}

object NotifierPeekLogic {
    /**
     * Newest-first distinct peeks across the whole tray (not per-package).
     * Distinctness ignores [NotifierPeekLines.appLabel] / package / post time — content only.
     */
    fun buildTrayQueue(items: List<NotifierPeekLines>): List<NotifierPeekLines> =
        items
            .sortedByDescending { it.postTimeMs }
            .map { it.normalizedForFlip() }
            .filter { it.hasContent }
            .distinctBy { Triple(it.title, it.subtitle, it.body) }
}
