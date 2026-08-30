package com.metro.lockscreen

/**
 * Lock fill source — WP8.1 lock screen background choices.
 */
enum class LockscreenBackgroundMode {
    /** Solid system accent fill. */
    Accent,

    /** User-cropped photo (same choose-photo / crop flow as Start background). */
    Custom,

    /** Bing picture of the day (cached locally). */
    Bing,
    ;

    fun toStorage(): String = name.lowercase()

    companion object {
        fun fromStorage(value: String?): LockscreenBackgroundMode {
            return when (value?.lowercase()) {
                "custom" -> Custom
                "bing" -> Bing
                else -> Accent
            }
        }
    }
}
