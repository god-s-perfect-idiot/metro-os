package com.metro.conversations.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metro.conversations.ConversationsListenerService
import com.metro.conversations.data.AppConversationGroup
import com.metro.conversations.data.ConversationsLogic
import com.metro.conversations.data.ConversationsRepository
import com.metro.conversations.data.FavoriteChat
import com.metro.conversations.data.FavoriteChatStore
import com.metro.conversations.data.HomeTile
import com.metro.conversations.data.ReplyableConversation

sealed class ConversationsRoute {
    data object Home : ConversationsRoute()

    /**
     * [packageName] null = all chats (with all/favorites pivot).
     * [startOnFavorites] selects the favorites pivot page when [packageName] is null.
     */
    data class AppList(
        val packageName: String?,
        val startOnFavorites: Boolean = false,
    ) : ConversationsRoute()

    data class Thread(
        val key: String,
        val listPackageName: String?,
        val listFavorites: Boolean = false,
    ) : ConversationsRoute()
}

class ConversationsState(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val repository = ConversationsRepository(appContext)
    private val favoriteStore = FavoriteChatStore(appContext)

    var generation by mutableIntStateOf(0)
        private set

    var hasAccess by mutableStateOf(false)
        private set

    var groups by mutableStateOf<List<AppConversationGroup>>(emptyList())
        private set

    var favorites by mutableStateOf<Set<FavoriteChat>>(emptySet())
        private set

    var route by mutableStateOf<ConversationsRoute>(ConversationsRoute.Home)
        private set

    var composerText by mutableStateOf("")
        private set

    var sending by mutableStateOf(false)
        private set

    val homeTiles: List<HomeTile>
        get() = ConversationsLogic.homeTiles(groups)

    val selectedConversation: ReplyableConversation?
        get() {
            val thread = route as? ConversationsRoute.Thread ?: return null
            return ConversationsLogic.findByKey(groups, thread.key)
        }

    fun listConversations(packageName: String?): List<ReplyableConversation> =
        ConversationsLogic.conversationsFor(groups, packageName)

    fun favoriteConversations(): List<ReplyableConversation> =
        ConversationsLogic.favoriteConversations(groups, favorites)

    fun isFavorite(conversation: ReplyableConversation): Boolean =
        favorites.any { it.matchesConversation(conversation) }

    fun startListening() {
        ConversationsListenerService.setChangeListener {
            refresh()
        }
        refresh()
    }

    fun stopListening() {
        ConversationsListenerService.setChangeListener(null)
    }

    fun refresh() {
        hasAccess = repository.hasNotificationAccess()
        groups = if (hasAccess) repository.loadGroups() else emptyList()
        favorites = favoriteStore.load()
        pruneStaleRoutes()
        generation++
    }

    fun accessSettingsIntent(): Intent = repository.notificationAccessSettingsIntent()

    fun openAllApps() {
        route = ConversationsRoute.AppList(packageName = null, startOnFavorites = false)
        composerText = ""
        generation++
    }

    fun openFavorites() {
        route = ConversationsRoute.AppList(packageName = null, startOnFavorites = true)
        composerText = ""
        generation++
    }

    fun openApp(packageName: String) {
        route = ConversationsRoute.AppList(packageName = packageName)
        composerText = ""
        generation++
    }

    fun openThread(key: String, fromFavorites: Boolean = false) {
        val listPackage: String?
        val listFavorites: Boolean
        when (val current = route) {
            is ConversationsRoute.AppList -> {
                listPackage = current.packageName
                listFavorites = current.packageName == null && fromFavorites
            }
            is ConversationsRoute.Thread -> {
                listPackage = current.listPackageName
                listFavorites = current.listFavorites
            }
            ConversationsRoute.Home -> {
                listPackage = null
                listFavorites = fromFavorites
            }
        }
        route = ConversationsRoute.Thread(
            key = key,
            listPackageName = listPackage,
            listFavorites = listFavorites,
        )
        composerText = ""
        generation++
    }

    fun goBack() {
        when (val current = route) {
            ConversationsRoute.Home -> Unit
            is ConversationsRoute.AppList -> {
                route = ConversationsRoute.Home
                composerText = ""
            }
            is ConversationsRoute.Thread -> {
                route = ConversationsRoute.AppList(
                    packageName = current.listPackageName,
                    startOnFavorites = current.listFavorites,
                )
                composerText = ""
            }
        }
        generation++
    }

    fun updateComposer(text: String) {
        composerText = text
        generation++
    }

    fun sendReply() {
        val thread = route as? ConversationsRoute.Thread ?: return
        val text = composerText
        if (text.isBlank() || sending) return
        sending = true
        generation++
        val ok = repository.sendReply(thread.key, text)
        sending = false
        if (ok) {
            composerText = ""
        }
        refresh()
    }

    fun openInApp() {
        val thread = route as? ConversationsRoute.Thread ?: return
        repository.openConversation(thread.key)
    }

    fun toggleFavorite(conversation: ReplyableConversation) {
        favoriteStore.toggle(conversation.packageName, conversation.title)
        favorites = favoriteStore.load()
        generation++
    }

    fun dismissConversation(conversation: ReplyableConversation) {
        repository.dismissConversation(conversation.key)
        refresh()
    }

    fun clearAllConversations() {
        repository.clearAllConversations()
        refresh()
    }

    private fun pruneStaleRoutes() {
        when (val current = route) {
            ConversationsRoute.Home -> Unit
            is ConversationsRoute.AppList -> {
                val pkg = current.packageName ?: return
                if (groups.none { it.packageName == pkg }) {
                    route = ConversationsRoute.Home
                    composerText = ""
                }
            }
            is ConversationsRoute.Thread -> {
                if (ConversationsLogic.findByKey(groups, current.key) == null) {
                    val pkg = current.listPackageName
                    route = if (pkg == null || groups.any { it.packageName == pkg }) {
                        ConversationsRoute.AppList(
                            packageName = pkg,
                            startOnFavorites = current.listFavorites,
                        )
                    } else {
                        ConversationsRoute.Home
                    }
                    composerText = ""
                }
            }
        }
    }
}
