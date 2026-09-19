package com.metro.people.data

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import com.metro.people.R

class ContactsRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    fun loadContacts(): List<PersonSummary> {
        val phoneById = loadPhoneNumbers()
        val emailById = loadEmails()
        val results = mutableListOf<PersonSummary>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
        )
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            "${ContactsContract.Contacts.IN_VISIBLE_GROUP}=1",
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
            val hasPhoneCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol)?.trim().orEmpty()
                if (name.isEmpty()) continue
                val hasPhone = cursor.getInt(hasPhoneCol) > 0
                val phone = phoneById[id]?.firstOrNull()
                val email = emailById[id]?.firstOrNull()
                results += PersonSummary(
                    id = id,
                    displayName = name,
                    photoUri = cursor.getString(photoCol),
                    hasPhone = hasPhone,
                    defaultPhone = phone,
                    defaultEmail = email,
                    sourceLabel = context.getString(R.string.account_device),
                    sortKey = PeopleContactsLogic.sortKeyFor(name, sortByLastName = false),
                )
            }
        }
        return results
    }

    fun loadDetail(contactId: Long): PersonDetail? {
        val summary = loadContacts().firstOrNull { it.id == contactId } ?: return null
        val phones = loadPhoneNumbers()[contactId].orEmpty().map {
            ContactMethod(ContactMethodType.Phone, "mobile", it)
        }
        val emails = loadEmails()[contactId].orEmpty().map {
            ContactMethod(ContactMethodType.Email, "email", it)
        }
        return PersonDetail(
            summary = summary,
            phones = phones,
            emails = emails,
            whatsApp = loadWhatsAppLink(contactId, summary.defaultPhone),
        )
    }

    fun discoverAccounts(contacts: List<PersonSummary>): Set<String> =
        contacts.map { it.sourceLabel }.toSet()

    fun accountOptions(): List<AccountOption> = listOf(
        AccountOption("exchange", "Exchange", "Exchange, Office 365"),
        AccountOption("outlook", "Outlook.com", "Outlook.com, Live.com, Hotmail.com, MSN"),
        AccountOption("yahoo", "Yahoo! Mail"),
        AccountOption("google", "Google"),
        AccountOption("icloud", "iCloud"),
        AccountOption("facebook", "Facebook"),
        AccountOption("twitter", "Twitter"),
        AccountOption("linkedin", "LinkedIn"),
    )

    private fun loadWhatsAppLink(contactId: Long, defaultPhone: String?): WhatsAppLink? {
        val installed = buildSet {
            if (isPackageInstalled(WhatsAppContactLogic.PACKAGE)) add(WhatsAppContactLogic.PACKAGE)
            if (isPackageInstalled(WhatsAppContactLogic.PACKAGE_BUSINESS)) {
                add(WhatsAppContactLogic.PACKAGE_BUSINESS)
            }
        }
        if (installed.isEmpty()) return null

        val dataRows = loadWhatsAppDataRows(contactId)
        val synced = loadWhatsAppSyncedPackages(contactId)
        return WhatsAppContactLogic.resolve(
            dataRows = dataRows,
            syncedPackages = synced,
            installedPackages = installed,
            phoneDigits = WhatsAppContactLogic.digitsOnly(defaultPhone),
        )
    }

    private fun loadWhatsAppDataRows(contactId: Long): List<WhatsAppDataRow> {
        val mimePlaceholders = WhatsAppContactLogic.ALL_MIMES.joinToString(",") { "?" }
        val selection = "${ContactsContract.Data.CONTACT_ID}=? AND " +
            "${ContactsContract.Data.MIMETYPE} IN ($mimePlaceholders)"
        val args = arrayOf(contactId.toString()) + WhatsAppContactLogic.ALL_MIMES
        val rows = mutableListOf<WhatsAppDataRow>()
        resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID, ContactsContract.Data.MIMETYPE),
            selection,
            args,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID)
            val mimeCol = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
            while (cursor.moveToNext()) {
                val mime = cursor.getString(mimeCol) ?: continue
                rows += WhatsAppDataRow(
                    dataId = cursor.getLong(idCol),
                    mimeType = mime,
                )
            }
        }
        return rows
    }

    private fun loadWhatsAppSyncedPackages(contactId: Long): Set<String> {
        val synced = mutableSetOf<String>()
        resolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts.ACCOUNT_TYPE),
            "${ContactsContract.RawContacts.CONTACT_ID}=?",
            arrayOf(contactId.toString()),
            null,
        )?.use { cursor ->
            val typeCol = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE)
            while (cursor.moveToNext()) {
                WhatsAppContactLogic.packageForAccountType(cursor.getString(typeCol))
                    ?.let { synced += it }
            }
        }
        return synced
    }

    private fun isPackageInstalled(packageName: String): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    private fun loadPhoneNumbers(): Map<Long, List<String>> {
        val map = mutableMapOf<Long, MutableList<String>>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numberCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val number = cursor.getString(numberCol)?.trim().orEmpty()
                if (number.isNotEmpty()) {
                    map.getOrPut(id) { mutableListOf() }.add(number)
                }
            }
        }
        return map
    }

    private fun loadEmails(): Map<Long, List<String>> {
        val map = mutableMapOf<Long, MutableList<String>>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
        )
        resolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val emailCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val email = cursor.getString(emailCol)?.trim().orEmpty()
                if (email.isNotEmpty()) {
                    map.getOrPut(id) { mutableListOf() }.add(email)
                }
            }
        }
        return map
    }
}
