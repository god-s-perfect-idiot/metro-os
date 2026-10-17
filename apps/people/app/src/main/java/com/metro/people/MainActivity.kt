package com.metro.people

import android.Manifest
import android.content.Intent
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
import com.metro.people.tiles.PeopleTileRefresh
import com.metro.people.ui.PeopleShell
import com.metro.people.ui.PeopleState
import com.metro.people.ui.PermissionScreen
import com.metro.ui.MetroActivities
import com.metro.ui.MetroSplash
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroSystemTheme

class MainActivity : ComponentActivity() {
    private val requestContacts = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Always re-read both READ/WRITE from the package manager — on some devices the
        // permissions are granted independently even though they share a group.
        permissionResult?.invoke(true)
        peopleState?.let { state ->
            state.refreshPermission(this)
            state.onPermissionResult(state.hasContactsPermission)
        }
    }

    private var permissionResult: ((Boolean) -> Unit)? = null
    private var peopleState: PeopleState? = null
    private var onDeepLink: ((Intent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { PeopleState(context).also { peopleState = it } }
            var permissionTick by remember { mutableStateOf(0) }
            var launchTick by remember { mutableStateOf(0) }
            var latestLaunch by remember { mutableStateOf(intent) }
            val generation = state.generation
            @Suppress("UNUSED_VARIABLE")
            val observePeopleState = generation

            DisposableEffect(Unit) {
                onDeepLink = { deepIntent ->
                    latestLaunch = deepIntent
                    launchTick++
                }
                onDispose { onDeepLink = null }
            }

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

            // Process cold-start / onNewIntent once permissions are known. Do not key on
            // hasContactsPermission — pendingOpenNewContact covers the grant path.
            LaunchedEffect(launchTick, state.permissionsChecked) {
                if (!state.permissionsChecked) return@LaunchedEffect
                val launch = latestLaunch
                state.handleLaunchIntent(launch)
                // Clear sticky insert/prefill intents so rotation does not reopen the editor.
                if (PeopleState.isNewContactIntent(launch) ||
                    PeopleState.parseContactPrefill(launch) != null
                ) {
                    val clean = Intent(this@MainActivity, MainActivity::class.java)
                    setIntent(clean)
                    latestLaunch = clean
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
                        state.hasContactsPermission -> {
                            PeopleShell(
                                state = state,
                                onRequestWriteContacts = {
                                    requestContacts.launch(
                                        arrayOf(Manifest.permission.WRITE_CONTACTS),
                                    )
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        else -> {
                            PermissionScreen(
                                onRequestPermission = {
                                    permissionResult = { _ ->
                                        state.refreshPermission(this@MainActivity)
                                        state.onPermissionResult(state.hasContactsPermission)
                                    }
                                    requestContacts.launch(
                                        arrayOf(
                                            Manifest.permission.READ_CONTACTS,
                                            Manifest.permission.WRITE_CONTACTS,
                                        ),
                                    )
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
        onDeepLink?.invoke(intent) ?: peopleState?.handleLaunchIntent(intent)
    }
}
