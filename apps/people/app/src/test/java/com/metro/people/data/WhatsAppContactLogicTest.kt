package com.metro.people.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppContactLogicTest {
    @Test
    fun resolve_returnsNullWhenWhatsAppNotInstalled() {
        val link = WhatsAppContactLogic.resolve(
            dataRows = listOf(
                WhatsAppDataRow(10, WhatsAppContactLogic.MIME_MESSAGE),
                WhatsAppDataRow(11, WhatsAppContactLogic.MIME_VOICE),
            ),
            syncedPackages = setOf(WhatsAppContactLogic.PACKAGE),
            installedPackages = emptySet(),
            phoneDigits = "15551212",
        )
        assertNull(link)
    }

    @Test
    fun resolve_usesMimeRowsWhenPresent() {
        val link = WhatsAppContactLogic.resolve(
            dataRows = listOf(
                WhatsAppDataRow(10, WhatsAppContactLogic.MIME_MESSAGE),
                WhatsAppDataRow(11, WhatsAppContactLogic.MIME_VOICE),
            ),
            syncedPackages = emptySet(),
            installedPackages = setOf(WhatsAppContactLogic.PACKAGE),
            phoneDigits = null,
        )
        assertNotNull(link)
        assertEquals(10L, link!!.messageDataId)
        assertEquals(11L, link.voiceCallDataId)
        assertTrue(link.canMessage)
        assertTrue(link.canVoiceCall)
    }

    @Test
    fun resolve_fallsBackToPhoneMessageWhenSyncedWithoutMime() {
        val link = WhatsAppContactLogic.resolve(
            dataRows = emptyList(),
            syncedPackages = setOf(WhatsAppContactLogic.PACKAGE),
            installedPackages = setOf(WhatsAppContactLogic.PACKAGE),
            phoneDigits = "15551212",
        )
        assertNotNull(link)
        assertNull(link!!.messageDataId)
        assertNull(link.voiceCallDataId)
        assertTrue(link.canMessage)
        assertFalse(link.canVoiceCall)
        assertEquals("15551212", link.phoneDigits)
    }

    @Test
    fun resolve_prefersConsumerOverBusiness() {
        val link = WhatsAppContactLogic.resolve(
            dataRows = listOf(
                WhatsAppDataRow(1, WhatsAppContactLogic.MIME_MESSAGE),
                WhatsAppDataRow(2, WhatsAppContactLogic.MIME_MESSAGE_BUSINESS),
            ),
            syncedPackages = setOf(
                WhatsAppContactLogic.PACKAGE,
                WhatsAppContactLogic.PACKAGE_BUSINESS,
            ),
            installedPackages = setOf(
                WhatsAppContactLogic.PACKAGE,
                WhatsAppContactLogic.PACKAGE_BUSINESS,
            ),
            phoneDigits = null,
        )
        assertEquals(WhatsAppContactLogic.PACKAGE, link!!.packageName)
        assertEquals(1L, link.messageDataId)
    }

    @Test
    fun resolve_voiceOnly_doesNotEnableTextFromUnrelatedPhone() {
        val link = WhatsAppContactLogic.resolve(
            dataRows = listOf(
                WhatsAppDataRow(11, WhatsAppContactLogic.MIME_VOICE),
            ),
            syncedPackages = emptySet(),
            installedPackages = setOf(WhatsAppContactLogic.PACKAGE),
            phoneDigits = "15551212",
        )
        assertNotNull(link)
        assertTrue(link!!.canVoiceCall)
        assertFalse(link.canMessage)
        assertNull(link.phoneDigits)
    }

    @Test
    fun digitsOnly_stripsFormatting() {
        assertEquals("15551212", WhatsAppContactLogic.digitsOnly("+1 (555) 1212"))
        assertNull(WhatsAppContactLogic.digitsOnly(" — "))
    }
}
