package com.metro.launcher

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.metro.launcher.data.TileNotificationInfo
import com.metro.launcher.data.TileNotificationStore
import com.metro.system.MetroTileUpdates

/**
 * Reads active notifications for every package and feeds [TileNotificationStore] so Start live
 * tiles can show WP8.1-style badges and flip/peek faces.
 */
class TileNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        publishAll()
    }

    override fun onListenerDisconnected() {
        TileNotificationStore.clear()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        publishAll()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        publishAll()
    }

    private fun publishAll() {
        val previous = TileNotificationStore.all()
        val active = runCatching { activeNotifications }.getOrNull()
        TileNotificationStore.replaceAll(active)
        val next = TileNotificationStore.all()
        changedPackages(previous, next).forEach { packageName ->
            MetroTileUpdates.requestUpdate(this, packageName)
        }
    }

    companion object {
        internal fun changedPackages(
            previous: Map<String, TileNotificationInfo>,
            next: Map<String, TileNotificationInfo>,
        ): Set<String> {
            val keys = previous.keys + next.keys
            return keys.filter { previous[it] != next[it] }.toSet()
        }
    }
}
