package com.metro.messaging.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Declared so the app satisfies the full default-SMS-app component contract (the role cannot be
 * granted without a `WAP_PUSH_DELIVER` receiver). MMS handling is out of scope for v1, so inbound
 * WAP push messages are intentionally ignored.
 */
class MmsWapPushDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op: text SMS only in v1.
    }
}
