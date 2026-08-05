package com.metro.music.ytmusic

import android.content.Context
import android.content.SharedPreferences

class YtMusicAuthStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var cookie: String
        get() = prefs.getString(KEY_COOKIE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_COOKIE, value).apply()

    var connected: Boolean
        get() = prefs.getBoolean(KEY_CONNECTED, false) && cookie.isNotBlank()
        set(value) = prefs.edit().putBoolean(KEY_CONNECTED, value).apply()

    /**
     * Innertube's anonymous visitor identity. Player requests without it come back
     * `LOGIN_REQUIRED`, so it is cached across launches and only refetched when stale.
     */
    var visitorData: String
        get() = prefs.getString(KEY_VISITOR_DATA, "").orEmpty()
        set(value) = prefs.edit()
            .putString(KEY_VISITOR_DATA, value)
            .putLong(KEY_VISITOR_DATA_AT, System.currentTimeMillis())
            .apply()

    val visitorDataFresh: Boolean
        get() = visitorData.isNotBlank() &&
            System.currentTimeMillis() - prefs.getLong(KEY_VISITOR_DATA_AT, 0L) < VISITOR_DATA_TTL_MS

    fun clearVisitorData() {
        prefs.edit().remove(KEY_VISITOR_DATA).remove(KEY_VISITOR_DATA_AT).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "ytmusic_auth"
        private const val KEY_COOKIE = "cookie"
        private const val KEY_CONNECTED = "connected"
        private const val KEY_VISITOR_DATA = "visitor_data"
        private const val KEY_VISITOR_DATA_AT = "visitor_data_at"
        private const val VISITOR_DATA_TTL_MS = 12L * 60L * 60L * 1000L
    }
}
