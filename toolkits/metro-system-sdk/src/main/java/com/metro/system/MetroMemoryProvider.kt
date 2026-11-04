package com.metro.system

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Debug

/**
 * Hosted by every suite app via SDK manifest merge. Reports this process's PSS to
 * [MetroMemoryContract.readPssBytes] callers (metro packages only).
 */
class MetroMemoryProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != MetroMemoryContract.METHOD_GET_PSS) return null
        val ctx = context ?: return null
        val caller = callingPackage
        if (caller == null || !caller.startsWith("com.metro.")) return null

        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        val pssKb = info.totalPss.toLong()
        return Bundle().apply {
            putLong(MetroMemoryContract.EXTRA_PSS_KB, pssKb)
            putLong(MetroMemoryContract.EXTRA_PSS_BYTES, pssKb * 1024L)
            putString(MetroMemoryContract.EXTRA_PACKAGE, ctx.packageName)
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
