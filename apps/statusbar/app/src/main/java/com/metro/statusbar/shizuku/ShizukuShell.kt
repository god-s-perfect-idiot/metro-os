package com.metro.statusbar.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Binds the [PrivilegedShellService] UserService and runs shell commands as shell/root.
 * Call only from a background thread — bind waits up to [BIND_TIMEOUT_MS].
 */
object ShizukuShell {
    private const val BIND_TIMEOUT_MS = 8_000L
    private const val SERVICE_TAG = "metro_statusbar_shell"
    private const val SERVICE_VERSION = 1

    data class ExecResult(val exitCode: Int, val error: String? = null) {
        val ok: Boolean get() = exitCode == 0 && error == null
    }

    fun canExec(): Boolean {
        if (!Shizuku.pingBinder() || Shizuku.isPreV11()) return false
        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    fun exec(context: Context, command: String): ExecResult {
        if (!canExec()) {
            return ExecResult(1, "Shizuku is not ready")
        }
        val appContext = context.applicationContext
        val shellRef = AtomicReference<IPrivilegedShell?>()
        val latch = CountDownLatch(1)
        val args = Shizuku.UserServiceArgs(
            ComponentName(appContext, PrivilegedShellService::class.java),
        )
            .daemon(false)
            .tag(SERVICE_TAG)
            .version(SERVICE_VERSION)
            .processNameSuffix("shell")

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                shellRef.set(IPrivilegedShell.Stub.asInterface(service))
                latch.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                shellRef.set(null)
            }
        }

        return try {
            Shizuku.bindUserService(args, connection)
            if (!latch.await(BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                return ExecResult(1, "Timed out waiting for Shizuku shell")
            }
            val shell = shellRef.get()
                ?: return ExecResult(1, "Shizuku shell unavailable")
            ExecResult(shell.exec(command))
        } catch (e: Exception) {
            ExecResult(1, e.message ?: "Shizuku exec failed")
        } finally {
            runCatching {
                Shizuku.unbindUserService(args, connection, true)
            }
        }
    }
}
