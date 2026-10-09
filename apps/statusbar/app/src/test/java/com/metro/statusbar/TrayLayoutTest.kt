package com.metro.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class TrayLayoutTest {
    @Test
    fun default_isWpIconOrderWithoutSpacers() {
        assertEquals(
            listOf(
                TrayLayoutIcon.Network,
                TrayLayoutIcon.Wifi,
                TrayLayoutIcon.Mute,
                TrayLayoutIcon.Notifications,
                TrayLayoutIcon.Hotspot,
                TrayLayoutIcon.BluetoothAudio,
                TrayLayoutIcon.Battery,
                TrayLayoutIcon.Clock,
            ),
            TrayLayout.DEFAULT.mapNotNull { (it as? TrayLayoutSlot.Icon)?.kind },
        )
        assertTrue(TrayLayout.DEFAULT.none { it is TrayLayoutSlot.Spacer })
    }

    @Test
    fun serializeAndParse_roundTripsIconsAndSpacers() {
        val slots = listOf(
            TrayLayoutSlot.Icon(TrayLayoutIcon.Network),
            TrayLayoutSlot.Spacer(id = "abc123", widthDp = 40),
            TrayLayoutSlot.Icon(TrayLayoutIcon.Wifi),
            TrayLayoutSlot.Icon(TrayLayoutIcon.Clock),
        )
        val parsed = TrayLayout.parse(TrayLayout.serialize(slots))
        assertEquals(TrayLayoutIcon.Network, (parsed[0] as TrayLayoutSlot.Icon).kind)
        val spacer = parsed[1] as TrayLayoutSlot.Spacer
        assertEquals("abc123", spacer.id)
        assertEquals(40, spacer.widthDp)
        assertEquals(TrayLayoutIcon.Wifi, (parsed[2] as TrayLayoutSlot.Icon).kind)
        // Missing default icons are appended before we lose Clock uniqueness.
        assertTrue(parsed.any { it is TrayLayoutSlot.Icon && it.kind == TrayLayoutIcon.Mute })
        assertTrue(parsed.any { it is TrayLayoutSlot.Icon && it.kind == TrayLayoutIcon.Clock })
    }

    @Test
    fun parse_nullOrBlank_returnsDefault() {
        assertEquals(TrayLayout.DEFAULT, TrayLayout.parse(null))
        assertEquals(TrayLayout.DEFAULT, TrayLayout.parse(""))
        assertEquals(TrayLayout.DEFAULT, TrayLayout.parse("   "))
    }

    @Test
    fun move_reordersSlots() {
        val slots = TrayLayout.DEFAULT
        val moved = TrayLayout.move(slots, fromIndex = 0, toIndex = 2)
        assertEquals(TrayLayoutIcon.Wifi, (moved[0] as TrayLayoutSlot.Icon).kind)
        assertEquals(TrayLayoutIcon.Mute, (moved[1] as TrayLayoutSlot.Icon).kind)
        assertEquals(TrayLayoutIcon.Network, (moved[2] as TrayLayoutSlot.Icon).kind)
    }

    @Test
    fun addAndRemoveSpacer() {
        val flags = TrayIconFlags()
        val withSpacer = TrayLayout.addSpacer(TrayLayout.DEFAULT, flags)
        assertEquals(TrayLayout.DEFAULT.size + 1, withSpacer.size)
        val spacer = withSpacer.last() as TrayLayoutSlot.Spacer
        assertEquals(TrayLayout.DEFAULT_SPACER_WIDTH_DP, spacer.widthDp)
        val removed = TrayLayout.removeSpacer(withSpacer, spacer.id)
        assertEquals(TrayLayout.DEFAULT.size, removed.size)
        assertTrue(removed.none { it is TrayLayoutSlot.Spacer })
    }

    @Test
    fun addTinySpacer_usesCompactWidth() {
        val flags = TrayIconFlags()
        val withTiny = TrayLayout.addSpacer(
            TrayLayout.DEFAULT,
            flags,
            widthDp = TrayLayout.TINY_SPACER_WIDTH_DP,
        )
        val spacer = withTiny.last() as TrayLayoutSlot.Spacer
        assertEquals(TrayLayout.TINY_SPACER_WIDTH_DP, spacer.widthDp)
    }

    @Test
    fun isTrailingSpacer_onlyAfterRightmostIcon() {
        val slots = listOf(
            TrayLayoutSlot.Spacer(id = "lead"),
            TrayLayoutSlot.Icon(TrayLayoutIcon.Network),
            TrayLayoutSlot.Icon(TrayLayoutIcon.Clock),
            TrayLayoutSlot.Spacer(id = "tail"),
        )
        assertFalse(TrayLayout.isTrailingSpacer(slots, 0))
        assertFalse(TrayLayout.isTrailingSpacer(slots, 1))
        assertFalse(TrayLayout.isTrailingSpacer(slots, 2))
        assertTrue(TrayLayout.isTrailingSpacer(slots, 3))
        assertEquals(listOf(0, 1, 2), TrayLayout.justifiedIndices(slots))
        assertEquals(listOf(3), TrayLayout.trailingSpacerIndices(slots))
    }

    @Test
    fun maxSpacers_growsWhenIconsDisabled() {
        val allOn = TrayIconFlags()
        val withAll = TrayLayout.maxSpacers(allOn)
        assertTrue(withAll >= 1)

        val oneSpacer = TrayLayout.addSpacer(TrayLayout.DEFAULT, allOn)
        assertEquals(1, TrayLayout.spacerCount(oneSpacer))

        val oneOff = allOn.copy(wifi = false)
        assertTrue(TrayLayout.maxSpacers(oneOff) >= TrayLayout.maxSpacers(allOn))
    }

    @Test
    fun canAddSpacer_onlyBlockedWhenNextWouldFold() {
        val flags = TrayIconFlags()
        assertFalse(
            TrayLayout.canAddSpacer(
                slots = TrayLayout.DEFAULT,
                flags = flags,
                availableWidthDp = 200,
            ),
        )
        var slots = TrayLayout.DEFAULT
        repeat(3) {
            assertTrue(
                TrayLayout.canAddSpacer(
                    slots = slots,
                    flags = flags,
                    availableWidthDp = 500,
                ),
            )
            slots = TrayLayout.addSpacer(
                slots = slots,
                flags = flags,
                availableWidthDp = 500,
            )
        }
        assertEquals(3, TrayLayout.spacerCount(slots))
    }

    @Test
    fun trimSpacersToMax_dropsExcessWhenIconsReenabled() {
        val fewIcons = TrayIconFlags(
            network = false,
            wifi = false,
            mute = false,
            notifications = true,
            hotspot = false,
            bluetoothAudio = false,
            battery = true,
        )
        var slots = TrayLayout.DEFAULT
        repeat(4) {
            slots = TrayLayout.addSpacer(
                slots = slots,
                flags = fewIcons,
                availableWidthDp = 500,
            )
        }
        assertEquals(4, TrayLayout.spacerCount(slots))
        val trimmed = TrayLayout.trimSpacersToMax(slots, TrayIconFlags())
        assertTrue(TrayLayout.spacerCount(trimmed) < 4)
        assertTrue(
            TrayLayout.spacerCount(trimmed) <= TrayLayout.maxSpacers(TrayIconFlags()),
        )
    }

    @Test
    fun wouldFold_trueWhenContentExceedsWidth() {
        val slots = TrayLayout.DEFAULT
        assertFalse(TrayLayout.wouldFold(slots, availableWidthDp = 500))
        assertTrue(TrayLayout.wouldFold(slots, availableWidthDp = 100))
    }

    @Test
    fun visible_filtersDisabledIconsKeepsSpacers() {
        val flags = TrayIconFlags(
            network = true,
            wifi = false,
            mute = true,
            notifications = false,
            hotspot = true,
            bluetoothAudio = false,
            battery = true,
        )
        val slots = TrayLayout.addSpacer(TrayLayout.DEFAULT, flags)
        val visible = TrayLayout.visible(slots, flags)
        val icons = visible.mapNotNull { (it as? TrayLayoutSlot.Icon)?.kind }
        assertEquals(
            listOf(
                TrayLayoutIcon.Network,
                TrayLayoutIcon.Mute,
                TrayLayoutIcon.Hotspot,
                TrayLayoutIcon.Battery,
                TrayLayoutIcon.Clock,
            ),
            icons,
        )
        assertTrue(visible.any { it is TrayLayoutSlot.Spacer })
    }

    @Test
    fun liveOccupying_keepsUnavailableIconsForReservation() {
        val flags = TrayIconFlags()
        val occupying = TrayLayout.liveOccupying(TrayLayout.DEFAULT, flags)
        assertEquals(
            TrayLayoutIcon.entries.toList(),
            occupying.mapNotNull { (it as? TrayLayoutSlot.Icon)?.kind },
        )
        assertTrue(
            !TrayLayout.isGlyphVisible(
                kind = TrayLayoutIcon.Wifi,
                wifiConnected = false,
                ringerMuted = false,
                hotspotActive = false,
                bluetoothAudio = null,
                notificationPackage = null,
                batteryPresent = true,
            ),
        )
        assertTrue(
            TrayLayout.isGlyphVisible(
                kind = TrayLayoutIcon.Wifi,
                wifiConnected = true,
                ringerMuted = false,
                hotspotActive = false,
                bluetoothAudio = null,
                notificationPackage = null,
                batteryPresent = true,
            ),
        )
    }

    @Test
    fun rightmostIconIndex_isLastIconIgnoringTrailingSpacers() {
        val slots = listOf(
            TrayLayoutSlot.Icon(TrayLayoutIcon.Network),
            TrayLayoutSlot.Icon(TrayLayoutIcon.Clock),
            TrayLayoutSlot.Spacer(id = "tail"),
        )
        assertEquals(1, TrayLayout.rightmostIconIndex(slots))
    }

    @Test
    fun animatingIcons_dropsPersistentRightmost() {
        val slots = listOf(
            TrayLayoutSlot.Icon(TrayLayoutIcon.Network),
            TrayLayoutSlot.Icon(TrayLayoutIcon.Wifi),
            TrayLayoutSlot.Icon(TrayLayoutIcon.Clock),
        )
        assertEquals(
            listOf(TrayLayoutIcon.Network, TrayLayoutIcon.Wifi),
            TrayLayout.animatingIcons(slots),
        )
        assertEquals(emptyList<TrayLayoutIcon>(), TrayLayout.animatingIcons(listOf(slots.last())))
    }
}
