package com.metro.messaging.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat

class MessagingRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val draftStore = DraftStore(appContext)
    private val localStore = LocalMessageStore(appContext)
    private val smsSource = SmsMessagingDataSource(appContext)

    var hasReadSmsPermission: Boolean = false
        private set
    var hasSendSmsPermission: Boolean = false
        private set
    var hasContactsPermission: Boolean = false
        private set

    val isDefaultSmsApp: Boolean
        get() = DefaultSmsApp.isDefault(appContext)

    /**
     * System SMS/MMS provider access: either the runtime READ_SMS grant, or holding the default
     * SMS role (which can read/write the Telephony provider even when the runtime flag lags).
     */
    val canAccessSystemSms: Boolean
        get() = hasReadSmsPermission || isDefaultSmsApp

    private val canSendSms: Boolean
        get() = hasSendSmsPermission || isDefaultSmsApp

    fun refreshPermissions(context: Context = appContext) {
        hasReadSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        hasSendSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        hasContactsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun loadThreads(): List<ConversationThread> {
        val smsThreads = if (canAccessSystemSms) {
            runCatching { smsSource.loadThreads() }.getOrElse { emptyList() }
        } else {
            emptyList()
        }
        val localThreads = localStore.localThreads()
        val demoThreads = if (canAccessSystemSms) emptyList() else StubMessagingDataSource.demoThreads()
        return MessagingLogic.mergeThreads(smsThreads, localThreads, demoThreads)
    }

    fun loadMessages(threadId: Long): List<MessageItem> {
        val smsMessages = if (canAccessSystemSms) {
            runCatching { smsSource.loadMessages(threadId) }.getOrElse { emptyList() }
        } else {
            StubMessagingDataSource.demoMessages(threadId)
        }
        val localMessages = localStore.messagesForThread(threadId)
        return MessagingLogic.mergeMessages(smsMessages, localMessages)
    }

    fun threadForAddress(address: String): ConversationThread {
        val normalized = MessagingLogic.normalizeAddress(address)
        if (canAccessSystemSms) {
            smsSource.threadForAddress(normalized)?.let { return it }
            val systemThreadId = smsSource.threadIdForAddress(normalized)
            return ConversationThread(
                id = systemThreadId,
                address = normalized,
                displayName = null,
                preview = "",
                timestamp = System.currentTimeMillis(),
                unreadCount = 0,
            )
        }
        val stubId = MessagingLogic.threadIdForAddress(normalized)
        loadThreads().firstOrNull { it.id == stubId || MessagingLogic.normalizeAddress(it.address) == normalized }
            ?.let { return it }
        return StubMessagingDataSource.threadForAddress(normalized, null)
    }

    fun loadDraft(threadId: Long): DraftState? = draftStore.load(threadId)

    fun saveDraft(threadId: Long, text: String) {
        draftStore.save(DraftState(threadId, text))
    }

    fun clearDraft(threadId: Long) {
        draftStore.clear(threadId)
    }

    fun sendMessage(thread: ConversationThread, body: String): MessageItem {
        val trimmed = body.trim()
        require(trimmed.isNotEmpty()) { "Cannot send empty message" }
        val messageId = System.nanoTime()
        val pending = MessageItem(
            id = messageId,
            threadId = thread.id,
            body = trimmed,
            timestamp = System.currentTimeMillis(),
            direction = MessageDirection.Outgoing,
            sendState = SendState.Sending,
        )
        localStore.append(pending, thread.address)
        clearDraft(thread.id)

        if (canSendSms) {
            return try {
                obtainSmsManager(appContext).sendText(thread.address, trimmed)
                // The default SMS app is responsible for persisting outgoing messages to the
                // provider; the platform no longer writes them automatically.
                if (isDefaultSmsApp) {
                    persistSentToProvider(thread.address, trimmed, pending.timestamp)
                    // Drop the local overlay so mergeMessages does not show the same send twice
                    // (provider _ID ≠ local nanoTime id).
                    localStore.remove(thread.id, pending.id)
                    pending.copy(sendState = SendState.Sent)
                } else {
                    val sent = pending.copy(sendState = SendState.Sent)
                    localStore.update(sent)
                    sent
                }
            } catch (_: Exception) {
                val failed = pending.copy(sendState = SendState.Failed)
                localStore.update(failed)
                failed
            }
        }

        val simulated = pending.copy(sendState = SendState.Sent)
        localStore.update(simulated)
        return simulated
    }

    private fun persistSentToProvider(address: String, body: String, timestamp: Long) {
        runCatching {
            val threadId = smsSource.threadIdForAddress(address)
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestamp)
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                put(Telephony.Sms.THREAD_ID, threadId)
            }
            appContext.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
        }
    }
}
