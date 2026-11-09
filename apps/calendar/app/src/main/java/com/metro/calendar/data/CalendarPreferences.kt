package com.metro.calendar.data

import android.content.Context

/** App-local calendar preferences (not suite MetroPreferences). */
class CalendarPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var useFahrenheit: Boolean
        get() = prefs.getBoolean(KEY_USE_FAHRENHEIT, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_FAHRENHEIT, value).apply()

    var showWeather: Boolean
        get() = prefs.getBoolean(KEY_SHOW_WEATHER, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_WEATHER, value).apply()

    var use24HourTime: Boolean
        get() = prefs.getBoolean(KEY_USE_24_HOUR, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_24_HOUR, value).apply()

    var defaultViewType: CalendarViewType
        get() = CalendarViewType.fromIndex(prefs.getInt(KEY_DEFAULT_VIEW, CalendarViewType.Day.ordinal))
        set(value) = prefs.edit().putInt(KEY_DEFAULT_VIEW, value.ordinal).apply()

    private companion object {
        const val PREFS_NAME = "metro_calendar_prefs"
        const val KEY_USE_FAHRENHEIT = "use_fahrenheit"
        const val KEY_SHOW_WEATHER = "show_weather"
        const val KEY_USE_24_HOUR = "use_24_hour"
        const val KEY_DEFAULT_VIEW = "default_view"
    }
}
