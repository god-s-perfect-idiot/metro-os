package com.metro.notifications

import android.app.Notification
import android.app.NotificationManager

/**
 * Whether an Android notification should raise a WP8.1 toast banner.
 *
 * Mirrors SystemUI peek rules as closely as a listener can: HIGH+ importance, interruption
 * filter match, interactive screen, not ongoing / shell FGS / active call.
 *
 * Group summaries are allowed: many apps only alert on the summary, so skipping them left
 * only the first child peeking. Callers debounce duplicate peeks for the same group key.
 */
object ToastDecision {
    fun shouldShow(
        packageName: String,
        flags: Int,
        importance: Int,
        matchesInterruptionFilter: Boolean,
        screenInteractive: Boolean,
        isActiveCall: Boolean,
        onlyAlertOnceAlreadyShown: Boolean,
        alreadySeenWithoutAlert: Boolean,
        ignoredPackages: Set<String> = ToastSpec.IgnoredPackages,
    ): Boolean {
        if (!screenInteractive) return false
        if (!matchesInterruptionFilter) return false
        if (packageName in ignoredPackages) return false
        if (isActiveCall) return false
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return false
        if (alreadySeenWithoutAlert) return false
        if (onlyAlertOnceAlreadyShown) return false
        if (importance < NotificationManager.IMPORTANCE_HIGH) return false
        return true
    }
}

data class ToastSnapshot(
    val key: String,
    val packageName: String,
    val title: String,
    val body: String?,
    /** Group / conversation name shown above title when present. */
    val groupTitle: String? = null,
) {
    /** Top row for group chats; null for 1:1 / non-conversation posts. */
    fun displayGroupRow(): String? =
        groupTitle?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * Message toast copy next to the icon: `sender: message` when both parts exist
     * (Messaging + social). Rendered as one line with ellipsis when it overflows.
     * Group name is rendered separately above this line.
     */
    fun displayLine(): String {
        val t = title.trim()
        val b = body?.trim().orEmpty()
        return when {
            t.isEmpty() -> b
            b.isEmpty() -> t
            b.startsWith(t, ignoreCase = true) -> b
            t.startsWith(b, ignoreCase = true) -> t
            else -> "$t: $b"
        }
    }

    /** Title row when two-row view is on (falls back to the single-line join when body is absent). */
    fun displayTitleRow(): String {
        val t = title.trim()
        if (t.isNotEmpty()) return t
        return body?.trim().orEmpty()
    }

    /**
     * Body row when two-row view is on. Null when there is no distinct body (redundant or empty),
     * so the banner can stay shorter.
     */
    fun displayBodyRow(): String? {
        val t = title.trim()
        val b = body?.trim().orEmpty()
        if (t.isEmpty() || b.isEmpty()) return null
        if (b.startsWith(t, ignoreCase = true) || t.startsWith(b, ignoreCase = true)) return null
        return b
    }
}
