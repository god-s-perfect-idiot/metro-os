package com.metro.dialer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.metro.dialer.R
import com.metro.dialer.telecom.MetroTelecomBridge
import com.metro.dialer.data.CallGroup
import com.metro.dialer.data.CallLogRepository
import com.metro.dialer.data.ContactSuggestion
import com.metro.dialer.data.ContactsLookup
import com.metro.dialer.data.DialerCallLogic
import com.metro.dialer.data.SpeedDialEntry
import com.metro.dialer.data.SpeedDialStore

class DialerState(context: Context) {
    private val appContext = context.applicationContext
    private val callLogRepository = CallLogRepository(appContext)
    private val contactsLookup = ContactsLookup(appContext)
    private val speedDialStore = SpeedDialStore(appContext)

    var route by mutableStateOf(DialerRoute.Main)
        private set

    var pivot by mutableStateOf(PhonePivot.History)
        private set

    var hasCallLogPermission by mutableStateOf(false)
        private set

    var hasContactsPermission by mutableStateOf(false)
        private set

    var hasCallPhonePermission by mutableStateOf(false)
        private set

    /** False until the first [refreshPermissions] completes — avoids flashing the permission gate. */
    var permissionsChecked by mutableStateOf(false)
        private set

    var callGroups by mutableStateOf<List<CallGroup>>(emptyList())
        private set

    var speedDialEntries by mutableStateOf<List<SpeedDialEntry>>(emptyList())
        private set

    var contactSuggestions by mutableStateOf<List<ContactSuggestion>>(emptyList())
        private set

    var dialString by mutableStateOf("")
        private set

    var searchQuery by mutableStateOf("")
        private set

    var searchVisible by mutableStateOf(false)
        private set

    var selectedGroup by mutableStateOf<CallGroup?>(null)
        private set

    val filteredGroups: List<CallGroup>
        get() = DialerCallLogic.filterGroups(callGroups, searchQuery)

    val t9Suggestions: List<ContactSuggestion>
        get() = DialerCallLogic.contactSuggestions(
            dialString.filter { it.isDigit() || it == '+' || it == '*' || it == '#' },
            contactSuggestions,
        )

    init {
        refreshPermissions(appContext)
        reloadSpeedDial()
        if (hasContactsPermission) {
            contactSuggestions = contactsLookup.loadPhoneContacts()
        }
        if (hasCallLogPermission) {
            reloadCallLog()
        }
    }

    fun refreshPermissions(context: Context) {
        hasCallLogPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG,
        ) == PackageManager.PERMISSION_GRANTED
        hasContactsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        hasCallPhonePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE,
        ) == PackageManager.PERMISSION_GRANTED
        permissionsChecked = true
    }

    fun onPermissionResult(callLogGranted: Boolean, contactsGranted: Boolean, callPhoneGranted: Boolean) {
        hasCallLogPermission = callLogGranted
        hasContactsPermission = contactsGranted
        hasCallPhonePermission = callPhoneGranted
        if (contactsGranted) {
            contactSuggestions = contactsLookup.loadPhoneContacts()
        }
        if (callLogGranted) {
            reloadCallLog()
        }
    }

    fun handleDialIntent(uri: Uri?) {
        val number = uri?.schemeSpecificPart?.trim().orEmpty()
        if (number.isNotEmpty()) {
            dialString = number
            pivot = PhonePivot.DialPad
            route = DialerRoute.Main
        }
    }

    fun reloadCallLog() {
        if (!hasCallLogPermission) {
            callGroups = emptyList()
            return
        }
        val entries = callLogRepository.loadRecentCalls()
        callGroups = DialerCallLogic.groupCalls(entries)
    }

    fun reloadSpeedDial() {
        speedDialEntries = speedDialStore.load()
    }

    fun setPivot(index: Int) {
        pivot = when (index) {
            1 -> PhonePivot.DialPad
            2 -> PhonePivot.SpeedDial
            else -> PhonePivot.History
        }
    }

    fun openDialPad() {
        pivot = PhonePivot.DialPad
        route = DialerRoute.Main
    }

    fun closeOverlay() {
        route = DialerRoute.Main
        selectedGroup = null
    }

    fun appendDialChar(char: Char) {
        dialString += char
    }

    fun deleteDialChar() {
        if (dialString.isNotEmpty()) {
            dialString = dialString.dropLast(1)
        }
    }

    fun replaceDialString(value: String) {
        dialString = value
    }

    fun toggleSearch() {
        searchVisible = !searchVisible
        if (!searchVisible) {
            searchQuery = ""
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun openCallDetail(group: CallGroup) {
        selectedGroup = group
        route = DialerRoute.CallDetail
    }

    fun addToSpeedDial(group: CallGroup) {
        val entry = SpeedDialEntry(
            id = System.currentTimeMillis().toString(),
            displayName = group.displayName,
            phoneNumber = group.phoneNumber,
        )
        speedDialStore.add(entry)
        reloadSpeedDial()
        Toast.makeText(appContext, R.string.add_speed_dial, Toast.LENGTH_SHORT).show()
    }

    fun removeSpeedDial(entry: SpeedDialEntry) {
        speedDialStore.remove(entry.phoneNumber)
        reloadSpeedDial()
    }

    fun placeCall(number: String, displayName: String? = null) {
        MetroTelecomBridge.placeOutgoingCall(appContext, number, displayName)
    }

    fun launchPeople() {
        val intent = appContext.packageManager.getLaunchIntentForPackage(PEOPLE_PACKAGE)
        if (intent == null) {
            Toast.makeText(appContext, R.string.people_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    fun saveContactInPeople() {
        val trimmed = dialString.trim()
        if (trimmed.isEmpty()) {
            Toast.makeText(appContext, R.string.save_contact_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = appContext.packageManager.getLaunchIntentForPackage(PEOPLE_PACKAGE)
        if (intent == null) {
            Toast.makeText(appContext, R.string.people_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        intent.putExtra(EXTRA_PREFILL_PHONE, trimmed)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    fun showSettingsStub() {
        Toast.makeText(appContext, R.string.settings_stub, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val PEOPLE_PACKAGE = "com.metro.people"
        const val EXTRA_PREFILL_PHONE = "com.metro.dialer.extra.PREFILL_PHONE"
    }
}
