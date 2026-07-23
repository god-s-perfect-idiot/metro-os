package com.metro.launcher

import android.content.Intent
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
import com.metro.system.MetroIntents
import com.metro.ui.MetroSystemTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var launcherState: LauncherState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { LauncherState(context).also { launcherState = it } }
            val scope = rememberCoroutineScope()
            val launchIntent = intent

            LaunchedEffect(state) {
                // Load Start first, then apply any cold-start PIN_TILE so refresh cannot
                // overwrite the newly pinned contact tile.
                state.refreshAllAsync()
                if (launchIntent?.action == MetroIntents.ACTION_PIN_TILE) {
                    state.handlePinTileIntent(launchIntent)
                }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle synchronously before ON_RESUME's refreshAllAsync can race with pin.
        if (intent.action == MetroIntents.ACTION_PIN_TILE) {
            launcherState?.handlePinTileIntent(intent)
        }
    }
}
