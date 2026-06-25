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

    fun buildTileData(): MetroTileData {
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

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
}
