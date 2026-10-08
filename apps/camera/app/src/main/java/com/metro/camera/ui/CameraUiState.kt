package com.metro.camera.ui

/** WP8.1 Microsoft Camera chrome state. */
data class CameraUiState(
    val mode: Mode = Mode.Photo,
    val screen: Screen = Screen.Viewfinder,
    val flashValue: String = "flash_auto",
    val cameraId: Int = 0,
    val iso: String = "auto",
    val whiteBalance: String = "auto",
    val scene: String = "auto",
    val statusMessage: String? = null,
    val lastCaptureUri: String? = null,
    val isRecording: Boolean = false,
    val burstFrames: List<ByteArray> = emptyList(),
    val burstRetentionDays: Int = 7,
    val viewfinderSlots: List<QuickSetting> = QuickSetting.defaults,
    val photoAspect: String = "16:9",
    val videoQuality: String = "720p",
    val continuousFocusVideo: Boolean = true,
    val focusAssistLight: Boolean = false,
) {
    enum class Mode { Photo, Burst, Video }
    enum class Screen { Viewfinder, More, PhotoSettings, VideoSettings, BurstReview }

    enum class QuickSetting(val label: String) {
        Flash("Flash"),
        SwitchCamera("Camera"),
        Lenses("Lenses"),
        Iso("ISO"),
        WhiteBalance("White balance"),
        Exposure("Exposure"),
        Scene("Scene");

        companion object {
            /** Left column under gallery: flash, flip, lens, iso (WP8.1 Microsoft Camera). */
            val defaults: List<QuickSetting> = listOf(
                Flash,
                SwitchCamera,
                Lenses,
                Iso,
            )
        }
    }
}
