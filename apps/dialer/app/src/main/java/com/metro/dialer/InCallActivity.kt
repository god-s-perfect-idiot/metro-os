package com.metro.dialer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import com.metro.dialer.data.DialerCallLogic
import com.metro.dialer.telecom.IncomingCallNotifier
import com.metro.dialer.telecom.MetroCallSession
import com.metro.dialer.telecom.ProximityScreenController
import com.metro.dialer.ui.InCallScreen
import com.metro.dialer.ui.IncomingCallScreen
import com.metro.ui.MetroStatusBarFullscreenEffect
import com.metro.ui.MetroSystemTheme

class InCallActivity : ComponentActivity() {
    private val proximityScreen by lazy { ProximityScreenController(this) }
    private var proximityWanted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableShowWhenLocked()
        enableEdgeToEdge()

        if (!MetroCallSession.hasActiveCall()) {
            finish()
            return
        }

        MetroCallSession.setOnCallEndedListener {
            runOnUiThread { finish() }
        }

        setContent {
            val call by MetroCallSession.activeCall
            val speakerOn by MetroCallSession.speakerOn
            val bluetoothOn by MetroCallSession.bluetoothOn

            LaunchedEffect(call) {
                if (call == null) finish()
            }

            LaunchedEffect(call, speakerOn, bluetoothOn) {
                val active = call
                proximityWanted = active != null &&
                    !DialerCallLogic.isIncomingRinging(active) &&
                    !speakerOn &&
                    !bluetoothOn
                syncProximityScreen()
            }

            MetroSystemTheme {
                MetroStatusBarFullscreenEffect(active = call != null)
                call?.let { activeCall ->
                    if (DialerCallLogic.isIncomingRinging(activeCall)) {
                        IncomingCallScreen(
                            call = activeCall,
                            onAnswer = MetroCallSession::answerCall,
                            onIgnore = {
                                MetroCallSession.rejectCall()
                                finish()
                            },
                            onTextReply = {
                                val number = activeCall.phoneNumber
                                MetroCallSession.rejectCall()
                                launchTextReply(number)
                                finish()
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        InCallScreen(
                            call = activeCall,
                            onEndCall = {
                                MetroCallSession.endCall(this@InCallActivity)
                                finish()
                            },
                            onConnected = MetroCallSession::markConnected,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Metro incoming / in-call UI is visible — dismiss Android notification chrome.
        IncomingCallNotifier.stop(this)
        MetroCallSession.hideMinimizedNotification(this)
    }

    override fun onResume() {
        super.onResume()
        syncProximityScreen()
    }

    override fun onPause() {
        proximityScreen.setEnabled(false)
        super.onPause()
    }

    override fun onStop() {
        // Start / Home minimizes an answered/outgoing call to the Metro green banner.
        // While still ringing on a locked/off screen, keep the FSI notification for wake-up.
        val call = MetroCallSession.activeCall.value
        if (call != null && !isFinishing) {
            if (DialerCallLogic.isIncomingRinging(call)) {
                if (IncomingCallNotifier.needsFullScreenIntent(this)) {
                    IncomingCallNotifier.show(this, call)
                }
            } else {
                MetroCallSession.showMinimizedNotification(this)
            }
        }
        super.onStop()
    }

    override fun onDestroy() {
        proximityWanted = false
        proximityScreen.setEnabled(false)
        MetroCallSession.setOnCallEndedListener(null)
        super.onDestroy()
    }

    private fun syncProximityScreen() {
        val resumeReady = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        proximityScreen.setEnabled(proximityWanted && resumeReady)
    }

    private fun enableShowWhenLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    private fun launchTextReply(phoneNumber: String) {
        if (phoneNumber.isBlank()) return
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.messaging_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}
