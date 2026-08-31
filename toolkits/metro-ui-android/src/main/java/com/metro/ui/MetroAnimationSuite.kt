package com.metro.ui

/**
 * Named decorative / feedback animations shared across metro-os.
 *
 * Distinct from [MetroTransitions] (page and chrome motion constants). Apps should
 * import these composables from the toolkit rather than reimplementing the sequences.
 */
object MetroAnimationSuite {
    /**
     * Windows Hello–style face recognition success: smile arc flips and spins,
     * eyes split apart, wink, then greets by name.
     *
     * @see MetroBiometricAnimation
     */
    const val Biometric = "biometric"
}
