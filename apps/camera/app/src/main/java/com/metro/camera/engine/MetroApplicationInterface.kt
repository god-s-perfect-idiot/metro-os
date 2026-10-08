package com.metro.camera.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.metro.camera.MainActivity
import com.metro.camera.ui.CameraUiState
import net.sourceforge.opencamera.cameracontroller.RawImage
import net.sourceforge.opencamera.preview.ApplicationInterface
import net.sourceforge.opencamera.preview.BasicApplicationInterface
import net.sourceforge.opencamera.preview.Preview
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bridges Open Camera [Preview] to Metro Camera UI / MediaStore saves.
 */
class MetroApplicationInterface(
    private val activity: MainActivity,
    private val stateProvider: () -> CameraUiState,
    private val onState: (CameraUiState) -> Unit,
    private val onPermissionNeeded: () -> Unit,
) : BasicApplicationInterface() {

    private var preview: Preview? = null

    fun attachPreview(preview: Preview) {
        this.preview = preview
    }

    override fun getContext(): Context = activity

    override fun useCamera2(): Boolean = true

    /** Metro owns status feedback in Compose — suppress Open Camera's Android toasts. */
    override fun getShowToastsPref(): Boolean = false

    override fun getCameraIdPref(): Int = stateProvider().cameraId

    override fun getFlashPref(): String = stateProvider().flashValue

    override fun getISOPref(): String = stateProvider().iso

    override fun getWhiteBalancePref(): String = stateProvider().whiteBalance

    override fun isVideoPref(): Boolean = stateProvider().mode == CameraUiState.Mode.Video

    override fun getRepeatPref(): String {
        return if (stateProvider().mode == CameraUiState.Mode.Burst) "5" else "1"
    }

    override fun getRepeatIntervalPref(): Long {
        return if (stateProvider().mode == CameraUiState.Mode.Burst) 100L else 0L
    }

    override fun createOutputVideoMethod(): ApplicationInterface.VideoMethod =
        ApplicationInterface.VideoMethod.MEDIASTORE

    override fun createOutputVideoFile(extension: String): File {
        throw IOException("Metro Camera uses MediaStore for video")
    }

    override fun createOutputVideoSAF(extension: String): Uri {
        throw IOException("SAF video not used")
    }

    override fun createOutputVideoMediaStore(extension: String): Uri {
        val name = "VID_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) +
            if (extension.startsWith(".")) extension else ".$extension"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, mimeForVideo(extension))
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera")
            }
        }
        return activity.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Unable to create MediaStore video Uri")
    }

    override fun createOutputVideoUri(): Uri {
        throw IOException("URI video method not used")
    }

    override fun requestCameraPermission() {
        onPermissionNeeded()
    }

    override fun needsStoragePermission(): Boolean = Build.VERSION.SDK_INT <= 28

    override fun requestStoragePermission() {
        onPermissionNeeded()
    }

    override fun requestRecordAudioPermission() {
        onPermissionNeeded()
    }

    override fun onPictureTaken(data: ByteArray, current_date: Date): Boolean {
        val state = stateProvider()
        if (state.mode == CameraUiState.Mode.Burst) {
            val frames = state.burstFrames + data
            onState(
                state.copy(
                    burstFrames = frames,
                    screen = CameraUiState.Screen.BurstReview,
                    statusMessage = "burst ${frames.size}",
                ),
            )
            // Still save each frame for v1 simplicity; burst review can delete later.
        }
        val uri = saveJpeg(data, current_date)
        if (uri != null) {
            onState(stateProvider().copy(lastCaptureUri = uri.toString(), statusMessage = "saved"))
        }
        return true
    }

    override fun onBurstPictureTaken(images: List<ByteArray>, current_date: Date): Boolean {
        onState(
            stateProvider().copy(
                burstFrames = images.toList(),
                screen = CameraUiState.Screen.BurstReview,
                statusMessage = "burst ${images.size}",
            ),
        )
        images.forEach { saveJpeg(it, current_date) }
        return true
    }

    override fun onRawPictureTaken(raw_image: RawImage, current_date: Date): Boolean = false

    override fun onRawBurstPictureTaken(raw_images: List<RawImage>, current_date: Date): Boolean =
        false

    override fun cameraSetup() {
        val p = preview ?: return
        onState(
            stateProvider().copy(
                flashValue = p.currentFlashValue ?: stateProvider().flashValue,
                cameraId = p.cameraId,
            ),
        )
    }

    override fun startedVideo() {
        onState(stateProvider().copy(isRecording = true, statusMessage = "recording"))
    }

    override fun stoppedVideo(
        video_method: ApplicationInterface.VideoMethod,
        uri: Uri?,
        filename: String?,
    ) {
        onState(
            stateProvider().copy(
                isRecording = false,
                lastCaptureUri = uri?.toString() ?: stateProvider().lastCaptureUri,
                statusMessage = "video saved",
            ),
        )
    }

    override fun onPhotoError() {
        onState(stateProvider().copy(statusMessage = "couldn't capture photo"))
    }

    override fun onCameraError() {
        Log.e(TAG, "camera error")
        onState(stateProvider().copy(statusMessage = "camera error"))
    }

    override fun onFailedStartPreview() {
        onState(stateProvider().copy(statusMessage = "couldn't start preview"))
    }

    private fun saveJpeg(data: ByteArray, date: Date): Uri? {
        return try {
            val name = "IMG_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(date) + ".jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_TAKEN, date.time)
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = activity.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values,
            ) ?: return null
            activity.contentResolver.openOutputStream(uri)?.use { it.write(data) }
            if (Build.VERSION.SDK_INT >= 29) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                activity.contentResolver.update(uri, values, null, null)
            }
            uri
        } catch (e: Exception) {
            Log.e(TAG, "saveJpeg failed", e)
            null
        }
    }

    private fun mimeForVideo(extension: String): String {
        val ext = extension.removePrefix(".").lowercase(Locale.US)
        return when (ext) {
            "3gp" -> "video/3gpp"
            "webm" -> "video/webm"
            else -> "video/mp4"
        }
    }

    companion object {
        private const val TAG = "MetroCameraIface"
    }
}
