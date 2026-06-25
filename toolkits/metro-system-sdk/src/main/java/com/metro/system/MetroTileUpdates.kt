package com.metro.system

import android.content.Context
import android.content.Intent

object MetroTileUpdates {
    private const val LAUNCHER_PACKAGE = "com.metro.launcher"

    fun requestUpdate(context: Context, packageName: String, tileId: String = MetroTileContract.DEFAULT_TILE_ID) {
        val intent = Intent(MetroBroadcasts.ACTION_TILE_UPDATE).apply {
            setPackage(LAUNCHER_PACKAGE)
            putExtra(MetroBroadcasts.EXTRA_TILE_PACKAGE, packageName)
            putExtra(MetroBroadcasts.EXTRA_TILE_ID, tileId)
        }
        context.sendBroadcast(intent)
    }
}
