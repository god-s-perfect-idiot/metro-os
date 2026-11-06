package com.metro.lockscreen

import android.content.Context
import android.text.format.DateFormat

/**
 * Local setup preferences for the Metro lock screen. Owned by this app (not
 * [com.metro.system.MetroPreferences]).
 */
class LockscreenPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val prefs =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Whether the user wants the Metro lock surface presented over the keyguard. */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /**
     * When enabled, a black AMOLED glance surface (time / day / date chrome only) is drawn over
     * the system Always-On Display while the keyguard is locked.
     */
    var glanceEnabled: Boolean
        get() = prefs.getBoolean(KEY_GLANCE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_GLANCE_ENABLED, value).apply()

    /**
     * Whether lock / glance chrome uses 24-hour time (e.g. `19:30` vs `07:30`).
     * Defaults to the system 24-hour setting until the user changes the setup toggle.
     */
    var use24HourFormat: Boolean
        get() = if (prefs.contains(KEY_USE_24_HOUR)) {
            prefs.getBoolean(KEY_USE_24_HOUR, false)
        } else {
            DateFormat.is24HourFormat(appContext)
        }
        set(value) = prefs.edit().putBoolean(KEY_USE_24_HOUR, value).apply()

    /** Accent / custom photo / Bing wallpaper. */
    var backgroundMode: LockscreenBackgroundMode
        get() = LockscreenBackgroundMode.fromStorage(prefs.getString(KEY_BACKGROUND_MODE, null))
        set(value) = prefs.edit().putString(KEY_BACKGROUND_MODE, value.toStorage()).apply()

    /** True when a cropped custom lock JPEG is stored. */
    var customBackgroundEnabled: Boolean
        get() = prefs.getBoolean(KEY_CUSTOM_BACKGROUND_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CUSTOM_BACKGROUND_ENABLED, value).apply()

    /** Bing `startdate` from the last successful HPImageArchive fetch. */
    var bingStartDate: String
        get() = prefs.getString(KEY_BING_START_DATE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_BING_START_DATE, value).apply()

    /** Epoch millis of the last successful Bing image download. */
    var bingFetchedAtMs: Long
        get() = prefs.getLong(KEY_BING_FETCHED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_BING_FETCHED_AT, value).apply()

    /** Up to five packages for lock quick-status glyphs (null / blank slot = unset). */
    fun quickStatusSlot(index: Int): String? {
        require(index in 0 until LockscreenQuickStatusLogic.SLOT_COUNT)
        return prefs.getString(quickStatusKey(index), null)?.takeIf { it.isNotBlank() }
    }

    fun setQuickStatusSlot(index: Int, packageName: String?) {
        require(index in 0 until LockscreenQuickStatusLogic.SLOT_COUNT)
        prefs.edit().apply {
            if (packageName.isNullOrBlank()) {
                remove(quickStatusKey(index))
            } else {
                putString(quickStatusKey(index), packageName)
            }
        }.apply()
    }

    fun quickStatusSlots(): List<String?> =
        List(LockscreenQuickStatusLogic.SLOT_COUNT) { quickStatusSlot(it) }

    companion object {
        private const val PREFS_NAME = "metro_lockscreen"
        private const val KEY_ENABLED = "lockscreen_enabled"
        private const val KEY_GLANCE_ENABLED = "lockscreen_glance_enabled"
        private const val KEY_USE_24_HOUR = "lockscreen_use_24_hour"
        private const val KEY_BACKGROUND_MODE = "lockscreen_background_mode"
        private const val KEY_CUSTOM_BACKGROUND_ENABLED = "lockscreen_custom_background_enabled"
        private const val KEY_BING_START_DATE = "lockscreen_bing_start_date"
        private const val KEY_BING_FETCHED_AT = "lockscreen_bing_fetched_at"

        private fun quickStatusKey(index: Int) = "quick_status_$index"
    }
}
