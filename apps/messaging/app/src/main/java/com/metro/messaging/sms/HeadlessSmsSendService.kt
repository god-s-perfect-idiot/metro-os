package com.metro.messaging.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.metro.messaging.data.obtainSmsManager
import com.metro.messaging.data.sendText

/**
 * Handles `RESPOND_VIA_MESSAGE` quick replies (e.g. dismissing an incoming call with a canned SMS).
 * Required for the default-SMS-app role. Sends the supplied text to every recipient encoded in the
 * intent's `smsto:`/`mmsto:` URI.
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESPOND_VIA_MESSAGE) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
            val recipients = recipientsFrom(intent)
            if (text.isNotEmpty() && recipients.isNotEmpty()) {
                val smsManager = obtainSmsManager(applicationContext)
                recipients.forEach { recipient ->
                    runCatching { smsManager.sendText(recipient, text) }
                }
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun recipientsFrom(intent: Intent): List<String> {
        val raw = intent.data?.schemeSpecificPart ?: return emptyList()
        return raw.split(';', ',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private companion object {
        const val ACTION_RESPOND_VIA_MESSAGE = "android.intent.action.RESPOND_VIA_MESSAGE"
    }
}
