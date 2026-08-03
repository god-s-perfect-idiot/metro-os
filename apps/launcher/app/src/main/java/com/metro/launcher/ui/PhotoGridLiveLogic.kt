package com.metro.launcher.ui

import com.metro.system.MetroTileGridCell
import kotlin.random.Random

/**
 * WP8.1 People hub mosaic: at most [MAX_VISIBLE_CONTACTS] **distinct** contacts at once
 * among a 3×3 / 6×3 grid; remaining cells are accent color. Live refresh rotates unused
 * pool contacts in — never duplicates, never sticks on the first four forever.
 */
object PhotoGridLiveLogic {
    const val MAX_VISIBLE_CONTACTS = 4

    fun contactPool(cells: List<MetroTileGridCell>): List<MetroTileGridCell> =
        cells.filter { !it.imageUri.isNullOrBlank() || !it.label.isNullOrBlank() }
            .distinctBy { it.imageUri ?: "label:${it.label}:${it.colorHex}" }

    fun accentTemplates(cells: List<MetroTileGridCell>): List<MetroTileGridCell> {
        val accents = cells.filter {
            it.imageUri.isNullOrBlank() && it.label.isNullOrBlank() && !it.colorHex.isNullOrBlank()
        }
        return accents.ifEmpty { listOf(MetroTileGridCell()) }
    }

    fun visibleContactCount(cells: List<MetroTileGridCell>): Int =
        cells.count { isContact(it) }

    fun isContact(cell: MetroTileGridCell): Boolean =
        !cell.imageUri.isNullOrBlank() || !cell.label.isNullOrBlank()

    fun contactKey(cell: MetroTileGridCell): String? = when {
        !cell.imageUri.isNullOrBlank() -> cell.imageUri
        !cell.label.isNullOrBlank() -> "label:${cell.label}:${cell.colorHex}"
        else -> null
    }

    /** True when ≤ 4 contacts and every contact key appears at most once. */
    fun isValidMosaic(cells: List<MetroTileGridCell>): Boolean {
        val keys = cells.mapNotNull { contactKey(it) }
        return keys.size <= MAX_VISIBLE_CONTACTS && keys.size == keys.toSet().size
    }

    fun initialLayout(
        pool: List<MetroTileGridCell>,
        accents: List<MetroTileGridCell>,
        cellCount: Int,
        random: Random,
    ): List<MetroTileGridCell> {
        if (cellCount <= 0) return emptyList()
        val layout = MutableList(cellCount) { index ->
            accents[index % accents.size]
        }
        if (pool.isEmpty()) return layout

        val uniquePool = dedupePool(pool)
        val visible = minOf(MAX_VISIBLE_CONTACTS, uniquePool.size, cellCount)
        val slots = (0 until cellCount).shuffled(random).take(visible)
        val contacts = uniquePool.shuffled(random).take(visible)
        slots.zip(contacts).forEach { (slot, contact) ->
            layout[slot] = contact
        }
        return layout
    }

    /**
     * Always bring an unused pool contact onto the mosaic when the pool is larger than the
     * visible set. Falls back to clearing a slot (so the next tick can place it elsewhere)
     * when every pool contact is already showing.
     */
    fun nextFlip(
        current: List<MetroTileGridCell>,
        pool: List<MetroTileGridCell>,
        accents: List<MetroTileGridCell>,
        random: Random,
    ): Pair<Int, MetroTileGridCell>? {
        if (current.isEmpty() || pool.isEmpty()) return null

        val uniquePool = dedupePool(pool)
        val contactIndices = current.indices.filter { isContact(current[it]) }
        val accentIndices = current.indices.filter { !isContact(current[it]) }
        val visibleKeys = contactIndices.mapNotNull { contactKey(current[it]) }.toSet()
        val unused = uniquePool.filter { contactKey(it) !in visibleKeys }

        if (unused.isNotEmpty()) {
            val incoming = unused[random.nextInt(unused.size)]
            // Prefer replacing a visible contact so faces actually change every tick.
            if (contactIndices.isNotEmpty() &&
                (contactIndices.size >= MAX_VISIBLE_CONTACTS || random.nextFloat() < 0.75f)
            ) {
                return contactIndices[random.nextInt(contactIndices.size)] to incoming
            }
            if (accentIndices.isNotEmpty() && contactIndices.size < MAX_VISIBLE_CONTACTS) {
                return accentIndices[random.nextInt(accentIndices.size)] to incoming
            }
            if (contactIndices.isNotEmpty()) {
                return contactIndices[random.nextInt(contactIndices.size)] to incoming
            }
            if (accentIndices.isNotEmpty()) {
                return accentIndices[random.nextInt(accentIndices.size)] to incoming
            }
            return null
        }

        // Pool fully visible (size ≤ 4): clear one slot so a later flip can move that contact.
        if (contactIndices.size <= 1 || accentIndices.isEmpty()) return null
        return contactIndices[random.nextInt(contactIndices.size)] to
            accents[random.nextInt(accents.size)]
    }

    private fun dedupePool(pool: List<MetroTileGridCell>): List<MetroTileGridCell> =
        pool.distinctBy { contactKey(it) ?: it.hashCode().toString() }
}
