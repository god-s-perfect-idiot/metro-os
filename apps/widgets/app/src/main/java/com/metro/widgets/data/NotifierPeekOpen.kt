package com.metro.widgets.data

import android.content.Context
import android.content.Intent

/** Opens the app for a Notifier peek (catalog tap / Start fallback). */
object NotifierPeekOpen {
    fun launch(context: Context, packageName: String): Boolean {
        val trimmed = packageName.trim()
        if (trimmed.isEmpty()) return false
        val launch = context.packageManager.getLaunchIntentForPackage(trimmed)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        } ?: return false
        return runCatching {
            context.startActivity(launch)
            true
        }.getOrDefault(false)
    }
}
