package com.metro.conversations.tiles

import android.content.Context
import com.metro.conversations.data.ConversationsLogic
import com.metro.conversations.data.ConversationsRepository
import com.metro.system.MetroPreferences
import com.metro.system.MetroTileData

class ConversationsTileDataSource(context: Context) {
    private val appContext = context.applicationContext
    private val repository = ConversationsRepository(appContext)

    fun buildTileData(): MetroTileData {
        val accentHex = MetroPreferences(appContext).accentColorHex
        val conversations = if (repository.hasNotificationAccess()) {
            ConversationsLogic.conversationsFor(repository.loadGroups(), packageName = null)
        } else {
            emptyList()
        }
        return ConversationsTileLogic.buildTileData(
            conversations = conversations,
            packageName = appContext.packageName,
            accentHex = accentHex,
        )
    }
}
