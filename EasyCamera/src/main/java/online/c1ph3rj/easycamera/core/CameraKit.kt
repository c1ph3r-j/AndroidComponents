package online.c1ph3rj.easycamera.core

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import online.c1ph3rj.easycamera.support.PermissionHelper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Public façade around [CameraImpl]. Handles permission orchestration and exposes a
 * Java-friendly Builder API.
 */
class CameraKit private constructor(
    private val owner: ComponentActivity,
    private val previewView: PreviewView,
    private val storage: StorageStrategy,
    private val tapToFocus: Boolean,
    private val pinchToZoom: Boolean,
    private val lensFacing: Int,
    private val listener: CameraKitListener?
) {

    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val permissionHelper   = PermissionHelper(owner)
    private val scope              = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val impl = CameraImpl(
        owner,
        previewView,
        storage,
        tapToFocus,
        pinchToZoom,
        lensFacing,
        listener,
        executor
    )

    // ==== public lifecycle ====

    /** Boots the preview. Shows a single permission dialog (CAMERA + MIC). */
    fun start() {
        permissionHelper.ensurePermissions { granted ->
            if (granted) impl.startCamera() else emitError(ERROR_PERMISSION_DENIED)
        }
    }

    fun release()       = impl.release()
    fun captureImage()  = impl.captureImage()

    /**
     * Starts or stops video recording. If the microphone permission was denied at launch,
     * we re-prompt the user and only proceed when it is granted.
     */
    fun captureVideo()  {
        val micGranted = ContextCompat.checkSelfPermission(owner, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (micGranted) impl.startRecording() else {
            permissionHelper.ensurePermissions { ok ->
                if (ok) impl.startRecording() else emitError(ERROR_MIC_PERMISSION_DENIED)
            }
        }
    }

    fun pauseVideo()    = impl.pauseRecording()
    fun resumeVideo()   = impl.resumeRecording()
    fun stopVideo()     = impl.stopRecording()
    fun switchLens()    = impl.switchCamera()
    fun toggleFlash()   = impl.toggleFlash()

    // ==== internal helpers ====
    private fun emitError(code: Int) {
        listener?.onRecordingError(code)
    }

    // ==== Builder ====
    class Builder private constructor(private val owner: ComponentActivity, private val preview: PreviewView) {
        private var storage: StorageStrategy = MediaStoreStrategy()
        private var tap = true
        private var zoom = true
        private var lens = CameraSelector.LENS_FACING_BACK
        private var cb: CameraKitListener? = null

        fun storageStrategy(s: StorageStrategy) = apply { storage = s }
        fun enableTapToFocus(enable: Boolean)   = apply { tap   = enable }
        fun enablePinchToZoom(enable: Boolean)  = apply { zoom  = enable }
        fun defaultLens(lensFacing: Int)        = apply { lens  = lensFacing }
        fun listener(l: CameraKitListener?)     = apply { cb    = l }

        fun build(): CameraKit = CameraKit(owner, preview, storage, tap, zoom, lens, cb)

        companion object { @JvmStatic fun with(owner: ComponentActivity, preview: PreviewView) = Builder(owner, preview) }
    }

    companion object {
        const val ERROR_PERMISSION_DENIED       = -100
        const val ERROR_MIC_PERMISSION_DENIED   = -101
        const val ERROR_STORAGE_LIMIT           = -102

        const val LENS_BACK  = CameraSelector.LENS_FACING_BACK
        const val LENS_FRONT = CameraSelector.LENS_FACING_FRONT

        /** Map error codes to professional, user-readable messages. */
        fun describe(code: Int): String = when (code) {
            ERROR_PERMISSION_DENIED     -> "Camera permission denied. Please allow access to use the camera."
            ERROR_MIC_PERMISSION_DENIED -> "Microphone permission denied. Please allow access to record video with sound."
            ERROR_STORAGE_LIMIT         -> "Insufficient storage space to start recording."
            else                        -> "Unknown error ($code)"
        }
    }
}
