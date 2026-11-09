package com.metro.hub.data

import android.content.Context

/**
 * Hub-local install preferences (not suite [com.metro.system.MetroPreferences]).
 *
 * [shizukuInstallEnabled] is the master toggle: when false, Hub always uses the stock
 * package-installer intent even if Shizuku permission is already granted.
 */
class HubInstallPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Prefer silent Shizuku installs when the host is ready.
     * Defaults to true so granting Shizuku immediately takes effect; turn off to force
     * the stock installer confirm UI.
     */
    var shizukuInstallEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHIZUKU_INSTALL, true)
        set(value) = prefs.edit().putBoolean(KEY_SHIZUKU_INSTALL, value).apply()

    companion object {
        private const val PREFS_NAME = "hub_install"
        private const val KEY_SHIZUKU_INSTALL = "shizuku_install_enabled"
    }
}
