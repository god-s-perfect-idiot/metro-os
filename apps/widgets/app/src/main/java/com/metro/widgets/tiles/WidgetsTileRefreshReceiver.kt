package com.metro.widgets.tiles

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.metro.system.MetroTileUpdates
import com.metro.widgets.data.TorchController
import com.metro.widgets.data.WidgetKind
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Process-wide torch for catalog + Start. Owns one [TorchController] and fans out state.
 */
object WidgetTorchStore {
    private val listeners = CopyOnWriteArrayList<(Boolean) -> Unit>()

    @Volatile
    private var controller: TorchController? = null

    @Volatile
    private var on: Boolean = false

    fun ensure(context: Context): TorchController {
        controller?.let { return it }
        synchronized(this) {
            controller?.let { return it }
            val appContext = context.applicationContext
            val created = TorchController(appContext)
            created.start { enabled ->
                on = enabled
                listeners.forEach { it(enabled) }
                MetroTileUpdates.requestUpdate(
                    appContext,
                    appContext.packageName,
                    WidgetKind.Torch.id,
                )
            }
            controller = created
            on = created.isEnabled()
            return created
        }
    }

    fun addListener(listener: (Boolean) -> Unit) {
        listeners += listener
        listener(on)
    }

    fun removeListener(listener: (Boolean) -> Unit) {
        listeners -= listener
    }

    fun isOn(context: Context): Boolean {
        ensure(context)
        return on
    }

    fun isAvailable(context: Context): Boolean = ensure(context).isAvailable

    fun toggle(context: Context): Boolean = ensure(context).toggle()

    fun hasCameraPermission(context: Context): Boolean = ensure(context).hasCameraPermission()
}

/**
 * Refreshes Start-pinned battery faces. Registered dynamically from the tile provider
 * (BATTERY_CHANGED is not safely manifest-registered on modern Android).
 */
class WidgetsTileRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val appContext = context.applicationContext
        val pkg = appContext.packageName
        when (action) {
            Intent.ACTION_BATTERY_CHANGED,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            -> MetroTileUpdates.requestUpdate(appContext, pkg, WidgetKind.Battery.id)
        }
    }

    companion object {
        @Volatile
        private var registered = false

        fun ensureRegistered(context: Context) {
            if (registered) return
            synchronized(this) {
                if (registered) return
                val appContext = context.applicationContext
                WidgetTorchStore.ensure(appContext)
                appContext.registerReceiver(
                    WidgetsTileRefreshReceiver(),
                    IntentFilter().apply {
                        addAction(Intent.ACTION_BATTERY_CHANGED)
                        addAction(Intent.ACTION_POWER_CONNECTED)
                        addAction(Intent.ACTION_POWER_DISCONNECTED)
                    },
                )
                registered = true
            }
        }
    }
}
