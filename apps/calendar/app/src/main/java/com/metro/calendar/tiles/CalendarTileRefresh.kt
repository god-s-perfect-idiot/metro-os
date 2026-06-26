package com.metro.calendar.tiles

import android.content.Context
import com.metro.system.MetroTileUpdates

object CalendarTileRefresh {
    fun request(context: Context) {
        MetroTileUpdates.requestUpdate(context.applicationContext, context.packageName)
    }
}
