package com.metro.dialer.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.metro.dialer.InCallActivity
import com.metro.dialer.data.CallLogRepository
import com.metro.dialer.data.DialerCallLogic

class MetroInCallService : InCallService() {
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            MetroCallSession.onCallStateChanged(state)
        }
    }

    override fun onCreate() {
        super.onCreate()
        MetroCallSession.bindInCallService(this)
    }

    override fun onDestroy() {
        MetroCallSession.unbindInCallService(this)
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callCallback)
        val number = call.details.handle?.schemeSpecificPart?.trim().orEmpty()
        val displayName = resolveDisplayName(number)
        MetroCallSession.bindTelecomCall(call, displayName)

        val isIncoming = when (call.details.callDirection) {
            Call.Details.DIRECTION_INCOMING -> true
            Call.Details.DIRECTION_OUTGOING -> false
            else -> call.state == Call.STATE_RINGING
        }
        if (isIncoming && call.state != Call.STATE_ACTIVE) {
            val intent = Intent(this, InCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            // Unlocked/interactive: Metro incoming UI only (no Android heads-up).
            // Locked/screen-off: FSI notification brings up InCallActivity.
            if (IncomingCallNotifier.needsFullScreenIntent(this)) {
                MetroCallSession.activeCall.value?.let { IncomingCallNotifier.show(this, it) }
            }
            runCatching { startActivity(intent) }
        }
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callCallback)
        super.onCallRemoved(call)
        if (MetroCallSession.hasActiveCall()) {
            MetroCallSession.clear()
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        MetroCallSession.onCallAudioStateChanged(audioState)
    }

    private fun resolveDisplayName(number: String): String {
        if (number.isEmpty()) return number
        val repository = CallLogRepository(applicationContext)
        val recent = repository.loadRecentCalls()
        val fromLog = recent.firstOrNull {
            DialerCallLogic.normalizeNumber(it.phoneNumber) ==
                DialerCallLogic.normalizeNumber(number)
        }?.contactName
        return fromLog?.takeIf { it.isNotBlank() }
            ?: DialerCallLogic.formatDisplayNumber(number)
    }
}
