package com.metro.messaging.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.messaging.data.ContactSuggestion
import com.metro.messaging.data.ContactsLookup
import com.metro.messaging.data.ConversationThread
import com.metro.messaging.data.MessageItem
import com.metro.messaging.data.MessagingLogic
import com.metro.messaging.data.MessagingRepository
import com.metro.messaging.data.SendState
import com.metro.messaging.tiles.MessagingTileRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val contactsLookup = ContactsLookup(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var threadsLoadJob: Job? = null
    private var conversationLoadJob: Job? = null
    private var threadsLoadGeneration = 0
    private var conversationLoadGeneration = 0
    private var sendInFlight = false
    private var newSendInFlight = false

    var generation by mutableIntStateOf(0)
        private set

    var route by mutableStateOf<MessagingRoute>(MessagingRoute.Threads)
        private set

    var threads by mutableStateOf<List<ConversationThread>>(emptyList())
        private set

    var messages by mutableStateOf<List<MessageItem>>(emptyList())
        private set

    /** True while [loadConversation] is awaiting SMS/provider IO. */
    var isLoadingMessages by mutableStateOf(false)
        private set

    /**
     * True until the first (or an empty-list) [reloadThreads] completes.
     * Starts true so cold open never flashes the empty placeholder.
     */
    var isLoadingThreads by mutableStateOf(true)
        private set

    var composerText by mutableStateOf("")
        private set

    var newRecipient by mutableStateOf("")
        private set

    var newBody by mutableStateOf("")
        private set

    var contactSuggestions by mutableStateOf<List<ContactSuggestion>>(emptyList())
        private set

    var usingDemoData by mutableStateOf(true)
        private set

    var isDefaultSmsApp by mutableStateOf(false)
        private set

    var skippedPermissions by mutableStateOf(false)
        private set

    private var allContacts: List<ContactSuggestion> = emptyList()

    val needsPermissionGate: Boolean
        get() = !canAccessSystemSms && !skippedPermissions

    val hasReadSmsPermission: Boolean
        get() = repository.hasReadSmsPermission

    val hasSendSmsPermission: Boolean
        get() = repository.hasSendSmsPermission

    val hasContactsPermission: Boolean
        get() = repository.hasContactsPermission

    val canAccessSystemSms: Boolean
        get() = repository.canAccessSystemSms

    fun clear() {
        threadsLoadJob?.cancel()
        conversationLoadJob?.cancel()
        scope.cancel()
    }

    fun refreshPermissions(context: Context = appContext) {
        repository.refreshPermissions(context)
        syncAccessFlags()
        reloadContactsIfNeeded()
        notifyChanged()
    }

    fun refreshDefaultStatus() {
        syncAccessFlags()
        if (canAccessSystemSms) {
            skippedPermissions = false
            reloadThreads()
        }
        notifyChanged()
    }

    fun onPermissionResult(readSms: Boolean, sendSms: Boolean, contacts: Boolean) {
        repository.refreshPermissions()
        syncAccessFlags()
        skippedPermissions = false
        reloadContactsIfNeeded()
        reloadThreads()
        notifyChanged()
    }

    fun continueWithDemo() {
        skippedPermissions = true
        usingDemoData = !canAccessSystemSms
        reloadThreads()
        notifyChanged()
    }

    private fun syncAccessFlags() {
        isDefaultSmsApp = repository.isDefaultSmsApp
        usingDemoData = !repository.canAccessSystemSms
    }

    fun reloadThreads() {
        val generation = ++threadsLoadGeneration
        val openThreadId = (route as? MessagingRoute.Conversation)?.threadId ?: -1L
        threadsLoadJob?.cancel()
        // Only block the threads UI when there is nothing to show yet — silent refresh
        // when the list already has rows.
        if (threads.isEmpty()) {
            isLoadingThreads = true
            notifyChanged()
        }
        threadsLoadJob = scope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    MessagingLogic.markThreadRead(repository.loadThreads(), openThreadId)
                }
                if (generation != threadsLoadGeneration) return@launch
                threads = loaded
                MessagingTileRefresh.request(appContext)
            } finally {
                if (generation == threadsLoadGeneration) {
                    isLoadingThreads = false
                    notifyChanged()
                }
            }
        }
    }

    fun openThread(thread: ConversationThread) {
        route = MessagingRoute.Conversation(thread.id)
        threads = MessagingLogic.markThreadRead(threads, thread.id)
        messages = emptyList()
        composerText = ""
        notifyChanged()
        MessagingTileRefresh.request(appContext)
        loadConversation(thread.id)
    }

    fun openAddress(address: String) {
        scope.launch {
            val thread = withContext(Dispatchers.IO) { repository.threadForAddress(address) }
            val existing = threads.firstOrNull {
                it.id == thread.id ||
                    MessagingLogic.normalizeAddress(it.address) == MessagingLogic.normalizeAddress(thread.address)
            }
            openThread(existing ?: thread)
        }
    }

    fun handleSendToUri(uri: Uri?) {
        val address = uri?.schemeSpecificPart?.trim().orEmpty()
        if (address.isEmpty()) return
        openAddress(address)
    }

    fun startNewMessage() {
        newRecipient = ""
        newBody = ""
        contactSuggestions = emptyList()
        reloadContactsIfNeeded()
        route = MessagingRoute.NewMessage
        notifyChanged()
    }

    fun updateNewRecipient(text: String) {
        newRecipient = text
        contactSuggestions = MessagingLogic.contactSuggestions(text, allContacts)
        notifyChanged()
    }

    fun selectContactSuggestion(suggestion: ContactSuggestion) {
        newRecipient = suggestion.phoneNumber
        contactSuggestions = emptyList()
        notifyChanged()
    }

    fun updateNewBody(text: String) {
        newBody = text
    }

    fun sendNewMessage() {
        if (newSendInFlight) return
        val addressInput = newRecipient
        val body = newBody.trim()
        if (body.isEmpty()) return
        val contactsSnapshot = allContacts

        newSendInFlight = true
        newBody = ""
        notifyChanged()
        scope.launch {
            try {
                val address = MessagingLogic.resolveRecipientAddress(addressInput, contactsSnapshot)
                if (address.isEmpty()) {
                    newBody = body
                    notifyChanged()
                    return@launch
                }

                val (thread, sent, conversationMessages) = withContext(Dispatchers.IO) {
                    val thread = repository.threadForAddress(address)
                    val sent = repository.sendMessage(thread, body)
                    Triple(thread, sent, repository.loadMessages(thread.id))
                }

                newRecipient = ""
                contactSuggestions = emptyList()
                route = MessagingRoute.Conversation(thread.id)
                messages = conversationMessages
                notifyChanged()
                reloadThreads()
                if (sent.sendState == SendState.Failed) {
                    Toast.makeText(appContext, "Message failed to send", Toast.LENGTH_SHORT).show()
                }
            } finally {
                newSendInFlight = false
            }
        }
    }

    fun backToThreads() {
        route = MessagingRoute.Threads
        composerText = ""
        newRecipient = ""
        newBody = ""
        contactSuggestions = emptyList()
        messages = emptyList()
        conversationLoadJob?.cancel()
        isLoadingMessages = false
        notifyChanged()
        reloadThreads()
    }

    private fun reloadContactsIfNeeded() {
        if (!repository.hasContactsPermission) {
            allContacts = emptyList()
            contactSuggestions = emptyList()
            return
        }
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                runCatching { contactsLookup.loadPhoneContacts() }.getOrDefault(emptyList())
            }
            allContacts = loaded
            if (route is MessagingRoute.NewMessage && newRecipient.isNotBlank()) {
                contactSuggestions = MessagingLogic.contactSuggestions(newRecipient, allContacts)
                notifyChanged()
            }
        }
    }

    fun loadConversation(threadId: Long) {
        val generation = ++conversationLoadGeneration
        conversationLoadJob?.cancel()
        isLoadingMessages = true
        notifyChanged()
        conversationLoadJob = scope.launch {
            try {
                val (loadedMessages, draft) = withContext(Dispatchers.IO) {
                    repository.loadMessages(threadId) to
                        (repository.loadDraft(threadId)?.text.orEmpty())
                }
                if (generation != conversationLoadGeneration) return@launch
                if ((route as? MessagingRoute.Conversation)?.threadId != threadId) return@launch
                messages = loadedMessages
                composerText = draft
            } finally {
                if (generation == conversationLoadGeneration) {
                    isLoadingMessages = false
                    notifyChanged()
                }
            }
        }
    }

    fun updateComposer(text: String) {
        composerText = text
        val threadId = (route as? MessagingRoute.Conversation)?.threadId ?: return
        scope.launch(Dispatchers.IO) {
            repository.saveDraft(threadId, text)
        }
    }

    fun sendMessage() {
        if (sendInFlight) return
        val threadId = (route as? MessagingRoute.Conversation)?.threadId ?: return
        val body = composerText.trim()
        if (body.isEmpty()) return
        val threadSnapshot = threads.firstOrNull { it.id == threadId }

        sendInFlight = true
        composerText = ""
        notifyChanged()

        scope.launch {
            try {
                val (sent, conversationMessages) = withContext(Dispatchers.IO) {
                    val thread = threadSnapshot
                        ?: repository.threadForAddress(threadId.toString())
                    val sent = repository.sendMessage(thread, body)
                    sent to repository.loadMessages(threadId)
                }
                if ((route as? MessagingRoute.Conversation)?.threadId == threadId) {
                    messages = conversationMessages
                    notifyChanged()
                }
                reloadThreads()
                if (sent.sendState == SendState.Failed) {
                    Toast.makeText(appContext, "Message failed to send", Toast.LENGTH_SHORT).show()
                }
            } finally {
                sendInFlight = false
            }
        }
    }

    fun deleteThread(thread: ConversationThread) {
        Toast.makeText(appContext, "Delete not available in v1", Toast.LENGTH_SHORT).show()
        notifyChanged()
    }

    private fun notifyChanged() {
        generation++
    }
}
