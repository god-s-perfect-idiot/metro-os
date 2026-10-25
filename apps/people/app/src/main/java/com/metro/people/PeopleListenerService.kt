package com.metro.people

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils

/**
 * Notification access for People what's-new (connected conversation apps in the shade).
 * Appears as **People** in system notification-access settings.
 */
class PeopleListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        notifyChanged()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        super.onListenerDisconnected()
        notifyChanged()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        notifyChanged()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        notifyChanged()
    }

    companion object {
        @Volatile
        private var instance: PeopleListenerService? = null

        @Volatile
        private var changeListener: (() -> Unit)? = null

        fun setChangeListener(listener: (() -> Unit)?) {
            changeListener = listener
        }

        fun isNotificationAccessEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
            if (TextUtils.isEmpty(flat)) return false
            val expected = ComponentName(context, PeopleListenerService::class.java)
            return flat.split(':').any { piece ->
                val component = ComponentName.unflattenFromString(piece) ?: return@any false
                component == expected
            }
        }

        fun notificationAccessSettingsIntent(): Intent =
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

        fun activeNotificationsOrEmpty(): Array<StatusBarNotification> {
            val service = instance ?: return emptyArray()
            return runCatching { service.activeNotifications }.getOrNull() ?: emptyArray()
        }

        fun findActive(key: String): StatusBarNotification? =
            activeNotificationsOrEmpty().firstOrNull { it.key == key }

        private fun notifyChanged() {
            changeListener?.invoke()
        }
    }
}
