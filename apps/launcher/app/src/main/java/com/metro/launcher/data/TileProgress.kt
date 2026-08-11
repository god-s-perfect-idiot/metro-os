package com.metro.launcher.data

/**
 * Progress overlay for a Start tile, mapped from an Android notification that shows a
 * progress bar (downloads, charging sessions, file transfers, …).
 *
 * Remaining time is preferred as the live caption — from a countdown chronometer when the
 * notification supplies one, otherwise parsed from title/text ("45 min remaining").
 */
data class TileProgressInfo(
    val progress: Int = 0,
    val progressMax: Int = 0,
    val indeterminate: Boolean = false,
    /** Notification title, e.g. "Charging". */
    val statusTitle: String? = null,
    /** Fallback status line when no remaining-time phrase is available. */
    val statusBody: String? = null,
    /** Remaining-time phrase taken from the notification copy. */
    val remainingLabel: String? = null,
    /** End timestamp for [Notification] countdown chronometers; Compose ticks locally. */
    val countdownEndsAtMs: Long? = null,
) {
    val fraction: Float?
        get() = when {
            indeterminate || progressMax <= 0 -> null
            else -> (progress.toFloat() / progressMax.toFloat()).coerceIn(0f, 1f)
        }

    val hasBar: Boolean
        get() = indeterminate || progressMax > 0

    fun remainingText(nowMs: Long): String? {
        val end = countdownEndsAtMs
        if (end != null) {
            val left = end - nowMs
            return if (left <= 0L) remainingLabel ?: "Almost done" else formatRemainingMs(left)
        }
        return remainingLabel
    }

    fun caption(nowMs: Long): String? =
        remainingText(nowMs) ?: statusBody
}

internal data class NotificationProgressFields(
    val title: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    val subText: String? = null,
    val infoText: String? = null,
    val progress: Int = 0,
    val progressMax: Int = 0,
    val indeterminate: Boolean = false,
    val hasMediaSession: Boolean = false,
    val showChronometer: Boolean = false,
    val chronometerCountDown: Boolean = false,
    val whenMs: Long = 0L,
    val ongoing: Boolean = false,
)

/**
 * Build a tile progress snapshot from notification extras. Returns null when the notification
 * is media (now-playing owns that tile) or has neither a progress bar nor remaining-time copy.
 */
internal fun resolveTileProgress(
    fields: NotificationProgressFields,
    nowMs: Long,
): TileProgressInfo? {
    if (fields.hasMediaSession) return null
    val hasDeterminateBar = fields.progressMax > 0
    val hasIndeterminateBar = fields.indeterminate
    val countdownEndsAtMs = if (
        fields.showChronometer &&
        fields.chronometerCountDown &&
        fields.whenMs > nowMs
    ) {
        fields.whenMs
    } else {
        null
    }
    val texts = listOfNotNull(
        fields.text,
        fields.bigText,
        fields.subText,
        fields.infoText,
        fields.title,
    )
    val remainingLabel = texts.firstNotNullOfOrNull { parseRemainingPhrase(it) }
    val hasProgress = hasDeterminateBar || hasIndeterminateBar ||
        countdownEndsAtMs != null ||
        (fields.ongoing && remainingLabel != null)
    if (!hasProgress) return null

    val title = fields.title?.takeIf { it.isNotBlank() }
        ?.takeUnless { parseRemainingPhrase(it) != null && remainingLabel == it }
    val percent = if (hasDeterminateBar) {
        ((fields.progress.toLong() * 100L) / fields.progressMax.toLong())
            .toInt()
            .coerceIn(0, 100)
            .toString() + "%"
    } else {
        null
    }
    val rawBody = fields.text ?: fields.subText ?: fields.infoText ?: fields.bigText
    val statusBody = when {
        rawBody.isNullOrBlank() -> percent
        remainingLabel != null && remainingLabel.equals(rawBody.trim(), ignoreCase = true) ->
            percent
        remainingLabel != null && remainingLabel in rawBody ->
            rawBody.takeUnless { it.equals(title, ignoreCase = true) } ?: percent
        rawBody.equals(title, ignoreCase = true) -> percent
        else -> rawBody
    }

    return TileProgressInfo(
        progress = fields.progress,
        progressMax = fields.progressMax,
        indeterminate = fields.indeterminate && !hasDeterminateBar,
        statusTitle = title,
        statusBody = statusBody,
        remainingLabel = remainingLabel,
        countdownEndsAtMs = countdownEndsAtMs,
    )
}

internal fun parseRemainingPhrase(raw: String?): String? {
    val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    remainingAfterDuration.findAll(text).lastOrNull()?.groupValues?.getOrNull(1)?.let { duration ->
        val compact = collapseWs(duration)
        if (compact.isNotEmpty()) return "$compact remaining"
    }
    remainingBeforeDuration.findAll(text).lastOrNull()?.groupValues?.getOrNull(1)?.let { duration ->
        val compact = collapseWs(duration)
        if (compact.isNotEmpty()) return "$compact remaining"
    }
    remainingClock.findAll(text).lastOrNull()?.groupValues?.getOrNull(1)?.let { clock ->
        if (clock.isNotBlank()) return "$clock remaining"
    }
    return null
}

internal fun formatRemainingMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val days = totalSec / 86_400L
    val hours = (totalSec % 86_400L) / 3_600L
    val minutes = (totalSec % 3_600L) / 60L
    return when {
        days > 0L && hours > 0L -> "${days}d ${hours}h remaining"
        days > 0L -> "${days}d remaining"
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}m remaining"
        hours > 0L -> "${hours}h remaining"
        minutes > 0L -> "$minutes min remaining"
        else -> "less than a minute"
    }
}

private fun collapseWs(value: String): String =
    value.trim().replace(whitespace, " ")

private val whitespace = Regex("""\s+""")

private const val UNIT =
    """(?:days?|d|hours?|hrs?|hr|h|minutes?|mins?|min|m|seconds?|secs?|sec|s)"""

private const val DURATION = """(?:\d+\s*$UNIT(?:\s*,?\s*)?)+"""

/** "45 min remaining", "1h 23m left", "1 hour 5 minutes to go". */
private val remainingAfterDuration = Regex(
    """(?i)($DURATION)\s*(?:remaining|left|to\s+go)\b""",
)

/** "Time remaining: 45 min", "Remaining 1h 23m", "ETA: 12 min". */
private val remainingBeforeDuration = Regex(
    """(?i)(?:time\s+)?(?:remaining|left|eta)[:\s]+($DURATION)""",
)

/** "1:23 remaining", "01:23:45 left". */
private val remainingClock = Regex(
    """(?i)(\d{1,2}:\d{2}(?::\d{2})?)\s*(?:remaining|left)\b""",
)
