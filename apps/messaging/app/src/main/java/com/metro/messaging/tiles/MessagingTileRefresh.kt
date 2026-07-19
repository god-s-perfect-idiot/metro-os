package com.metro.messaging.tiles

import android.content.Context
import com.metro.system.MetroTileUpdates

object MessagingTileRefresh {
    fun request(context: Context) {
        MetroTileUpdates.requestUpdate(context.applicationContext, context.packageName)
    }
}
