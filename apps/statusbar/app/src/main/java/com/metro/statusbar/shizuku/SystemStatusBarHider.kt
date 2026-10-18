package com.metro.statusbar.shizuku

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.metro.statusbar.StatusTrayPreferences
import java.util.concurrent.Executors

/**
 * Hides / restores Android SystemUI status-bar icons via Shizuku shell commands.
 *
 * Uses `cmd statusbar send-disable-flag` only — **not** `policy_control immersive.status=*`.
 * Immersive status policy would report status bars as hidden to [android.view.WindowInsets],
 * and the Metro tray would creep away (see [com.metro.statusbar.SystemStatusBarsDetector]).
 *
 * Hide and restore are always paired: turning the preference off (or stopping the Metro tray)
 * must call [restore] so the user can get SystemUI icons back.
 */
object SystemStatusBarHider {
    /**
     * Empty SystemUI clock / system-info / notification icons. Keeps the status-bar *region*
     * so WindowInsets still report bars visible and the Metro overlay stays.
     */
    internal const val HIDE_COMMAND =
        "am broadcast -a com.android.systemui.demo -e command exit >/dev/null 2>&1; " +
            "cmd statusbar send-disable-flag system-icons clock notification-icons"

    /** Clears disable flags and exits SystemUI demo mode. */
    internal const val RESTORE_COMMAND =
        "cmd statusbar send-disable-flag none; " +
            "am broadcast -a com.android.systemui.demo -e command exit >/dev/null 2>&1"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    data class Result(val ok: Boolean, val message: String? = null)

    /** Applies hide or restore to match [StatusTrayPreferences.hideSystemStatusBar]. */
    fun applyDesiredAsync(context: Context, onDone: ((Result) -> Unit)? = null) {
        val hide = StatusTrayPreferences(context).hideSystemStatusBar
        if (hide) {
            hideAsync(context, onDone)
        } else {
            // Only restore when we previously asked to hide — avoid touching SystemUI otherwise.
            onDone?.let { mainHandler.post { it(Result(true)) } }
        }
    }

    fun hideAsync(context: Context, onDone: ((Result) -> Unit)? = null) {
        runAsync(context, HIDE_COMMAND, onDone)
    }

    fun restoreAsync(context: Context, onDone: ((Result) -> Unit)? = null) {
        runAsync(context, RESTORE_COMMAND, onDone)
    }

    /**
     * When the Metro tray master toggle turns off, restore SystemUI icons if we had hidden them
     * so the user is never left with an empty top chrome. Preference stays so re-enabling the
     * Metro tray can re-apply hide.
     */
    fun restoreIfHiddenAsync(context: Context, onDone: ((Result) -> Unit)? = null) {
        if (!StatusTrayPreferences(context).hideSystemStatusBar) {
            onDone?.let { mainHandler.post { it(Result(true)) } }
            return
        }
        restoreAsync(context, onDone)
    }

    private fun runAsync(
        context: Context,
        command: String,
        onDone: ((Result) -> Unit)?,
    ) {
        val app = context.applicationContext
        executor.execute {
            val exec = ShizukuShell.exec(app, command)
            val result = if (exec.ok) {
                Result(true)
            } else {
                Result(false, exec.error ?: "Command failed (exit ${exec.exitCode})")
            }
            if (onDone != null) {
                mainHandler.post { onDone(result) }
            }
        }
    }
}
