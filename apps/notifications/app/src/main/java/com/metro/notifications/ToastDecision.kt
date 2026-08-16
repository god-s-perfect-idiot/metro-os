package com.metro.notifications

import android.app.Notification
import android.app.NotificationManager

/**
 * Whether an Android notification should raise a WP8.1 toast banner.
 *
 * Mirrors SystemUI peek rules as closely as a listener can: HIGH+ importance, interruption
 * filter match, interactive screen, not ongoing / group summary / shell FGS / active call.
 */
object ToastDecision {
    fun shouldShow(
        packageName: String,
        flags: Int,
        importance: Int,
        matchesInterruptionFilter: Boolean,
        screenInteractive: Boolean,
        isGroupSummary: Boolean,
        isActiveCall: Boolean,
        onlyAlertOnceAlreadyShown: Boolean,
        ignoredPackages: Set<String> = ToastSpec.IgnoredPackages,
    ): Boolean {
        if (!screenInteractive) return false
        if (!matchesInterruptionFilter) return false
        if (packageName in ignoredPackages) return false
        if (isGroupSummary) return false
        if (isActiveCall) return false
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return false
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
) {
    /**
     * Message toast copy next to the icon: `sender: message` when both parts exist
     * (Messaging + social). Rendered as one line with ellipsis when it overflows.
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
}
