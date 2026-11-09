package com.metro.clock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AlarmLogicTest {
    @Test
    fun formatTime_midnightAndNoon() {
        assertEquals("12:00 AM", AlarmLogic.formatTime(0, 0))
        assertEquals("12:00 PM", AlarmLogic.formatTime(12, 0))
        assertEquals("8:05 AM", AlarmLogic.formatTime(8, 5))
        assertEquals("1:56 PM", AlarmLogic.formatTime(13, 56))
    }

    @Test
    fun formatRepeatSummary_variants() {
        assertEquals("only once", AlarmLogic.formatRepeatSummary(emptySet()))
        assertEquals(
            "Mon, Wed, Fri",
            AlarmLogic.formatRepeatSummary(
                setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY),
            ),
        )
        assertEquals(
            "weekdays",
            AlarmLogic.formatRepeatSummary(
                setOf(
                    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                    Calendar.THURSDAY, Calendar.FRIDAY,
                ),
            ),
        )
    }

    @Test
    fun hour24RoundTrip() {
        assertEquals(0, AlarmLogic.toHour24(12, isAm = true))
        assertEquals(12, AlarmLogic.toHour24(12, isAm = false))
        assertEquals(7, AlarmLogic.toHour24(7, isAm = true))
        assertEquals(19, AlarmLogic.toHour24(7, isAm = false))
        assertEquals(12 to true, AlarmLogic.fromHour24(0))
        assertEquals(7 to false, AlarmLogic.fromHour24(19))
    }

    @Test
    fun nextTrigger_disabledIsNull() {
        val alarm = AlarmItem(hour = 8, minute = 0, enabled = false)
        assertNull(AlarmLogic.nextTriggerMillis(alarm))
    }

    @Test
    fun nextTrigger_onceIsInFuture() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val alarm = AlarmItem(hour = 8, minute = 0, enabled = true)
        val next = AlarmLogic.nextTriggerMillis(alarm, now)!!
        assertTrue(next > now)
    }

    @Test
    fun statusLabel() {
        assertEquals("On: Wake", AlarmLogic.statusLabel(true, "Wake"))
        assertEquals("Off: Alarm", AlarmLogic.statusLabel(false, "Alarm"))
    }
}
