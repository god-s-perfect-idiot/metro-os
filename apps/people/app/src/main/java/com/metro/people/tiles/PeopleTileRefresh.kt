package com.metro.people.tiles

import android.content.Context
import com.metro.system.MetroTileUpdates

object PeopleTileRefresh {
    fun request(context: Context) {
        MetroTileUpdates.requestUpdate(context.applicationContext, context.packageName)
    }
}
