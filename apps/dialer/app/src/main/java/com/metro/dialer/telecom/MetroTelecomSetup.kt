package com.metro.dialer.telecom

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

object MetroTelecomSetup {
    private const val PHONE_ACCOUNT_ID = "metro_phone"

    fun phoneAccountHandle(context: Context): PhoneAccountHandle {
        val componentName = ComponentName(context, MetroConnectionService::class.java)
        return PhoneAccountHandle(componentName, PHONE_ACCOUNT_ID)
    }

    fun registerPhoneAccount(context: Context) {
        val telecomManager = context.getSystemService(TelecomManager::class.java) ?: return
        val handle = phoneAccountHandle(context)
        val phoneAccount = PhoneAccount.builder(handle, context.getString(com.metro.dialer.R.string.app_name))
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .build()
        telecomManager.registerPhoneAccount(phoneAccount)
    }

    fun createDefaultDialerRequestIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
            if (!roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) return null
            if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) return null
            roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            val telecomManager = context.getSystemService(TelecomManager::class.java) ?: return null
            if (telecomManager.defaultDialerPackage == context.packageName) return null
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
        }
    }
}
