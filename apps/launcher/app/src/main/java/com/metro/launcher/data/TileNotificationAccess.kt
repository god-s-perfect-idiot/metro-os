package com.metro.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

/** Helpers for Android notification-listener access (required to drive live-tile peeks). */
object TileNotificationAccess {
    fun isEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        if (TextUtils.isEmpty(flat)) return false
        val expected = ComponentName(context, com.metro.launcher.TileNotificationListenerService::class.java)
        return flat.split(':').any { piece ->
            val component = ComponentName.unflattenFromString(piece) ?: return@any false
            component == expected
        }
    }

    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
