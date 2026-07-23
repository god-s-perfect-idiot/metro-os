package com.metro.dialer

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.dialer.telecom.MetroTelecomBridge
import com.metro.dialer.telecom.MetroTelecomSetup
import com.metro.dialer.ui.DialerShell
import com.metro.dialer.ui.DialerState
import com.metro.dialer.ui.PermissionScreen
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroSystemTheme

class MainActivity : ComponentActivity() {
    private val dialNavigationSignal = mutableStateOf(0)

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        permissionResult?.invoke(
            results[Manifest.permission.READ_CALL_LOG] == true,
            results[Manifest.permission.READ_CONTACTS] == true,
            results[Manifest.permission.CALL_PHONE] == true,
        )
    }

    private val requestDefaultDialer = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* role result handled on next resume */ }

    private var permissionResult: ((Boolean, Boolean, Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MetroTelecomSetup.registerPhoneAccount(this)
        dialNavigationSignal.value++
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { DialerState(context) }
            var permissionTick by remember { mutableStateOf(0) }
            var skippedDefaultDialer by remember { mutableStateOf(false) }
            val pendingDialNavigation by dialNavigationSignal

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
                if (state.hasCallLogPermission) {
                    state.reloadCallLog()
                }
                // People (and others) may have added entries via AddSpeedDialReceiver while
                // this process stayed alive — reload so the pivot updates without a restart.
                state.reloadSpeedDial()
                onDispose { }
            }

            val isDefaultDialer = MetroTelecomBridge.isDefaultDialer(context)
            val needsPermissions = !state.hasCallLogPermission || !state.hasCallPhonePermission
            val needsSetup = needsPermissions ||
                (!isDefaultDialer && !skippedDefaultDialer)

            LaunchedEffect(pendingDialNavigation) {
                if (pendingDialNavigation == 0) return@LaunchedEffect
                val dialIntent = intent ?: return@LaunchedEffect
                when (dialIntent.action) {
                    Intent.ACTION_CALL -> {
                        val uri = dialIntent.data ?: return@LaunchedEffect
                        MetroTelecomBridge.handleCallIntent(context, uri)
                        setIntent(Intent(Intent.ACTION_MAIN))
                        if (MetroTelecomBridge.isDefaultDialer(context)) {
                            finish()
                        }
                    }
                    else -> {
                        val uri = dialIntent.dialUriOrNull()
                        if (uri != null) {
                            state.handleDialIntent(uri)
                        } else if (dialIntent.action == Intent.ACTION_DIAL) {
                            state.openDialPad()
                        }
                    }
                }
            }

            MetroSystemTheme {
                when {
                    !state.permissionsChecked -> {
                        MetroLoadingScreen(modifier = Modifier.fillMaxSize())
                    }
                    needsSetup -> {
                        PermissionScreen(
                            hasCallLogPermission = state.hasCallLogPermission,
                            hasCallPhonePermission = state.hasCallPhonePermission,
                            isDefaultDialer = isDefaultDialer,
                            onRequestPermissions = {
                                permissionResult = { callLog, contacts, callPhone ->
                                    state.onPermissionResult(callLog, contacts, callPhone)
                                }
                                requestPermissions.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CALL_LOG,
                                        Manifest.permission.READ_CONTACTS,
                                        Manifest.permission.CALL_PHONE,
                                    ),
                                )
                            },
                            onRequestDefaultDialer = {
                                MetroTelecomSetup.createDefaultDialerRequestIntent(context)?.let { roleIntent ->
                                    requestDefaultDialer.launch(roleIntent)
                                }
                            },
                            onContinue = { skippedDefaultDialer = true },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        DialerShell(
                            state = state,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dialNavigationSignal.value++
    }

    private fun Intent?.dialUriOrNull(): Uri? = this?.data?.takeIf { it.scheme == "tel" }
}
