package com.metro.system

/**
 * WP8.1 ease-of-access Text size uses a discrete 7-step slider.
 * Values are absolute Compose/Android fontScale multipliers (1.0 = default).
 */
object MetroFontScale {
    const val DEFAULT = 1.0f

    val STEPS: FloatArray = floatArrayOf(
        0.85f,
        0.925f,
        1.0f,
        1.15f,
        1.3f,
        1.45f,
        1.6f,
    )

    const val STEP_COUNT: Int = 7

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
