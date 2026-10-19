package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.annotation.IntRange
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.isVisible
import com.metro.system.MetroPreferences

/**
 * WP8.1 dancing-dots loader for reader page buffering.
 *
 * Drawn in [onDraw] (no child views / hardware layers). Webtoon placeholders are a full
 * recycler tall — holders must top-align this view; centering buries it below the viewport.
 * Color follows the suite accent from [MetroPreferences].
 */
class ReaderProgressIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val trackWidthPx = (TRACK_WIDTH_DP * density)
    private val dotSizePx = (DOT_SIZE_DP * density).coerceAtLeast(1f)
    private val containerShiftEndPx = 20f * density
    private val travelMidPx = 98f * density
    private val travelEndPx = 212f * density
    private val startOffsetsPx = floatArrayOf(
        -13f * density,
        -26f * density,
        -39f * density,
        -52f * density,
    )
    private val delaysMs = intArrayOf(150, 300, 450, 600)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private fun refreshAccent() {
        // Keep RGB; [onDraw] modulates alpha per square.
        paint.color = MetroPreferences(context).accentColor.toArgb() or 0xFF000000.toInt()
    }

    private var ticking = false
    private val tick = object : Runnable {
        override fun run() {
            if (!ticking || !isAttachedToWindow || !isVisible) {
                ticking = false
                return
            }
            invalidate()
            postOnAnimation(this)
        }
    }

    init {
        layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER_HORIZONTAL)
        // Software layer: HW property animators + RecyclerView recycling left the View-based
        // dots blank on device; canvas invalidation always paints.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        contentDescription = "Loading"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    fun show() {
        refreshAccent()
        isVisible = true
        bringToFront()
        start()
    }

    fun hide() {
        isVisible = false
        stop()
    }

    @Suppress("UNUSED_PARAMETER")
    fun setProgress(@IntRange(from = 0, to = 100) progress: Int) {
        // Dots are indeterminate; progress is ignored.
    }

    private fun start() {
        if (ticking) {
            invalidate()
            return
        }
        ticking = true
        invalidate()
        postOnAnimation(tick)
    }

    private fun stop() {
        ticking = false
        removeCallbacks(tick)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        refreshAccent()
        if (isVisible) start()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(trackWidthPx.toInt().coerceAtLeast(1), dotSizePx.toInt().coerceAtLeast(1))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cycleMs = CYCLE_MS
        val now = SystemClock.uptimeMillis()
        val t = (now % cycleMs).toFloat() / cycleMs
        val containerShift = if (t < 0.25f) {
            0f
        } else {
            ((t - 0.25f) / 0.75f).coerceIn(0f, 1f) * containerShiftEndPx
        }

        var drew = false
        for (i in 0 until DOT_COUNT) {
            val localT = ((now + cycleMs - delaysMs[i].toLong()) % cycleMs).toFloat() / cycleMs
            val start = startOffsetsPx[i]
            val travel = when {
                localT < 0.55f -> {
                    val u = localT / 0.55f
                    val e = 1f - (1f - u) * (1f - u)
                    travelMidPx * e
                }
                localT < 0.75f -> {
                    val u = (localT - 0.55f) / 0.20f
                    val e = u * u * (3f - 2f * u)
                    travelMidPx + (travelEndPx - travelMidPx) * e
                }
                else -> travelEndPx
            }
            val alpha = when {
                localT < 0.55f -> 1f
                localT < 0.90f -> 1f - 0.8f * ((localT - 0.55f) / 0.35f)
                else -> 0.2f + 0.8f * ((localT - 0.90f) / 0.10f)
            }.coerceIn(0.25f, 1f)

            val left = containerShift + start + travel
            val right = left + dotSizePx
            if (right < 0f || left > trackWidthPx) continue
            val clippedLeft = left.coerceIn(0f, trackWidthPx)
            val clippedRight = right.coerceIn(0f, trackWidthPx)
            if (clippedRight - clippedLeft < 0.5f) continue
            paint.alpha = (alpha * 255f).toInt().coerceIn(64, 255)
            canvas.drawRect(clippedLeft, 0f, clippedRight, dotSizePx, paint)
            drew = true
        }
        if (!drew) {
            paint.alpha = 255
            val mid = trackWidthPx / 2f
            canvas.drawRect(mid - dotSizePx / 2f, 0f, mid + dotSizePx / 2f, dotSizePx, paint)
        }
    }

    companion object {
        private const val CYCLE_MS = 1800L
        private const val DOT_COUNT = 4
        private const val DOT_SIZE_DP = 3f
        private const val TRACK_WIDTH_DP = 140f
    }
}
