package com.metro.widgets.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * Camera LED torch for the 1×1 Torch catalog / Start tile.
 * [stop] unregisters callbacks but does not force the LED off (Start pins may still be lit).
 */
class TorchController(private val appContext: Context) {
    private val cameraManager =
        appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val torchCameraId: String? = findTorchCameraId(cameraManager)

    val isAvailable: Boolean get() = torchCameraId != null

    private var onChanged: ((Boolean) -> Unit)? = null
    private var started = false
    private var enabled = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId != torchCameraId) return
            this@TorchController.enabled = enabled
            onChanged?.invoke(enabled)
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId != torchCameraId) return
            this@TorchController.enabled = false
            onChanged?.invoke(false)
        }
    }

    fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    fun start(onChanged: (Boolean) -> Unit) {
        if (started) return
        started = true
        this.onChanged = onChanged
        cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
        onChanged(enabled)
    }

    fun stop() {
        if (!started) return
        started = false
        onChanged = null
        runCatching { cameraManager.unregisterTorchCallback(torchCallback) }
        // Do not force the LED off — Start-pinned torch tiles may still be on.
    }

    /** Last known torch mode from callbacks / [setEnabled]. */
    fun isEnabled(): Boolean = enabled

    fun toggle(): Boolean = setEnabled(!enabled)

    fun setEnabled(on: Boolean): Boolean {
        val id = torchCameraId ?: return false
        if (!hasCameraPermission()) return false
        return try {
            cameraManager.setTorchMode(id, on)
            enabled = on
            onChanged?.invoke(on)
            true
        } catch (_: Exception) {
            enabled = false
            onChanged?.invoke(false)
            false
        }
    }

    companion object {
        fun findTorchCameraId(cameraManager: CameraManager): String? {
            return cameraManager.cameraIdList.firstOrNull { id ->
                val chars = runCatching { cameraManager.getCameraCharacteristics(id) }.getOrNull()
                    ?: return@firstOrNull false
                val flash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                flash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull { id ->
                runCatching {
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }.getOrDefault(false)
            }
        }
    }
}
