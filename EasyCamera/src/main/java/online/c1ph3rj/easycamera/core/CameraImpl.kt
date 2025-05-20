package online.c1ph3rj.easycamera.core

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.ScaleGestureDetector
import android.view.Surface
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * Internal implementation that houses the full CameraX plumbing. It mirrors jp's original
 * Java CameraService but removes hard‑coded paths and surfaces everything through [StorageStrategy]
 * and [CameraKitListener].
 */
internal class CameraImpl(
    private val activity: Activity,
    private val previewView: PreviewView,
    private val storage: StorageStrategy,
    private var tapToFocus: Boolean,
    private var pinchToZoom: Boolean,
    private var lensFacing: Int,
    private val listener: CameraKitListener?,
    private val executor: Executor
) {

    // ==================== CameraX state ====================
    private var camera: Camera? = null
    private var cameraInfo: CameraInfo? = null
    private var cameraControl: CameraControl? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    // ==================== Timer ====================
    private val handler = Handler(Looper.getMainLooper())
    private var isTimerRunning = false
    private var elapsed = 0L
    private val timerRunnable = object : Runnable {
        override fun run() {
            elapsed += INTERVAL
            listener?.onTimerUpdate(elapsed)
            handler.postDelayed(this, INTERVAL)
        }
    }

    // ==================== State flags ====================
    private var isPaused = false
    private var isTorchOn = false
    private var savedVideoAbsPath: String? = null

    companion object { private const val INTERVAL = 1_000L }

    // ---------------------------------------------------------------------
    // Public operations
    // ---------------------------------------------------------------------
    fun captureImage() {
        if (recording != null) {
            Toast.makeText(activity, "Video in progress", Toast.LENGTH_SHORT).show(); return
        }
        if (freeSpaceGB() < 0.5f) {
            Toast.makeText(activity, "Not enough storage", Toast.LENGTH_SHORT).show(); return
        }
        takePictureInternal()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        recordVideoInternal()
    }

    fun pauseRecording() {
        recording?.pause()
        listener?.onVideoStateChanged(true)
        pauseTimer()
    }

    fun resumeRecording() {
        recording?.resume()
        listener?.onVideoStateChanged(false)
        resumeTimer()
    }

    /** Explicitly stop an in-progress recording (and save the file). */
    fun stopRecording() {
        stopVideo()
    }

    fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        listener?.onLensSwitch(lensFacing)
        startCamera() // rebind
    }

    fun toggleFlash() {
        camera?.cameraControl?.enableTorch(!isTorchOn)
        isTorchOn = !isTorchOn
        listener?.onFlashStateChange(isTorchOn)
    }

    fun release() {
        ProcessCameraProvider.getInstance(activity).get().unbindAll()
        handler.removeCallbacksAndMessages(null)
    }

    // ---------------------------------------------------------------------
    // Camera start‑up
    // ---------------------------------------------------------------------
    fun startCamera() {
        val providerFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(activity)
        providerFuture.addListener({
            val provider = providerFuture.get(); provider.unbindAll()

            val preview = Preview.Builder().setResolutionSelector(
                ResolutionSelector.Builder().setAspectRatioStrategy(
                    AspectRatioStrategy(AspectRatio.RATIO_16_9, AspectRatioStrategy.FALLBACK_RULE_AUTO)
                ).build()
            ).build().apply { surfaceProvider = previewView.surfaceProvider }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                .build()

            val recorder = Recorder.Builder()
                .setExecutor(executor)
                .setQualitySelector(QualitySelector.from(
                    Quality.FHD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)
                )).build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            camera = provider.bindToLifecycle(
                activity as LifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture
            )
            cameraControl = camera?.cameraControl
            cameraInfo = camera?.cameraInfo

            setupGestures(); setupOrientationListener(); listener?.onCameraReady()
        }, ContextCompat.getMainExecutor(activity))
    }

    // ---------------------------------------------------------------------
    // Gestures
    // ---------------------------------------------------------------------
    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private var zoomRatio = cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!pinchToZoom) return false
                zoomRatio *= detector.scaleFactor
                val state = cameraInfo?.zoomState?.value
                zoomRatio = zoomRatio.coerceIn(state?.minZoomRatio ?: 1f, state?.maxZoomRatio ?: 1f)
                cameraControl?.setZoomRatio(zoomRatio); return true
            }
        }
        val scaleDetector = ScaleGestureDetector(activity, scaleListener)

        previewView.setOnTouchListener { _, e ->
            if (tapToFocus && e.action == MotionEvent.ACTION_UP) {
                val factory = previewView.meteringPointFactory
                val pt = factory.createPoint(e.x, e.y)
                val action = FocusMeteringAction.Builder(pt, FocusMeteringAction.FLAG_AF)
                    .setAutoCancelDuration(2, TimeUnit.SECONDS).build()
                cameraControl?.startFocusAndMetering(action)
            }
            scaleDetector.onTouchEvent(e); true
        }
    }

    // ---------------------------------------------------------------------
    // Orientation updates
    // ---------------------------------------------------------------------
    private fun setupOrientationListener() {
        object : OrientationEventListener(activity) {
            override fun onOrientationChanged(o: Int) {
                val rot = when (o) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                imageCapture?.targetRotation = rot; videoCapture?.targetRotation = rot
            }
        }.enable()
    }

    // ---------------------------------------------------------------------
    // Image capture
    // ---------------------------------------------------------------------
    private fun takePictureInternal() {
        val file = storage.createImageFile()
        val opts = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture?.takePicture(opts, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                listener?.onImageSaved(file.absolutePath)
            }
            override fun onError(exc: ImageCaptureException) {
                listener?.onImageError(exc)
            }
        })
    }

    // ---------------------------------------------------------------------
    // Video capture
    // ---------------------------------------------------------------------
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun recordVideoInternal() {
        recording?.let { it.stop(); recording = null; return } // toggle stop

        val (values, relPath) = storage.createVideoOptions()
        savedVideoAbsPath = Environment.getExternalStoragePublicDirectory(relPath).absolutePath

        val mediaOpts = MediaStoreOutputOptions.Builder(
            activity.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(values).build()

        recording = videoCapture?.output?.prepareRecording(activity, mediaOpts)?.withAudioEnabled()?.start(executor) {
            when (it) {
                is VideoRecordEvent.Start -> { listener?.onRecordingInit(); startTimer(); listener?.onRecordingStart() }
                is VideoRecordEvent.Finalize -> {
                    stopTimer()
                    if (it.hasError()) listener?.onRecordingError(it.error)
                    else listener?.onRecordingComplete(savedVideoAbsPath ?: "")
                }
            }
            if (recording != null && freeSpaceGB() < 1f) { Toast.makeText(activity, "Storage <1GB", Toast.LENGTH_SHORT).show(); stopVideo() }
        }
    }

    private fun stopVideo() { recording?.stop(); recording?.close(); recording = null }

    // ---------------------------------------------------------------------
    // Timer helpers
    // ---------------------------------------------------------------------
    private fun startTimer() { if (isTimerRunning) return; isTimerRunning = true; elapsed = 0; handler.postDelayed(timerRunnable, INTERVAL); listener?.onTimerStart() }
    private fun pauseTimer() { if (!isTimerRunning) return; isTimerRunning = false; handler.removeCallbacks(timerRunnable); listener?.onTimerPause() }
    private fun resumeTimer() { if (isTimerRunning) return; isTimerRunning = true; handler.postDelayed(timerRunnable, INTERVAL); listener?.onTimerResume() }
    private fun stopTimer() { isTimerRunning = false; handler.removeCallbacks(timerRunnable); listener?.onTimerStop(); listener?.onTimerDone() }

    // ---------------------------------------------------------------------
    // Storage util
    // ---------------------------------------------------------------------
    private fun freeSpaceGB(): Float {
        val bytes = Environment.getExternalStorageDirectory().freeSpace.toFloat()
        return String.format(Locale.US, "%.1f", bytes / (1024 * 1024 * 1024)).toFloat()
    }
}
