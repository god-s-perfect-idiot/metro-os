package com.metro.statusbar

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
/**
 * Reads and toggles the four default Action Center quick actions.
 *
 * Many radios are restricted on modern Android; when a direct toggle is blocked we open the
 * matching system panel / settings screen (documented in README § Platform exceptions).
 */
class QuickActionController(context: Context) {
    private val appContext = context.applicationContext

    fun snapshot(): List<QuickActionSlot> = listOf(
        wifiSlot(),
        bluetoothSlot(),
        airplaneSlot(),
        internetSharingSlot(),
    )

    fun toggle(type: QuickActionType) {
        when (type) {
            QuickActionType.Wifi -> toggleWifi()
            QuickActionType.Bluetooth -> toggleBluetooth()
            QuickActionType.Airplane -> toggleAirplane()
            QuickActionType.InternetSharing -> toggleInternetSharing()
        }
    }

    private fun wifiSlot(): QuickActionSlot {
        val wifi = wifiManager()
        val enabled = wifi?.isWifiEnabled == true
        val ssid = if (enabled) currentSsid(wifi) else null
        return QuickActionSlot(
            type = QuickActionType.Wifi,
            enabled = enabled,
            label = ssid?.takeIf { it.isNotBlank() } ?: labelFor(QuickActionType.Wifi),
        )
    }

    private fun bluetoothSlot(): QuickActionSlot {
        val adapter = bluetoothAdapter()
        val enabled = adapter?.isEnabled == true
        val name = if (enabled) connectedBluetoothName(adapter) else null
        return QuickActionSlot(
            type = QuickActionType.Bluetooth,
            enabled = enabled,
            label = name?.takeIf { it.isNotBlank() } ?: labelFor(QuickActionType.Bluetooth),
        )
    }

    private fun airplaneSlot(): QuickActionSlot {
        val enabled = isAirplaneModeOn()
        return QuickActionSlot(
            type = QuickActionType.Airplane,
            enabled = enabled,
            label = labelFor(QuickActionType.Airplane),
        )
    }

    private fun internetSharingSlot(): QuickActionSlot {
        val enabled = isHotspotEnabled()
        return QuickActionSlot(
            type = QuickActionType.InternetSharing,
            enabled = enabled,
            label = labelFor(QuickActionType.InternetSharing),
        )
    }

    private fun toggleWifi() {
        val wifi = wifiManager()
        if (wifi != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            wifi.isWifiEnabled = !wifi.isWifiEnabled
            return
        }
        openPanelOrSettings(
            panelAction = Settings.Panel.ACTION_WIFI,
            settingsAction = Settings.ACTION_WIFI_SETTINGS,
        )
    }

    private fun toggleBluetooth() {
        val adapter = bluetoothAdapter()
        if (adapter != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                @Suppress("DEPRECATION")
                if (adapter.isEnabled) adapter.disable() else adapter.enable()
            }.onSuccess { return }
        }
        openPanelOrSettings(
            panelAction = null,
            settingsAction = Settings.ACTION_BLUETOOTH_SETTINGS,
        )
    }

    private fun toggleAirplane() {
        openPanelOrSettings(
            panelAction = null,
            settingsAction = Settings.ACTION_AIRPLANE_MODE_SETTINGS,
        )
    }

    private fun toggleInternetSharing() {
        // No public toggle API for tether/hotspot; jump to the system tether UI when present.
        val tether = Intent("android.settings.TETHER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tether.resolveActivity(appContext.packageManager) != null) {
            appContext.startActivity(tether)
            return
        }
        openPanelOrSettings(
            panelAction = null,
            settingsAction = Settings.ACTION_WIRELESS_SETTINGS,
        )
    }

    private fun openPanelOrSettings(panelAction: String?, settingsAction: String) {
        if (panelAction != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                appContext.startActivity(
                    Intent(panelAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                return
            }
        }
        appContext.startActivity(
            Intent(settingsAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun wifiManager(): WifiManager? =
        appContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private fun bluetoothAdapter(): BluetoothAdapter? {
        val manager = appContext.getSystemService(BluetoothManager::class.java)
        return manager?.adapter
    }

    private fun isAirplaneModeOn(): Boolean =
        Settings.Global.getInt(appContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

    @Suppress("DEPRECATION")
    private fun currentSsid(wifi: WifiManager?): String? {
        val info = wifi?.connectionInfo ?: return null
        val raw = info.ssid ?: return null
        if (raw == WifiManager.UNKNOWN_SSID || raw == "<unknown ssid>") return null
        return raw.trim('"').takeIf { it.isNotBlank() }
    }

    private fun connectedBluetoothName(adapter: BluetoothAdapter?): String? {
        if (adapter == null) return null
        return runCatching {
            @Suppress("DEPRECATION")
            adapter.bondedDevices
                ?.firstOrNull { device ->
                    runCatching {
                        // Connected profile check is privileged; bonded name is the WP-style label.
                        true
                    }.getOrDefault(false)
                }
                ?.name
                ?: adapter.name
        }.getOrNull()
    }

    private fun isHotspotEnabled(): Boolean {
        // Best-effort: some OEMs expose tether via ConnectivityManager reflection; keep false when unknown.
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        return runCatching {
            val method = cm.javaClass.getDeclaredMethod("getTetheredIfaces")
            @Suppress("DiscouragedPrivateApi")
            method.isAccessible = true
            val ifaces = method.invoke(cm) as? Array<*>
            ifaces != null && ifaces.isNotEmpty()
        }.getOrDefault(false)
    }

    companion object {
        fun labelFor(type: QuickActionType): String = when (type) {
            QuickActionType.Wifi -> "WI-FI"
            QuickActionType.Bluetooth -> "BLUETOOTH"
            QuickActionType.Airplane -> "AIRPLANE MODE"
            QuickActionType.InternetSharing -> "INTERNET SHARING"
        }
    }
}
