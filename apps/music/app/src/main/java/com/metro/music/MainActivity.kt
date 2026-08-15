package com.metro.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.music.ui.MusicShell
import com.metro.music.ui.MusicState
import com.metro.music.ui.PermissionScreen
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
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        // Music uses a custom launch/exit transition (not MetroAppPivotShell).
        enableEdgeToEdge()
        setContent {
            val state = remember { MusicState(this) }
            var permissionTick by remember { mutableIntStateOf(0) }

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
                if (!state.hasAudioPermission) {
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
                    MusicShell(state = state)
                }
            }
        }
    }
}
