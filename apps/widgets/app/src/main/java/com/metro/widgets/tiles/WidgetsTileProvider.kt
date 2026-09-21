package com.metro.widgets.tiles

import android.content.Context
import com.metro.system.MetroTileData
import com.metro.system.MetroTileProvider

class WidgetsTileProvider : MetroTileProvider() {
    override fun onCreate(): Boolean {
        val ctx = context?.applicationContext
        if (ctx != null) {
            WidgetsTileRefreshReceiver.ensureRegistered(ctx)
        }
        return super.onCreate()
    }

    override fun buildTileData(tileId: String): MetroTileData? {
        val ctx = context ?: return null
        return WidgetsTileDataSource(ctx).buildTileData(tileId)
    }
}
