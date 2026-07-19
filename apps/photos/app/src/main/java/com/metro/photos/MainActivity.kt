package com.metro.photos

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.photos.ui.PermissionScreen
import com.metro.photos.ui.PhotosShell
import com.metro.photos.ui.PhotosState
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroTheme

class MainActivity : ComponentActivity() {
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            results[Manifest.permission.READ_MEDIA_IMAGES] == true
        } else {
            results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        permissionResult?.invoke(granted)
    }

    private var permissionResult: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { PhotosState(context) }
            var permissionTick by remember { mutableStateOf(0) }

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
                if (state.hasMediaPermission) {
                    state.reloadPhotos()
                }
                onDispose { }
            }

            MetroTheme {
                when {
                    !state.permissionsChecked -> {
                        MetroLoadingScreen(modifier = Modifier.fillMaxSize())
                    }
                    state.needsPermissionGate -> {
                        PermissionScreen(
                            onRequestPermissions = {
                                permissionResult = { granted ->
                                    if (granted) {
                                        state.onPermissionGranted()
                                    }
                                }
                                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                                } else {
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                                requestPermissions.launch(permissions)
                            },
                            onContinueWithout = state::continueWithoutPhotos,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        PhotosShell(
                            state = state,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
