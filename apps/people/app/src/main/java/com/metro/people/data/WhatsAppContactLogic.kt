package com.metro.people.data

/**
 * Detects WhatsApp message/voice bindings written into [android.provider.ContactsContract]
 * when WhatsApp (or WhatsApp Business) has synced a contact.
 */
object WhatsAppContactLogic {
    const val PACKAGE = "com.whatsapp"
    const val PACKAGE_BUSINESS = "com.whatsapp.w4b"

    const val MIME_MESSAGE = "vnd.android.cursor.item/vnd.com.whatsapp.profile"
    const val MIME_VOICE = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
    const val MIME_MESSAGE_BUSINESS = "vnd.android.cursor.item/vnd.com.whatsapp.w4b.profile"
    const val MIME_VOICE_BUSINESS = "vnd.android.cursor.item/vnd.com.whatsapp.w4b.voip.call"

    val ALL_MIMES = arrayOf(
        MIME_MESSAGE,
        MIME_VOICE,
        MIME_MESSAGE_BUSINESS,
        MIME_VOICE_BUSINESS,
    )

    fun packageForMime(mimeType: String): String? = when (mimeType) {
        MIME_MESSAGE, MIME_VOICE -> PACKAGE
        MIME_MESSAGE_BUSINESS, MIME_VOICE_BUSINESS -> PACKAGE_BUSINESS
        else -> null
    }

    fun isMessageMime(mimeType: String): Boolean =
        mimeType == MIME_MESSAGE || mimeType == MIME_MESSAGE_BUSINESS

    fun isVoiceMime(mimeType: String): Boolean =
        mimeType == MIME_VOICE || mimeType == MIME_VOICE_BUSINESS

    fun packageForAccountType(accountType: String?): String? = when (accountType) {
        PACKAGE, "WhatsApp" -> PACKAGE
        PACKAGE_BUSINESS -> PACKAGE_BUSINESS
        else -> null
    }

    /**
     * Prefer consumer WhatsApp over Business when both are present and installed.
     * Message can fall back to a phone deep-link when a WhatsApp raw contact exists
     * but the profile mime row is missing.
     */
    fun resolve(
        dataRows: List<WhatsAppDataRow>,
        syncedPackages: Set<String>,
        installedPackages: Set<String>,
        phoneDigits: String?,
    ): WhatsAppLink? {
        fun build(pkg: String): WhatsAppLink? {
            if (pkg !in installedPackages) return null
            val rows = dataRows.filter { packageForMime(it.mimeType) == pkg }
            val messageId = rows.firstOrNull { isMessageMime(it.mimeType) }?.dataId
            val voiceId = rows.firstOrNull { isVoiceMime(it.mimeType) }?.dataId
            val synced = pkg in syncedPackages
            val canMessageViaPhone = synced && !phoneDigits.isNullOrBlank()
            if (messageId == null && voiceId == null && !canMessageViaPhone) return null
            return WhatsAppLink(
                packageName = pkg,
                messageDataId = messageId,
                voiceCallDataId = voiceId,
                phoneDigits = phoneDigits
                    .takeUnless { it.isNullOrBlank() }
                    ?.takeIf { messageId != null || canMessageViaPhone },
            )
        }
        return build(PACKAGE) ?: build(PACKAGE_BUSINESS)
    }

    fun digitsOnly(number: String?): String? {
        if (number.isNullOrBlank()) return null
        val digits = number.filter { it.isDigit() }
        return digits.ifEmpty { null }
    }
}

data class WhatsAppDataRow(
    val dataId: Long,
    val mimeType: String,
)

data class WhatsAppLink(
    val packageName: String,
    val messageDataId: Long? = null,
    val voiceCallDataId: Long? = null,
    val phoneDigits: String? = null,
) {
    val canMessage: Boolean
        get() = messageDataId != null || !phoneDigits.isNullOrBlank()

    val canVoiceCall: Boolean
        get() = voiceCallDataId != null
}
