package com.metro.statusbar

import java.util.UUID

/**
 * Icons that can appear in the configure-page layout (icons-tab glyphs + clock).
 * Network is one slot (cellular bars + data label).
 */
enum class TrayLayoutIcon(val storageKey: String) {
    Network("network"),
    Wifi("wifi"),
    Mute("mute"),
    Notifications("notifications"),
    Hotspot("hotspot"),
    BluetoothAudio("bluetooth"),
    Battery("battery"),
    Clock("clock"),
    ;

    fun isEnabled(flags: TrayIconFlags): Boolean = when (this) {
        Network -> flags.network
        Wifi -> flags.wifi
        Mute -> flags.mute
        Notifications -> flags.notifications
        Hotspot -> flags.hotspot
        BluetoothAudio -> flags.bluetoothAudio
        Battery -> flags.battery
        Clock -> true
    }

    companion object {
        fun fromStorage(key: String): TrayLayoutIcon? =
            entries.firstOrNull { it.storageKey == key }
    }
}

/**
 * One slot in the configurable tray row — an icon or a blank spacer used to clear
 * notch / privacy-mic regions.
 */
sealed class TrayLayoutSlot {
    abstract val id: String

    data class Icon(val kind: TrayLayoutIcon) : TrayLayoutSlot() {
        override val id: String get() = kind.storageKey
    }

    data class Spacer(
        override val id: String,
        val widthDp: Int = TrayLayout.DEFAULT_SPACER_WIDTH_DP,
    ) : TrayLayoutSlot()
}

object TrayLayout {
    const val DEFAULT_SPACER_WIDTH_DP = 40
    /** Compact spacer for fine notch / privacy-dot clearance. */
    const val TINY_SPACER_WIDTH_DP = 16

    /**
     * Safety ceiling so a pathological available-width never loops forever. Practical max is
     * whatever fits without folding — see [maxSpacers].
     */
    const val ABSOLUTE_MAX_SPACERS = 16

    /** Icons the user can toggle off (clock is always shown). */
    val TOGGLEABLE_ICONS: List<TrayLayoutIcon> =
        TrayLayoutIcon.entries.filter { it != TrayLayoutIcon.Clock }

    /** WP-style default: all icons L→R, no spacers. */
    val DEFAULT: List<TrayLayoutSlot> = TrayLayoutIcon.entries.map { TrayLayoutSlot.Icon(it) }

    fun spacerCount(slots: List<TrayLayoutSlot>): Int =
        slots.count { it is TrayLayoutSlot.Spacer }

    fun disabledToggleableCount(flags: TrayIconFlags): Int =
        TOGGLEABLE_ICONS.count { !it.isEnabled(flags) }

    /**
     * Estimated intrinsic width (dp) for a layout icon — matches tray glyph sizes so spacer
     * budgeting mirrors real folding.
     */
    fun estimatedIconWidthDp(kind: TrayLayoutIcon): Int = when (kind) {
        TrayLayoutIcon.Network -> 42 // cellular 18 + gap 2 + data label 22
        TrayLayoutIcon.Wifi -> 13
        TrayLayoutIcon.Mute -> 18
        TrayLayoutIcon.Notifications -> 20
        TrayLayoutIcon.Hotspot -> 20
        TrayLayoutIcon.BluetoothAudio -> 20
        TrayLayoutIcon.Battery -> 29
        TrayLayoutIcon.Clock -> 36 // ~"12:00" at tray clock size
    }

    /** Estimated intrinsic content width for [slots] (SpaceBetween free gaps do not add). */
    fun estimatedContentWidthDp(slots: List<TrayLayoutSlot>): Int {
        if (slots.isEmpty()) return 0
        return slots.sumOf { slot ->
            when (slot) {
                is TrayLayoutSlot.Icon -> estimatedIconWidthDp(slot.kind)
                is TrayLayoutSlot.Spacer -> slot.widthDp
            }
        }
    }

    /**
     * True when [slots] (already filtered to visible) would exceed [availableWidthDp] —
     * the only hard limit for spacers.
     */
    fun wouldFold(
        slots: List<TrayLayoutSlot>,
        availableWidthDp: Int = TraySpec.DEFAULT_CONTENT_WIDTH_DP,
    ): Boolean =
        estimatedContentWidthDp(slots) > availableWidthDp.coerceAtLeast(0)

    /**
     * Spacers allowed for the current icons-tab set: as many as fit without folding.
     * Disabled icons free width for more.
     */
    fun maxSpacers(
        flags: TrayIconFlags,
        availableWidthDp: Int = TraySpec.DEFAULT_CONTENT_WIDTH_DP,
    ): Int {
        val icons = TrayLayoutIcon.entries
            .filter { it.isEnabled(flags) }
            .map { TrayLayoutSlot.Icon(it) }
        var allowed = 0
        while (allowed < ABSOLUTE_MAX_SPACERS) {
            val trial = icons + List(allowed + 1) {
                TrayLayoutSlot.Spacer(id = "budget_$it")
            }
            if (wouldFold(trial, availableWidthDp)) break
            allowed++
        }
        return allowed
    }

    fun canAddSpacer(
        slots: List<TrayLayoutSlot>,
        flags: TrayIconFlags,
        availableWidthDp: Int = TraySpec.DEFAULT_CONTENT_WIDTH_DP,
        widthDp: Int = DEFAULT_SPACER_WIDTH_DP,
    ): Boolean {
        if (spacerCount(slots) >= ABSOLUTE_MAX_SPACERS) return false
        val withSpacer = slots + TrayLayoutSlot.Spacer(id = "probe", widthDp = widthDp)
        val visible = withSpacer.filter { slot ->
            when (slot) {
                is TrayLayoutSlot.Spacer -> true
                is TrayLayoutSlot.Icon -> slot.kind.isEnabled(flags)
            }
        }
        return !wouldFold(visible, availableWidthDp)
    }

    /**
     * Drops trailing excess spacers when icons are re-enabled and the folding budget shrinks.
     * Keeps earlier spacers (left-to-right) so notch placement stays; respects each spacer's
     * actual width (tiny vs default).
     */
    fun trimSpacersToMax(
        slots: List<TrayLayoutSlot>,
        flags: TrayIconFlags,
        availableWidthDp: Int = TraySpec.DEFAULT_CONTENT_WIDTH_DP,
    ): List<TrayLayoutSlot> {
        fun visibleOf(list: List<TrayLayoutSlot>): List<TrayLayoutSlot> =
            list.filter { slot ->
                when (slot) {
                    is TrayLayoutSlot.Spacer -> true
                    is TrayLayoutSlot.Icon -> slot.kind.isEnabled(flags)
                }
            }
        var result = slots
        while (
            spacerCount(result) > 0 &&
            wouldFold(visibleOf(result), availableWidthDp)
        ) {
            val dropAt = result.indexOfLast { it is TrayLayoutSlot.Spacer }
            if (dropAt < 0) break
            result = result.filterIndexed { index, _ -> index != dropAt }
        }
        return result
    }

    fun serialize(slots: List<TrayLayoutSlot>): String =
        slots.joinToString(",") { slot ->
            when (slot) {
                is TrayLayoutSlot.Icon -> slot.kind.storageKey
                is TrayLayoutSlot.Spacer -> "spacer:${slot.id}:${slot.widthDp}"
            }
        }

    /**
     * Parses a stored layout string. Unknown tokens are skipped; missing icons from the
     * default set are appended so upgrades never drop a new glyph slot.
     */
    fun parse(raw: String?): List<TrayLayoutSlot> {
        if (raw.isNullOrBlank()) return DEFAULT
        val parsed = mutableListOf<TrayLayoutSlot>()
        val seenIcons = mutableSetOf<TrayLayoutIcon>()
        for (token in raw.split(',')) {
            val part = token.trim()
            if (part.isEmpty()) continue
            if (part.startsWith("spacer:")) {
                val bits = part.split(':')
                if (bits.size < 2) continue
                val id = bits[1].ifBlank { newSpacerId() }
                val width = bits.getOrNull(2)?.toIntOrNull()?.coerceIn(16, 120)
                    ?: DEFAULT_SPACER_WIDTH_DP
                parsed += TrayLayoutSlot.Spacer(id = id, widthDp = width)
            } else {
                val icon = TrayLayoutIcon.fromStorage(part) ?: continue
                if (icon in seenIcons) continue
                seenIcons += icon
                parsed += TrayLayoutSlot.Icon(icon)
            }
        }
        for (icon in TrayLayoutIcon.entries) {
            if (icon !in seenIcons) {
                parsed += TrayLayoutSlot.Icon(icon)
            }
        }
        if (parsed.none { it is TrayLayoutSlot.Icon && it.kind == TrayLayoutIcon.Clock }) {
            parsed += TrayLayoutSlot.Icon(TrayLayoutIcon.Clock)
        }
        return parsed
    }

    /** Visible configure / tray slots; spacers trimmed to the folding budget. */
    fun visible(
        slots: List<TrayLayoutSlot>,
        flags: TrayIconFlags,
        availableWidthDp: Int = TraySpec.DEFAULT_CONTENT_WIDTH_DP,
    ): List<TrayLayoutSlot> =
        trimSpacersToMax(
            slots = slots.filter { slot ->
                when (slot) {
                    is TrayLayoutSlot.Spacer -> true
                    is TrayLayoutSlot.Icon -> slot.kind.isEnabled(flags)
                }
            },
            flags = flags,
            availableWidthDp = availableWidthDp,
        )

    /**
     * Slots that occupy space in the live / preview tray: every icons-tab–enabled icon plus
     * spacers (within the folding budget). Live telemetry does **not** drop a slot —
     * unavailable glyphs stay reserved and are drawn invisible via [isGlyphVisible] so the
     * justified layout stays stable.
     */
    fun liveOccupying(
        slots: List<TrayLayoutSlot>,
        flags: TrayIconFlags,
        availableWidthDp: Int = TraySpec.DEFAULT_CONTENT_WIDTH_DP,
    ): List<TrayLayoutSlot> = visible(slots, flags, availableWidthDp)

    /**
     * Whether the glyph for [kind] should paint. When false the slot still occupies layout
     * space (blank reservation) so disconnecting Wi-Fi / clearing mute does not reshuffle
     * neighbors.
     */
    fun isGlyphVisible(
        kind: TrayLayoutIcon,
        wifiConnected: Boolean,
        ringerMuted: Boolean,
        hotspotActive: Boolean,
        bluetoothAudio: BluetoothAudioKind?,
        notificationPackage: String?,
        batteryPresent: Boolean,
    ): Boolean = when (kind) {
        TrayLayoutIcon.Clock,
        TrayLayoutIcon.Network,
        -> true
        TrayLayoutIcon.Wifi -> wifiConnected
        TrayLayoutIcon.Mute -> ringerMuted
        TrayLayoutIcon.Notifications -> !notificationPackage.isNullOrBlank()
        TrayLayoutIcon.Hotspot -> hotspotActive
        TrayLayoutIcon.BluetoothAudio -> bluetoothAudio != null
        TrayLayoutIcon.Battery -> batteryPresent
    }

    /** Index of the rightmost icon slot in [slots], or -1 when none. */
    fun rightmostIconIndex(slots: List<TrayLayoutSlot>): Int =
        slots.indexOfLast { it is TrayLayoutSlot.Icon }

    /**
     * True for spacers that sit after the persistent rightmost icon — they stay reserved in
     * the collapsed tray (after auto-hide), outside the SpaceBetween row so the clock does
     * not pick up a free-justify gap that disappears on collapse.
     */
    fun isTrailingSpacer(slots: List<TrayLayoutSlot>, index: Int): Boolean {
        if (index !in slots.indices) return false
        if (slots[index] !is TrayLayoutSlot.Spacer) return false
        val rightmost = rightmostIconIndex(slots)
        return rightmost >= 0 && index > rightmost
    }

    /** Indices of slots that participate in SpaceBetween (everything up to and including the rightmost icon). */
    fun justifiedIndices(slots: List<TrayLayoutSlot>): List<Int> {
        val rightmost = rightmostIconIndex(slots)
        if (rightmost < 0) return emptyList()
        return (0..rightmost).toList()
    }

    /** Indices of spacers after the rightmost icon. */
    fun trailingSpacerIndices(slots: List<TrayLayoutSlot>): List<Int> {
        val rightmost = rightmostIconIndex(slots)
        if (rightmost < 0) return emptyList()
        return ((rightmost + 1) until slots.size).filter { slots[it] is TrayLayoutSlot.Spacer }
    }

    /**
     * Icons that stagger on expand/collapse — every occupying icon except the persistent
     * rightmost.
     */
    fun animatingIcons(slots: List<TrayLayoutSlot>): List<TrayLayoutIcon> {
        val icons = slots.mapNotNull { (it as? TrayLayoutSlot.Icon)?.kind }
        if (icons.isEmpty()) return emptyList()
        return icons.dropLast(1)
    }

    fun move(slots: List<TrayLayoutSlot>, fromIndex: Int, toIndex: Int): List<TrayLayoutSlot> {
        if (fromIndex == toIndex) return slots
        if (fromIndex !in slots.indices || toIndex !in slots.indices) return slots
        val mutable = slots.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable
    }

    fun addSpacer(
        slots: List<TrayLayoutSlot>,
        flags: TrayIconFlags,
        availableWidthDp: Int = TraySpec.DEFAULT_CONTENT_WIDTH_DP,
        widthDp: Int = DEFAULT_SPACER_WIDTH_DP,
    ): List<TrayLayoutSlot> {
        if (!canAddSpacer(slots, flags, availableWidthDp, widthDp)) {
            return slots
        }
        return slots + TrayLayoutSlot.Spacer(id = newSpacerId(), widthDp = widthDp)
    }

    fun removeSpacer(slots: List<TrayLayoutSlot>, spacerId: String): List<TrayLayoutSlot> =
        slots.filterNot { it is TrayLayoutSlot.Spacer && it.id == spacerId }

    fun newSpacerId(): String = UUID.randomUUID().toString().take(8)
}
