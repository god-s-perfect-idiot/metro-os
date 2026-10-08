package com.metro.metron.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.metro.system.MetroAppBranding
import com.metro.ui.MetroColors
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import eu.kanade.tachiyomi.util.system.ToastHandle
import eu.kanade.tachiyomi.util.system.ToastPresenter
import eu.kanade.tachiyomi.util.system.ToastPresenterBridge
import java.lang.ref.WeakReference
import kotlinx.coroutines.delay

/**
 * In-app WP8.1 toast banner (accent bar + square app logo + one line), matching
 * `com.metro.notifications` toast chrome. Installed from [Application.onCreate].
 *
 * Shows are posted to the main looper so [Activity.finish] + toast settles on the next
 * resumed activity (e.g. reader "Source not found").
 */
object MetronToast : ToastPresenter, Application.ActivityLifecycleCallbacks {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var resumedActivity: WeakReference<Activity>? = null
    private var pending: PendingToast? = null
    private var session: ToastSession? = null
    private var generation = 0L

    fun install(application: Application) {
        ToastPresenterBridge.presenter = this
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun show(context: Context, text: String, durationMs: Long): ToastHandle {
        val id = ++generation
        val handle = ToastHandle {
            mainHandler.post { dismiss(id) }
        }
        // Defer so finish()/navigation can resume the underlying activity first.
        mainHandler.post {
            val target = resumedActivity?.get()?.takeUnless { it.isFinishing || it.isDestroyed }
            if (target != null) {
                showOn(target, text, durationMs, id)
            } else {
                pending = PendingToast(text, durationMs, id)
            }
        }
        return handle
    }

    override fun onActivityResumed(activity: Activity) {
        resumedActivity = WeakReference(activity)
        val waiting = pending ?: return
        pending = null
        if (!activity.isFinishing && !activity.isDestroyed) {
            showOn(activity, waiting.text, waiting.durationMs, waiting.id)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumedActivity?.get() === activity) {
            resumedActivity = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (session?.activity === activity) {
            session?.detach(immediate = true)
            session = null
        }
        if (resumedActivity?.get() === activity) {
            resumedActivity = null
        }
    }

    private fun showOn(activity: Activity, text: String, durationMs: Long, id: Long) {
        val host = activity as? ComponentActivity ?: run {
            pending = PendingToast(text, durationMs, id)
            return
        }
        session?.detach(immediate = true)
        session = null
        val composeView = ComposeView(host).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            clipChildren = false
            clipToPadding = false
            setViewTreeLifecycleOwner(host)
            setViewTreeSavedStateRegistryOwner(host)
        }
        var exiting by mutableStateOf(false)
        composeView.setContent {
            MetroSystemTheme {
                key(id) {
                    MetronToastBanner(
                        message = text,
                        accent = MetroTheme.colors.accent,
                        packageName = host.packageName,
                        exiting = exiting,
                        onExitFinished = {
                            mainHandler.post {
                                if (session?.id == id) {
                                    session?.detach(immediate = true)
                                    session = null
                                }
                            }
                        },
                    )
                }
            }
        }
        val decor = host.window.decorView as ViewGroup
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP,
        )
        decor.addView(composeView, lp)
        val dismissRunnable = Runnable { dismiss(id) }
        val active = ToastSession(
            id = id,
            activity = host,
            view = composeView,
            dismissRunnable = dismissRunnable,
            requestExit = { exiting = true },
        )
        session = active
        mainHandler.postDelayed(dismissRunnable, durationMs)
    }

    private fun dismiss(id: Long) {
        val active = session ?: return
        if (active.id != id) return
        mainHandler.removeCallbacks(active.dismissRunnable)
        active.requestExit()
    }

    private data class PendingToast(
        val text: String,
        val durationMs: Long,
        val id: Long,
    )

    private class ToastSession(
        val id: Long,
        val activity: Activity,
        val view: ComposeView,
        val dismissRunnable: Runnable,
        val requestExit: () -> Unit,
    ) {
        fun detach(immediate: Boolean) {
            mainHandler.removeCallbacks(dismissRunnable)
            val parent = view.parent as? ViewGroup
            if (immediate) {
                parent?.removeView(view)
            }
        }
    }
}

/** Accent strip height: square icon + single-line message (matches notifications ToastSpec). */
private const val ToastHeightDp = 52
private const val ToastIconDp = 32
private const val ToastHorizontalPaddingDp = 12
private const val ToastIconTextGapDp = 10
private const val ToastFlipProjectionPadDp = 24
private const val ToastFlipStartDegrees = 90f
private const val ToastFlipCameraWidthFactor = 0.9f
private const val SkiaPointsPerInch = 72f

@Composable
private fun MetronToastBanner(
    message: String,
    accent: Color,
    packageName: String,
    exiting: Boolean,
    onExitFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val iconAsset = remember(packageName, accent) {
        MetroAppBranding.loadAppIconAsset(context, packageName)
    }

    LaunchedEffect(exiting) {
        if (!exiting) return@LaunchedEffect
        delay(MetroTransitions.JumpListFlipMs.toLong())
        onExitFinished()
    }

    ToastFlip(
        exiting = exiting,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = ToastFlipProjectionPadDp.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(accent)
                .statusBarsPadding()
                .height(ToastHeightDp.dp)
                .padding(horizontal = ToastHorizontalPaddingDp.dp)
                .testTag("metron_toast_banner"),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToastAppGlyph(
                    drawable = iconAsset.drawable,
                    background = iconAsset.backgroundColor,
                    modifier = Modifier.size(ToastIconDp.dp),
                )
                Spacer(modifier = Modifier.width(ToastIconTextGapDp.dp))
                MetroText(
                    text = message,
                    style = MetroTextStyle.DialogBody,
                    color = MetroColors.TileContentOnAccent,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ToastFlip(
    exiting: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val rotationX = remember { Animatable(ToastFlipStartDegrees) }
    LaunchedEffect(exiting) {
        if (exiting) {
            rotationX.animateTo(
                targetValue = ToastFlipStartDegrees,
                animationSpec = MetroTransitions.jumpListFlipTween(),
            )
        } else {
            rotationX.snapTo(ToastFlipStartDegrees)
            rotationX.animateTo(
                targetValue = 0f,
                animationSpec = MetroTransitions.jumpListFlipTween(),
            )
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            this.rotationX = rotationX.value
            transformOrigin = TransformOrigin(0.5f, 0.5f)
            clip = false
            cameraDistance = flipCameraInches(size.width)
        },
    ) {
        content()
    }
}

private fun flipCameraInches(widthPx: Float): Float {
    if (widthPx <= 0f) return 8f
    return (widthPx / SkiaPointsPerInch) * ToastFlipCameraWidthFactor
}

@Composable
private fun ToastAppGlyph(
    drawable: Drawable?,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (drawable != null) {
            val bitmap = remember(drawable) {
                drawable.toBitmap(
                    width = ToastIconDp * 3,
                    height = ToastIconDp * 3,
                )
            }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
