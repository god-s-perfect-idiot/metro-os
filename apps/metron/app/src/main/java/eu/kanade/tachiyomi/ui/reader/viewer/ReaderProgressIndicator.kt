package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.annotation.IntRange
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.isVisible
import com.metro.ui.MetroColors
import com.metro.ui.MetroLoadingDotsView

/**
 * WP8.1 dancing-dots loader for reader page buffering.
 *
 * View-backed (not Compose) so measure size is reliable inside pager/webtoon holders.
 */
class ReaderProgressIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val dots = MetroLoadingDotsView(context).apply {
        dotColor = MetroColors.AccentBlue.toArgb()
    }

    init {
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
        )
        addView(
            dots,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER),
        )
    }

    fun show() {
        isVisible = true
        dots.ensureStarted()
    }

    fun hide() {
        isVisible = false
    }

    @Suppress("UNUSED_PARAMETER")
    fun setProgress(@IntRange(from = 0, to = 100) progress: Int) {
        // Dots are indeterminate; progress is ignored.
    }
}
