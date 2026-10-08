package com.metro.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.OrientationEventListener
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.metro.camera.engine.MetroApplicationInterface
import com.metro.camera.ui.CameraRoute
import com.metro.camera.ui.CameraUiState
import com.metro.ui.MetroActivities
import com.metro.ui.MetroSplash
import com.metro.ui.MetroSystemTheme
import net.sourceforge.opencamera.preview.Preview

class MainActivity : ComponentActivity() {
    private var uiState by mutableStateOf(CameraUiState())
    private var hasCameraPermission by mutableStateOf(false)
    /** Device tilt quantized to 0/90/180/270 — used only to spin glyphs, never to reflow chrome. */
    private var deviceOrientationDegrees by mutableIntStateOf(0)

    private var previewHost: FrameLayout? = null
    private var preview: Preview? = null
    private var appInterface: MetroApplicationInterface? = null
    private var orientationListener: OrientationEventListener? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            hasCameraPermission = grants[Manifest.permission.CAMERA] == true
            if (hasCameraPermission) {
                ensurePreview()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        orientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val next = when {
                    orientation >= 315 || orientation < 45 -> 0
                    orientation < 135 -> 90
                    orientation < 225 -> 180
                    else -> 270
                }
                if (next != deviceOrientationDegrees) {
                    deviceOrientationDegrees = next
                }
            }
        }

        setContent {
            MetroSystemTheme {
                CameraRoute(
                    state = uiState,
                    hasCameraPermission = hasCameraPermission,
                    glyphRotationDegrees = -deviceOrientationDegrees.toFloat(),
                    onRequestPermission = { requestCameraPermissions() },
                    onUpdate = { uiState = it },
                    onShutter = { takeShutterAction() },
                    onCycleFlash = { cycleFlash() },
                    onSwitchCamera = { switchCamera() },
                    onOpenGallery = { openGallery() },
                    previewContent = {
                        AndroidView(
                            factory = { context ->
                                FrameLayout(context).also { host ->
                                    previewHost = host
                                    if (hasCameraPermission) {
                                        ensurePreview()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = {
                                if (hasCameraPermission) {
                                    ensurePreview()
                                }
                            },
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (!hasCameraPermission) {
            requestCameraPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        orientationListener?.let { listener ->
            if (listener.canDetectOrientation()) listener.enable()
        }
        preview?.onResume()
    }

    override fun onPause() {
        orientationListener?.disable()
        preview?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        orientationListener?.disable()
        orientationListener = null
        preview?.onPause()
        preview = null
        appInterface = null
        previewHost = null
        super.onDestroy()
    }

    private fun requestCameraPermissions() {
        val needed = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT <= 28) {
            needed += Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(needed.toTypedArray())
    }

    private fun ensurePreview() {
        val host = previewHost ?: return
        if (preview != null) return
        val iface = MetroApplicationInterface(
            activity = this,
            stateProvider = { uiState },
            onState = { uiState = it },
            onPermissionNeeded = { requestCameraPermissions() },
        )
        appInterface = iface
        preview = Preview(iface, host).also {
            iface.attachPreview(it)
            it.onResume()
        }
    }

    private fun takeShutterAction() {
        val p = preview ?: return
        when (uiState.mode) {
            CameraUiState.Mode.Video -> {
                if (!p.isVideo) {
                    p.switchVideo(false, true)
                }
                if (p.isVideoRecording) {
                    p.stopVideo(false)
                } else {
                    p.takePicturePressed(false, false)
                }
            }
            CameraUiState.Mode.Burst -> {
                uiState = uiState.copy(statusMessage = "burst")
                p.takePicturePressed(false, false)
            }
            CameraUiState.Mode.Photo -> {
                if (p.isVideo) {
                    p.switchVideo(false, true)
                }
                p.takePicturePressed(false, false)
            }
        }
    }

    private fun cycleFlash() {
        val p = preview ?: return
        val values = p.supportedFlashValues ?: return
        if (values.isEmpty()) return
        val current = p.currentFlashValue
        val idx = values.indexOf(current).let { if (it < 0) 0 else (it + 1) % values.size }
        val next = values[idx]
        p.updateFlash(next)
        uiState = uiState.copy(
            flashValue = next,
            statusMessage = flashLabel(next),
        )
    }

    private fun switchCamera() {
        val p = preview ?: return
        val count = p.cameraControllerManager.numberOfCameras
        if (count <= 1) return
        val next = (p.cameraId + 1) % count
        p.setCamera(next)
        uiState = uiState.copy(
            cameraId = next,
            statusMessage = "camera $next",
        )
    }

    private fun openGallery() {
        val last = uiState.lastCaptureUri
        if (last != null) {
            val uri = Uri.parse(last)
            val view = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { startActivity(view) }.onSuccess { return }
        }
        val photos = packageManager.getLaunchIntentForPackage("com.metro.photos")
        if (photos != null) {
            startActivity(photos)
        } else {
            uiState = uiState.copy(statusMessage = "no photos")
        }
    }

    private fun flashLabel(value: String): String = when (value) {
        "flash_off" -> "flash off"
        "flash_auto" -> "flash auto"
        "flash_on" -> "flash on"
        "flash_torch" -> "torch"
        "flash_red_eye" -> "red eye"
        else -> value.replace('_', ' ')
    }
}
