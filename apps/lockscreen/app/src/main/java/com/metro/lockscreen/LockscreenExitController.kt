package com.metro.lockscreen

/**
 * Bridge so [LockscreenHostService] can ask the Compose lock surface to play the same
 * swipe-up exit used for a committed finger swipe (e.g. after biometric unlock).
 */
class LockscreenExitController {
    @Volatile
    private var playExit: (() -> Unit)? = null

    fun bind(playExit: () -> Unit) {
        this.playExit = playExit
    }

    fun unbind(playExit: () -> Unit) {
        if (this.playExit === playExit) {
            this.playExit = null
        }
    }

    /** @return true if a Compose exit animation was started. */
    fun requestExit(): Boolean {
        val action = playExit ?: return false
        action.invoke()
        return true
    }
}

/** Why the lock fill finished sliding off-screen. */
enum class LockscreenExitReason {
    /** User committed an upward swipe — host must open SystemUI lock input. */
    SwipeCommit,

    /** Fingerprint / face unlocked the keyguard — host only tears the fill down. */
    BiometricUnlock,
}
