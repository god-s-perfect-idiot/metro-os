package com.metro.music

import android.os.Bundle
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.music.ui.MusicShell
import com.metro.music.ui.MusicState
import com.metro.music.ui.PermissionScreen
import com.metro.ui.MetroActivities
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroSplash

class MainActivity : ComponentActivity() {
    private var permissionCallback: ((Boolean) -> Unit)? = null

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results.values.any { it }
        permissionCallback?.invoke(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = MetroSplash.install(this)
        // Hold the system splash until Compose draws its matching loader (avoids a black gap
        // while MusicState / first library pass come up).
        var composeSplashReady = false
        splash.setKeepOnScreenCondition { !composeSplashReady }
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        setContent {
            val state = remember { MusicState(this) }
            var permissionTick by remember { mutableIntStateOf(0) }
            var handoffDone by remember { mutableStateOf(false) }

            DisposableEffect(this) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionTick++
                        state.refreshYtAuth()
                    }
                }
                lifecycle.addObserver(observer)
                state.connectPlayer()
                onDispose {
                    lifecycle.removeObserver(observer)
                    state.releasePlayer()
                }
            }

            DisposableEffect(permissionTick) {
                state.refreshPermissions(this@MainActivity)
                if (state.hasAudioPermission) {
                    state.reloadLibrary()
                }
                onDispose { }
            }

            MetroSystemTheme {
                MetroAppPivotShell(
                    modifier = Modifier.fillMaxSize(),
                    onExit = { MetroActivities.finishWithExitTransition(this@MainActivity) },
                ) {
                    if (!state.hasAudioPermission) {
                        // Permission gate — lift the platform splash so the grant UI is visible.
                        LaunchedEffect(Unit) {
                            if (!handoffDone) {
                                handoffDone = true
                                composeSplashReady = true
                            }
                        }
                        PermissionScreen(
                            onGrant = {
                                permissionCallback = { granted ->
                                    state.refreshPermissions(this@MainActivity)
                                    if (granted) state.reloadLibrary()
                                }
                                requestPermission.launch(MusicState.audioPermissions())
                            },
                        )
                    } else {
                        MusicShell(
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
        }
    }
}
