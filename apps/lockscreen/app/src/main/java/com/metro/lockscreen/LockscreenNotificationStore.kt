package com.metro.lockscreen

import android.app.Notification
import android.service.notification.StatusBarNotification
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory unread counts keyed by package, fed by [LockscreenNotificationListenerService].
 */
object LockscreenNotificationStore {
    private val counts = ConcurrentHashMap<String, Int>()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    /** Shell packages whose FGS notifications must not drive quick status. */
    private val ignoredPackages = setOf(
        "com.metro.launcher",
        "com.metro.statusbar",
        "com.metro.navbar",
        "com.metro.notifications",
        "com.metro.volume",
        "com.metro.lockscreen",
    )

    fun countFor(packageName: String): Int = counts[packageName] ?: 0

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    fun clear() {
        counts.clear()
        notifyListeners()
    }

    fun replaceAll(active: Array<StatusBarNotification>?) {
        val next = aggregate(active)
        counts.clear()
        counts.putAll(next)
        notifyListeners()
    }

    internal fun aggregate(active: Array<StatusBarNotification>?): Map<String, Int> {
        if (active.isNullOrEmpty()) return emptyMap()
        return active
            .filter { isEligible(it) }
            .groupBy { it.packageName }
            .mapValues { (_, items) ->
                items.sumOf { item ->
                    val flags = item.notification.flags
                    @Suppress("DEPRECATION")
                    val foregroundService = flags and Notification.FLAG_FOREGROUND_SERVICE != 0
                    val ongoing = flags and Notification.FLAG_ONGOING_EVENT != 0 || foregroundService
                    if (ongoing) 0
                    else {
                        val n = item.notification.number
                        if (n > 0) n else 1
                    }
                }
            }
            .filterValues { it > 0 }
    }

    private fun isEligible(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName in ignoredPackages) return false
        val flags = sbn.notification.flags
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        return true
    }

    private fun notifyListeners() {
        listeners.forEach { it.invoke() }
    }
}
