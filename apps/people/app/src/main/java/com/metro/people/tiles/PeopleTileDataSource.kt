package com.metro.people.tiles

import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.content.ContextCompat
import com.metro.people.data.ContactsRepository
import com.metro.system.MetroAppRegistry
import com.metro.system.MetroPreferences
import com.metro.system.MetroTileContract
import com.metro.system.MetroTileData
import com.metro.system.MetroTilePhotoGrid

class PeopleTileDataSource(context: Context) {
    private val appContext = context.applicationContext
    private val authority = MetroTileContract.authorityFor(appContext.packageName)

    fun buildTileData(tileId: String = MetroTileContract.DEFAULT_TILE_ID): MetroTileData? {
        val contactId = PeopleTileLogic.parseContactTileId(tileId)
        return if (contactId != null) {
            buildContactTile(contactId)
        } else if (tileId == MetroTileContract.DEFAULT_TILE_ID) {
            buildHubTile()
        } else {
            null
        }
    }

    private fun buildHubTile(): MetroTileData {
        val accentHex = MetroPreferences(appContext).accentColorHex
        val contacts = if (hasContactsPermission()) {
            runCatching { ContactsRepository(appContext).loadContacts() }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val cells = if (contacts.isEmpty()) {
            PeopleTileLogic.fallbackCells(PeopleTileLogic.MAX_CELLS, accentHex)
        } else {
            PeopleTileLogic.cellsFromContacts(contacts, authority, accentHex)
        }
        return MetroTileData(
            title = MetroAppRegistry.label(appContext.packageName) ?: "People",
            backgroundColorHex = accentHex,
            photoGrid = MetroTilePhotoGrid(cells),
            deepLinkUri = null,
        )
    }

    private fun buildContactTile(contactId: Long): MetroTileData? {
        if (!hasContactsPermission()) return null
        val contact = runCatching {
            ContactsRepository(appContext).loadContacts().firstOrNull { it.id == contactId }
        }.getOrNull() ?: return null
        val accentHex = MetroPreferences(appContext).accentColorHex
        val color = PeopleTileLogic.colorForContact(contactId, accentHex)
        // Full-bleed contact photo on the front face; launcher flips to the People icon
        // (no Ken Burns / cycle). 1×1 shows the photo statically without flipping.
        val photo = contact.photoUri?.let { PeopleTileLogic.photoUri(authority, contactId) }
        return MetroTileData(
            title = contact.displayName,
            backgroundColorHex = color,
            imageUri = photo,
            deepLinkUri = PeopleTileLogic.contactDeepLinkUri(contactId),
        )
    }

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
}
