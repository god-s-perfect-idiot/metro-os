package com.metro.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.launcher.ui.LauncherShell
import com.metro.launcher.ui.LauncherState
import com.metro.ui.MetroSystemTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { LauncherState(context) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(state) {
                state.refreshAllAsync()
            }
            DisposableEffect(state) {
                state.registerReceivers(context)
                var skipNextResume = true
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        if (skipNextResume) {
                            skipNextResume = false
                        } else {
                            scope.launch { state.refreshAllAsync() }
                        }
                    }
                }
                lifecycle.addObserver(observer)
                onDispose {
                    lifecycle.removeObserver(observer)
                    state.unregisterReceivers(context)
                }
            }
            // Suite theme + font scale from Settings; LauncherState still tracks accent for tiles.
            MetroSystemTheme {
                LauncherShell(state = state)
            }
        }
    }
}
