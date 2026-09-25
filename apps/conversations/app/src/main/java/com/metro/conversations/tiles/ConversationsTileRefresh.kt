package com.metro.conversations.tiles

import android.content.Context
import com.metro.system.MetroTileUpdates

object ConversationsTileRefresh {
    fun request(context: Context) {
        MetroTileUpdates.requestUpdate(context.applicationContext, context.packageName)
    }
}
