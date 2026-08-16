package com.metro.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle

/**
 * Activity-level helpers for Metro page / task transitions.
 *
 * Suppresses platform open/close animation. App open splash pivot is owned by
 * the Start launcher ([MetroAppOpenSplash]); suite apps keep Compose flip-out on
 * Back via [finishWithExitTransition].
 *
 * Call [applyLaunchTransition] from `onCreate` before `setContent`.
 * Start / app list launches use [startActivityWithoutTransition].
 *
 * For cold-start chrome, use [MetroSplash.install] before `super.onCreate()` and
 * point the launcher activity at `@style/Theme.Metro.Splash`.
 */
object MetroActivities {
    fun applyLaunchTransition(activity: Activity) {
        suppressOpenTransition(activity)
    }

    fun applyExitTransition(activity: Activity) {
        suppressCloseTransition(activity)
    }

    /** Finish the activity with no platform close animation. */
    fun finishWithExitTransition(activity: Activity) {
        activity.finish()
        applyExitTransition(activity)
    }

    /** Bundle for [Context.startActivity] / shortcut launches with no window animation. */
    fun optionsBundleWithoutTransition(context: Context): Bundle =
        ActivityOptions.makeCustomAnimation(
            context,
            R.anim.metro_no_anim,
            R.anim.metro_no_anim,
        ).toBundle()

    /** Start [intent] with no platform open/close animation. */
    fun startActivityWithoutTransition(context: Context, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        context.startActivity(intent, optionsBundleWithoutTransition(context))
        context.findActivity()?.let { suppressOpenTransition(it) }
    }

    private fun suppressOpenTransition(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
        }
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(0, 0)
    }

    private fun suppressCloseTransition(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(0, 0)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
