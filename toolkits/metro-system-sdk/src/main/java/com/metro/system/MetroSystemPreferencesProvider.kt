package com.metro.system

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * Cross-app system preferences. Hosted by `com.metro.settings` (exported authority
 * [MetroContentProviderContract.AUTHORITY]). Other apps read via [MetroPreferences].
 */
class MetroSystemPreferencesProvider : ContentProvider() {
    private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(MetroContentProviderContract.AUTHORITY, MetroContentProviderContract.PATH_PREFERENCES, CODE_PREFS)
        addURI(
            MetroContentProviderContract.AUTHORITY,
            "${MetroContentProviderContract.PATH_PREFERENCES}/*",
            CODE_PREF_KEY,
        )
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val context = context ?: return null
        val prefs = context.getSharedPreferences(MetroPreferenceKeys.PREFS_NAME, Context.MODE_PRIVATE)
        return when (matcher.match(uri)) {
            CODE_PREFS -> {
                val columns = projection?.toList() ?: ALL_KEYS
                val cursor = MatrixCursor(columns.toTypedArray())
                val row = columns.map { key -> readValue(prefs, key) }.toTypedArray()
                cursor.addRow(row)
                cursor
            }
            CODE_PREF_KEY -> {
                val key = uri.lastPathSegment ?: return null
                val cursor = MatrixCursor(arrayOf(COLUMN_VALUE))
                cursor.addRow(arrayOf(readValue(prefs, key)))
                cursor
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String? = when (matcher.match(uri)) {
        CODE_PREFS -> "vnd.android.cursor.dir/vnd.metro.preferences"
        CODE_PREF_KEY -> "vnd.android.cursor.item/vnd.metro.preference"
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (matcher.match(uri) != CODE_PREFS || values == null) return null
        val context = context ?: return null
        writeValues(context, values)
        context.contentResolver.notifyChange(PREFERENCES_URI, null)
        return PREFERENCES_URI
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        if (values == null) return 0
        val context = context ?: return 0
        when (matcher.match(uri)) {
            CODE_PREFS -> writeValues(context, values)
            CODE_PREF_KEY -> {
                val key = uri.lastPathSegment ?: return 0
                val v = values.get(COLUMN_VALUE) ?: values.get(key) ?: return 0
                val single = ContentValues().apply { putValue(key, v) }
                writeValues(context, single)
            }
            else -> return 0
        }
        context.contentResolver.notifyChange(PREFERENCES_URI, null)
        return 1
    }

    private fun writeValues(context: Context, values: ContentValues) {
        val prefs = context.getSharedPreferences(MetroPreferenceKeys.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (key in values.keySet()) {
            when (val v = values.get(key)) {
                null -> editor.remove(key)
                is String -> editor.putString(key, v)
                is Float -> editor.putFloat(key, v)
                is Double -> editor.putFloat(key, v.toFloat())
                is Int -> when (key) {
                    MetroPreferenceKeys.NAV_BAR_ENABLED -> editor.putBoolean(key, v != 0)
                    MetroPreferenceKeys.FONT_SCALE -> editor.putFloat(key, v.toFloat())
                    else -> editor.putString(key, v.toString())
                }
                is Long -> editor.putString(key, v.toString())
                is Boolean -> editor.putBoolean(key, v)
                else -> editor.putString(key, v.toString())
            }
        }
        editor.commit()
    }

    private fun ContentValues.putValue(key: String, value: Any) {
        when (value) {
            is String -> put(key, value)
            is Float -> put(key, value)
            is Double -> put(key, value.toFloat())
            is Int -> put(key, value)
            is Long -> put(key, value)
            is Boolean -> put(key, value)
            else -> put(key, value.toString())
        }
    }

    private fun readValue(
        prefs: android.content.SharedPreferences,
        key: String,
    ): Any? = when (key) {
        MetroPreferenceKeys.THEME_MODE ->
            prefs.getString(key, MetroThemeMode.Dark.storageValue)
        MetroPreferenceKeys.ACCENT_COLOR ->
            prefs.getString(key, MetroPreferences.DEFAULT_ACCENT_HEX)
        MetroPreferenceKeys.FONT_SCALE ->
            prefs.getFloat(key, MetroFontScale.DEFAULT)
        MetroPreferenceKeys.NAV_BAR_COLOR ->
            prefs.getString(key, null)
        MetroPreferenceKeys.NAV_BAR_ENABLED ->
            if (prefs.getBoolean(key, false)) 1 else 0
        else -> prefs.all[key]
    }

    companion object {
        const val COLUMN_VALUE = "value"
        private const val CODE_PREFS = 1
        private const val CODE_PREF_KEY = 2

        val PREFERENCES_URI: Uri =
            Uri.parse("content://${MetroContentProviderContract.AUTHORITY}/${MetroContentProviderContract.PATH_PREFERENCES}")

        val ALL_KEYS = listOf(
            MetroPreferenceKeys.THEME_MODE,
            MetroPreferenceKeys.ACCENT_COLOR,
            MetroPreferenceKeys.FONT_SCALE,
            MetroPreferenceKeys.NAV_BAR_COLOR,
            MetroPreferenceKeys.NAV_BAR_ENABLED,
        )

        fun keyUri(key: String): Uri = PREFERENCES_URI.buildUpon().appendPath(key).build()
    }
}
