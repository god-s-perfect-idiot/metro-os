package com.metro.dialer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.metro.dialer.telecom.MetroCallSession
import com.metro.dialer.ui.InCallScreen
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

    override fun onDestroy() {
        MetroCallSession.setOnCallEndedListener(null)
        super.onDestroy()
    }
}
