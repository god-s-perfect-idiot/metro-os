package com.metro.system

/**
 * WP8.1 ease-of-access Text size uses a discrete slider.
 * Values are absolute Compose/Android fontScale multipliers (1.0 = default).
 *
 * The original WP8.1 ladder is the 7 steps from 0.85 upward; metro-os prepends three
 * smaller steps (same 0.075 spacing) so more content fits on modern high-density panels.
 */
object MetroFontScale {
    const val DEFAULT = 1.0f

    val STEPS: FloatArray = floatArrayOf(
        0.625f,
        0.7f,
        0.775f,
        0.85f,
        0.925f,
        1.0f,
        1.15f,
        1.3f,
        1.45f,
        1.6f,
    )

    const val STEP_COUNT: Int = 10

    fun coerceToStep(value: Float): Float {
        var best = STEPS[0]
        var bestDist = kotlin.math.abs(value - best)
        for (step in STEPS) {
            val d = kotlin.math.abs(value - step)
            if (d < bestDist) {
                best = step
                bestDist = d
            }
        }
        return best
    }

    fun indexOf(value: Float): Int {
        val coerced = coerceToStep(value)
        return STEPS.indexOfFirst { it == coerced }.coerceAtLeast(0)
    }

    fun fromIndex(index: Int): Float =
        STEPS[index.coerceIn(0, STEPS.lastIndex)]
}
