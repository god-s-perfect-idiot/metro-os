package com.metro.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.launcher.ui.LauncherShell
import com.metro.launcher.ui.LauncherState
import com.metro.system.MetroIntents
import com.metro.system.MetroStatusBar
import com.metro.ui.MetroSplash
import com.metro.ui.MetroSystemTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var launcherState: LauncherState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = MetroSplash.install(this)
        // Hold the system splash until Compose draws its matching loader (avoids a black gap
        // while LauncherState / first frame come up).
        var composeSplashReady = false
        splash.setKeepOnScreenCondition { !composeSplashReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { LauncherState(context).also { launcherState = it } }
            val scope = rememberCoroutineScope()
            val launchIntent = intent
            var handoffDone by remember { mutableStateOf(false) }

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
                        // Going home / Start: briefly reveal status-tray indicators.
                        MetroStatusBar.requestExpand(context)
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
                LauncherShell(
                    state = state,
                    onComposeSplashReady = {
                        if (!handoffDone) {
                            handoffDone = true
                            composeSplashReady = true
                        }
                    },
                )
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
