package com.metro.people.tiles

import android.graphics.Color
import com.metro.people.data.PersonSummary
import com.metro.system.MetroTileContract
import com.metro.system.MetroTileGridCell
import com.metro.system.MetroPreferences
import kotlin.random.Random

object PeopleTileLogic {
    const val MAX_CELLS = MetroTileContract.MAX_PHOTO_GRID_CELLS
    /** Contacts exported for hub mosaic rotation (launcher shows ≤ 4 at once). */
    const val LIVE_POOL_SIZE = 40
    const val CONTACT_TILE_PREFIX = "contact:"
    const val DEEP_LINK_SCHEME = "metro"
    const val DEEP_LINK_HOST = "people"

    fun contactTileId(contactId: Long): String = "$CONTACT_TILE_PREFIX$contactId"

    fun parseContactTileId(tileId: String): Long? {
        if (!tileId.startsWith(CONTACT_TILE_PREFIX)) return null
        return tileId.removePrefix(CONTACT_TILE_PREFIX).toLongOrNull()
    }

    fun contactDeepLinkUri(contactId: Long): String =
        "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/contact/$contactId"

    fun parseContactDeepLink(uri: android.net.Uri?): Long? {
        if (uri == null) return null
        if (uri.scheme != DEEP_LINK_SCHEME || uri.host != DEEP_LINK_HOST) return null
        val segments = uri.pathSegments
        if (segments.size < 2 || segments[0] != "contact") return null
        return segments[1].toLongOrNull()
    }

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
        accentShades(accentHex, MAX_CELLS)[index.floorMod(MAX_CELLS)]

    fun colorForContact(contactId: Long, accentHex: String): String =
        colorForIndex((contactId % MAX_CELLS).toInt(), accentHex)

    fun photoUri(authority: String, contactId: Long): String =
        "content://$authority/photo/$contactId"

    /** Single capital letter for mosaic faces without a photo bitmap. */
    fun tileLabel(displayName: String): String {
        val ch = displayName.trim().firstOrNull { it.isLetterOrDigit() } ?: '#'
        return ch.uppercaseChar().toString()
    }

    fun fallbackCells(count: Int, accentHex: String): List<MetroTileGridCell> =
        accentShades(accentHex, count).map { MetroTileGridCell(colorHex = it) }

    /**
     * Contact pool for the People hub mosaic. Every contact gets a stable photo URI + letter
     * label (bitmap when available, letter otherwise). Shuffled on each export so the launcher
     * receives a rotating sample. Launcher shows ≤ 4 at once and live-flips through the pool.
     */
    fun cellsFromContacts(
        contacts: List<PersonSummary>,
        authority: String,
        accentHex: String,
        random: Random = Random.Default,
    ): List<MetroTileGridCell> {
        // Prefer photo-backed contacts, but shuffle within each group so the same first-4
        // alphabetical contacts are not sticky across provider reads.
        val withPhoto = contacts.filter { !it.photoUri.isNullOrBlank() }.shuffled(random)
        val withoutPhoto = contacts.filter { it.photoUri.isNullOrBlank() }.shuffled(random)
        val ordered = (withPhoto + withoutPhoto).take(LIVE_POOL_SIZE)
        val contactCells = ordered.map { contact ->
            MetroTileGridCell(
                colorHex = colorForContact(contact.id, accentHex),
                imageUri = photoUri(authority, contact.id),
                label = tileLabel(contact.displayName),
            )
        }
        return contactCells + fallbackCells(MAX_CELLS, accentHex)
    }

    private fun Int.floorMod(mod: Int): Int {
        val r = this % mod
        return if (r >= 0) r else r + mod
    }

    private fun toHex(hue: Float, saturation: Float, value: Float): String {
        val rgb = Color.HSVToColor(floatArrayOf(hue, saturation, value))
        return String.format("#%06X", rgb and 0xFFFFFF)
    }
}
