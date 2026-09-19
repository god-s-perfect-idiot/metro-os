package com.metro.lockscreen

import android.app.Notification
import android.service.notification.StatusBarNotification
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory unread counts / peeks keyed by package, fed by [LockscreenNotificationListenerService].
 *
 * [flipGenerationFor] bumps when a new eligible notification is posted so Glance can flip that
 * quick-status tile — reconnect / remove / replace snapshots do not bump.
 */
object LockscreenNotificationStore {
    data class PackageStatus(
        val count: Int,
        val peekTitle: String = "",
        val peekBody: String? = null,
        val flipGeneration: Long = 0L,
    )

    /** Framework-free snapshot used by [aggregate] / unit tests. */
    internal data class ActiveNotification(
        val packageName: String,
        val number: Int,
        val ongoing: Boolean,
        val groupSummary: Boolean,
        val postTime: Long,
        val title: String,
        val body: String?,
    )

    private val statuses = ConcurrentHashMap<String, PackageStatus>()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val flipClock = AtomicLong(0L)

    /** Shell packages whose FGS notifications must not drive quick status. */
    private val ignoredPackages = setOf(
        "com.metro.launcher",
        "com.metro.statusbar",
        "com.metro.navbar",
        "com.metro.notifications",
        "com.metro.volume",
        "com.metro.lockscreen",
    )

    fun countFor(packageName: String): Int = statuses[packageName]?.count ?: 0

    fun statusFor(packageName: String): PackageStatus =
        statuses[packageName] ?: PackageStatus(count = 0)

    fun flipGenerationFor(packageName: String): Long =
        statuses[packageName]?.flipGeneration ?: 0L

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    fun clear() {
        statuses.clear()
        notifyListeners()
    }

    fun replaceAll(active: Array<StatusBarNotification>?) {
        replaceAllMapped(active?.map { it.toActive() })
    }

    /**
     * Full snapshot refresh plus a flip bump for [posted] when it is eligible for quick status.
     */
    fun onNotificationPosted(
        posted: StatusBarNotification?,
        active: Array<StatusBarNotification>?,
    ) {
        onNotificationPostedMapped(
            posted = posted?.toActive(),
            active = active?.map { it.toActive() },
        )
    }

    internal fun replaceAllMapped(active: List<ActiveNotification>?) {
        val previousFlip = statuses.mapValues { it.value.flipGeneration }
        val next = aggregate(active)
        statuses.clear()
        next.forEach { (pkg, status) ->
            statuses[pkg] = status.copy(flipGeneration = previousFlip[pkg] ?: 0L)
        }
        notifyListeners()
    }

    internal fun onNotificationPostedMapped(
        posted: ActiveNotification?,
        active: List<ActiveNotification>?,
    ) {
        replaceAllMapped(active)
        if (posted != null && isEligible(posted) && !posted.ongoing) {
            bumpFlip(posted.packageName, posted.title to posted.body)
        }
    }

    internal fun aggregate(active: Array<StatusBarNotification>?): Map<String, PackageStatus> =
        aggregate(active?.map { it.toActive() })

    internal fun aggregate(active: List<ActiveNotification>?): Map<String, PackageStatus> {
        if (active.isNullOrEmpty()) return emptyMap()
        return active
            .filter { isEligible(it) }
            .groupBy { it.packageName }
            .mapNotNull { (pkg, items) ->
                var count = 0
                var newest: ActiveNotification? = null
                for (item in items) {
                    if (item.ongoing) continue
                    count += if (item.number > 0) item.number else 1
                    if (newest == null || item.postTime >= newest.postTime) {
                        newest = item
                    }
                }
                if (count <= 0) return@mapNotNull null
                pkg to PackageStatus(
                    count = count,
                    peekTitle = newest?.title.orEmpty(),
                    peekBody = newest?.body,
                )
            }
            .toMap()
    }

    private fun bumpFlip(packageName: String, peek: Pair<String, String?>) {
        val current = statuses[packageName] ?: return
        val gen = flipClock.incrementAndGet()
        statuses[packageName] = current.copy(
            peekTitle = peek.first,
            peekBody = peek.second,
            flipGeneration = gen,
        )
        notifyListeners()
    }

    private fun isEligible(item: ActiveNotification): Boolean {
        if (item.packageName in ignoredPackages) return false
        if (item.groupSummary) return false
        return true
    }

    private fun StatusBarNotification.toActive(): ActiveNotification {
        val flags = notification.flags
        @Suppress("DEPRECATION")
        val foregroundService = flags and Notification.FLAG_FOREGROUND_SERVICE != 0
        val ongoing = flags and Notification.FLAG_ONGOING_EVENT != 0 || foregroundService
        val extras = notification.extras
        return ActiveNotification(
            packageName = packageName,
            number = notification.number,
            ongoing = ongoing,
            groupSummary = flags and Notification.FLAG_GROUP_SUMMARY != 0,
            postTime = postTime,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty(),
            body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() },
        )
    }

    private fun notifyListeners() {
        listeners.forEach { it.invoke() }
    }
}
