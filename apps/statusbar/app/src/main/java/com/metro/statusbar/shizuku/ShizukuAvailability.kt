package com.metro.statusbar.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

/** Readiness of the Shizuku (or Sui) host for privileged status-bar commands. */
enum class ShizukuReadyState {
    /** Shizuku / Sui not installed. */
    NotInstalled,
    /** Installed but binder not alive (user must start Shizuku). */
    NotRunning,
    /** Binder alive but this app has not been granted Shizuku permission. */
    NeedPermission,
    /** Ready to run privileged shell commands. */
    Ready,
}

object ShizukuAvailability {
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    const val DOWNLOAD_URL = "https://shizuku.rikka.app/download/"
    const val PERMISSION_REQUEST_CODE = 0x4D53 // "MS" Metro Statusbar

    fun state(context: Context): ShizukuReadyState {
        if (!isInstalled(context)) return ShizukuReadyState.NotInstalled
        if (!Shizuku.pingBinder()) return ShizukuReadyState.NotRunning
        if (Shizuku.isPreV11()) return ShizukuReadyState.NotRunning
        return if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            ShizukuReadyState.Ready
        } else {
            ShizukuReadyState.NeedPermission
        }
    }

    fun isInstalled(context: Context): Boolean {
        if (packageInstalled(context, SHIZUKU_PACKAGE)) return true
        // Sui / alternate hosts expose the Shizuku permission without the Play package.
        return try {
            context.packageManager.getPermissionInfo(ShizukuProvider.PERMISSION, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun openShizuku(context: Context): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(launch) }.isSuccess
    }

    fun openDownload(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun requestPermission() {
        if (!Shizuku.pingBinder() || Shizuku.isPreV11()) return
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return
        Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
    }

    private fun packageInstalled(context: Context, packageName: String): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
}
