package com.metro.messaging.sms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.metro.messaging.tiles.MessagingTileRefresh

/**
 * Handles `SMS_DELIVER`, the broadcast delivered only to the **default SMS app**. The platform no
 * longer writes inbound messages to the provider automatically for the default app, so we persist
 * them to [Telephony.Sms.Inbox] here. Demo/non-default installs never receive this broadcast.
 */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val address = messages.first().displayOriginatingAddress ?: return
        val body = buildString {
            messages.forEach { part ->
                append(part.displayMessageBody ?: part.messageBody.orEmpty())
            }
        }
        val timestamp = messages.first().timestampMillis

        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, timestamp)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            runCatching {
                put(Telephony.Sms.THREAD_ID, Telephony.Threads.getOrCreateThreadId(context, address))
            }
        }

        runCatching {
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        }
        MessagingTileRefresh.request(context)
    }
}
