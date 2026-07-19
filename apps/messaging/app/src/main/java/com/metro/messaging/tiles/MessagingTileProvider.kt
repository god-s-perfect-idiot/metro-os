package com.metro.messaging.tiles

import com.metro.system.MetroTileContract
import com.metro.system.MetroTileData
import com.metro.system.MetroTileProvider

class MessagingTileProvider : MetroTileProvider() {
    override fun buildTileData(tileId: String): MetroTileData? {
        if (tileId != MetroTileContract.DEFAULT_TILE_ID) return null
        val ctx = context ?: return null
        return MessagingTileDataSource(ctx).buildTileData()
    }
}
