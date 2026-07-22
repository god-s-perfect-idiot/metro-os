package com.metro.dialer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.metro.dialer.data.DialerCallLogic
import com.metro.dialer.telecom.MetroCallSession
import com.metro.dialer.ui.InCallScreen
import com.metro.dialer.ui.IncomingCallScreen
import com.metro.ui.MetroTheme

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

            LaunchedEffect(call) {
                if (call == null) finish()
            }

            MetroTheme {
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
                                MetroCallSession.endCall()
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

    override fun onDestroy() {
        MetroCallSession.setOnCallEndedListener(null)
        super.onDestroy()
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
