package com.metro.dialer.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles the End action on the active-call notification. */
class EndCallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_END) return
        MetroCallSession.endCall(context)
    }

    companion object {
        const val ACTION_END = "com.metro.dialer.action.END_ACTIVE_CALL"
    }
}
