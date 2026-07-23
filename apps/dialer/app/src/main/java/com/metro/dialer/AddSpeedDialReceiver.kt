package com.metro.dialer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.metro.dialer.data.SpeedDialEntry
import com.metro.dialer.data.SpeedDialStore
import com.metro.system.MetroIntents

/**
 * Receives [MetroIntents.ACTION_ADD_SPEED_DIAL] from People (and other Metro apps).
 */
class AddSpeedDialReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != MetroIntents.ACTION_ADD_SPEED_DIAL) return
        val phoneNumber = intent.getStringExtra(MetroIntents.EXTRA_PHONE_NUMBER)?.trim().orEmpty()
        if (phoneNumber.isEmpty()) return
        val displayName = intent.getStringExtra(MetroIntents.EXTRA_DISPLAY_NAME)
            ?.trim()
            .orEmpty()
            .ifEmpty { phoneNumber }
        val store = SpeedDialStore(context.applicationContext)
        store.add(
            SpeedDialEntry(
                id = System.currentTimeMillis().toString(),
                displayName = displayName,
                phoneNumber = phoneNumber,
            ),
        )
    }
}
