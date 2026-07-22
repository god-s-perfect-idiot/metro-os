package com.metro.dialer.telecom

import android.os.Handler
import android.os.Looper
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

class MetroConnectionService : ConnectionService() {
    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection = MetroConnection(request, outgoing = true)

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection = MetroConnection(request, outgoing = false)
}

private class MetroConnection(
    request: ConnectionRequest?,
    private val outgoing: Boolean,
) : Connection() {
    private val handler = Handler(Looper.getMainLooper())

    init {
        val address = request?.address
        if (address != null) {
            setAddress(address, TelecomManager.PRESENTATION_ALLOWED)
            setCallerDisplayName(
                address.schemeSpecificPart.orEmpty(),
                TelecomManager.PRESENTATION_ALLOWED,
            )
        }
        connectionCapabilities = CAPABILITY_HOLD or CAPABILITY_SUPPORT_HOLD or CAPABILITY_MUTE
        if (outgoing) {
            setDialing()
            handler.postDelayed({
                if (state == STATE_DISCONNECTED) return@postDelayed
                setActive()
            }, 1500)
        } else {
            setRinging()
        }
    }

    override fun onAnswer() {
        setActive()
    }

    override fun onReject() {
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }

    override fun onDisconnect() {
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onAbort() {
        setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
        destroy()
    }
}
