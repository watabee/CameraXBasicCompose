package com.github.watabee.cameraxbasiccompose

import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.window.layout.WindowMetricsCalculator
import com.github.watabee.cameraxbasiccompose.utils.getActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun rememberCameraViewState(photoFileController: PhotoFileController): CameraViewState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    return remember(context, lifecycleOwner, photoFileController) {
        CameraViewState(context, lifecycleOwner, photoFileController, coroutineScope)
    }
}

@Stable
class CameraViewState(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val photoFileController: PhotoFileController,
    coroutineScope: CoroutineScope
) : RememberObserver {
    private var processCameraProvider: ProcessCameraProvider? by mutableStateOf(null)

    var lensFacing: Int? by mutableStateOf(null)
        private set

    private var imageCapture: ImageCapture? = null
        set(value) {
            field = value
            canTakePicture = value != null && cameraExecutor != null
        }
    private var cameraExecutor: ExecutorService? = null
        set(value) {
            field = value
            canTakePicture = value != null && imageCapture != null
        }

    private val hasBackCamera: Boolean by derivedStateOf {
        try {
            processCameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
        } catch (e: CameraInfoUnavailableException) {
            false
        }
    }
    private val hasFrontCamera: Boolean by derivedStateOf {
        try {
            processCameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
        } catch (e: CameraInfoUnavailableException) {
            false
        }
    }
    val canToggleLensFacing: Boolean by derivedStateOf {
        hasBackCamera && hasFrontCamera
    }
    var canTakePicture: Boolean by mutableStateOf(false)
        private set

    init {
        coroutineScope.launch {
            processCameraProvider = ProcessCameraProvider.getInstance(context).await()
            lensFacing = when {
                hasBackCamera -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }
        }
    }

    fun toggleLensFacing() {
        if (!canToggleLensFacing) {
            return
        }
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
    }

    suspend fun bindCameraUseCases(previewView: PreviewView) {
        Timber.w("*** bindCameraUseCases")
        val processCameraProvider: ProcessCameraProvider = snapshotFlow { processCameraProvider }.first { it != null }!!
        val lensFacing = lensFacing ?: throw IllegalStateException()

        val context = previewView.context
        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context.getActivity()!!).bounds
        val screenAspectRatio = aspectRatio(metrics.width(), metrics.height())
        val rotation = previewView.display.rotation

        val preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        processCameraProvider.unbindAll()
        try {
            val camera = processCameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            preview.setSurfaceProvider(previewView.surfaceProvider)
            observeCameraState(camera.cameraInfo, lifecycleOwner)
        } catch (e: Throwable) {
            Timber.e(e)
        }
    }

    fun setImageCaptureRotation(rotation: Int) {
        imageCapture?.targetRotation = rotation
    }

    fun takePicture() {
        val cameraExecutor = cameraExecutor ?: throw IllegalStateException()
        val photoFile = photoFileController.createPhotoFile()

        val metadata = ImageCapture.Metadata()
            .apply {
                isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
            }
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .setMetadata(metadata)
            .build()

        imageCapture?.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                Timber.w("Photo capture succeeded: $savedUri")
                photoFileController.onPhotoSaved(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                Timber.e(exception, "Photo capture failed: ${exception.message}")
            }
        })
    }

    override fun onRemembered() {
        if (cameraExecutor != null) {
            return
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onAbandoned() {
        shutdown()
    }

    override fun onForgotten() {
        shutdown()
    }

    private fun shutdown() {
        cameraExecutor?.shutdown()
        cameraExecutor = null
    }

    private fun observeCameraState(cameraInfo: CameraInfo, lifecycleOwner: LifecycleOwner) {
        cameraInfo.cameraState.observe(lifecycleOwner) { cameraState ->
            run {
                when (cameraState.type) {
                    CameraState.Type.PENDING_OPEN -> {
                        // Ask the user to close other camera apps
                        Timber.w("CameraState: Pending Open")
                    }
                    CameraState.Type.OPENING -> {
                        // Show the Camera UI
                        Timber.w("CameraState: Opening")
                    }
                    CameraState.Type.OPEN -> {
                        // Setup Camera resources and begin processing
                        Timber.w("CameraState: Open")
                    }
                    CameraState.Type.CLOSING -> {
                        // Close camera UI
                        Timber.w("CameraState: Closing")
                    }
                    CameraState.Type.CLOSED -> {
                        // Free camera resources
                        Timber.w("CameraState: Closed")
                    }
                }
            }

            cameraState.error?.let { error ->
                when (error.code) {
                    // Open errors
                    CameraState.ERROR_STREAM_CONFIG -> {
                        // Make sure to setup the use cases properly
                        Timber.e("Stream config error")
                    }
                    // Opening errors
                    CameraState.ERROR_CAMERA_IN_USE -> {
                        // Close the camera or ask user to close another camera app that's using the
                        // camera
                        Timber.e("Camera in use")
                    }
                    CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                        // Close another open camera in the app, or ask the user to close another
                        // camera app that's using the camera
                        Timber.e("Max cameras in use")
                    }
                    CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {
                        Timber.e("Other recoverable error")
                    }
                    // Closing errors
                    CameraState.ERROR_CAMERA_DISABLED -> {
                        // Ask the user to enable the device's cameras
                        Timber.e("Camera disabled")
                    }
                    CameraState.ERROR_CAMERA_FATAL_ERROR -> {
                        // Ask the user to reboot the device to restore camera function
                        Timber.e("Fatal error")
                    }
                    // Closed errors
                    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                        // Ask the user to disable the "Do Not Disturb" mode, then reopen the camera
                        Timber.e("Do not disturb mode enabled")
                    }
                }
            }
        }
    }

    companion object {
        private fun aspectRatio(width: Int, height: Int): Int {
            val previewRatio = max(width, height).toDouble() / min(width, height)
            if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
                return AspectRatio.RATIO_4_3
            }
            return AspectRatio.RATIO_16_9
        }

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}

@Composable
fun CameraView(
    modifier: Modifier = Modifier,
    cameraViewState: CameraViewState
) {
    val context: Context = LocalContext.current
    val previewView = remember(context) { PreviewView(context) }

    if (cameraViewState.lensFacing != null) {
        val configuration = LocalConfiguration.current

        LaunchedEffect(previewView, configuration, cameraViewState.lensFacing) {
            cameraViewState.bindCameraUseCases(previewView)
        }
    }

    DisplayRotationChangedEffect(cameraViewState::setImageCaptureRotation)

    AndroidView(
        modifier = modifier,
        factory = { previewView }
    )
}

@Composable
private fun DisplayRotationChangedEffect(onDisplayRotationChanged: (rotation: Int) -> Unit) {
    val context: Context = LocalContext.current
    val view = LocalView.current
    val initialDisplayId = remember { view.display.displayId }
    val currentOnDisplayRotationChanged by rememberUpdatedState(onDisplayRotationChanged)

    DisposableEffect(context, view) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == initialDisplayId) {
                    Timber.w("Rotation changed: ${view.display.rotation}")
                    currentOnDisplayRotationChanged(view.display.rotation)
                }
            }
        }
        displayManager.registerDisplayListener(displayListener, null)

        onDispose {
            Timber.w("onDispose: displayManager.unregisterDisplayListener")
            displayManager.unregisterDisplayListener(displayListener)
        }
    }
}
