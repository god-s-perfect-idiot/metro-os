package com.metro.system

object MetroIntents {
    const val ACTION_LAUNCH_APP = "com.metro.action.LAUNCH_APP"
    const val ACTION_SEARCH = "com.metro.action.SEARCH"
    const val ACTION_SHARE = "com.metro.action.SHARE"
    const val ACTION_PIN_TILE = "com.metro.action.PIN_TILE"

    const val EXTRA_PACKAGE = "package"
    const val EXTRA_QUERY = "query"
    const val EXTRA_URI = "uri"
    const val EXTRA_MIME = "mime"
    const val EXTRA_TILE_ID = "tile_id"
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
    const val EXTRA_TILE_PACKAGE = MetroIntents.EXTRA_PACKAGE
    const val EXTRA_TILE_ID = MetroIntents.EXTRA_TILE_ID
    const val EXTRA_NAVBAR_ENABLED = MetroPreferenceKeys.NAV_BAR_ENABLED
}

object MetroContentProviderContract {
    const val AUTHORITY = "com.metro.system"
    const val PATH_PREFERENCES = "preferences"
    const val PATH_APPS = "apps"
}
