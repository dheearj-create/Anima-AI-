package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.Face
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CameraGazeData(
    val gazeX: Float = 0f, // -1f (user looking left of screen) to 1f (right of screen)
    val gazeY: Float = 0f, // -1f (user looking top of screen) to 1f (bottom)
    val isFaceDetected: Boolean = false,
    val faceConfidence: Float = 0f,
    val isTrackingActive: Boolean = false
)

class CameraGazeTracker(private val context: Context) {

    companion object {
        private const val TAG = "CameraGazeTracker"
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private val _gazeFlow = MutableStateFlow(CameraGazeData())
    val gazeFlow: StateFlow<CameraGazeData> = _gazeFlow.asStateFlow()

    private var activeArraySize: Rect? = null
    private var isFrontFacing = false

    // Exponential smoothing for steady gaze tracking
    private var smoothX = 0f
    private var smoothY = 0f

    val isCameraSupported: Boolean
        get() = findFrontCameraId() != null

    private fun findFrontCameraId(): String? {
        val manager = cameraManager ?: return null
        return try {
            manager.cameraIdList.firstOrNull { id ->
                val chars = manager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                facing == CameraCharacteristics.LENS_FACING_FRONT
            } ?: manager.cameraIdList.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (cameraDevice != null || captureSession != null) return

        val cameraId = findFrontCameraId() ?: return
        val manager = cameraManager ?: return

        try {
            val chars = manager.getCameraCharacteristics(cameraId)
            activeArraySize = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            isFrontFacing = chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT

            startBackgroundThread()

            imageReader = ImageReader.newInstance(320, 240, ImageFormat.YUV_420_888, 2).apply {
                setOnImageAvailableListener({ reader ->
                    // Drop frames to keep pipeline fast and battery efficient
                    reader.acquireLatestImage()?.close()
                }, backgroundHandler)
            }

            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCameraCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    stopTracking()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera open error: $error")
                    stopTracking()
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start camera gaze tracker", e)
            stopTracking()
        }
    }

    private fun createCameraCaptureSession() {
        val device = cameraDevice ?: return
        val reader = imageReader ?: return

        try {
            val surface = reader.surface
            val previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                // Enable hardware face and eye detection
                set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE)
            }

            device.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    try {
                        session.setRepeatingRequest(previewRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                result: TotalCaptureResult
                            ) {
                                processCaptureResult(result)
                            }
                        }, backgroundHandler)

                        _gazeFlow.value = _gazeFlow.value.copy(isTrackingActive = true)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to set repeating capture request", e)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Camera capture session configuration failed")
                    stopTracking()
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create camera capture session", e)
        }
    }

    private fun processCaptureResult(result: CaptureResult) {
        val faces: Array<Face>? = result.get(CaptureResult.STATISTICS_FACES)
        val sensor = activeArraySize

        if (faces != null && faces.isNotEmpty() && sensor != null && sensor.width() > 0 && sensor.height() > 0) {
            val primaryFace = faces[0]
            val bounds = primaryFace.bounds

            // Face center relative to sensor active array
            val faceCenterX = bounds.centerX().toFloat()
            val faceCenterY = bounds.centerY().toFloat()

            // Normalize to -1.0 .. 1.0
            val rawNormX = ((faceCenterX / sensor.width().toFloat()) * 2f - 1f)
            val rawNormY = ((faceCenterY / sensor.height().toFloat()) * 2f - 1f)

            // Front camera mirroring: invert X so if user moves head to their right, the eyes track accordingly
            val targetX = if (isFrontFacing) -rawNormX else rawNormX
            val targetY = rawNormY

            // Exponential low-pass filter (smoothes out sensor jitter while keeping responsive tracking)
            smoothX = smoothX * 0.72f + targetX * 0.28f
            smoothY = smoothY * 0.72f + targetY * 0.28f

            _gazeFlow.value = CameraGazeData(
                gazeX = smoothX.coerceIn(-1f, 1f),
                gazeY = smoothY.coerceIn(-1f, 1f),
                isFaceDetected = true,
                faceConfidence = primaryFace.score / 100f,
                isTrackingActive = true
            )
        } else {
            // No face currently visible in sensor view
            if (_gazeFlow.value.isFaceDetected) {
                _gazeFlow.value = _gazeFlow.value.copy(isFaceDetected = false)
            }
        }
    }

    fun stopTracking() {
        try {
            captureSession?.stopRepeating()
            captureSession?.close()
        } catch (_: Exception) {}
        captureSession = null

        try {
            cameraDevice?.close()
        } catch (_: Exception) {}
        cameraDevice = null

        try {
            imageReader?.close()
        } catch (_: Exception) {}
        imageReader = null

        stopBackgroundThread()
        _gazeFlow.value = _gazeFlow.value.copy(isTrackingActive = false, isFaceDetected = false)
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraGazeTrackerThread").apply {
                start()
                backgroundHandler = Handler(looper)
            }
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join(500)
        } catch (_: Exception) {}
        backgroundThread = null
        backgroundHandler = null
    }

    fun release() {
        stopTracking()
    }
}
