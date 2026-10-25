package com.metro.conversations.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import com.metro.conversations.ConversationsListenerService
import com.metro.system.MetroConversationFeed

/**
 * Exposes active connected-app shade conversations for People (and other suite apps)
 * without cross-app code imports.
 *
 * `content://com.metro.conversations.feed/active`
 */
class ConversationsFeedProvider : ContentProvider() {
    private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(MetroConversationFeed.AUTHORITY, MetroConversationFeed.PATH_ACTIVE, CODE_ACTIVE)
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (matcher.match(uri) != CODE_ACTIVE) return null
        val ctx = context ?: return emptyCursor()
        if (!ConversationsListenerService.isNotificationAccessEnabled(ctx)) {
            return emptyCursor()
        }
        val conversations = ConversationsLogic.conversationsFor(
            ConversationsRepository(ctx).loadGroups(),
            packageName = null,
        )
        val cursor = MatrixCursor(COLUMNS)
        for (item in conversations) {
            val badge = item.messages.size.coerceAtLeast(0)
            cursor.addRow(
                arrayOf(
                    item.key,
                    item.packageName,
                    item.appLabel,
                    item.title,
                    item.preview,
                    item.postTimeMs,
                    badge,
                ),
            )
        }
        return cursor
    }

    override fun getType(uri: Uri): String? =
        if (matcher.match(uri) == CODE_ACTIVE) {
            "vnd.android.cursor.dir/vnd.metro.conversation"
        } else {
            null
        }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        return when (method) {
            MetroConversationFeed.METHOD_HAS_ACCESS -> Bundle().apply {
                putBoolean(
                    MetroConversationFeed.EXTRA_HAS_ACCESS,
                    ConversationsListenerService.isNotificationAccessEnabled(ctx),
                )
            }
            MetroConversationFeed.METHOD_OPEN -> {
                val key = arg?.trim().orEmpty()
                val opened = key.isNotEmpty() && ConversationsRepository(ctx).openConversation(key)
                Bundle().apply { putBoolean("opened", opened) }
            }
            else -> null
        }
    }

    private fun emptyCursor(): Cursor = MatrixCursor(COLUMNS)

    companion object {
        private const val CODE_ACTIVE = 1

        private val COLUMNS = arrayOf(
            MetroConversationFeed.Columns.KEY,
            MetroConversationFeed.Columns.PACKAGE_NAME,
            MetroConversationFeed.Columns.APP_LABEL,
            MetroConversationFeed.Columns.TITLE,
            MetroConversationFeed.Columns.PREVIEW,
            MetroConversationFeed.Columns.POST_TIME_MS,
            MetroConversationFeed.Columns.BADGE_COUNT,
        )
    }
}
