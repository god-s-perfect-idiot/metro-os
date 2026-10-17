package com.metro.people.data

import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.Groups
import android.provider.ContactsContract.RawContacts
import java.io.ByteArrayOutputStream

class ContactWriter(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    fun availableAccounts(): List<ContactAccountTarget> {
        val targets = linkedMapOf<String, ContactAccountTarget>()
        targets["device"] = ContactAccountTarget.Device.copy(
            label = context.getString(com.metro.people.R.string.account_device),
        )
        val contactAccountTypes = runCatching {
            ContentResolver.getSyncAdapterTypes()
                .asSequence()
                .filter { it.authority == ContactsContract.AUTHORITY }
                .map { it.accountType }
                .toSet()
        }.getOrDefault(emptySet())
        if (contactAccountTypes.isEmpty()) return targets.values.toList()

        runCatching {
            for (account in AccountManager.get(context).accounts) {
                if (account.type !in contactAccountTypes) continue
                val key = "${account.type}:${account.name}"
                targets.putIfAbsent(
                    key,
                    ContactAccountTarget(
                        accountName = account.name,
                        accountType = account.type,
                        label = account.name,
                    ),
                )
            }
        }
        return targets.values.toList()
    }

    /**
     * Inserts [draft] into the Contacts Provider.
     * @return aggregated contact lookup URI, or null on failure / invalid draft.
     */
    fun insert(draft: ContactDraft): Uri? {
        if (!ContactEditorLogic.canSave(draft)) return null
        val photoBytes = draft.photoUri?.let { readPhotoBytes(it) }
        val preferred = resolveAccount(draft.account)
        val preferredIsLocal = preferred.accountName == null && preferred.accountType == null
        // Prefer the selected account; fall back to local Device if the provider rejects it
        // (common when a third-party app tries to write into a Google/Exchange raw contact).
        return insertForAccount(draft, photoBytes, preferred)
            ?: if (!preferredIsLocal) {
                insertForAccount(draft, photoBytes, resolveAccount(ContactAccountTarget.Device))
            } else {
                null
            }
    }

    private fun resolveAccount(preferred: ContactAccountTarget): ContactAccountTarget {
        val isLocal = preferred.accountName == null && preferred.accountType == null
        if (!isLocal) return preferred
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val name = RawContacts.getLocalAccountName(context)
            val type = RawContacts.getLocalAccountType(context)
            if (name != null || type != null) {
                return ContactAccountTarget(
                    accountName = name,
                    accountType = type,
                    label = context.getString(com.metro.people.R.string.account_device),
                )
            }
        }
        return preferred.copy(label = context.getString(com.metro.people.R.string.account_device))
    }

    /** Default "My Contacts" / auto-add group so the contact is globally visible. */
    private fun findVisibleGroupId(account: ContactAccountTarget): Long? {
        val projection = arrayOf(Groups._ID)
        val (selection, args) = if (account.accountName == null && account.accountType == null) {
            (
                "${Groups.ACCOUNT_NAME} IS NULL AND ${Groups.ACCOUNT_TYPE} IS NULL AND " +
                    "${Groups.DELETED}=0 AND (${Groups.AUTO_ADD}!=0 OR ${Groups.GROUP_IS_READ_ONLY}=0)"
                ) to emptyArray<String>()
        } else {
            (
                "${Groups.ACCOUNT_NAME}=? AND ${Groups.ACCOUNT_TYPE}=? AND ${Groups.DELETED}=0 AND " +
                    "(${Groups.AUTO_ADD}!=0 OR ${Groups.TITLE}=?)"
                ) to arrayOf(
                account.accountName.orEmpty(),
                account.accountType.orEmpty(),
                "My Contacts",
            )
        }
        resolver.query(Groups.CONTENT_URI, projection, selection, args, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        return null
    }

    private fun insertForAccount(
        draft: ContactDraft,
        photoBytes: ByteArray?,
        account: ContactAccountTarget,
    ): Uri? {
        return try {
            val groupId = findVisibleGroupId(account)
            val ops = buildInsertOperations(
                draft = draft.copy(account = account),
                photoBytes = photoBytes,
                groupRowId = groupId,
            )
            val results = resolver.applyBatch(ContactsContract.AUTHORITY, ops)
            val rawContactUri = results.firstOrNull()?.uri
                ?: error("Raw contact insert returned no URI")
            val rawId = rawContactUri.lastPathSegment?.toLongOrNull()
                ?: return rawContactUri
            lookupContactUri(rawId) ?: rawContactUri
        } catch (e: Exception) {
            android.util.Log.e("ContactWriter", "Insert failed for ${account.label}", e)
            null
        }
    }

    private fun lookupContactUri(rawContactId: Long): Uri? {
        resolver.query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts.CONTACT_ID),
            "${RawContacts._ID}=?",
            arrayOf(rawContactId.toString()),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val contactId = cursor.getLong(0)
                if (contactId > 0L) {
                    return Uri.withAppendedPath(
                        ContactsContract.Contacts.CONTENT_URI,
                        contactId.toString(),
                    )
                }
            }
        }
        return null
    }

    private fun readPhotoBytes(uri: Uri): ByteArray? {
        return runCatching {
            resolver.openInputStream(uri)?.use { input ->
                val original = BitmapFactory.decodeStream(input) ?: return@use null
                val scaled = scaleDown(original, maxSide = 720)
                try {
                    ByteArrayOutputStream().use { out ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        out.toByteArray()
                    }
                } finally {
                    if (scaled !== original) scaled.recycle()
                    original.recycle()
                }
            }
        }.getOrNull()
    }

    private fun scaleDown(source: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= maxSide) return source
        val scale = maxSide.toFloat() / longest.toFloat()
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, w, h, true)
    }

    companion object {
        fun buildInsertOperations(
            draft: ContactDraft,
            photoBytes: ByteArray?,
            groupRowId: Long? = null,
        ): ArrayList<ContentProviderOperation> {
            val ops = ArrayList<ContentProviderOperation>()
            // Local device storage uses explicit null account columns (provider fills local).
            ops += ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_NAME, draft.account.accountName)
                .withValue(RawContacts.ACCOUNT_TYPE, draft.account.accountType)
                .build()

            if (groupRowId != null && groupRowId > 0L) {
                ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE)
                    .withValue(GroupMembership.GROUP_ROW_ID, groupRowId)
                    .build()
            }

            val displayName = ContactEditorLogic.composedDisplayName(draft)
            val name = ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, displayName)
            draft.namePrefix.trim().takeIf { it.isNotEmpty() }?.let {
                name.withValue(StructuredName.PREFIX, it)
            }
            draft.givenName.trim().takeIf { it.isNotEmpty() }?.let {
                name.withValue(StructuredName.GIVEN_NAME, it)
            }
            draft.middleName.trim().takeIf { it.isNotEmpty() }?.let {
                name.withValue(StructuredName.MIDDLE_NAME, it)
            }
            draft.familyName.trim().takeIf { it.isNotEmpty() }?.let {
                name.withValue(StructuredName.FAMILY_NAME, it)
            }
            draft.nameSuffix.trim().takeIf { it.isNotEmpty() }?.let {
                name.withValue(StructuredName.SUFFIX, it)
            }
            draft.phoneticName.trim().takeIf { it.isNotEmpty() }?.let {
                name.withValue(StructuredName.PHONETIC_GIVEN_NAME, it)
            }
            ops += name.build()

            if (draft.nickname.isNotBlank()) {
                ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
                    .withValue(Nickname.NAME, draft.nickname.trim())
                    .withValue(Nickname.TYPE, Nickname.TYPE_DEFAULT)
                    .build()
            }

            draft.phones.filter { it.value.isNotBlank() }.forEach { phone ->
                val builder = ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, phone.value.trim())
                    .withValue(Phone.TYPE, phone.type)
                if (phone.type == Phone.TYPE_CUSTOM && phone.customLabel.isNotBlank()) {
                    builder.withValue(Phone.LABEL, phone.customLabel.trim())
                }
                ops += builder.build()
            }

            draft.emails.filter { it.value.isNotBlank() }.forEach { email ->
                val builder = ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                    .withValue(Email.ADDRESS, email.value.trim())
                    .withValue(Email.TYPE, email.type)
                if (email.type == Email.TYPE_CUSTOM && email.customLabel.isNotBlank()) {
                    builder.withValue(Email.LABEL, email.customLabel.trim())
                }
                ops += builder.build()
            }

            val hasOrg = listOf(draft.company, draft.jobTitle, draft.department)
                .any { it.isNotBlank() }
            if (hasOrg) {
                val org = ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
                    .withValue(Organization.TYPE, Organization.TYPE_WORK)
                draft.company.trim().takeIf { it.isNotEmpty() }?.let {
                    org.withValue(Organization.COMPANY, it)
                }
                draft.jobTitle.trim().takeIf { it.isNotEmpty() }?.let {
                    org.withValue(Organization.TITLE, it)
                }
                draft.department.trim().takeIf { it.isNotEmpty() }?.let {
                    org.withValue(Organization.DEPARTMENT, it)
                }
                ops += org.build()
            }

            val hasPostal = listOf(
                draft.postalStreet,
                draft.postalCity,
                draft.postalRegion,
                draft.postalPostcode,
                draft.postalCountry,
            ).any { it.isNotBlank() }
            if (hasPostal) {
                val postal = ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
                    .withValue(StructuredPostal.TYPE, draft.postalType)
                draft.postalStreet.trim().takeIf { it.isNotEmpty() }?.let {
                    postal.withValue(StructuredPostal.STREET, it)
                }
                draft.postalCity.trim().takeIf { it.isNotEmpty() }?.let {
                    postal.withValue(StructuredPostal.CITY, it)
                }
                draft.postalRegion.trim().takeIf { it.isNotEmpty() }?.let {
                    postal.withValue(StructuredPostal.REGION, it)
                }
                draft.postalPostcode.trim().takeIf { it.isNotEmpty() }?.let {
                    postal.withValue(StructuredPostal.POSTCODE, it)
                }
                draft.postalCountry.trim().takeIf { it.isNotEmpty() }?.let {
                    postal.withValue(StructuredPostal.COUNTRY, it)
                }
                ops += postal.build()
            }

            if (draft.website.isNotBlank()) {
                ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Website.CONTENT_ITEM_TYPE)
                    .withValue(Website.URL, draft.website.trim())
                    .withValue(Website.TYPE, Website.TYPE_HOME)
                    .build()
            }

            if (draft.imHandle.isNotBlank()) {
                ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE)
                    .withValue(Im.DATA, draft.imHandle.trim())
                    .withValue(Im.PROTOCOL, draft.imProtocol)
                    .withValue(Im.TYPE, Im.TYPE_HOME)
                    .build()
            }

            ContactEditorLogic.normalizeBirthday(draft.birthday)?.let { date ->
                ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE)
                    .withValue(Event.START_DATE, date)
                    .withValue(Event.TYPE, Event.TYPE_BIRTHDAY)
                    .build()
            }

            ContactEditorLogic.normalizeBirthday(draft.anniversary)?.let { date ->
                ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE)
                    .withValue(Event.START_DATE, date)
                    .withValue(Event.TYPE, Event.TYPE_ANNIVERSARY)
                    .build()
            }

            if (draft.notes.isNotBlank()) {
                ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                    .withValue(Note.NOTE, draft.notes.trim())
                    .build()
            }

            if (draft.relation.isNotBlank()) {
                ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Relation.CONTENT_ITEM_TYPE)
                    .withValue(Relation.NAME, draft.relation.trim())
                    .withValue(Relation.TYPE, draft.relationType)
                    .build()
            }

            if (photoBytes != null && photoBytes.isNotEmpty()) {
                ops += ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                    .withValue(Photo.PHOTO, photoBytes)
                    .build()
            }

            return ops
        }
    }
}
