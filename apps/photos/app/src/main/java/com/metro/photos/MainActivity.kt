package com.metro.photos

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.photos.ui.PermissionScreen
import com.metro.photos.ui.PhotosShell
import com.metro.photos.ui.PhotosState
import com.metro.ui.MetroActivities
import com.metro.ui.MetroSplash
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroSystemTheme

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
    private val viewIntentSignal = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember {
                PhotosState(context).also { initial ->
                    // Apply VIEW / Open-with before first frame so the permission gate does not flash.
                    applyViewIntent(initial, intent)
                }
            }
            var permissionTick by remember { mutableStateOf(0) }
            val viewTick = viewIntentSignal.intValue

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

            LaunchedEffect(viewTick) {
                // viewTick == 0 was already applied in remember { }; only react to onNewIntent.
                if (viewTick > 0) {
                    applyViewIntent(state, intent)
                }
            }

            MetroSystemTheme {
                MetroAppPivotShell(
                    modifier = Modifier.fillMaxSize(),
                    onExit = { MetroActivities.finishWithExitTransition(this@MainActivity) },
                ) {
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
                                onExitApp = {
                                    MetroActivities.finishWithExitTransition(this@MainActivity)
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewIntentSignal.intValue++
    }

    private fun applyViewIntent(state: PhotosState, intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri: Uri = intent.data ?: return
        try {
            val flags = intent.flags and
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (flags != 0) {
                contentResolver.takePersistableUriPermission(uri, flags)
            }
        } catch (_: SecurityException) {
            // Temporary grant from the sender is enough for this session.
        } catch (_: Exception) {
            // Not a persistable URI — use the session grant only.
        }
        state.handleViewIntent(uri)
    }
}
