package com.metro.lockscreen

import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat

/**
 * Transparent show-when-locked trampoline. Exists only to call
 * [KeyguardManager.requestDismissKeyguard] so **SystemUI** shows its biometric / PIN
 * bouncer. Finishing (cancel / error / success) must not restore the Metro lock fill —
 * that stays suppressed until the next screen-off.
 */
class LockscreenBouncerActivity : ComponentActivity() {

    private var requested = false
    private var dismissAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        enableShowWhenLocked()
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(android.view.View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        })
        LockscreenHostService.notifyBouncerVisible()
        Log.i(TAG, "onCreate — will request SystemUI bouncer")
    }

    override fun onResume() {
        super.onResume()
        window.decorView.post { requestSystemBouncer() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Host may re-launch after a failed BAL start — allow another dismiss request.
        requested = false
        window.decorView.post { requestSystemBouncer() }
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun enableShowWhenLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setInheritShowWhenLocked(true)
        }
    }

    private fun requestSystemBouncer() {
        if (requested) return
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.e(TAG, "KeyguardManager unavailable")
            finish()
            return
        }
        if (!keyguard.isKeyguardLocked) {
            Log.i(TAG, "already unlocked")
            finish()
            return
        }

        requested = true
        Log.i(TAG, "requestDismissKeyguard → SystemUI bouncer (attempt ${dismissAttempts + 1})")
        keyguard.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissError() {
                    Log.e(TAG, "onDismissError")
                    LockscreenHostService.requestGestureBouncerFallback()
                    if (dismissAttempts < 1) {
                        dismissAttempts++
                        requested = false
                        window.decorView.postDelayed({ requestSystemBouncer() }, 400L)
                    } else {
                        window.decorView.postDelayed({
                            if (!isFinishing) finish()
                        }, 800L)
                    }
                }

                override fun onDismissSucceeded() {
                    Log.i(TAG, "onDismissSucceeded")
                    finish()
                }

                override fun onDismissCancelled() {
                    Log.i(TAG, "onDismissCancelled")
                    finish()
                }
            },
        )
    }

    companion object {
        private const val TAG = "LockscreenBouncer"

        @Volatile
        private var instance: LockscreenBouncerActivity? = null

        fun isShowing(): Boolean = instance != null

        fun launch(context: android.content.Context) {
            val intent = Intent(context, LockscreenBouncerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            context.startActivity(intent)
        }

        fun finishIfShowing() {
            instance?.finish()
        }
    }
}
