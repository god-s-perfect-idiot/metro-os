package com.metro.messaging.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Declared so the app satisfies the full default-SMS-app component contract (the role cannot be
 * granted without a `WAP_PUSH_DELIVER` receiver). Already-stored MMS parts are shown in the
 * conversation UI; downloading new MMS from the carrier MMSC is still out of scope, so inbound
 * WAP push is ignored here.
 */
class MmsWapPushDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op: read downloaded MMS from Telephony; do not fetch from MMSC yet.
    }
}
