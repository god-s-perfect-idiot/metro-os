package com.metro.people.tiles

import android.graphics.Color
import com.metro.people.data.PersonSummary
import com.metro.system.MetroTileContract
import com.metro.system.MetroTileGridCell
import com.metro.system.MetroPreferences

object PeopleTileLogic {
    const val MAX_CELLS = MetroTileContract.MAX_PHOTO_GRID_CELLS

    fun accentShades(accentHex: String, count: Int): List<String> {
        if (count <= 0) return emptyList()
        val parsed = runCatching { Color.parseColor(accentHex) }.getOrElse {
            Color.parseColor(MetroPreferences.DEFAULT_ACCENT_HEX)
        }
        val hsv = FloatArray(3)
        Color.colorToHSV(parsed, hsv)
        return List(count) { index ->
            val step = index.toFloat() / (count - 1).coerceAtLeast(1)
            val value = (0.45f + step * 0.55f).coerceIn(0.35f, 1f)
            val saturation = (hsv[1] * (0.7f + (index % 3) * 0.12f)).coerceIn(0.35f, 1f)
            toHex(hsv[0], saturation, value)
        }
    }

    fun colorForIndex(index: Int, accentHex: String): String =
        accentShades(accentHex, MAX_CELLS)[index % MAX_CELLS]

    fun colorForContact(contactId: Long, accentHex: String): String =
        colorForIndex(contactId.toInt(), accentHex)

    fun photoUri(authority: String, contactId: Long): String =
        "content://$authority/photo/$contactId"

    fun fallbackCells(count: Int, accentHex: String): List<MetroTileGridCell> =
        accentShades(accentHex, count).map { MetroTileGridCell(colorHex = it) }

    fun cellsFromContacts(
        contacts: List<PersonSummary>,
        authority: String,
        accentHex: String,
    ): List<MetroTileGridCell> {
        val contactCells = contacts.take(MAX_CELLS).map { contact ->
            MetroTileGridCell(
                colorHex = colorForContact(contact.id, accentHex),
                imageUri = contact.photoUri?.let { photoUri(authority, contact.id) },
            )
        }
        return if (contactCells.size >= MAX_CELLS) {
            contactCells
        } else {
            contactCells + fallbackCells(MAX_CELLS - contactCells.size, accentHex)
        }
    }

    private fun toHex(hue: Float, saturation: Float, value: Float): String {
        val rgb = Color.HSVToColor(floatArrayOf(hue, saturation, value))
        return String.format("#%06X", rgb and 0xFFFFFF)
    }
}
