package com.metro.lockscreen

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Reads active notifications so the lock quick-status row can show WP8.1-style counts.
 */
class LockscreenNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        publishAll()
    }

    override fun onListenerDisconnected() {
        LockscreenNotificationStore.clear()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        publishAll()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        publishAll()
    }

    private fun publishAll() {
        val active = runCatching { activeNotifications }.getOrNull()
        LockscreenNotificationStore.replaceAll(active)
    }
}
