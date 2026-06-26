package com.metro.messaging.data

import android.content.Context
import android.os.Build
import android.telephony.SmsManager

/**
 * Returns the appropriate [SmsManager] for the running API level. `getDefault()` is deprecated on
 * Android 12+, which prefers the system-service instance.
 */
fun obtainSmsManager(context: Context): SmsManager =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(SmsManager::class.java)
    } else {
        @Suppress("DEPRECATION")
        SmsManager.getDefault()
    }

/** Sends [body] to [address], splitting into multiple parts when it exceeds a single SMS. */
fun SmsManager.sendText(address: String, body: String) {
    val parts = divideMessage(body)
    if (parts.size > 1) {
        sendMultipartTextMessage(address, null, parts, null, null)
    } else {
        sendTextMessage(address, null, body, null, null)
    }
}
