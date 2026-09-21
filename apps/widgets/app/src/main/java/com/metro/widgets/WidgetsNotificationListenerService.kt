package com.metro.widgets

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.metro.widgets.data.NotifierTrayStore

/**
 * Feeds [NotifierTrayStore] so the Notifier live tile can cycle tray peeks like Start.
 */
class WidgetsNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        publishAll()
    }

    override fun onListenerDisconnected() {
        NotifierTrayStore.clear()
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
        NotifierTrayStore.replaceAll(this, active)
    }
}
