package com.metro.system

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicInteger

/**
 * Uninstalls a package and reports the outcome to Metro UI (confirm + toast).
 *
 * Order:
 * 1. [PackageInstaller.uninstall] — silent when the caller holds
 *    [android.Manifest.permission.DELETE_PACKAGES] (priv-app / platform-signed).
 * 2. If Android returns [PackageInstaller.STATUS_PENDING_USER_ACTION], launch the supplied
 *    confirmation Intent (required on ordinary sideload installs) and keep listening for the
 *    final success / abort status.
 * 3. Shell `pm uninstall` only as a last resort (root / shell).
 *
 * Never use a bare [Intent.ACTION_DELETE] as the primary path — that always forces the stock
 * UI without a status callback. ACTION_DELETE is only a fallback when PackageInstaller cannot
 * start.
 */
object MetroPackageUninstaller {
    private val requestCodes = AtomicInteger(1)
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    fun uninstall(
        context: Context,
        packageName: String,
        onResult: (MetroUninstallResult) -> Unit,
    ) {
        val appContext = context.applicationContext
        if (packageName.isBlank() || packageName == appContext.packageName) {
            dispatch(onResult, MetroUninstallResult.Failed("invalid package"))
            return
        }
        if (!isInstalled(appContext, packageName)) {
            dispatch(onResult, MetroUninstallResult.Failed("not installed"))
            return
        }

        val action = "${appContext.packageName}.METRO_UNINSTALL_RESULT"
        val requestCode = requestCodes.getAndIncrement()
        val filter = IntentFilter(action)

        lateinit var receiver: BroadcastReceiver
        var finished = false
        val finish: (MetroUninstallResult) -> Unit = finish@{ result ->
            if (finished) return@finish
            finished = true
            mainHandler.removeCallbacksAndMessages(receiver)
            runCatching { appContext.unregisterReceiver(receiver) }
            dispatch(onResult, result)
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != action) return
                val status = intent.getIntExtra(
                    PackageInstaller.EXTRA_STATUS,
                    PackageInstaller.STATUS_FAILURE,
                )
                when (status) {
                    PackageInstaller.STATUS_SUCCESS -> finish(MetroUninstallResult.Success)
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        // Keep this receiver registered — SUCCESS / ABORTED arrives after the
                        // stock confirmation completes.
                        val confirm = confirmIntentFrom(intent) ?: run {
                            if (tryShellUninstall(packageName)) {
                                finish(MetroUninstallResult.Success)
                            } else {
                                startActionDelete(context, packageName)
                                watchPackageRemoved(appContext, packageName, finish)
                            }
                            return
                        }
                        try {
                            startConfirmActivity(context, confirm)
                            // If the user dismisses without a status callback, clear the receiver.
                            mainHandler.postAtTime(
                                { finish(MetroUninstallResult.Failed("cancelled")) },
                                receiver,
                                android.os.SystemClock.uptimeMillis() + PACKAGE_REMOVED_TIMEOUT_MS,
                            )
                        } catch (_: Exception) {
                            startActionDelete(context, packageName)
                            watchPackageRemoved(appContext, packageName, finish)
                        }
                    }
                    PackageInstaller.STATUS_FAILURE_ABORTED ->
                        finish(MetroUninstallResult.Cancelled)
                    else -> {
                        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                            ?.takeIf { it.isNotBlank() }
                            ?: "uninstall failed"
                        if (tryShellUninstall(packageName)) {
                            finish(MetroUninstallResult.Success)
                        } else {
                            finish(MetroUninstallResult.Failed(message))
                        }
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        val resultIntent = Intent(action).setPackage(appContext.packageName)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        val sender = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            resultIntent,
            flags,
        ).intentSender

        try {
            // Callers hold REQUEST_DELETE_PACKAGES; silent success needs DELETE_PACKAGES (priv-app).
            appContext.packageManager.packageInstaller.uninstall(packageName, sender)
        } catch (t: Throwable) {
            runCatching { appContext.unregisterReceiver(receiver) }
            if (tryShellUninstall(packageName)) {
                dispatch(onResult, MetroUninstallResult.Success)
            } else {
                try {
                    startActionDelete(context, packageName)
                    watchPackageRemoved(appContext, packageName) { result ->
                        dispatch(onResult, result)
                    }
                } catch (_: Exception) {
                    dispatch(
                        onResult,
                        MetroUninstallResult.Failed(t.message ?: "uninstall failed"),
                    )
                }
            }
        }
    }

    private fun confirmIntentFrom(statusIntent: Intent): Intent? {
        val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            statusIntent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            statusIntent.getParcelableExtra(Intent.EXTRA_INTENT)
        }
        return confirm
    }

    private fun startConfirmActivity(context: Context, confirm: Intent) {
        confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(confirm)
    }

    private fun startActionDelete(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Used when we had to fall back to [Intent.ACTION_DELETE] and will not receive a
     * PackageInstaller status callback.
     */
    private fun watchPackageRemoved(
        appContext: Context,
        packageName: String,
        finish: (MetroUninstallResult) -> Unit,
    ) {
        val filter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply {
            addDataScheme("package")
        }
        val timeoutToken = Any()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_PACKAGE_REMOVED) return
                val removed = intent.data?.schemeSpecificPart
                if (removed != packageName) return
                mainHandler.removeCallbacksAndMessages(timeoutToken)
                runCatching { appContext.unregisterReceiver(this) }
                finish(MetroUninstallResult.Success)
            }
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        // If the user cancels the stock UI, clear the watcher after a generous timeout.
        mainHandler.postAtTime(
            {
                runCatching { appContext.unregisterReceiver(receiver) }
                finish(MetroUninstallResult.Cancelled)
            },
            timeoutToken,
            android.os.SystemClock.uptimeMillis() + PACKAGE_REMOVED_TIMEOUT_MS,
        )
    }

    private fun isInstalled(context: Context, packageName: String): Boolean =
        try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    private fun tryShellUninstall(packageName: String): Boolean {
        val commands = listOf(
            arrayOf("pm", "uninstall", "--user", "0", packageName),
            arrayOf("cmd", "package", "uninstall", "--user", "0", packageName),
            arrayOf("su", "-c", "pm uninstall --user 0 $packageName"),
        )
        for (cmd in commands) {
            try {
                val process = Runtime.getRuntime().exec(cmd)
                val code = process.waitFor()
                if (code == 0) return true
            } catch (_: Exception) {
                // try next
            }
        }
        return false
    }

    private fun dispatch(onResult: (MetroUninstallResult) -> Unit, result: MetroUninstallResult) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onResult(result)
        } else {
            mainHandler.post { onResult(result) }
        }
    }

    private const val PACKAGE_REMOVED_TIMEOUT_MS = 120_000L
}

sealed class MetroUninstallResult {
    data object Success : MetroUninstallResult()
    /** User dismissed the stock confirmation without uninstalling. */
    data object Cancelled : MetroUninstallResult()
    data class Failed(val reason: String) : MetroUninstallResult()
}
