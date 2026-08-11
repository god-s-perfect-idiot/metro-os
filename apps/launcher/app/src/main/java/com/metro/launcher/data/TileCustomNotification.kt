package com.metro.launcher.data

import android.app.Notification
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.RemoteViews
import android.widget.TextView

/**
 * Content scraped from a custom [RemoteViews] notification (Bolt.Earth charging monitor, …)
 * where [Notification.EXTRA_TITLE] / [Notification.EXTRA_TEXT] / progress extras are empty.
 */
internal data class CustomNotificationSnapshot(
    val texts: List<String>,
    val progress: Int = 0,
    val progressMax: Int = 0,
    val indeterminate: Boolean = false,
    val hasProgressBar: Boolean = false,
)

internal fun snapshotCustomNotification(
    context: Context,
    notification: Notification,
): CustomNotificationSnapshot? {
    val remote = notification.customRemoteViews() ?: return null
    return inflateRemoteViews(context, remote) ?: snapshotViaReflection(remote)
}

/**
 * Read setText / setProgress / setMax / setIndeterminate actions without inflating.
 * Used when [RemoteViews.apply] cannot load the foreign package's resources.
 */
@Suppress("UNCHECKED_CAST")
private fun snapshotViaReflection(remote: RemoteViews): CustomNotificationSnapshot? {
    return runCatching {
        val field = RemoteViews::class.java.getDeclaredField("mActions").apply {
            isAccessible = true
        }
        val actions = field.get(remote) as? Collection<*> ?: return@runCatching null
        val texts = ArrayList<String>()
        var progress = 0
        var progressMax = 0
        var indeterminate = false
        var hasProgressBar = false
        for (action in actions) {
            if (action == null) continue
            val method = action.fieldOrNull("methodName") as? String ?: continue
            val value = action.fieldOrNull("value")
            when (method) {
                "setText" -> {
                    value?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { texts += it }
                }
                "setProgress" -> {
                    hasProgressBar = true
                    if (value is Int) progress = value
                }
                "setMax" -> {
                    hasProgressBar = true
                    if (value is Int) progressMax = value
                }
                "setIndeterminate" -> {
                    hasProgressBar = true
                    if (value is Boolean) indeterminate = value
                }
            }
        }
        if (texts.isEmpty() && !hasProgressBar) null
        else CustomNotificationSnapshot(
            texts = texts,
            progress = progress,
            progressMax = progressMax,
            indeterminate = indeterminate,
            hasProgressBar = hasProgressBar,
        )
    }.getOrNull()
}

private fun Any.fieldOrNull(name: String): Any? = runCatching {
    var clazz: Class<*>? = javaClass
    while (clazz != null) {
        val field = runCatching { clazz!!.getDeclaredField(name) }.getOrNull()
        if (field != null) {
            field.isAccessible = true
            return@runCatching field.get(this)
        }
        clazz = clazz.superclass
    }
    null
}.getOrNull()

@Suppress("DEPRECATION")
private fun Notification.customRemoteViews(): RemoteViews? =
    bigContentView ?: contentView ?: headsUpContentView

private fun inflateRemoteViews(context: Context, remote: RemoteViews): CustomNotificationSnapshot? {
    val host = FrameLayout(context.applicationContext)
    val root = runCatching { remote.apply(context.applicationContext, host) }.getOrNull()
        ?: return null
    val texts = ArrayList<String>()
    var progress = 0
    var progressMax = 0
    var indeterminate = false
    var hasProgressBar = false
    fun walk(view: View) {
        when (view) {
            is ProgressBar -> {
                hasProgressBar = true
                progress = view.progress
                progressMax = view.max
                indeterminate = view.isIndeterminate
            }
            is TextView -> {
                view.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { texts += it }
            }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                walk(view.getChildAt(index))
            }
        }
    }
    walk(root)
    if (texts.isEmpty() && !hasProgressBar) return null
    return CustomNotificationSnapshot(
        texts = texts,
        progress = progress,
        progressMax = progressMax,
        indeterminate = indeterminate,
        hasProgressBar = hasProgressBar,
    )
}
