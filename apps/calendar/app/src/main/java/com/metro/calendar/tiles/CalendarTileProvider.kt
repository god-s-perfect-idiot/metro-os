package com.metro.calendar.tiles

import com.metro.calendar.data.CalendarLogic
import com.metro.calendar.data.CalendarRepository
import com.metro.system.MetroAppRegistry
import com.metro.system.MetroTileContract
import com.metro.system.MetroTileData
import com.metro.system.MetroTileProvider

class CalendarTileProvider : MetroTileProvider() {
    override fun buildTileData(tileId: String): MetroTileData? {
        if (tileId != MetroTileContract.DEFAULT_TILE_ID) return null
        val ctx = context ?: return null
        return CalendarTileDataSource(ctx).buildTileData()
    }
}
