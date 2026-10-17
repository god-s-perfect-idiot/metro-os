package com.metro.people.data

import android.content.ContentUris
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import android.provider.ContactsContract.Data
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
        // Do not filter IN_VISIBLE_GROUP — locally created contacts are often outside a
        // sync group's "My Contacts" until membership is written, but must still appear.
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
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
                val phone = phoneById[id]?.firstOrNull()?.value
                val email = emailById[id]?.firstOrNull()?.value
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
        val summary = loadContactSummary(contactId) ?: return null
        val phones = loadPhoneNumbers()[contactId].orEmpty()
        val emails = loadEmails()[contactId].orEmpty()
        var namePrefix = ""
        var givenName = ""
        var middleName = ""
        var familyName = ""
        var nameSuffix = ""
        var phoneticName = ""
        var nickname = ""
        var company = ""
        var jobTitle = ""
        var department = ""
        var postal: PostalAddress? = null
        var website = ""
        var imHandle = ""
        var imProtocolLabel = ""
        var birthday = ""
        var anniversary = ""
        var notes = ""
        var relation = ""
        var relationLabel = ""

        resolver.query(
            Data.CONTENT_URI,
            arrayOf(
                Data.MIMETYPE,
                Data.DATA1,
                Data.DATA2,
                Data.DATA3,
                Data.DATA4,
                Data.DATA5,
                Data.DATA6,
                Data.DATA7,
                Data.DATA8,
                Data.DATA9,
                Data.DATA10,
            ),
            "${Data.CONTACT_ID}=?",
            arrayOf(contactId.toString()),
            null,
        )?.use { cursor ->
            val mimeCol = cursor.getColumnIndexOrThrow(Data.MIMETYPE)
            val d1 = cursor.getColumnIndexOrThrow(Data.DATA1)
            val d2 = cursor.getColumnIndexOrThrow(Data.DATA2)
            val d3 = cursor.getColumnIndexOrThrow(Data.DATA3)
            val d4 = cursor.getColumnIndexOrThrow(Data.DATA4)
            val d5 = cursor.getColumnIndexOrThrow(Data.DATA5)
            val d6 = cursor.getColumnIndexOrThrow(Data.DATA6)
            val d7 = cursor.getColumnIndexOrThrow(Data.DATA7)
            val d8 = cursor.getColumnIndexOrThrow(Data.DATA8)
            val d9 = cursor.getColumnIndexOrThrow(Data.DATA9)
            val d10 = cursor.getColumnIndexOrThrow(Data.DATA10)
            while (cursor.moveToNext()) {
                when (cursor.getString(mimeCol)) {
                    StructuredName.CONTENT_ITEM_TYPE -> {
                        namePrefix = cursor.getString(d4).orEmpty()
                        givenName = cursor.getString(d2).orEmpty()
                        middleName = cursor.getString(d5).orEmpty()
                        familyName = cursor.getString(d3).orEmpty()
                        nameSuffix = cursor.getString(d6).orEmpty()
                        phoneticName = cursor.getString(d7).orEmpty()
                    }
                    Nickname.CONTENT_ITEM_TYPE -> {
                        nickname = cursor.getString(d1).orEmpty()
                    }
                    Organization.CONTENT_ITEM_TYPE -> {
                        company = cursor.getString(d1).orEmpty()
                        jobTitle = cursor.getString(d4).orEmpty()
                        department = cursor.getString(d5).orEmpty()
                    }
                    StructuredPostal.CONTENT_ITEM_TYPE -> {
                        val type = cursor.getInt(d2)
                        postal = PostalAddress(
                            typeLabel = ContactEditorLogic.postalTypes
                                .firstOrNull { it.type == type }?.label ?: "address",
                            street = cursor.getString(d4).orEmpty(),
                            city = cursor.getString(d7).orEmpty(),
                            region = cursor.getString(d8).orEmpty(),
                            postcode = cursor.getString(d9).orEmpty(),
                            country = cursor.getString(d10).orEmpty(),
                        )
                    }
                    Website.CONTENT_ITEM_TYPE -> {
                        website = cursor.getString(d1).orEmpty()
                    }
                    Im.CONTENT_ITEM_TYPE -> {
                        imHandle = cursor.getString(d1).orEmpty()
                        val protocol = cursor.getInt(d5)
                        imProtocolLabel = ContactEditorLogic.imProtocols
                            .firstOrNull { it.type == protocol }?.label.orEmpty()
                    }
                    Event.CONTENT_ITEM_TYPE -> {
                        val type = cursor.getInt(d2)
                        val date = cursor.getString(d1).orEmpty()
                        when (type) {
                            Event.TYPE_BIRTHDAY -> birthday = date
                            Event.TYPE_ANNIVERSARY -> anniversary = date
                        }
                    }
                    Note.CONTENT_ITEM_TYPE -> {
                        notes = cursor.getString(d1).orEmpty()
                    }
                    Relation.CONTENT_ITEM_TYPE -> {
                        relation = cursor.getString(d1).orEmpty()
                        val type = cursor.getInt(d2)
                        relationLabel = ContactEditorLogic.relationTypes
                            .firstOrNull { it.type == type }?.label.orEmpty()
                    }
                }
            }
        }

        return PersonDetail(
            summary = summary.copy(
                defaultPhone = phones.firstOrNull()?.value ?: summary.defaultPhone,
                defaultEmail = emails.firstOrNull()?.value ?: summary.defaultEmail,
                hasPhone = phones.isNotEmpty(),
            ),
            phones = phones,
            emails = emails,
            namePrefix = namePrefix,
            givenName = givenName,
            middleName = middleName,
            familyName = familyName,
            nameSuffix = nameSuffix,
            phoneticName = phoneticName,
            nickname = nickname,
            company = company,
            jobTitle = jobTitle,
            department = department,
            postal = postal,
            website = website,
            imHandle = imHandle,
            imProtocolLabel = imProtocolLabel,
            birthday = birthday,
            anniversary = anniversary,
            notes = notes,
            relation = relation,
            relationLabel = relationLabel,
            whatsApp = loadWhatsAppLink(contactId, phones.firstOrNull()?.value),
        )
    }

    fun deleteContact(contactId: Long): Boolean {
        return try {
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            resolver.delete(uri, null, null) > 0
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
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

    private fun loadContactSummary(contactId: Long): PersonSummary? {
        val phoneById = loadPhoneNumbers()
        val emailById = loadEmails()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
        )
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            "${ContactsContract.Contacts._ID}=?",
            arrayOf(contactId.toString()),
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val name = cursor.getString(
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            )?.trim().orEmpty()
            if (name.isEmpty()) return null
            val hasPhone = cursor.getInt(
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER),
            ) > 0
            return PersonSummary(
                id = contactId,
                displayName = name,
                photoUri = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI),
                ),
                hasPhone = hasPhone,
                defaultPhone = phoneById[contactId]?.firstOrNull()?.value,
                defaultEmail = emailById[contactId]?.firstOrNull()?.value,
                sourceLabel = context.getString(R.string.account_device),
                sortKey = PeopleContactsLogic.sortKeyFor(name, sortByLastName = false),
            )
        }
        return null
    }

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
        val selection = "${Data.CONTACT_ID}=? AND ${Data.MIMETYPE} IN ($mimePlaceholders)"
        val args = arrayOf(contactId.toString()) + WhatsAppContactLogic.ALL_MIMES
        val rows = mutableListOf<WhatsAppDataRow>()
        resolver.query(
            Data.CONTENT_URI,
            arrayOf(Data._ID, Data.MIMETYPE),
            selection,
            args,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Data._ID)
            val mimeCol = cursor.getColumnIndexOrThrow(Data.MIMETYPE)
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

    private fun loadPhoneNumbers(): Map<Long, List<ContactMethod>> {
        val map = mutableMapOf<Long, MutableList<ContactMethod>>()
        val projection = arrayOf(
            Phone.CONTACT_ID,
            Phone.NUMBER,
            Phone.TYPE,
            Phone.LABEL,
        )
        resolver.query(Phone.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Phone.CONTACT_ID)
            val numberCol = cursor.getColumnIndexOrThrow(Phone.NUMBER)
            val typeCol = cursor.getColumnIndexOrThrow(Phone.TYPE)
            val labelCol = cursor.getColumnIndexOrThrow(Phone.LABEL)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val number = cursor.getString(numberCol)?.trim().orEmpty()
                if (number.isEmpty()) continue
                val type = cursor.getInt(typeCol)
                val custom = cursor.getString(labelCol)
                val label = if (type == Phone.TYPE_CUSTOM && !custom.isNullOrBlank()) {
                    custom
                } else {
                    ContactEditorLogic.phoneTypes.firstOrNull { it.type == type }?.label ?: "phone"
                }
                map.getOrPut(id) { mutableListOf() }.add(
                    ContactMethod(ContactMethodType.Phone, label, number),
                )
            }
        }
        return map
    }

    private fun loadEmails(): Map<Long, List<ContactMethod>> {
        val map = mutableMapOf<Long, MutableList<ContactMethod>>()
        val projection = arrayOf(
            Email.CONTACT_ID,
            Email.ADDRESS,
            Email.TYPE,
            Email.LABEL,
        )
        resolver.query(Email.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Email.CONTACT_ID)
            val emailCol = cursor.getColumnIndexOrThrow(Email.ADDRESS)
            val typeCol = cursor.getColumnIndexOrThrow(Email.TYPE)
            val labelCol = cursor.getColumnIndexOrThrow(Email.LABEL)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val email = cursor.getString(emailCol)?.trim().orEmpty()
                if (email.isEmpty()) continue
                val type = cursor.getInt(typeCol)
                val custom = cursor.getString(labelCol)
                val label = if (type == Email.TYPE_CUSTOM && !custom.isNullOrBlank()) {
                    custom
                } else {
                    ContactEditorLogic.emailTypes.firstOrNull { it.type == type }?.label ?: "email"
                }
                map.getOrPut(id) { mutableListOf() }.add(
                    ContactMethod(ContactMethodType.Email, label, email),
                )
            }
        }
        return map
    }
}
