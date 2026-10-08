package com.metro.metron.ui

import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.util.system.toast

/**
 * Observes [SnackbarHostState] and shows messages as Metro toast banners instead of a
 * Material Snackbar. Action labels are ignored (toast auto-dismisses); use a Metro dialog
 * when the user must confirm.
 */
@Composable
fun MetronSnackbarHost(hostState: SnackbarHostState) {
    val context = LocalContext.current
    val data = hostState.currentSnackbarData
    LaunchedEffect(data) {
        if (data == null) return@LaunchedEffect
        val duration = when (data.visuals.duration) {
            SnackbarDuration.Long, SnackbarDuration.Indefinite -> Toast.LENGTH_LONG
            SnackbarDuration.Short -> Toast.LENGTH_SHORT
        }
        context.toast(data.visuals.message, duration)
        data.dismiss()
    }
}
