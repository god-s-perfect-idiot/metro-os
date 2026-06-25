package com.metro.dialer.telecom

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.metro.dialer.InCallActivity
import com.metro.dialer.data.DialerCallLogic

object MetroTelecomBridge {
    fun isDefaultDialer(context: Context): Boolean {
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return false
        return telecom.defaultDialerPackage == context.packageName
    }

    fun hasCallPhonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun placeOutgoingCall(
        context: Context,
        phoneNumber: String,
        displayName: String? = null,
    ) {
        val trimmed = phoneNumber.trim()
        if (trimmed.isEmpty()) return

        val label = displayName?.takeIf { it.isNotBlank() }
            ?: DialerCallLogic.formatDisplayNumber(trimmed)
        val uri = Uri.fromParts("tel", trimmed, null)

        launchInCallUi(context, trimmed, label)

        if (isDefaultDialer(context) && hasCallPhonePermission(context)) {
            val telecom = context.getSystemService(TelecomManager::class.java) ?: return
            val extras = Bundle().apply {
                putParcelable(
                    TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                    MetroTelecomSetup.phoneAccountHandle(context),
                )
            }
            telecom.placeCall(uri, extras)
        }
    }

    fun handleCallIntent(context: Context, uri: Uri, displayName: String? = null) {
        val number = uri.schemeSpecificPart?.trim().orEmpty()
        if (number.isEmpty()) return
        placeOutgoingCall(context, number, displayName)
    }

    private fun launchInCallUi(context: Context, phoneNumber: String, displayName: String) {
        MetroCallSession.startLocalCall(phoneNumber, displayName)
        val inCallIntent = Intent(context, InCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(inCallIntent)
    }
}
