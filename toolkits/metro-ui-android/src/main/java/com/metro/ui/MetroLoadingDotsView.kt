package com.metro.ui

import android.animation.Animator
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * View-backed WP8.1 dancing dots. Pure [ObjectAnimator] property animation (no update
 * listeners) so the RenderThread can keep the indicator moving when the UI thread is
 * busy — Compose [MetroLoadingDots] shares the UI thread and freezes under load.
 *
 * Animators are started individually (not via [android.animation.AnimatorSet]): a set
 * with infinite-repeating children often never runs or stops after one cycle.
 */
class MetroLoadingDotsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val density = resources.displayMetrics.density
    private val dotSizePx = (DOT_SIZE_DP * density).toInt().coerceAtLeast(1)
    private val trackWidthPx = (TRACK_WIDTH_DP * density).toInt()
    private val containerShiftEndPx = 20f * density
    private val travelMidPx = 98f * density
    private val travelEndPx = 212f * density
    private val startOffsetsPx = floatArrayOf(
        -13f * density,
        -26f * density,
        -39f * density,
        -52f * density,
    )

    private val easeOut = PathInterpolator(0.23f, 1f, 0.32f, 1f)
    private val easeInOut = PathInterpolator(0.785f, 0.135f, 0.15f, 0.86f)

    private val dots = Array(DOT_COUNT) {
        View(context).also { dot ->
            dot.layoutParams = LayoutParams(dotSizePx, dotSizePx)
            dot.setBackgroundColor(Color.WHITE)
            // Hardware layers let translation/alpha run on the RenderThread.
            dot.setLayerType(LAYER_TYPE_HARDWARE, null)
            addView(dot)
        }
    }

    private val running = ArrayList<Animator>(DOT_COUNT * 2 + 1)
    private var startedNotified = false

    /** Fired once per attach after animators have been started (post-layout). */
    var onStarted: (() -> Unit)? = null

    @ColorInt
    var dotColor: Int = Color.WHITE
        set(value) {
            field = value
            dots.forEach { it.setBackgroundColor(value) }
        }

    init {
        clipChildren = true
        clipToPadding = true
        setLayerType(LAYER_TYPE_HARDWARE, null)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "Loading"
        // Park dots at their staged offsets before the first frame so a brief flash
        // still shows the track is “armed” (not four stacked squares at x=0).
        applyRestingPositions()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scheduleStartAfterLayout()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    /** Idempotent entry for [AndroidView] update — restarts if a prior attach was torn down. */
    fun ensureStarted() {
        if (!isAttachedToWindow) return
        if (running.any { it.isStarted }) return
        scheduleStartAfterLayout()
    }

    private fun scheduleStartAfterLayout() {
        if (width > 0 && height > 0) {
            start()
            return
        }
        // Compose AndroidView can attach before the first measure; View.post alone is not
        // always flushed on every host, which left cold-start splash stuck on the static icon.
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (width <= 0 || height <= 0) return
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (!isAttachedToWindow) return
                start()
            }
        })
    }

    fun start() {
        if (running.any { it.isStarted }) return
        stop()
        applyRestingPositions()

        running.add(
            ObjectAnimator.ofPropertyValuesHolder(
                this,
                PropertyValuesHolder.ofKeyframe(
                    TRANSLATION_X,
                    Keyframe.ofFloat(0f, 0f),
                    Keyframe.ofFloat(0.25f, 0f),
                    Keyframe.ofFloat(1f, containerShiftEndPx),
                ),
            ).apply {
                duration = CYCLE_MS
                repeatCount = ValueAnimator.INFINITE
                start()
            },
        )

        dots.forEachIndexed { index, dot ->
            val delay = DOT_DELAYS_MS[index].toLong()
            val start = startOffsetsPx[index]

            val tMid = Keyframe.ofFloat(0.55f, start + travelMidPx).apply {
                interpolator = easeOut
            }
            val tEnd = Keyframe.ofFloat(0.75f, start + travelEndPx).apply {
                interpolator = easeInOut
            }
            running.add(
                ObjectAnimator.ofPropertyValuesHolder(
                    dot,
                    PropertyValuesHolder.ofKeyframe(
                        TRANSLATION_X,
                        Keyframe.ofFloat(0f, start),
                        tMid,
                        tEnd,
                        Keyframe.ofFloat(1f, start + travelEndPx),
                    ),
                ).apply {
                    duration = CYCLE_MS
                    startDelay = delay
                    repeatCount = ValueAnimator.INFINITE
                    start()
                },
            )

            running.add(
                ObjectAnimator.ofPropertyValuesHolder(
                    dot,
                    PropertyValuesHolder.ofKeyframe(
                        ALPHA,
                        Keyframe.ofFloat(0f, 1f),
                        Keyframe.ofFloat(0.55f, 1f),
                        Keyframe.ofFloat(0.90f, 0.2f),
                        Keyframe.ofFloat(1f, 1f),
                    ),
                ).apply {
                    duration = CYCLE_MS
                    startDelay = delay
                    repeatCount = ValueAnimator.INFINITE
                    start()
                },
            )
        }
        notifyStartedOnce()
    }

    private fun notifyStartedOnce() {
        if (startedNotified) return
        startedNotified = true
        onStarted?.invoke()
    }

    fun stop() {
        running.forEach { it.cancel() }
        running.clear()
        translationX = 0f
        applyRestingPositions()
        dots.forEach { it.alpha = 1f }
        startedNotified = false
    }

    private fun applyRestingPositions() {
        dots.forEachIndexed { index, dot ->
            dot.translationX = startOffsetsPx[index]
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(trackWidthPx, dotSizePx)
    }

    companion object {
        private const val CYCLE_MS = 1800L
        private const val DOT_COUNT = 4
        private const val DOT_SIZE_DP = 3f
        private const val TRACK_WIDTH_DP = 140f
        private val DOT_DELAYS_MS = intArrayOf(150, 300, 450, 600)
    }
}

/** Compose bridge for splash / shell awaits — survives UI-thread jank better than [MetroLoadingDots]. */
@Composable
fun MetroLoadingDotsAndroid(
    modifier: Modifier = Modifier,
    color: ComposeColor = MetroTheme.colors.accent,
    onStarted: () -> Unit = {},
) {
    val argb = color.toArgb()
    val startedCallback = rememberUpdatedState(onStarted)
    AndroidView(
        factory = { context ->
            MetroLoadingDotsView(context).also { view ->
                view.onStarted = { startedCallback.value() }
            }
        },
        modifier = modifier
            .width(140.dp)
            .height(3.dp),
        update = { view ->
            view.dotColor = argb
            view.onStarted = { startedCallback.value() }
            view.ensureStarted()
        },
    )
}
