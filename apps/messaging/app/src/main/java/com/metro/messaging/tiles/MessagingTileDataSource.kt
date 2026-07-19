package com.metro.messaging.tiles

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.metro.messaging.data.MessagingLogic
import com.metro.messaging.data.SmsMessagingDataSource
import com.metro.messaging.data.SmsTilePeek
import com.metro.messaging.data.StubMessagingDataSource
import com.metro.system.MetroAppRegistry
import com.metro.system.MetroPreferences
import com.metro.system.MetroTileData

object MessagingTileLogic {
    fun buildTileData(
        peek: SmsTilePeek,
        packageName: String,
        accentHex: String,
    ): MetroTileData {
        val label = MetroAppRegistry.label(packageName) ?: "Messaging"
        val unread = peek.unreadCount.takeIf { it > 0 }
        return MetroTileData(
            title = label,
            backgroundColorHex = accentHex,
            counter = unread,
            backFaceTitle = unread?.let { peek.latestUnreadLabel },
            deepLinkUri = null,
        )
    }

    fun peekFromDemoThreads(): SmsTilePeek {
        val threads = StubMessagingDataSource.demoThreads()
        val unread = threads.sumOf { it.unreadCount.coerceAtLeast(0) }
        val latest = threads
            .filter { it.unreadCount > 0 }
            .maxByOrNull { it.timestamp }
        return SmsTilePeek(
            unreadCount = unread,
            latestUnreadLabel = latest?.let { MessagingLogic.displayLabel(it) },
        )
    }
}

class MessagingTileDataSource(context: Context) {
    private val appContext = context.applicationContext
    private val smsSource = SmsMessagingDataSource(appContext)

    fun buildTileData(): MetroTileData {
        val accentHex = MetroPreferences(appContext).accentColorHex
        val peek = if (hasSmsPermission()) {
            runCatching { smsSource.loadTilePeek() }.getOrElse { MessagingTileLogic.peekFromDemoThreads() }
        } else {
            MessagingTileLogic.peekFromDemoThreads()
        }
        return MessagingTileLogic.buildTileData(
            peek = peek,
            packageName = appContext.packageName,
            accentHex = accentHex,
        )
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
}
