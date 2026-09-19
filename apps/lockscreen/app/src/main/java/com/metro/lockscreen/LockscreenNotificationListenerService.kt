package com.metro.lockscreen

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Reads active notifications so the lock quick-status row can show WP8.1-style counts
 * and Glance can flip a configured tile when a new notification arrives while locked.
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
        val active = runCatching { activeNotifications }.getOrNull()
        LockscreenNotificationStore.onNotificationPosted(sbn, active)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        publishAll()
    }

    private fun publishAll() {
        val active = runCatching { activeNotifications }.getOrNull()
        LockscreenNotificationStore.replaceAll(active)
    }
}
