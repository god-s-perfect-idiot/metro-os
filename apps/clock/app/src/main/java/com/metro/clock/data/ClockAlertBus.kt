package com.metro.clock.data

/**
 * Delivers alarm / timer alerts to a live [com.metro.clock.MainActivity] so the UI can show
 * an in-app message dialog instead of launching a full-screen ring activity.
 */
object ClockAlertBus {
    @Volatile
    private var listener: ((ClockAlert) -> Unit)? = null

    fun setListener(listener: ((ClockAlert) -> Unit)?) {
        this.listener = listener
    }

    /** @return true if an in-app listener handled the event. */
    fun offer(alert: ClockAlert): Boolean {
        val current = listener ?: return false
        current.invoke(alert)
        return true
    }
}

sealed class ClockAlert {
    data class Alarm(val item: AlarmItem) : ClockAlert()
    data object Timer : ClockAlert()
}
