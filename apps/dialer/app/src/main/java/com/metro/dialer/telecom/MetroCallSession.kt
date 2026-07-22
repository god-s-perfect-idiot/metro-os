package com.metro.dialer.telecom

import android.telecom.Call
import android.telecom.VideoProfile
import androidx.compose.runtime.mutableStateOf
import com.metro.dialer.data.ActiveCall
import com.metro.dialer.data.CallDirection

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
            direction = resolveDirection(call),
            connected = call.state == Call.STATE_ACTIVE,
        )
    }

    fun startLocalCall(phoneNumber: String, displayName: String) {
        telecomCall = null
        _activeCall.value = ActiveCall(
            phoneNumber = phoneNumber,
            displayName = displayName,
            startedAtMillis = System.currentTimeMillis(),
            direction = CallDirection.Outgoing,
            connected = false,
        )
    }

    fun markConnected() {
        _activeCall.value = _activeCall.value?.copy(
            connected = true,
            startedAtMillis = System.currentTimeMillis(),
        )
    }

    fun answerCall() {
        val call = telecomCall
        if (call != null) {
            call.answer(VideoProfile.STATE_AUDIO_ONLY)
        } else {
            markConnected()
        }
    }

    fun rejectCall() {
        val call = telecomCall
        if (call != null) {
            when (call.state) {
                Call.STATE_RINGING -> call.reject(false, null)
                else -> call.disconnect()
            }
        }
        clear()
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

    private fun resolveDirection(call: Call): CallDirection {
        return when (call.details.callDirection) {
            Call.Details.DIRECTION_INCOMING -> CallDirection.Incoming
            Call.Details.DIRECTION_OUTGOING -> CallDirection.Outgoing
            else -> when (call.state) {
                Call.STATE_RINGING -> CallDirection.Incoming
                else -> CallDirection.Outgoing
            }
        }
    }
}
