package com.metro.messaging

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.messaging.data.DefaultSmsApp
import com.metro.messaging.ui.MessagingShell
import com.metro.messaging.ui.MessagingState
import com.metro.messaging.ui.PermissionScreen
import com.metro.ui.MetroTheme

class MainActivity : ComponentActivity() {
    private val navigationSignal = mutableIntStateOf(0)

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        permissionResult?.invoke(
            results[Manifest.permission.READ_SMS] == true,
            results[Manifest.permission.SEND_SMS] == true,
            results[Manifest.permission.READ_CONTACTS] == true,
        )
    }

    private val requestDefaultSmsApp = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        defaultAppResult?.invoke()
    }

    private var permissionResult: ((Boolean, Boolean, Boolean) -> Unit)? = null
    private var defaultAppResult: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigationSignal.intValue++
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { MessagingState(context) }
            var permissionTick by remember { mutableStateOf(0) }
            val pendingNavigation by navigationSignal

            DisposableEffect(state) {
                onDispose { state.clear() }
            }

            DisposableEffect(this@MainActivity) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionTick++
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            DisposableEffect(permissionTick) {
                state.refreshPermissions(context)
                state.refreshDefaultStatus()
                if (state.canAccessSystemSms || state.skippedPermissions) {
                    state.reloadThreads()
                }
                onDispose { }
            }

            LaunchedEffect(pendingNavigation) {
                if (pendingNavigation == 0) return@LaunchedEffect
                val messagingIntent = intent
                if (messagingIntent?.data?.scheme == "smsto" ||
                    messagingIntent?.data?.scheme == "sms"
                ) {
                    state.continueWithDemo()
                }
                handleMessagingIntent(state, messagingIntent)
            }

            MetroTheme {
                if (state.needsPermissionGate) {
                    PermissionScreen(
                        onRequestPermissions = {
                            permissionResult = { readSms, sendSms, contacts ->
                                state.onPermissionResult(readSms, sendSms, contacts)
                            }
                            requestPermissions.launch(
                                arrayOf(
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.SEND_SMS,
                                    Manifest.permission.READ_CONTACTS,
                                ),
                            )
                        },
                        onContinueWithDemo = state::continueWithDemo,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    MessagingShell(
                        state = state,
                        onRequestDefaultApp = {
                            val roleIntent = DefaultSmsApp.requestIntent(context)
                            if (roleIntent == null) {
                                Toast.makeText(
                                    context,
                                    "Already the default messaging app",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                state.refreshPermissions(context)
                                state.refreshDefaultStatus()
                            } else {
                                defaultAppResult = {
                                    // Role grant also unlocks Telephony provider access — refresh
                                    // permissions and switch off demo data immediately.
                                    state.refreshPermissions(context)
                                    state.refreshDefaultStatus()
                                }
                                requestDefaultSmsApp.launch(roleIntent)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navigationSignal.intValue++
    }

    private fun handleMessagingIntent(state: MessagingState, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SENDTO, Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return
                if (uri.scheme == "smsto" || uri.scheme == "sms") {
                    state.handleSendToUri(uri)
                }
            }
        }
    }
}
