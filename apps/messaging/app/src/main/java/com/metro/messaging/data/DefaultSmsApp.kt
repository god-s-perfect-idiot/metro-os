package com.metro.messaging.data

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony

/**
 * Helpers for becoming the system **default SMS app**. On Android 10+ this goes through the
 * [RoleManager] role-request dialog; on older releases it uses the legacy
 * [Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT] prompt.
 */
object DefaultSmsApp {
    fun isDefault(context: Context): Boolean =
        Telephony.Sms.getDefaultSmsPackage(context) == context.packageName

    /**
     * Intent that prompts the user to make this app the default messaging app, or `null` when the
     * app is already default or the role cannot be requested on this device.
     */
    fun requestIntent(context: Context): Intent? {
        if (isDefault(context)) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_SMS) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_SMS)
            ) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
            } else {
                null
            }
        } else {
            @Suppress("DEPRECATION")
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            }
        }
    }
}
