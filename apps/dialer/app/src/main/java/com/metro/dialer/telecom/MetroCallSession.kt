package com.metro.dialer.telecom

import android.telecom.Call
import androidx.compose.runtime.mutableStateOf
import com.metro.dialer.data.ActiveCall

/**
 * Shared call state between [MetroInCallService] and [com.metro.dialer.InCallActivity].
 */
object MetroCallSession {
    private val _activeCall = mutableStateOf<ActiveCall?>(null)
    val activeCall = _activeCall

    private var telecomCall: Call? = null
    private var onCallEnded: (() -> Unit)? = null

    fun bindTelecomCall(call: Call, displayName: String) {
        telecomCall = call
        val number = call.details.handle?.schemeSpecificPart?.trim().orEmpty()
        _activeCall.value = ActiveCall(
            phoneNumber = number,
            displayName = displayName,
            startedAtMillis = System.currentTimeMillis(),
            connected = call.state == Call.STATE_ACTIVE,
        )
    }

    fun startLocalCall(phoneNumber: String, displayName: String) {
        telecomCall = null
        _activeCall.value = ActiveCall(
            phoneNumber = phoneNumber,
            displayName = displayName,
            startedAtMillis = System.currentTimeMillis(),
            connected = false,
        )
    }

    fun markConnected() {
        _activeCall.value = _activeCall.value?.copy(connected = true)
    }

    fun endCall() {
        telecomCall?.disconnect()
        clear()
    }

    fun clear() {
        telecomCall = null
        _activeCall.value = null
        onCallEnded?.invoke()
    }

    fun setOnCallEndedListener(listener: (() -> Unit)?) {
        onCallEnded = listener
    }

    fun hasActiveCall(): Boolean = _activeCall.value != null
}
