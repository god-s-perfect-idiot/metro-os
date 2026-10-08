package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.widget.Toast
import dev.icerock.moko.resources.StringResource
import tachiyomi.core.common.i18n.stringResource

/** Cancelable handle returned by [Context.toast] (Metro banner or platform fallback). */
class ToastHandle(
    private val onCancel: () -> Unit,
) {
    fun cancel() = onCancel()
}

/**
 * App-installed presenter that shows WP8.1-style toast banners instead of Android Toast.
 * Set from Application.onCreate; null falls back to [Toast.makeText] (unit tests).
 */
fun interface ToastPresenter {
    fun show(context: Context, text: String, durationMs: Long): ToastHandle
}

object ToastPresenterBridge {
    @Volatile
    var presenter: ToastPresenter? = null
}

/**
 * Display a toast in this context.
 *
 * @param resource the text resource.
 * @param duration the duration of the toast. Defaults to short.
 */
fun Context.toast(
    resource: StringResource,
    duration: Int = Toast.LENGTH_SHORT,
    block: (ToastHandle) -> Unit = {},
): ToastHandle {
    return toast(stringResource(resource), duration, block)
}

/**
 * Display a toast in this context.
 *
 * @param text the text to display.
 * @param duration the duration of the toast. Defaults to short.
 */
fun Context.toast(
    text: String?,
    duration: Int = Toast.LENGTH_SHORT,
    block: (ToastHandle) -> Unit = {},
): ToastHandle {
    val message = text.orEmpty()
    val durationMs = when (duration) {
        Toast.LENGTH_LONG -> TOAST_LONG_MS
        else -> TOAST_SHORT_MS
    }
    val presenter = ToastPresenterBridge.presenter
    val handle = if (presenter != null) {
        presenter.show(this, message, durationMs)
    } else {
        val platform = Toast.makeText(applicationContext, message, duration).also { it.show() }
        ToastHandle { platform.cancel() }
    }
    return handle.also(block)
}

private const val TOAST_SHORT_MS = 3_000L
private const val TOAST_LONG_MS = 5_000L
