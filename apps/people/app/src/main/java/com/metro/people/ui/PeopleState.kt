package com.metro.people.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.metro.people.R
import com.metro.people.data.AccountOption
import com.metro.people.data.ContactAccountTarget
import com.metro.people.data.ContactDraft
import com.metro.people.data.ContactEditorLogic
import com.metro.people.data.ContactPrefill
import com.metro.people.data.ContactWriter
import com.metro.people.data.ContactsRepository
import com.metro.people.data.PeopleContactsLogic
import com.metro.people.data.PeopleFilter
import com.metro.people.data.PersonDetail
import com.metro.people.data.PersonSummary
import com.metro.people.data.WhatsAppLink
import com.metro.people.tiles.PeopleTileLogic
import com.metro.system.MetroIntents

sealed class PeopleRoute {
    data object Hub : PeopleRoute()
    data object Filter : PeopleRoute()
    data object Accounts : PeopleRoute()
    data object NewContact : PeopleRoute()
    data class Detail(val contactId: Long) : PeopleRoute()
}

class PeopleState(context: Context) {
    private val repository = ContactsRepository(context)
    private val contactWriter = ContactWriter(context)
    internal val appContext = context.applicationContext

    /** Bumped on every mutation so Compose recomposes. */
    var generation by mutableIntStateOf(0)
        private set

    private fun notifyChanged() {
        generation++
    }

    var hasContactsPermission: Boolean = false
        private set

    var hasWriteContactsPermission: Boolean = false
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

    var newContactPrefill: ContactPrefill? = null
        private set

    /** When true, open new-contact after contacts permission is granted. */
    private var pendingOpenNewContact: Boolean = false

    /** Draft waiting for WRITE_CONTACTS before insert. */
    private var pendingSaveDraft: ContactDraft? = null

    /** In-app Metro toast message; consumed by [PeopleShell]. */
    var toastMessage by mutableStateOf<String?>(null)
        private set

    val accountOptions: List<AccountOption> = repository.accountOptions()

    val saveAccounts: List<ContactAccountTarget>
        get() = contactWriter.availableAccounts()

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
        hasWriteContactsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        permissionsChecked = true
        if (hasContactsPermission && pendingOpenNewContact) {
            pendingOpenNewContact = false
            openNewContact(newContactPrefill)
        }
        notifyChanged()
    }

    fun onPermissionResult(granted: Boolean) {
        hasContactsPermission = granted || ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        hasWriteContactsPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.WRITE_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasContactsPermission) {
            reloadContacts()
            if (pendingOpenNewContact) {
                pendingOpenNewContact = false
                openNewContact(newContactPrefill)
            }
        }
        notifyChanged()
    }

    /** Returns a draft that was waiting on WRITE_CONTACTS, if write is now granted. */
    fun consumePendingSaveDraft(): ContactDraft? {
        if (!hasWriteContactsPermission) return null
        val draft = pendingSaveDraft ?: return null
        pendingSaveDraft = null
        return draft
    }

    fun queuePendingSaveDraft(draft: ContactDraft) {
        pendingSaveDraft = draft
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

    fun openNewContact(prefill: ContactPrefill? = null) {
        dismissSearch()
        jumpListVisible = false
        newContactPrefill = prefill
        if (!hasContactsPermission) {
            pendingOpenNewContact = true
            notifyChanged()
            return
        }
        route = PeopleRoute.NewContact
        notifyChanged()
    }

    fun closeOverlay() {
        route = PeopleRoute.Hub
        jumpListVisible = false
        newContactPrefill = null
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
        notifyChanged()
    }

    /**
     * Inserts [draft] on the calling thread (use a background dispatcher).
     * Does not mutate Compose state or show UI — callers refresh / toast on main.
     */
    fun saveNewContact(draft: ContactDraft): SaveContactResult {
        // Refresh in case the user just granted write from the system dialog.
        hasWriteContactsPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.WRITE_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasWriteContactsPermission) {
            pendingSaveDraft = draft
            return SaveContactResult.NeedsWritePermission
        }
        if (!ContactEditorLogic.canSave(draft)) {
            return SaveContactResult.NeedsIdentity
        }
        return try {
            val uri = contactWriter.insert(draft)
            if (uri == null) {
                Log.e(TAG, "Contact insert returned null")
                SaveContactResult.Failed
            } else {
                pendingSaveDraft = null
                SaveContactResult.Saved
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save contact", e)
            SaveContactResult.Failed
        }
    }

    enum class SaveContactResult {
        Saved,
        NeedsIdentity,
        NeedsWritePermission,
        Failed,
    }

    fun showToast(message: String) {
        toastMessage = message
        notifyChanged()
    }

    fun showToast(resId: Int) {
        showToast(appContext.getString(resId))
    }

    fun consumeToast() {
        toastMessage = null
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
            showToast(R.string.speed_dial_needs_phone)
            return
        }
        MetroIntents.requestAddSpeedDial(
            context = appContext,
            displayName = person.displayName,
            phoneNumber = number,
        )
        showToast(R.string.added_to_speed_dial)
    }

    fun pinToStart(person: PersonSummary) {
        MetroIntents.requestPinTile(
            context = appContext,
            packageName = MetroIntents.PACKAGE_PEOPLE,
            tileId = PeopleTileLogic.contactTileId(person.id),
        )
        showToast(R.string.pinned_to_start)
    }

    fun deleteContact(person: PersonSummary): Boolean {
        val ok = repository.deleteContact(person.id)
        if (ok) {
            if (route is PeopleRoute.Detail && (route as PeopleRoute.Detail).contactId == person.id) {
                route = PeopleRoute.Hub
            }
            reloadContacts()
            showToast(R.string.contact_deleted)
        } else {
            showToast(R.string.contact_delete_failed)
        }
        return ok
    }

    fun handleLaunchIntent(intent: Intent?) {
        if (intent == null) return
        val prefill = parseContactPrefill(intent)
        if (isNewContactIntent(intent) || prefill != null) {
            openNewContact(prefill)
            return
        }
        openDeepLink(intent.data)
    }

    fun openDeepLink(uri: Uri?) {
        if (isNewContactDeepLink(uri)) {
            openNewContact(prefillFromDeepLink(uri))
            return
        }
        val contactId = PeopleTileLogic.parseContactDeepLink(uri) ?: return
        openDetail(contactId)
    }

    fun textContact(person: PersonSummary) {
        val number = person.defaultPhone ?: return
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    fun whatsAppCall(link: WhatsAppLink) {
        val dataId = link.voiceCallDataId ?: return
        openWhatsAppData(dataId, link.packageName)
    }

    fun whatsAppText(link: WhatsAppLink) {
        val dataId = link.messageDataId
        if (dataId != null) {
            openWhatsAppData(dataId, link.packageName)
            return
        }
        val digits = link.phoneDigits ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")).apply {
            setPackage(link.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appContext.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "WhatsApp text deep-link failed", e)
            showToast(R.string.whatsapp_unavailable)
        }
    }

    private fun openWhatsAppData(dataId: Long, packageName: String) {
        val uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataId)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setData(uri)
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appContext.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "WhatsApp contact action failed", e)
            showToast(R.string.whatsapp_unavailable)
        }
    }

    fun emailContact(address: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    fun showExternalStub(message: String) {
        showToast(message)
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
            setOf(appContext.getString(R.string.account_device))
        } else {
            allContacts.map { it.sourceLabel }.toSet()
        }

    companion object {
        private const val TAG = "PeopleState"

        fun isNewContactIntent(intent: Intent): Boolean {
            if (intent.action == Intent.ACTION_INSERT) return true
            if (intent.action == MetroIntents.ACTION_NEW_CONTACT) return true
            return isNewContactDeepLink(intent.data)
        }

        fun isNewContactDeepLink(uri: Uri?): Boolean {
            if (uri == null) return false
            if (uri.scheme != "metro" || uri.host != "people") return false
            val path = uri.path.orEmpty()
            return path == "/new" || path == "new"
        }

        fun parseContactPrefill(intent: Intent): ContactPrefill? {
            val phone = intent.getStringExtra(ContactsContract.Intents.Insert.PHONE)
                ?: intent.getStringExtra(DialerPrefill.EXTRA_PREFILL_PHONE)
                ?: intent.getStringExtra(MetroIntents.EXTRA_PHONE_NUMBER)
            val email = intent.getStringExtra(ContactsContract.Intents.Insert.EMAIL)
            val name = intent.getStringExtra(ContactsContract.Intents.Insert.NAME)
                ?: intent.getStringExtra(MetroIntents.EXTRA_DISPLAY_NAME)
            val company = intent.getStringExtra(ContactsContract.Intents.Insert.COMPANY)
            val jobTitle = intent.getStringExtra(ContactsContract.Intents.Insert.JOB_TITLE)
            val notes = intent.getStringExtra(ContactsContract.Intents.Insert.NOTES)
            val postal = intent.getStringExtra(ContactsContract.Intents.Insert.POSTAL)
            val deep = prefillFromDeepLink(intent.data)
            val merged = ContactPrefill(
                phone = phone?.trim()?.takeIf { it.isNotEmpty() } ?: deep?.phone,
                email = email?.trim()?.takeIf { it.isNotEmpty() } ?: deep?.email,
                name = name?.trim()?.takeIf { it.isNotEmpty() } ?: deep?.name,
                company = company?.trim()?.takeIf { it.isNotEmpty() } ?: deep?.company,
                jobTitle = jobTitle?.trim()?.takeIf { it.isNotEmpty() },
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                postal = postal?.trim()?.takeIf { it.isNotEmpty() },
            )
            return if (
                merged.phone == null &&
                merged.email == null &&
                merged.name == null &&
                merged.company == null &&
                merged.jobTitle == null &&
                merged.notes == null &&
                merged.postal == null
            ) {
                null
            } else {
                merged
            }
        }

        fun prefillFromDeepLink(uri: Uri?): ContactPrefill? {
            if (!isNewContactDeepLink(uri) || uri == null) return null
            val phone = uri.getQueryParameter("phone")?.trim()?.takeIf { it.isNotEmpty() }
            val email = uri.getQueryParameter("email")?.trim()?.takeIf { it.isNotEmpty() }
            val name = uri.getQueryParameter("name")?.trim()?.takeIf { it.isNotEmpty() }
            val company = uri.getQueryParameter("company")?.trim()?.takeIf { it.isNotEmpty() }
            if (phone == null && email == null && name == null && company == null) {
                return ContactPrefill()
            }
            return ContactPrefill(phone = phone, email = email, name = name, company = company)
        }
    }
}

/** Dialer extras kept in sync with Phone's save-contact launch. */
object DialerPrefill {
    const val EXTRA_PREFILL_PHONE = "com.metro.dialer.extra.PREFILL_PHONE"
}
