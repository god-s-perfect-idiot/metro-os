package com.metro.statusbar

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Feeds [ActionNotificationStore] so Action Center can list system notifications grouped by app.
 */
class ActionNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        publishAll()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        ActionNotificationStore.clear()
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
        ActionNotificationStore.replaceAll(this, active)
    }

    companion object {
        @Volatile
        private var instance: ActionNotificationListenerService? = null

        fun clearAll() {
            val service = instance ?: return
            val active = runCatching { service.activeNotifications }.getOrNull() ?: return
            active.filter { ActionNotificationStore.isEligible(it) }.forEach { sbn ->
                runCatching {
                    service.cancelNotification(sbn.key)
                }
            }
            ActionNotificationStore.clear()
        }

        fun openNotification(itemKey: String, cancelAfterOpen: Boolean = true) {
            val service = instance ?: return
            val active = runCatching { service.activeNotifications }.getOrNull() ?: return
            val match = active.firstOrNull { sbn ->
                "${sbn.packageName}:${sbn.id}:${sbn.tag.orEmpty()}" == itemKey
            } ?: return
            val intent = match.notification.contentIntent
            runCatching {
                intent?.send()
                if (cancelAfterOpen) {
                    service.cancelNotification(match.key)
                }
            }
        }

        fun isEnabled(context: android.content.Context): Boolean {
            val expected = ComponentName(context, ActionNotificationListenerService::class.java)
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
            return flat.split(':').any {
                ComponentName.unflattenFromString(it)?.flattenToString() == expected.flattenToString()
            }
        }
    }
}
