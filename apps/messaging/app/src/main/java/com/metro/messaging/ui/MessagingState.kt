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
import com.metro.messaging.data.MessagingSessionStore
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

    data class ImageViewer(val threadId: Long, val uri: String) : MessagingRoute()
}

class MessagingState(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val repository = MessagingRepository(appContext)
    private val contactsLookup = ContactsLookup(appContext)
    private val sessionStore = MessagingSessionStore(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var threadsLoadJob: Job? = null
    private var conversationLoadJob: Job? = null
    private var threadsLoadGeneration = 0
    private var conversationLoadGeneration = 0
    private var sendInFlight = false
    private var newSendInFlight = false
    private var sessionRestored = false
    private var deepLinkHandled = false
    private val messageCache = LinkedHashMap<Long, List<MessageItem>>(16, 0.75f, true)

    var generation by mutableIntStateOf(0)
        private set

    var route by mutableStateOf<MessagingRoute>(MessagingRoute.Threads)
        private set

    var threads by mutableStateOf<List<ConversationThread>>(emptyList())
        private set

    var messages by mutableStateOf<List<MessageItem>>(emptyList())
        private set

    /** True while [loadConversation] is awaiting SMS/provider IO and no cache is ready. */
    var isLoadingMessages by mutableStateOf(false)
        private set

    /**
     * True only while the first paint has nothing to show.
     * Disk snapshots and silent refreshes never flip this back to a blocking loader.
     */
    var isLoadingThreads by mutableStateOf(false)
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

    /** Synced in init — UI never waits on a permission-check loader. */
    var permissionsChecked by mutableStateOf(true)
        private set

    /** 0 = threads (all), 1 = chats (saved contacts). */
    var pivotPage by mutableIntStateOf(sessionStore.lastPivotPage())
        private set

    private var allContacts: List<ContactSuggestion> = emptyList()
    /** threadId → opened-at millis; suppresses unread until a newer message arrives. */
    private var localReadAtByThread: Map<Long, Long> = sessionStore.loadLocalReadAt()

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

    /** Saved-contact conversations for the chats pivot. */
    val chatThreads: List<ConversationThread>
        get() = MessagingLogic.chatThreads(threads)

    init {
        repository.refreshPermissions(appContext)
        syncAccessFlags()
        repository.seedContactNames(sessionStore.loadContactNames())
        val snapshot = sessionStore.loadThreadSnapshot()
        if (snapshot.isNotEmpty()) {
            threads = applyReadState(snapshot)
        }
        sessionStore.lastOpenedThreadId()?.let { threadId ->
            val cachedMessages = sessionStore.loadMessagesSnapshot(threadId)
            if (cachedMessages.isNotEmpty()) {
                putMessageCache(threadId, cachedMessages)
            }
        }
    }

    fun clear() {
        threadsLoadJob?.cancel()
        conversationLoadJob?.cancel()
        scope.cancel()
    }

    fun selectPivotPage(page: Int) {
        val clamped = page.coerceIn(0, 1)
        if (pivotPage == clamped) return
        pivotPage = clamped
        sessionStore.savePivotPage(clamped)
        notifyChanged()
    }

    fun refreshPermissions(context: Context = appContext) {
        repository.refreshPermissions(context)
        syncAccessFlags()
        reloadContactsIfNeeded()
        permissionsChecked = true
        notifyChanged()
    }

    fun refreshDefaultStatus() {
        syncAccessFlags()
        if (canAccessSystemSms) {
            skippedPermissions = false
        }
        notifyChanged()
    }

    fun onPermissionResult(readSms: Boolean, sendSms: Boolean, contacts: Boolean) {
        repository.refreshPermissions()
        syncAccessFlags()
        skippedPermissions = false
        reloadContactsIfNeeded()
        reloadThreads(force = true)
        notifyChanged()
    }

    fun continueWithDemo() {
        skippedPermissions = true
        usingDemoData = !canAccessSystemSms
        reloadThreads(force = true)
        notifyChanged()
    }

    private fun syncAccessFlags() {
        isDefaultSmsApp = repository.isDefaultSmsApp
        usingDemoData = !repository.canAccessSystemSms
    }

    fun reloadThreads(force: Boolean = false) {
        if (!force && threadsLoadJob?.isActive == true) return
        val generation = ++threadsLoadGeneration
        threadsLoadJob?.cancel()
        // Never block the UI when a snapshot (or prior load) already painted rows.
        if (threads.isEmpty()) {
            isLoadingThreads = true
            notifyChanged()
        }
        threadsLoadJob = scope.launch {
            try {
                repository.seedContactNames(sessionStore.loadContactNames())
                val fast = withContext(Dispatchers.IO) { repository.loadThreadsFast() }
                if (generation != threadsLoadGeneration) return@launch
                publishThreads(fast)
                isLoadingThreads = false

                val enriched = withContext(Dispatchers.IO) { repository.enrichThreads(fast) }
                if (generation != threadsLoadGeneration) return@launch
                publishThreads(enriched)
                sessionStore.saveContactNames(repository.knownContactNames())
                MessagingTileRefresh.request(appContext)
            } finally {
                if (generation == threadsLoadGeneration) {
                    isLoadingThreads = false
                    notifyChanged()
                }
            }
        }
    }

    /** Applies local + currently-open read state, then publishes and snapshots. */
    private fun publishThreads(loaded: List<ConversationThread>) {
        val next = applyReadState(loaded)
        if (next == threads) return
        threads = next
        sessionStore.saveThreadSnapshot(next)
        notifyChanged()
    }

    private fun applyReadState(loaded: List<ConversationThread>): List<ConversationThread> {
        val openThreadId = (route as? MessagingRoute.Conversation)?.threadId ?: -1L
        return MessagingLogic.markThreadRead(
            MessagingLogic.applyLocalReads(loaded, localReadAtByThread),
            openThreadId,
        )
    }

    private fun rememberThreadRead(thread: ConversationThread) {
        val readAt = maxOf(thread.timestamp, System.currentTimeMillis())
        localReadAtByThread = localReadAtByThread + (thread.id to readAt)
        sessionStore.saveLocalReadAt(thread.id, readAt)
        threads = MessagingLogic.markThreadRead(threads, thread.id)
        sessionStore.saveThreadSnapshot(threads)
    }

    /**
     * Re-opens the last conversation immediately on cold start (unless a deep link wins).
     * Safe to call before the thread list finishes loading.
     */
    fun restoreSessionIfNeeded() {
        if (sessionRestored || deepLinkHandled) return
        sessionRestored = true
        val threadId = sessionStore.lastOpenedThreadId() ?: return
        if (route !is MessagingRoute.Threads) return
        val thread = threads.firstOrNull { it.id == threadId }
            ?: ConversationThread(
                id = threadId,
                address = threadId.toString(),
                displayName = null,
                preview = "",
                timestamp = 0L,
                unreadCount = 0,
            )
        openThread(thread, persist = false)
    }

    /** Prevents session restore when an sms:/smsto: intent is about to navigate. */
    fun suppressSessionRestore() {
        deepLinkHandled = true
        sessionRestored = true
    }

    fun openThread(thread: ConversationThread, persist: Boolean = true) {
        route = MessagingRoute.Conversation(thread.id)
        rememberThreadRead(thread)
        if (persist) {
            sessionStore.saveLastOpenedThreadId(thread.id)
        }
        val cached = messageCache[thread.id]
            ?: sessionStore.loadMessagesSnapshot(thread.id).also { disk ->
                if (disk.isNotEmpty()) putMessageCache(thread.id, disk)
            }.takeIf { it.isNotEmpty() }
        if (cached != null) {
            messages = cached
            isLoadingMessages = false
        } else {
            messages = emptyList()
        }
        composerText = ""
        notifyChanged()
        MessagingTileRefresh.request(appContext)
        scope.launch(Dispatchers.IO) {
            repository.markThreadRead(thread.id)
        }
        loadConversation(thread.id, preferCache = cached != null)
    }

    fun openAddress(address: String) {
        deepLinkHandled = true
        sessionRestored = true
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
        sessionStore.saveLastOpenedThreadId(null)
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
                putMessageCache(thread.id, conversationMessages)
                sessionStore.saveLastOpenedThreadId(thread.id)
                sessionStore.saveMessagesSnapshot(thread.id, conversationMessages)
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

    fun openImage(threadId: Long, uri: String) {
        route = MessagingRoute.ImageViewer(threadId = threadId, uri = uri)
        notifyChanged()
    }

    /** Pops image viewer → conversation, or conversation/new → threads. */
    fun goBack() {
        when (val current = route) {
            is MessagingRoute.ImageViewer -> {
                route = MessagingRoute.Conversation(current.threadId)
                notifyChanged()
            }
            else -> backToThreads()
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
        sessionStore.saveLastOpenedThreadId(null)
        notifyChanged()
        // Do not reloadThreads() here — a two-phase Telephony refresh was rewriting the list
        // on every back and flickering rows. Resume / send paths still refresh.
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

    fun loadConversation(threadId: Long, preferCache: Boolean = false) {
        val generation = ++conversationLoadGeneration
        conversationLoadJob?.cancel()
        // Keep showing cached messages while a silent refresh runs — never flash the loader.
        if (!preferCache && messages.isEmpty()) {
            isLoadingMessages = true
            notifyChanged()
        }
        conversationLoadJob = scope.launch {
            try {
                val (loadedMessages, draft) = withContext(Dispatchers.IO) {
                    repository.loadMessages(threadId) to
                        (repository.loadDraft(threadId)?.text.orEmpty())
                }
                if (generation != conversationLoadGeneration) return@launch
                if ((route as? MessagingRoute.Conversation)?.threadId != threadId) return@launch
                messages = loadedMessages
                putMessageCache(threadId, loadedMessages)
                sessionStore.saveMessagesSnapshot(threadId, loadedMessages)
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
                    putMessageCache(threadId, conversationMessages)
                    sessionStore.saveMessagesSnapshot(threadId, conversationMessages)
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

    private fun putMessageCache(threadId: Long, messages: List<MessageItem>) {
        messageCache[threadId] = messages
        while (messageCache.size > MESSAGE_CACHE_LIMIT) {
            val eldestKey = messageCache.keys.firstOrNull() ?: break
            messageCache.remove(eldestKey)
        }
    }

    private fun notifyChanged() {
        generation++
    }

    companion object {
        private const val MESSAGE_CACHE_LIMIT = 8
    }
}
