package com.metro.system

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.Settings

/**
 * Cross-app read of active Conversations shade posts (connected conversation apps).
 * Hosted by `com.metro.conversations` — People and others query; no cross-app imports.
 */
object MetroConversationFeed {
    const val AUTHORITY = "com.metro.conversations.feed"
    const val PATH_ACTIVE = "active"
    const val METHOD_OPEN = "open"
    const val METHOD_HAS_ACCESS = "has_access"
    const val EXTRA_HAS_ACCESS = "has_access"

    val CONTENT_URI: Uri =
        Uri.parse("content://$AUTHORITY/$PATH_ACTIVE")

    object Columns {
        const val KEY = "key"
        const val PACKAGE_NAME = "package_name"
        const val APP_LABEL = "app_label"
        const val TITLE = "title"
        const val PREVIEW = "preview"
        const val POST_TIME_MS = "post_time_ms"
        const val BADGE_COUNT = "badge_count"
    }

    data class Item(
        val key: String,
        val packageName: String,
        val appLabel: String,
        val title: String,
        val preview: String,
        val postTimeMs: Long,
        val badgeCount: Int,
    )

    fun loadActive(context: Context): List<Item> {
        val resolver = context.contentResolver
        return runCatching {
            resolver.query(CONTENT_URI, null, null, null, null)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.toItem())
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    fun hasNotificationAccess(context: Context): Boolean {
        val result = runCatching {
            context.contentResolver.call(
                Uri.parse("content://$AUTHORITY"),
                METHOD_HAS_ACCESS,
                null,
                null,
            )
        }.getOrNull()
        return result?.getBoolean(EXTRA_HAS_ACCESS, false) == true
    }

    fun open(context: Context, key: String): Boolean {
        if (key.isBlank()) return false
        val result = runCatching {
            context.contentResolver.call(
                Uri.parse("content://$AUTHORITY"),
                METHOD_OPEN,
                key,
                null,
            )
        }.getOrNull()
        return result?.getBoolean("opened", false) == true
    }

    fun notificationAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    private fun Cursor.toItem(): Item {
        fun str(column: String): String {
            val index = getColumnIndex(column)
            if (index < 0) return ""
            return getString(index)?.trim().orEmpty()
        }
        fun long(column: String): Long {
            val index = getColumnIndex(column)
            if (index < 0) return 0L
            return getLong(index)
        }
        fun int(column: String): Int {
            val index = getColumnIndex(column)
            if (index < 0) return 0
            return getInt(index)
        }
        return Item(
            key = str(Columns.KEY),
            packageName = str(Columns.PACKAGE_NAME),
            appLabel = str(Columns.APP_LABEL),
            title = str(Columns.TITLE),
            preview = str(Columns.PREVIEW),
            postTimeMs = long(Columns.POST_TIME_MS),
            badgeCount = int(Columns.BADGE_COUNT),
        )
    }
}
