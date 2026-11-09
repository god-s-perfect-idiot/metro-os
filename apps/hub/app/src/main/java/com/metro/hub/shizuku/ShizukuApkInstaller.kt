package com.metro.hub.shizuku

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageInstaller
import android.content.res.AssetFileDescriptor
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Silent APK install / update via a Shizuku UserService [ShizukuInstallerService].
 * Call only when [ShizukuAvailability.isReady] and the Hub master toggle is on.
 */
object ShizukuApkInstaller {
    private const val ACTION_INSTALL_RESULT = "com.metro.hub.ACTION_SHIZUKU_INSTALL_RESULT"
    private const val BIND_TIMEOUT_MS = 15_000L

    suspend fun install(context: Context, apkFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        if (!ShizukuAvailability.isReady(context)) {
            return@withContext Result.failure(IllegalStateException("Shizuku is not ready"))
        }
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            return@withContext Result.failure(IllegalArgumentException("APK missing: ${apkFile.name}"))
        }

        val app = context.applicationContext
        val binderRef = AtomicReference<IBinder?>()
        val bound = CountDownLatch(1)
        val args = Shizuku.UserServiceArgs(
            ComponentName(app, ShizukuInstallerService::class.java),
        )
            .tag("hub_shizuku_installer")
            .processNameSuffix("shizuku_installer")
            .daemon(false)
            .version(1)

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                binderRef.set(service)
                bound.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                binderRef.set(null)
            }
        }

        try {
            Shizuku.bindUserService(args, connection)
            if (!bound.await(BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                return@withContext Result.failure(IllegalStateException("Timed out waiting for Shizuku installer"))
            }
            val binder = binderRef.get()
                ?: return@withContext Result.failure(IllegalStateException("Shizuku installer unavailable"))
            val shell = IShizukuInstaller.Stub.asInterface(binder)

            suspendCancellableCoroutine { cont ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        val status = intent.getIntExtra(
                            PackageInstaller.EXTRA_STATUS,
                            PackageInstaller.STATUS_FAILURE,
                        )
                        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                        runCatching { app.unregisterReceiver(this) }
                        if (cont.isActive) {
                            if (status == PackageInstaller.STATUS_SUCCESS) {
                                cont.resume(Result.success(Unit))
                            } else {
                                cont.resume(
                                    Result.failure(
                                        IllegalStateException(
                                            message ?: "Shizuku install failed ($status)",
                                        ),
                                    ),
                                )
                            }
                        }
                    }
                }

                ContextCompat.registerReceiver(
                    app,
                    receiver,
                    IntentFilter(ACTION_INSTALL_RESULT),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )

                cont.invokeOnCancellation {
                    runCatching { app.unregisterReceiver(receiver) }
                }

                val statusIntent = PendingIntent.getBroadcast(
                    app,
                    apkFile.name.hashCode(),
                    Intent(ACTION_INSTALL_RESULT).setPackage(app.packageName),
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )

                try {
                    ParcelFileDescriptor.open(apkFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        val afd = AssetFileDescriptor(pfd, 0, apkFile.length())
                        shell.install(afd, statusIntent.intentSender)
                    }
                } catch (e: Exception) {
                    runCatching { app.unregisterReceiver(receiver) }
                    if (cont.isActive) {
                        cont.resume(Result.failure(e))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching {
                Shizuku.unbindUserService(args, connection, true)
            }
        }
    }

    /** Whether Hub should route this install through Shizuku right now. */
    fun shouldUseShizuku(context: Context, enabledPreference: Boolean): Boolean =
        enabledPreference && ShizukuAvailability.isReady(context)
}
