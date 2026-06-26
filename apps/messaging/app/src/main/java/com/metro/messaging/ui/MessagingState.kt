package com.metro.messaging.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.messaging.data.ConversationThread
import com.metro.messaging.data.MessageItem
import com.metro.messaging.data.MessagingLogic
import com.metro.messaging.data.MessagingRepository

sealed class MessagingRoute {
    data object Threads : MessagingRoute()

    data object NewMessage : MessagingRoute()

    data class Conversation(val threadId: Long) : MessagingRoute()
}

class MessagingState(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val repository = MessagingRepository(appContext)

    var generation by mutableIntStateOf(0)
        private set

    var route by mutableStateOf<MessagingRoute>(MessagingRoute.Threads)
        private set

    var threads by mutableStateOf<List<ConversationThread>>(emptyList())
        private set

    var messages by mutableStateOf<List<MessageItem>>(emptyList())
        private set

    var composerText by mutableStateOf("")
        private set

    var newRecipient by mutableStateOf("")
        private set

    var newBody by mutableStateOf("")
        private set

    var usingDemoData by mutableStateOf(true)
        private set

    var isDefaultSmsApp by mutableStateOf(false)
        private set

    var skippedPermissions by mutableStateOf(false)
        private set

    val needsPermissionGate: Boolean
        get() = !hasReadSmsPermission && !skippedPermissions

    val hasReadSmsPermission: Boolean
        get() = repository.hasReadSmsPermission

    val hasSendSmsPermission: Boolean
        get() = repository.hasSendSmsPermission

    fun refreshPermissions(context: Context = appContext) {
        repository.refreshPermissions(context)
        usingDemoData = !repository.hasReadSmsPermission
        isDefaultSmsApp = repository.isDefaultSmsApp
        notifyChanged()
    }

    fun refreshDefaultStatus() {
        isDefaultSmsApp = repository.isDefaultSmsApp
        notifyChanged()
    }

    fun onPermissionResult(readSms: Boolean, sendSms: Boolean, contacts: Boolean) {
        repository.refreshPermissions()
        usingDemoData = !repository.hasReadSmsPermission
        skippedPermissions = false
        reloadThreads()
        notifyChanged()
    }

    fun continueWithDemo() {
        skippedPermissions = true
        usingDemoData = true
        reloadThreads()
        notifyChanged()
    }

    fun reloadThreads() {
        threads = MessagingLogic.markThreadRead(
            repository.loadThreads(),
            (route as? MessagingRoute.Conversation)?.threadId ?: -1L,
        )
        notifyChanged()
    }

    fun openThread(thread: ConversationThread) {
        route = MessagingRoute.Conversation(thread.id)
        threads = MessagingLogic.markThreadRead(threads, thread.id)
        loadConversation(thread.id)
        notifyChanged()
    }

    fun openAddress(address: String) {
        val thread = repository.threadForAddress(address)
        val existing = threads.firstOrNull {
            it.id == thread.id ||
                MessagingLogic.normalizeAddress(it.address) == MessagingLogic.normalizeAddress(thread.address)
        }
        openThread(existing ?: thread)
    }

    fun handleSendToUri(uri: Uri?) {
        val address = uri?.schemeSpecificPart?.trim().orEmpty()
        if (address.isEmpty()) return
        openAddress(address)
    }

    fun startNewMessage() {
        newRecipient = ""
        newBody = ""
        route = MessagingRoute.NewMessage
        notifyChanged()
    }

    fun updateNewRecipient(text: String) {
        newRecipient = text
    }

    fun updateNewBody(text: String) {
        newBody = text
    }

    fun sendNewMessage() {
        val address = newRecipient.trim()
        val body = newBody.trim()
        if (address.isEmpty() || body.isEmpty()) return

        val thread = repository.threadForAddress(address)
        val sent = repository.sendMessage(thread, body)
        newRecipient = ""
        newBody = ""
        route = MessagingRoute.Conversation(thread.id)
        messages = repository.loadMessages(thread.id)
        reloadThreads()
        if (sent.sendState == com.metro.messaging.data.SendState.Failed) {
            Toast.makeText(appContext, "Message failed to send", Toast.LENGTH_SHORT).show()
        }
        notifyChanged()
    }

    fun backToThreads() {
        route = MessagingRoute.Threads
        composerText = ""
        newRecipient = ""
        newBody = ""
        reloadThreads()
    }

    fun loadConversation(threadId: Long) {
        messages = repository.loadMessages(threadId)
        composerText = repository.loadDraft(threadId)?.text.orEmpty()
        notifyChanged()
    }

    fun updateComposer(text: String) {
        composerText = text
        val threadId = (route as? MessagingRoute.Conversation)?.threadId ?: return
        repository.saveDraft(threadId, text)
    }

    fun sendMessage() {
        val threadId = (route as? MessagingRoute.Conversation)?.threadId ?: return
        val thread = threads.firstOrNull { it.id == threadId }
            ?: repository.threadForAddress(threadId.toString())
        val body = composerText.trim()
        if (body.isEmpty()) return

        val sent = repository.sendMessage(thread, body)
        composerText = ""
        messages = repository.loadMessages(threadId)
        reloadThreads()
        if (sent.sendState == com.metro.messaging.data.SendState.Failed) {
            Toast.makeText(appContext, "Message failed to send", Toast.LENGTH_SHORT).show()
        }
        notifyChanged()
    }

    fun deleteThread(thread: ConversationThread) {
        Toast.makeText(appContext, "Delete not available in v1", Toast.LENGTH_SHORT).show()
        notifyChanged()
    }

    private fun notifyChanged() {
        generation++
    }
}
