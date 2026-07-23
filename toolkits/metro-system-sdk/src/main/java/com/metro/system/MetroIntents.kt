package com.metro.system

import android.content.Context
import android.content.Intent

object MetroIntents {
    const val ACTION_LAUNCH_APP = "com.metro.action.LAUNCH_APP"
    const val ACTION_SEARCH = "com.metro.action.SEARCH"
    const val ACTION_SHARE = "com.metro.action.SHARE"
    const val ACTION_PIN_TILE = "com.metro.action.PIN_TILE"
    /** Explicit broadcast to Phone — extras [EXTRA_DISPLAY_NAME], [EXTRA_PHONE_NUMBER]. */
    const val ACTION_ADD_SPEED_DIAL = "com.metro.action.ADD_SPEED_DIAL"

    const val EXTRA_PACKAGE = "package"
    const val EXTRA_QUERY = "query"
    const val EXTRA_URI = "uri"
    const val EXTRA_MIME = "mime"
    const val EXTRA_TILE_ID = "tile_id"
    const val EXTRA_DISPLAY_NAME = "display_name"
    const val EXTRA_PHONE_NUMBER = "phone_number"

    const val PACKAGE_LAUNCHER = "com.metro.launcher"
    const val PACKAGE_DIALER = "com.metro.dialer"
    const val PACKAGE_PEOPLE = "com.metro.people"

    /**
     * Ask the launcher to pin a (possibly secondary) tile for [packageName]/[tileId].
     * Launcher brings Start forward after pinning.
     */
    fun requestPinTile(
        context: Context,
        packageName: String,
        tileId: String = MetroTileContract.DEFAULT_TILE_ID,
    ) {
        val intent = Intent(ACTION_PIN_TILE).apply {
            setPackage(PACKAGE_LAUNCHER)
            putExtra(EXTRA_PACKAGE, packageName)
            putExtra(EXTRA_TILE_ID, tileId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { context.startActivity(intent) }
    }

    /** Ask Phone to append a speed-dial entry (no UI required). */
    fun requestAddSpeedDial(
        context: Context,
        displayName: String,
        phoneNumber: String,
    ) {
        val trimmed = phoneNumber.trim()
        if (trimmed.isEmpty()) return
        val intent = Intent(ACTION_ADD_SPEED_DIAL).apply {
            setPackage(PACKAGE_DIALER)
            putExtra(EXTRA_DISPLAY_NAME, displayName.ifBlank { trimmed })
            putExtra(EXTRA_PHONE_NUMBER, trimmed)
        }
        context.sendBroadcast(intent)
    }
}

object MetroBroadcasts {
    const val ACTION_THEME_CHANGED = "com.metro.system.THEME_CHANGED"
    const val ACTION_TILE_UPDATE = "com.metro.system.TILE_UPDATE"

    /** Sent by the navbar app whenever the overlay is enabled or disabled. */
    const val ACTION_NAVBAR_CHANGED = "com.metro.system.NAVBAR_CHANGED"

    /**
     * Sent by any app (targeted at [MetroNavBar.PACKAGE]) to ask the navbar to re-announce its
     * current state via [ACTION_NAVBAR_CHANGED]. Used to recover the correct state on cold start.
     */
    const val ACTION_NAVBAR_QUERY = "com.metro.system.NAVBAR_QUERY"

    const val EXTRA_THEME_MODE = MetroPreferenceKeys.THEME_MODE
    const val EXTRA_ACCENT_COLOR = MetroPreferenceKeys.ACCENT_COLOR
    const val EXTRA_FONT_SCALE = MetroPreferenceKeys.FONT_SCALE
    const val EXTRA_NAV_BAR_COLOR = MetroPreferenceKeys.NAV_BAR_COLOR
    const val EXTRA_TILE_PACKAGE = MetroIntents.EXTRA_PACKAGE
    const val EXTRA_TILE_ID = MetroIntents.EXTRA_TILE_ID
    const val EXTRA_NAVBAR_ENABLED = MetroPreferenceKeys.NAV_BAR_ENABLED
}

object MetroContentProviderContract {
    const val AUTHORITY = "com.metro.system"
    /** Package that hosts [AUTHORITY] via [MetroSystemPreferencesProvider]. */
    const val HOST_PACKAGE = "com.metro.settings"
    const val PATH_PREFERENCES = "preferences"
    const val PATH_APPS = "apps"
}
