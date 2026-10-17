package com.metro.files.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileOpenerTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun delete_removesFile() {
        val file = temp.newFile("notes.txt")
        file.writeText("hello")
        val entry = FileEntry(
            id = "f",
            name = file.name,
            absolutePath = file.absolutePath,
            kind = FileKind.FILE,
            sizeBytes = file.length(),
        )
        assertEquals(FileOpener.DeleteResult.Deleted, FileOpener.delete(entry))
        assertFalse(file.exists())
    }

    @Test
    fun delete_removesDirectoryRecursively() {
        val dir = temp.newFolder("album")
        val nested = java.io.File(dir, "shot.jpg")
        nested.writeText("img")
        val entry = FileEntry(
            id = "d",
            name = dir.name,
            absolutePath = dir.absolutePath,
            kind = FileKind.DIRECTORY,
            childCount = 1,
        )
        assertEquals(FileOpener.DeleteResult.Deleted, FileOpener.delete(entry))
        assertFalse(dir.exists())
        assertFalse(nested.exists())
    }

    @Test
    fun delete_refusesVolume() {
        val entry = FileEntry(
            id = "v",
            name = "phone",
            absolutePath = "/storage/emulated/0",
            kind = FileKind.VOLUME,
        )
        assertEquals(FileOpener.DeleteResult.NotAllowed, FileOpener.delete(entry))
    }

    @Test
    fun delete_missingReturnsMissing() {
        val entry = FileEntry(
            id = "f",
            name = "gone.txt",
            absolutePath = temp.root.resolve("gone.txt").absolutePath,
            kind = FileKind.FILE,
        )
        assertTrue(!java.io.File(entry.absolutePath).exists())
        assertEquals(FileOpener.DeleteResult.Missing, FileOpener.delete(entry))
    }
}
