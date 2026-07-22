package com.metro.people

import android.Manifest
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
import com.metro.people.tiles.PeopleTileRefresh
import com.metro.people.ui.PeopleShell
import com.metro.people.ui.PeopleState
import com.metro.people.ui.PermissionScreen
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroSystemTheme

class MainActivity : ComponentActivity() {
    private val requestContacts = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionResult?.invoke(granted)
    }

    private var permissionResult: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { PeopleState(context) }
            var permissionTick by remember { mutableStateOf(0) }
            val generation = state.generation
            @Suppress("UNUSED_VARIABLE")
            val observePeopleState = generation

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
                state.refreshPermission(context)
                if (state.hasContactsPermission) {
                    state.reloadContacts()
                }
                PeopleTileRefresh.request(context)
                onDispose { }
            }

            MetroSystemTheme {
                when {
                    !state.permissionsChecked -> {
                        MetroLoadingScreen(modifier = Modifier.fillMaxSize())
                    }
                    state.hasContactsPermission -> {
                        PeopleShell(
                            state = state,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        PermissionScreen(
                            onRequestPermission = {
                                permissionResult = { granted ->
                                    state.onPermissionResult(granted)
                                }
                                requestContacts.launch(Manifest.permission.READ_CONTACTS)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
