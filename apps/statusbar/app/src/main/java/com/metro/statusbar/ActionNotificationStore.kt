package com.metro.statusbar

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory Action Center notifications, grouped by app (newest group first, newest item first).
 * Fed by [ActionNotificationListenerService].
 */
object ActionNotificationStore {
    /** Shell FGS / overlay notifications must never appear in Action Center. */
    val IgnoredPackages: Set<String> = setOf(
        "com.metro.statusbar",
        "com.metro.navbar",
        "com.metro.launcher",
    )

    private val groups = CopyOnWriteArrayList<ActionNotificationGroup>()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun all(): List<ActionNotificationGroup> = groups.toList()

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    fun clear() {
        groups.clear()
        notifyListeners()
    }

    fun replaceAll(context: Context, active: Array<StatusBarNotification>?) {
        groups.clear()
        groups.addAll(aggregate(context, active))
        notifyListeners()
    }

    internal fun aggregate(
        context: Context,
        active: Array<StatusBarNotification>?,
    ): List<ActionNotificationGroup> {
        if (active.isNullOrEmpty()) return emptyList()
        val eligible = active.filter { isEligible(it) }
        if (eligible.isEmpty()) return emptyList()
        val pm = context.packageManager
        return eligible
            .groupBy { it.packageName }
            .map { (packageName, items) ->
                val sorted = items.sortedByDescending { it.postTime }
                ActionNotificationGroup(
                    packageName = packageName,
                    appLabel = appLabel(pm, packageName),
                    items = sorted.map { toItem(it) },
                )
            }
            .sortedByDescending { group -> group.items.maxOfOrNull { it.postedAtMs } ?: 0L }
    }

    internal fun isEligible(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName in IgnoredPackages) return false
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        // Ongoing / FGS chrome is not toast-style Action Center content.
        if (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return false
        return true
    }

    internal fun formatTime(postedAtMs: Long, now: ZonedDateTime = ZonedDateTime.now()): String {
        val posted = Instant.ofEpochMilli(postedAtMs).atZone(ZoneId.systemDefault())
        // WP8.1 Action Center: "12:49p" / "7:54a"
        val pattern = if (posted.toLocalDate() == now.toLocalDate()) "h:mma" else "M/d"
        val raw = DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(posted)
        return raw.replace("AM", "a").replace("PM", "p").replace("am", "a").replace("pm", "p")
    }

    private fun toItem(sbn: StatusBarNotification): ActionNotificationItem {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
            .ifEmpty { extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.trim().orEmpty() }
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
        return ActionNotificationItem(
            key = "${sbn.packageName}:${sbn.id}:${sbn.tag.orEmpty()}",
            packageName = sbn.packageName,
            title = title.ifEmpty { body.orEmpty() }.ifEmpty { sbn.packageName },
            body = if (title.isEmpty()) null else body,
            timeText = formatTime(sbn.postTime),
            postedAtMs = sbn.postTime,
        )
    }

    private fun appLabel(pm: PackageManager, packageName: String): String =
        try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }

    private fun notifyListeners() {
        listeners.forEach { it() }
    }
}
