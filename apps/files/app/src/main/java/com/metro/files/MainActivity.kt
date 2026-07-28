package com.metro.files

import android.Manifest
import android.content.Intent
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
import com.metro.files.data.StorageAccess
import com.metro.files.ui.FilesShell
import com.metro.files.ui.FilesState
import com.metro.files.ui.PermissionScreen
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroSystemTheme

class MainActivity : ComponentActivity() {
    private val requestLegacyRead = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        permissionTickHandler?.invoke()
    }

    private var permissionTickHandler: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { FilesState(context) }
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
                state.refreshPermissions()
                onDispose { }
            }

            permissionTickHandler = { permissionTick++ }

            MetroSystemTheme {
                when {
                    !state.permissionsChecked -> {
                        MetroLoadingScreen(modifier = Modifier.fillMaxSize())
                    }
                    !state.hasStorageAccess -> {
                        PermissionScreen(
                            onRequestAccess = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    startActivity(StorageAccess.manageAllFilesIntent(this@MainActivity))
                                } else {
                                    requestLegacyRead.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        FilesShell(
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
    }
}
