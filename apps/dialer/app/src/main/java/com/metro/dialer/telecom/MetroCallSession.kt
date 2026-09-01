package com.metro.dialer.telecom

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.compose.runtime.mutableStateOf
import com.metro.dialer.data.ActiveCall
import com.metro.dialer.data.CallDirection
import com.metro.system.MetroLockscreen

/**
 * Shared call state between [MetroInCallService] and [com.metro.dialer.InCallActivity].
 */
object MetroCallSession {
    private val _activeCall = mutableStateOf<ActiveCall?>(null)
    val activeCall = _activeCall

    private val _muted = mutableStateOf(false)
    val muted = _muted

    private val _speakerOn = mutableStateOf(false)
    val speakerOn = _speakerOn

    private val _bluetoothOn = mutableStateOf(false)
    val bluetoothOn = _bluetoothOn

    private val _onHold = mutableStateOf(false)
    val onHold = _onHold

    private val _bluetoothAvailable = mutableStateOf(false)
    val bluetoothAvailable = _bluetoothAvailable

    private var telecomCall: Call? = null
    private var inCallService: InCallService? = null
    private var onCallEnded: (() -> Unit)? = null
    private var appContext: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun bindInCallService(service: InCallService) {
        inCallService = service
        appContext = service.applicationContext
    }

    fun unbindInCallService(service: InCallService) {
        if (inCallService === service) {
            inCallService = null
        }
    }

    fun bindTelecomCall(call: Call, displayName: String) {
        telecomCall = call
        val number = call.details.handle?.schemeSpecificPart?.trim().orEmpty()
        val direction = resolveDirection(call)
        _activeCall.value = ActiveCall(
            phoneNumber = number,
            displayName = displayName,
            startedAtMillis = System.currentTimeMillis(),
            direction = direction,
            connected = call.state == Call.STATE_ACTIVE,
        )
        syncHoldFromCallState(call.state)
        syncIncomingRingtone(call.state, direction)
    }

    fun startLocalCall(phoneNumber: String, displayName: String) {
        telecomCall = null
        IncomingRingtonePlayer.stop()
        resetAudioUiState()
        _activeCall.value = ActiveCall(
            phoneNumber = phoneNumber,
            displayName = displayName,
            startedAtMillis = System.currentTimeMillis(),
            direction = CallDirection.Outgoing,
            connected = false,
        )
    }

    fun markConnected() {
        IncomingRingtonePlayer.stop()
        val updated = _activeCall.value?.copy(
            connected = true,
            startedAtMillis = System.currentTimeMillis(),
        ) ?: return
        _activeCall.value = updated
        _onHold.value = false
        appContext?.let { ActiveCallNotifier.update(it, updated) }
    }

    /** Post the Metro return-to-call notification while the in-call UI is backgrounded. */
    fun showMinimizedNotification(context: Context) {
        val call = _activeCall.value ?: return
        appContext = context.applicationContext
        ActiveCallNotifier.start(context, call)
    }

    fun hideMinimizedNotification(context: Context) {
        ActiveCallNotifier.stop(context)
    }

    fun onCallStateChanged(state: Int) {
        when (state) {
            Call.STATE_ACTIVE -> {
                IncomingRingtonePlayer.stop()
                appContext?.let { IncomingCallNotifier.stop(it) }
                if (_activeCall.value?.connected != true) {
                    markConnected()
                } else {
                    _onHold.value = false
                }
            }
            Call.STATE_HOLDING -> {
                IncomingRingtonePlayer.stop()
                _onHold.value = true
            }
            Call.STATE_DISCONNECTED -> {
                IncomingRingtonePlayer.stop()
                if (hasActiveCall()) clear()
            }
            else -> syncIncomingRingtone(state, _activeCall.value?.direction)
        }
    }

    fun onCallAudioStateChanged(state: CallAudioState?) {
        if (state == null) return
        _muted.value = state.isMuted
        _speakerOn.value = state.route == CallAudioState.ROUTE_SPEAKER
        _bluetoothOn.value = state.route == CallAudioState.ROUTE_BLUETOOTH
        _bluetoothAvailable.value =
            (state.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH) != 0
    }

    fun answerCall() {
        IncomingRingtonePlayer.stop()
        appContext?.let { IncomingCallNotifier.stop(it) }
        val call = telecomCall
        if (call != null) {
            call.answer(VideoProfile.STATE_AUDIO_ONLY)
        } else {
            markConnected()
        }
    }

    fun rejectCall() {
        IncomingRingtonePlayer.stop()
        appContext?.let { IncomingCallNotifier.stop(it) }
        val call = telecomCall
        if (call != null) {
            when (call.state) {
                Call.STATE_RINGING -> call.reject(false, null)
                else -> call.disconnect()
            }
        }
        clear()
    }

    fun endCall(context: Context? = null) {
        IncomingRingtonePlayer.stop()
        telecomCall?.disconnect()
        context?.let { resetPlatformAudio(it) }
        clear()
    }

    fun setMuted(context: Context, muted: Boolean) {
        _muted.value = muted
        val service = inCallService
        if (service != null) {
            service.setMuted(muted)
        } else {
            audioManager(context)?.isMicrophoneMute = muted
        }
    }

    fun setSpeaker(context: Context, on: Boolean) {
        _speakerOn.value = on
        if (on) _bluetoothOn.value = false
        val service = inCallService
        if (service != null) {
            service.setAudioRoute(
                if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE,
            )
        } else {
            audioManager(context)?.let { am ->
                @Suppress("DEPRECATION")
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = on
            }
        }
    }

    fun setBluetooth(context: Context, on: Boolean) {
        if (!_bluetoothAvailable.value && on) return
        _bluetoothOn.value = on
        if (on) _speakerOn.value = false
        val service = inCallService
        if (service != null) {
            service.setAudioRoute(
                if (on) CallAudioState.ROUTE_BLUETOOTH else CallAudioState.ROUTE_EARPIECE,
            )
        } else {
            audioManager(context)?.let { am ->
                @Suppress("DEPRECATION")
                if (on) {
                    am.startBluetoothSco()
                    am.isBluetoothScoOn = true
                } else {
                    am.isBluetoothScoOn = false
                    am.stopBluetoothSco()
                }
            }
        }
    }

    fun setOnHold(held: Boolean) {
        val call = telecomCall
        if (call != null) {
            if (held) call.hold() else call.unhold()
        }
        _onHold.value = held
    }

    fun playDtmf(digit: Char) {
        val call = telecomCall ?: return
        call.playDtmfTone(digit)
        mainHandler.postDelayed({ call.stopDtmfTone() }, 160L)
    }

    fun clear() {
        IncomingRingtonePlayer.stop()
        telecomCall = null
        resetAudioUiState()
        _activeCall.value = null
        appContext?.let {
            MetroLockscreen.requestSuppress(it, false)
            IncomingCallNotifier.stop(it)
            ActiveCallNotifier.stop(it)
        }
        onCallEnded?.invoke()
    }

    fun setOnCallEndedListener(listener: (() -> Unit)?) {
        onCallEnded = listener
    }

    fun hasActiveCall(): Boolean = _activeCall.value != null

    /**
     * Starts the system ringtone for an unanswered incoming call.
     * [MetroInCallService] owns ringing (`IN_CALL_SERVICE_RINGING`), so the OS will not.
     */
    private fun syncIncomingRingtone(state: Int, direction: CallDirection?) {
        val context = appContext ?: return
        val shouldRing = direction == CallDirection.Incoming &&
            state != Call.STATE_ACTIVE &&
            state != Call.STATE_DISCONNECTED &&
            state != Call.STATE_HOLDING
        if (shouldRing) {
            IncomingRingtonePlayer.start(context)
        } else {
            IncomingRingtonePlayer.stop()
        }
    }

    private fun resetAudioUiState() {
        _muted.value = false
        _speakerOn.value = false
        _bluetoothOn.value = false
        _onHold.value = false
        _bluetoothAvailable.value = false
    }

    private fun resetPlatformAudio(context: Context) {
        audioManager(context)?.let { am ->
            am.isMicrophoneMute = false
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn = false
            @Suppress("DEPRECATION")
            if (am.isBluetoothScoOn) {
                am.isBluetoothScoOn = false
                am.stopBluetoothSco()
            }
        }
    }

    private fun syncHoldFromCallState(state: Int) {
        _onHold.value = state == Call.STATE_HOLDING
    }

    private fun audioManager(context: Context): AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

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
