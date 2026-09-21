package com.metro.widgets.tiles

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.metro.system.MetroIntents
import com.metro.system.MetroLockscreen
import com.metro.system.MetroTileUpdates
import com.metro.widgets.data.NotifierAccess
import com.metro.widgets.data.NotifierPeekOpen
import com.metro.widgets.data.NotifierTrayStore
import com.metro.widgets.data.WidgetKind
import com.metro.widgets.data.WidgetPinLogic

/** Start-tap action for pinned catalog widgets (`MetroTileData.tapAction`). */
object WidgetsTileActions {
    const val ACTION_TAP = "com.metro.widgets.action.TILE_TAP"

    fun handleTap(context: Context, tileId: String) {
        val appContext = context.applicationContext
        val kind = WidgetPinLogic.kindForTileId(tileId) ?: return
        when (kind) {
            WidgetKind.Torch -> {
                WidgetTorchStore.toggle(appContext)
                MetroTileUpdates.requestUpdate(appContext, appContext.packageName, tileId)
            }
            WidgetKind.Lock -> {
                if (MetroLockscreen.isAccessibilityEnabled(appContext)) {
                    MetroLockscreen.requestLock(appContext)
                } else {
                    MetroLockscreen.openAccessibilitySettings(appContext)
                }
            }
            WidgetKind.Notifier -> {
                if (!NotifierAccess.isEnabled(appContext)) {
                    NotifierAccess.openSettings(appContext)
                } else {
                    // Prefer the newest tray peek when Start cannot report the visible face
                    // (idle / access grants still fall through from the launcher).
                    val peek = NotifierTrayStore.snapshot().peeks.firstOrNull()
                    if (peek != null) {
                        NotifierPeekOpen.launch(appContext, peek.packageName)
                    }
                }
            }
            WidgetKind.Time,
            WidgetKind.Battery,
            WidgetKind.AnalogClock,
            -> Unit
        }
    }
}

class WidgetsTileActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != WidgetsTileActions.ACTION_TAP) return
        val tileId = intent.getStringExtra(MetroIntents.EXTRA_TILE_ID)?.takeIf { it.isNotBlank() }
            ?: return
        WidgetsTileActions.handleTap(context, tileId)
    }
}
