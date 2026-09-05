package com.metro.volume

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Notification listener used only to authorize [android.media.session.MediaSessionManager]
 * access for non–suite music apps. Notifications themselves are ignored.
 */
class VolumeMediaNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        VolumeMediaSessionStore.bindNotificationListener(
            context = this,
            listenerComponent = ComponentName(this, VolumeMediaNotificationListenerService::class.java),
        )
    }

    override fun onListenerDisconnected() {
        VolumeMediaSessionStore.unbindNotificationListener()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = Unit

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = Unit
}
