package com.metro.people.data

import android.provider.ContactsContract.CommonDataKinds.Phone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactEditorLogicTest {
    @Test
    fun applyPrefill_setsPhoneAndSplitsName() {
        val draft = ContactEditorLogic.applyPrefill(
            ContactPrefill(phone = "555-0100", name = "Ada Lovelace"),
        )
        assertEquals("Ada", draft.givenName)
        assertEquals("Lovelace", draft.familyName)
        assertEquals("555-0100", draft.phones.single().value)
        assertEquals(Phone.TYPE_MOBILE, draft.phones.single().type)
    }

    @Test
    fun canSave_acceptsPhoneOnlyIdentity() {
        val draft = ContactDraft(
            phones = listOf(TypedContactValue(value = "+15551212", type = Phone.TYPE_MOBILE)),
        )
        assertTrue(ContactEditorLogic.canSave(draft))
        assertEquals("+15551212", ContactEditorLogic.composedDisplayName(draft))
    }

    @Test
    fun canSave_rejectsEmptyDraft() {
        assertFalse(ContactEditorLogic.canSave(ContactDraft()))
    }

    @Test
    fun normalizeBirthday_acceptsSlashForm() {
        assertEquals("1990-03-05", ContactEditorLogic.normalizeBirthday("3/5/1990"))
        assertEquals("1990-03-05", ContactEditorLogic.normalizeBirthday("1990-03-05"))
    }

    @Test
    fun composedDisplayName_prefersStructuredName() {
        val draft = ContactDraft(
            namePrefix = "Dr",
            givenName = "Grace",
            familyName = "Hopper",
        )
        assertEquals("Dr Grace Hopper", ContactEditorLogic.composedDisplayName(draft))
    }
}
