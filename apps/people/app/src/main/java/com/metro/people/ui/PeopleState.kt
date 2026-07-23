package com.metro.people.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.metro.people.R
import com.metro.people.data.AccountOption
import com.metro.people.data.ContactsRepository
import com.metro.people.data.PeopleContactsLogic
import com.metro.people.data.PeopleFilter
import com.metro.people.data.PersonDetail
import com.metro.people.data.PersonSummary
import com.metro.people.tiles.PeopleTileLogic
import com.metro.system.MetroIntents

sealed class PeopleRoute {
    data object Hub : PeopleRoute()
    data object Filter : PeopleRoute()
    data object Accounts : PeopleRoute()
    data class Detail(val contactId: Long) : PeopleRoute()
}

class PeopleState(context: Context) {
    private val repository = ContactsRepository(context)
    internal val appContext = context.applicationContext

    /** Bumped on every mutation so Compose recomposes. */
    var generation by mutableIntStateOf(0)
        private set

    private fun notifyChanged() {
        generation++
    }

    var hasContactsPermission: Boolean = false
        private set

    /** False until the first [refreshPermission] completes — avoids flashing the permission gate. */
    var permissionsChecked: Boolean = false
        private set

    var route: PeopleRoute = PeopleRoute.Hub
        private set

    var allContacts: List<PersonSummary> = emptyList()
        private set

    var filter: PeopleFilter = PeopleFilter(hideWithoutPhone = true)
        private set

    var searchQuery: String = ""
        private set

    var searchVisible: Boolean = false
        private set

    var jumpListVisible: Boolean = false
        private set

    var selectedDetail: PersonDetail? = null
        private set

    val accountOptions: List<AccountOption> = repository.accountOptions()

    private var knownAccounts: Set<String> = emptySet()

    val visibleContacts: List<PersonSummary>
        get() {
            val filtered = PeopleContactsLogic.applyFilter(allContacts, filter, knownAccounts)
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return filtered
            return filtered.filter { it.displayName.lowercase().contains(query) }
        }

    val groupedContacts: Map<Char, List<PersonSummary>>
        get() = PeopleContactsLogic.groupBySortKey(visibleContacts)

    val filterLabel: String
        get() = PeopleContactsLogic.filterLabel(filter)

    fun refreshPermission(context: Context) {
        hasContactsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        permissionsChecked = true
        notifyChanged()
    }

    fun onPermissionResult(granted: Boolean) {
        hasContactsPermission = granted
        if (granted) reloadContacts()
        notifyChanged()
    }

    fun reloadContacts() {
        if (!hasContactsPermission) return
        allContacts = repository.loadContacts()
        knownAccounts = repository.discoverAccounts(allContacts)
        if (filter.visibleAccounts.isEmpty()) {
            filter = filter.copy(visibleAccounts = knownAccounts)
        }
        notifyChanged()
    }

    fun openFilter() {
        dismissSearch()
        route = PeopleRoute.Filter
        notifyChanged()
    }

    fun openAccounts() {
        dismissSearch()
        route = PeopleRoute.Accounts
        notifyChanged()
    }

    fun closeOverlay() {
        route = PeopleRoute.Hub
        jumpListVisible = false
        notifyChanged()
    }

    fun openDetail(contactId: Long) {
        dismissSearch()
        selectedDetail = repository.loadDetail(contactId)
        route = PeopleRoute.Detail(contactId)
        notifyChanged()
    }

    fun openSearch() {
        jumpListVisible = false
        searchVisible = true
        notifyChanged()
    }

    fun dismissSearch() {
        if (!searchVisible && searchQuery.isEmpty()) return
        searchVisible = false
        searchQuery = ""
        notifyChanged()
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        notifyChanged()
    }

    fun saveFilter(newFilter: PeopleFilter) {
        filter = newFilter
        route = PeopleRoute.Hub
        notifyChanged()
    }

    fun callContact(person: PersonSummary) {
        val number = person.defaultPhone ?: return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    fun addToSpeedDial(person: PersonSummary) {
        val number = person.defaultPhone
        if (number.isNullOrBlank()) {
            Toast.makeText(appContext, R.string.speed_dial_needs_phone, Toast.LENGTH_SHORT).show()
            return
        }
        MetroIntents.requestAddSpeedDial(
            context = appContext,
            displayName = person.displayName,
            phoneNumber = number,
        )
        Toast.makeText(appContext, R.string.added_to_speed_dial, Toast.LENGTH_SHORT).show()
    }

    fun pinToStart(person: PersonSummary) {
        MetroIntents.requestPinTile(
            context = appContext,
            packageName = MetroIntents.PACKAGE_PEOPLE,
            tileId = PeopleTileLogic.contactTileId(person.id),
        )
        Toast.makeText(appContext, R.string.pinned_to_start, Toast.LENGTH_SHORT).show()
    }

    fun openDeepLink(uri: Uri?) {
        val contactId = PeopleTileLogic.parseContactDeepLink(uri) ?: return
        openDetail(contactId)
    }

    fun textContact(person: PersonSummary) {
        val number = person.defaultPhone ?: return
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    fun emailContact(address: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    fun showExternalStub(message: String) {
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }

    fun toggleJumpList() {
        if (searchVisible) return
        jumpListVisible = !jumpListVisible
        notifyChanged()
    }

    fun dismissJumpList() {
        jumpListVisible = false
        notifyChanged()
    }

    fun knownAccounts(): Set<String> =
        if (allContacts.isEmpty()) {
            setOf(appContext.getString(com.metro.people.R.string.account_device))
        } else {
            allContacts.map { it.sourceLabel }.toSet()
        }
}
